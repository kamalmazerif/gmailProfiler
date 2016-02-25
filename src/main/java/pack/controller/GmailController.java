package pack.controller;

import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryLabelAdded;
import com.google.api.services.gmail.model.HistoryLabelRemoved;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.HistoryMessageDeleted;
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
import pack.Launch;
import pack.data.GmailLabelUpdate;
import pack.data.GmailMessage;
import pack.data.Schema;
import pack.service.GmailApiService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
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
    private Dao<Schema, String> schemaDao;

    @PostConstruct
    private void databaseInit() throws Exception { // Throwing on @PostConstruct method will cause application to exit
        final String appName = Launch.SCHEMA_APP_NAME;

        try {
            ConnectionSource connectionSource = new JdbcConnectionSource(DATABASE_URL_DISK);

            TableUtils.createTableIfNotExists(connectionSource, Schema.class);

            //TableUtils.dropTable(connectionSource, GmailLabelUpdate.class, true);
            //TableUtils.dropTable(connectionSource, GmailMessage.class, true);


            schemaDao = DaoManager.createDao(connectionSource, Schema.class);
            labelDao = DaoManager.createDao(connectionSource, GmailLabelUpdate.class);
            messageDao = DaoManager.createDao(connectionSource, GmailMessage.class);


            HashMap<String, Object> queryMap = new HashMap<>();
            queryMap.put(Schema.FIELD_SCHEMA_NAME, appName);
            final List<Schema> schemas = schemaDao.queryForFieldValues(queryMap);


            Schema schemaObject;
            if (schemas.size() == 0) {
                TableUtils.createTableIfNotExists(connectionSource, GmailLabelUpdate.class);
                TableUtils.createTableIfNotExists(connectionSource, GmailMessage.class);
                Schema newSchema = new Schema(appName);
                newSchema.setSchemaVersion(1L);
                schemaObject = newSchema;
                schemaDao.create(newSchema);

            } else if (schemas.size() == 1) {
                schemaObject = schemas.get(0);

            } else {
                throw new Exception("Unexpected number of matching schema versions: " + schemas.size());
            }

            System.out.println("Schema version for " + appName + " : " + schemaObject.getSchemaVersion());

            if (schemaObject.getSchemaVersion() == 1) {
                // Usually should do a version check before upgrading database
                messageDao.executeRaw("ALTER TABLE `" + Schema.TABLE_NAME_GMAIL_MESSAGES + "` ADD COLUMN internalDate BIGINT;");
                schemaObject.setSchemaVersion(2L);
                schemaDao.update(schemaObject);
                System.out.println("Upgraded schema for " + appName + " to verison " + schemaObject.getSchemaVersion());

            } else if (schemaObject.getSchemaVersion() == 2) {
                // Expected Schema version
                // Upgrade code would go here
            } else {
                throw new Exception("Unknown schema version: " + schemaObject.getSchemaVersion());
            }

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
        final Message latestMessage = GmailApiService.getMessageByIdFromApi(latestMessageId);
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


                    if (
                            (oldThreadId != null || newThreadId != null)
                                    && !oldThreadId.equals(newThreadId)
                            ) {
                        System.out.println("ThreadId has changed for message: " + firstResult.getId() + " old value: ");
                        firstResult.setThreadId(newThreadId);
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

        updateMessageDetails(); // Collects axtra info on messages using api
        sortMessageDetails(); // Prepares and displays statistics
    }

    private GmailMessage getMessageByIdFromDatabase(String messageId) throws Exception {
        GmailMessage gmailMessage = null;

        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put(GmailMessage.FIELD_MESSAGE_ID, messageId);
        final List<GmailMessage> messages = messageDao.queryForFieldValues(queryMap);

        GmailMessage returnMessage = null;
        if (messages.size() > 1) {
            throw new Exception("Expected only one message in database with messageId: " + messageId);
        } else if (messages.size() == 1) {
            returnMessage = messages.get(0);
        }

        return returnMessage;
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

    // Gets extra information from message Id (i.e. header information)
    private void updateMessageDetails() throws SQLException, IOException {
        QueryBuilder<GmailMessage, String> qb = messageDao.queryBuilder();
        qb.where().isNull(GmailMessage.FIELD_HEADER_FROM)
                .or().isNull(GmailMessage.FIELD_INTERNAL_DATE);

        final GenericRawResults<GmailMessage> matchingMessagesResultsContainer = messageDao.queryRaw(qb.prepareStatementString(), messageDao.getRawRowMapper());
        final List<GmailMessage> matchingMessagesResults = matchingMessagesResultsContainer.getResults();

        System.out.println("Found " + matchingMessagesResults.size() + " messages without From header data");

        int messagesUpdated = 0;
        for (GmailMessage nextMessageToPopulate : matchingMessagesResults) {
            final String nextMessageId = nextMessageToPopulate.getMessageId();
            final Message latestPopulatedMessageFromApi = GmailApiService.getMessageByIdFromApi(nextMessageId);

            // Set InternalDate on DB Entity
            final Long internalDateFromApi = latestPopulatedMessageFromApi.getInternalDate();
            if (internalDateFromApi != null) {
                nextMessageToPopulate.setInternalDate(internalDateFromApi);
            } else {
                System.out.println("Message from API has no InternalDate value");
            }


            // Set "From" header value on DB Entity
            String fromHeaderValue = null;
            final List<MessagePartHeader> messageHeaders = latestPopulatedMessageFromApi.getPayload().getHeaders();
            for (MessagePartHeader header : messageHeaders) {
                if (header.getName().equals("From")) {
                    fromHeaderValue = header.getValue();
                }
            }

            if (fromHeaderValue == null) {
                System.out.println("Could not find 'From' header for messageId: " + nextMessageId);
            } else {
                nextMessageToPopulate.setHeaderFrom(fromHeaderValue);
            }

            messageDao.update(nextMessageToPopulate); // Assumes there was some change...
            messagesUpdated++;
        }

        System.out.println("Message Details were updated for " + messagesUpdated + " messages");
    }

    public void testUpdateMessageHistory() throws Exception {
        updateMessageHistory("4755298");
    }


    public void updateMessageHistory(String historyId) throws Exception {
        final List<History> messageHistory = GmailApiService.getMessageHistoryFrom(historyId);
        extractEventsFromHistory(messageHistory);
    }

    private void extractEventsFromHistory(List<History> histories) throws Exception {
        for (History history : histories) {
            List<HistoryLabelAdded> labelsAdded = history.getLabelsAdded();
            List<HistoryLabelRemoved> labelsRemoved = history.getLabelsRemoved();
            List<HistoryMessageAdded> messagesAdded = history.getMessagesAdded();
            List<HistoryMessageDeleted> messagesDeleted = history.getMessagesDeleted();
            List<Message> messages = history.getMessages(); // Not clear this will be used for anything, per docs
            BigInteger historyId = history.getId();

            System.out.println("Checking historyId: " + historyId);

            if (labelsAdded != null && labelsAdded.size() > 0) {
                System.out.println("Labels Added: " + labelsAdded.size());
            }

            if (labelsRemoved != null && labelsRemoved.size() > 0) {
                System.out.println("Labels Removed: " + labelsRemoved.size());
                for (HistoryLabelRemoved labelRemoved : labelsRemoved) {
                    List<String> labelIds = labelRemoved.getLabelIds();
                    Message message = labelRemoved.getMessage();

                    if (labelIds.contains("INBOX")) {
                        System.out.println("Message was removed from inbox");
                    }

                    if (labelIds.contains("UNREAD")) {
                        // how to tell if it was read while in the inbox?
                        System.out.println("Message was read");
                    }

                    labelIds.remove("INBOX");
                    labelIds.remove("UNREAD");
                    if (labelIds.size() > 0) {
                        System.out.println("Unknown label(s) removed from message: " + labelIds);
                    }
                }
            }

            if (messagesAdded != null && messagesAdded.size() > 0) {
                System.out.println("Messages Added: " + messagesAdded.size());


                for (HistoryMessageAdded messageAdded : messagesAdded) {
                    List<String> labelIds = messageAdded.getMessage().getLabelIds();

                    if (labelIds.contains("INBOX") && labelIds.contains("UNREAD")) {
                        final String messageId = messageAdded.getMessage().getId();
                        final GmailMessage messageFromDatabase = getMessageByIdFromDatabase(messageId);

                        if (messageFromDatabase == null) {
                            System.out.println("Message referenced in history does not exist in database");
                            // Should probably fetch the info on it here, and add it to database...!
                            continue;
                        }

                        final Long internalDate = messageFromDatabase.getInternalDate();
                        if (internalDate == null) {
                            System.out.println("Message in database has null internalDate");
                        } else {
                            final Date convertedDate = new Date(internalDate);
                            System.out.println("Internal Date of new inbox message: " + convertedDate);
                        }



                    }

                    if (labelIds.contains("INBOX")) {
                        System.out.println("Message added to inbox");
                    }

                    if (labelIds.contains("UNREAD")) {
                        System.out.println("Message newly unread (might be newly received)");
                    }

                    // Do we care about any of these?
                    for (String remainingLabel : labelIds) {
                        if (remainingLabel.equals("INBOX") || remainingLabel.equals("UNREAD")
                                || remainingLabel.equals("CHAT") || remainingLabel.equals("SPAM")
                                || remainingLabel.equals("CATEGORY_PROMOTIONS") || remainingLabel.equals("CATEGORY_UPDATES")
                                || remainingLabel.equals("CATEGORY_SOCIAL") || remainingLabel.equals("CATEGORY_PERSONAL")
                                || remainingLabel.equals("CATEGORY_FORUMS")

                                        || remainingLabel.startsWith("Label_")
                                ) {
                            continue;
                        }

                        System.out.println("Message added with unknown labelIds: " + labelIds);
                        break;
                    }
                }
            }

            if (messagesDeleted != null && messagesDeleted.size() > 0) {
                System.out.println("Messages Deleted: (" + messagesDeleted.size() + ")");
                // Unless it was directly deleted from inbox, not sure if we care

                for (HistoryMessageDeleted messageDeleted : messagesDeleted) {
                    List<String> labelIds = messageDeleted.getMessage().getLabelIds();

                    if (labelIds.contains("INBOX")) {
                        System.out.println("Message deleted from INBOX");
                    }


                    for (String remainingLabel : labelIds) {
                        if (remainingLabel.equals("INBOX") || remainingLabel.equals("UNREAD") ||
                                remainingLabel.equals("CATEGORY_PROMOTIONS") || remainingLabel.equals("CATEGORY_UPDATES") ||
                                remainingLabel.equals("CATEGORY_SOCIAL") || remainingLabel.equals("CATEGORY_PERSONAL") ||
                                remainingLabel.equals("CHAT") || remainingLabel.equals("SPAM")
                                || remainingLabel.startsWith("Label_")
                                ) {
                            continue;
                        }

                        System.out.println("Message deleted with unknown labelIds: " + labelIds);
                        break;
                    }

                }
            }


        }

//        for (History history : histories) {
//            System.out.println(" --- Next history: ");
//            System.out.println(history.toPrettyString());
//        }


    }
}
