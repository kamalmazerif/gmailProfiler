package pack.controller;

import com.google.api.services.gmail.model.Message;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.springframework.stereotype.Component;
import pack.data.GmailLabel;
import pack.data.GmailMessage;
import pack.service.GmailQuickstart;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by User on 1/3/2016.
 */
@Component
public class GmailController {

    private final static String DATABASE_URL_MEMORY = "jdbc:h2:mem:account";  // Check if table name must be here
    private final static String DATABASE_URL_DISK = "jdbc:h2:testdb";


    private Dao<GmailMessage, String> messageDao;
    private Dao<GmailLabel, String> labelDao;


    public void resyncInbox() throws Exception {
        long updateTime = System.currentTimeMillis();
        ConnectionSource connectionSource = null;


        // create our data-source for the database
        connectionSource = new JdbcConnectionSource(DATABASE_URL_DISK);
        labelDao = DaoManager.createDao(connectionSource, GmailLabel.class);
        messageDao = DaoManager.createDao(connectionSource, GmailMessage.class);

        //TableUtils.dropTable(connectionSource, GmailLabel.class, true);
        TableUtils.createTableIfNotExists(connectionSource, GmailLabel.class);


        // Probably *should* drop the GmailMessage table if we are re-syncing from start?
        // ... but not if we are updating i.e. most common senders
        //TableUtils.dropTable(connectionSource, GmailMessage.class, true);
        TableUtils.createTableIfNotExists(connectionSource, GmailMessage.class);

        // ---------- Update Label info
        com.google.api.services.gmail.model.Label targetLabel = GmailQuickstart.getLabelInfo("INBOX");
        GmailLabel gmailLabel = new GmailLabel();
        gmailLabel.setLabelName(targetLabel.getName());
        gmailLabel.setMessagesTotal(targetLabel.getMessagesTotal());
        gmailLabel.setMessagesUnread(targetLabel.getMessagesUnread());
        gmailLabel.setThreadsTotal(targetLabel.getThreadsTotal());
        gmailLabel.setThreadsUnread(targetLabel.getThreadsUnread());
        gmailLabel.setUpdateTimeMillis(updateTime);
        labelDao.create(gmailLabel);


        // ---------- Get all messages
        List<com.google.api.services.gmail.model.Message> inboxMessages = GmailQuickstart.scanAllMessagesWithLabel("INBOX");

        // ---------- Record historyId of latest message with label info for use in future updates
        final String latestMessageId = inboxMessages.get(0).getId();
        final Message latestMessage = GmailQuickstart.getMessageInfo(latestMessageId);
        final String historyIdBigIntegerToString = latestMessage.getHistoryId().toString();
        gmailLabel.setLastHistoryId(Long.valueOf(historyIdBigIntegerToString));
        labelDao.update(gmailLabel);
        System.out.println("Updated label info with last history Id: " + historyIdBigIntegerToString);


        // ---------- Process all messages that were fetched
        // we specify a SelectArg here to generate a ? in statement string below
        QueryBuilder<GmailMessage, String> qb = messageDao.queryBuilder();
        qb.where().eq(GmailMessage.FIELD_MESSAGE_ID, new SelectArg());

        int messagesAdded = 0;
        int messagesUpdated = 0;
        for (com.google.api.services.gmail.model.Message nextMessage : inboxMessages) {

            final GenericRawResults<GmailMessage> matchingMessagesResultsContainer = messageDao.queryRaw(qb.prepareStatementString(), messageDao.getRawRowMapper(), nextMessage.getId());
            final List<GmailMessage> matchingMessagesResults = matchingMessagesResultsContainer.getResults();
            int numberOfMatches = matchingMessagesResults.size();
            if (numberOfMatches > 1) {
                throw new Exception ("Unexpected number of matches on existing message ID " + nextMessage.getId());
            } else if (numberOfMatches == 1) {
                final GmailMessage firstResult = matchingMessagesResults.get(0);
                firstResult.setThreadId(nextMessage.getThreadId());
                messageDao.update(firstResult);
                messagesUpdated++;

            } else {
                GmailMessage gmailMessageToPersist = new GmailMessage(nextMessage.getId(), nextMessage.getThreadId());
                messageDao.create(gmailMessageToPersist);
                messagesAdded++;

            }
        }





////        // Will not work, need to do a get on this message ID in order to get the last history ID.
////        // Should probably do this after the rest of the operation is done
////        BigInteger lastHistoryId = inboxMessages.get(0).getHistoryId();
////        gmailLabel.setLastHistoryId(lastHistoryId);
////        labelDao.update(gmailLabel);

        System.out.println("Added " + messagesAdded + " messages, updated " + messagesUpdated);
        System.out.println("Messages total: " + messageDao.countOf());
    }
}
