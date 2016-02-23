package pack.controller;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.springframework.stereotype.Component;
import pack.data.GmailLabelUpdate;
import pack.data.GmailMessage;
import pack.service.GmailApiService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by User on 1/3/2016.
 */
@Component
public class GmailController {

    private final static String DATABASE_URL_MEMORY = "jdbc:h2:mem:account";  // Check if table name must be here
    private final static String DATABASE_URL_DISK = "jdbc:h2:db/testdb";


    // Second generic parameter appears to be wrong, should match the type of ID field
    private Dao<GmailMessage, String> messageDao;
    private Dao<GmailLabelUpdate, String> labelDao;

    @PostConstruct
    private void databaseInit() throws SQLException { // Throwing on @PostConstruct method will cause application to exit

        try {
            ConnectionSource connectionSource = new JdbcConnectionSource(DATABASE_URL_DISK);
            //TableUtils.dropTable(connectionSource, GmailLabelUpdate.class, true);
            TableUtils.createTableIfNotExists(connectionSource, GmailLabelUpdate.class);


            // Probably *should* drop the GmailMessage table if we are re-syncing from start?
            // ... but not if we are updating i.e. most common senders
            //TableUtils.dropTable(connectionSource, GmailMessage.class, true);
            TableUtils.createTableIfNotExists(connectionSource, GmailMessage.class);


            labelDao = DaoManager.createDao(connectionSource, GmailLabelUpdate.class);
            messageDao = DaoManager.createDao(connectionSource, GmailMessage.class);

            // Usually should do a version check before upgrading database
            //messageDao.executeRaw("ALTER TABLE `gmailMessages` ADD COLUMN headerFrom VARCHAR(255);");
        } catch (SQLException e) {
            System.out.println("Problem initializing database");
            throw e;
        }
    }

    public List<GmailLabelUpdate> getLabelInfo() throws SQLException {
        List<GmailLabelUpdate> gmailLabelUpdates = labelDao.queryForAll();
        return gmailLabelUpdates;
    }

    public void resyncInbox() throws Exception {
        long updateTime = System.currentTimeMillis();

        // ---------- Update Label info
        com.google.api.services.gmail.model.Label targetLabel = GmailApiService.getLabelInfo("INBOX");
        GmailLabelUpdate gmailLabelUpdate = new GmailLabelUpdate();
        gmailLabelUpdate.setLabelName(targetLabel.getName());
        gmailLabelUpdate.setMessagesTotal(targetLabel.getMessagesTotal());
        gmailLabelUpdate.setMessagesUnread(targetLabel.getMessagesUnread());
        gmailLabelUpdate.setThreadsTotal(targetLabel.getThreadsTotal());
        gmailLabelUpdate.setThreadsUnread(targetLabel.getThreadsUnread());
        gmailLabelUpdate.setUpdateTimeMillis(updateTime);
        labelDao.create(gmailLabelUpdate);


        // ---------- Get all messages
        List<com.google.api.services.gmail.model.Message> inboxMessages = GmailApiService.scanAllMessagesWithLabel("INBOX");

        // ---------- Record historyId of latest message with label info for use in future updates
        final String latestMessageId = inboxMessages.get(0).getId();
        final Message latestMessage = GmailApiService.getMessageInfo(latestMessageId);
        final String historyIdBigIntegerToString = latestMessage.getHistoryId().toString();
        gmailLabelUpdate.setLastHistoryId(Long.valueOf(historyIdBigIntegerToString));
        labelDao.update(gmailLabelUpdate);
        System.out.println("Updated label info with last history Id: " + historyIdBigIntegerToString);


        // ---------- Process all messages that were fetched
        // we specify a SelectArg here to generate a ? in statement string below
        QueryBuilder<GmailMessage, String> qb = messageDao.queryBuilder();
        qb.where().eq(GmailMessage.FIELD_MESSAGE_ID, new SelectArg());

        int messagesAdded = 0;
        int messagesUpdated = 0;
        int messagesUnchanged = 0;
        for (com.google.api.services.gmail.model.Message nextMessage : inboxMessages) {
            final GenericRawResults<GmailMessage> matchingMessagesResultsContainer = messageDao.queryRaw(qb.prepareStatementString(), messageDao.getRawRowMapper(), nextMessage.getId());
            final List<GmailMessage> matchingMessagesResults = matchingMessagesResultsContainer.getResults();
            int numberOfMatches = matchingMessagesResults.size();

            final String newThreadId = nextMessage.getThreadId();

            if (numberOfMatches > 1) {
                throw new Exception ("Unexpected number of matches on existing message ID " + nextMessage.getId());


            } else {
                final Long newInternalDate = nextMessage.getInternalDate();

                if (numberOfMatches == 1) {
                    boolean updated = false;
                    final GmailMessage firstResult = matchingMessagesResults.get(0);
                    final String oldThreadId = firstResult.getThreadId();
                    final Long oldInternalDate = firstResult.getInternalDate();



                    if ( (oldThreadId == null && newThreadId != null) || !oldThreadId.equals(newThreadId)) {
                        System.out.println("ThreadId has changed for message: " + firstResult.getId() + " old value: ");
                        firstResult.setThreadId(newThreadId);
                        updated = true;
                    }


                    if ( (oldInternalDate == null && newInternalDate != null) || !oldInternalDate.equals(newInternalDate)) {
                        System.out.println("InternalDate has changed for message: " + firstResult.getId());
                        firstResult.setInternalDate(newInternalDate);
                        updated = true;
                    }

                    if (updated) {
                        messageDao.update(firstResult);
                        messagesUpdated++;
                    } else {
                        messagesUnchanged++;
                    }


                } else {
                    GmailMessage gmailMessageToPersist = new GmailMessage(nextMessage.getId(), newThreadId);
                    gmailMessageToPersist.setInternalDate(newInternalDate);
                    messageDao.create(gmailMessageToPersist);
                    messagesAdded++;
                }
            }
        }

        System.out.println("Added " + messagesAdded + " messages, updated " + messagesUpdated +" messages, " + messagesUnchanged + " messages unchanged");
        System.out.println("Messages total: " + messageDao.countOf());

        updateMessageDetails();
        sortMessageDetails();
    }

    private void getMessageById(String messageId) throws SQLException {
        GmailMessage gmailMessage = messageDao.queryForId(messageId);
    }

    private void sortMessageDetails() throws SQLException {
        final List<GmailMessage> gmailMessages = messageDao.queryForAll();
        HashMap<String, Integer> senderCountMap = new HashMap<>();

        for (GmailMessage message : gmailMessages) {
            final String key = message.getHeaderFrom();
            if (!senderCountMap.containsKey(key)) {
                senderCountMap.put(key, 1);
            } else {
                final Integer oldCount = senderCountMap.get(key);
                senderCountMap.put(key, oldCount+1);
            }
        }

        Stream<Map.Entry<String,Integer>> sorted = senderCountMap.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()));
        final Object[] objects = sorted.toArray();
        for (Object nextObject : objects) {
            System.out.println(nextObject);
        }
    }

    private void updateMessageDetails() throws SQLException, IOException {
        QueryBuilder<GmailMessage, String> qb = messageDao.queryBuilder();
        qb.where().isNull(GmailMessage.FIELD_HEADER_FROM);

        final GenericRawResults<GmailMessage> matchingMessagesResultsContainer = messageDao.queryRaw(qb.prepareStatementString(), messageDao.getRawRowMapper());
        final List<GmailMessage> matchingMessagesResults = matchingMessagesResultsContainer.getResults();

        System.out.println("Found " + matchingMessagesResults.size() + " messages without From header data");

        int messagesUpdated = 0;
        for (GmailMessage nextMessage : matchingMessagesResults) {
            final String nextMessageId = nextMessage.getMessageId();
            final Message latestMessage = GmailApiService.getMessageInfo(nextMessageId);

            String fromHeaderValue = null;
            final List<MessagePartHeader> messageHeaders = latestMessage.getPayload().getHeaders();
            for (MessagePartHeader header : messageHeaders) {
                if (header.getName().equals("From")) {
                    fromHeaderValue = header.getValue();
                }
            }

            if (fromHeaderValue == null) {
                System.out.println("Could not find 'From' header for messageId: " + nextMessageId);
            } else {
                nextMessage.setHeaderFrom(fromHeaderValue);
                messageDao.update(nextMessage);
            }

            messagesUpdated++;
        }

        System.out.println("Message Details were updated for " + messagesUpdated + " messages");
    }
}
