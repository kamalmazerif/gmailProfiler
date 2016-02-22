package pack.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by User on 1/3/2016.
 */

// {"id":"INBOX","labelListVisibility":"labelShow","messageListVisibility":"hide",
// "messagesTotal":1976,"messagesUnread":1117,"name":"INBOX",
// "threadsTotal":1881,"threadsUnread":1086,"type":"system"}
@DatabaseTable(tableName = "gmailLabels")
public class GmailLabelUpdate {

    public static final String FIELD_LABEL_NAME = "labelName";
    public static final String FIELD_LAST_HISTORY_ID = "lastHistoryId";
    public static final String FIELD_UPDATE_TIME_MILLIS = "updateTimeMillis";
    public static final String FIELD_MESSAGES_TOTAL = "messagesTotal";
    public static final String FIELD_MESSAGES_UNREAD = "messagesUnread";
    public static final String FIELD_THREADS_TOTAL = "threadsTotal";
    public static final String FIELD_THREADS_UNREAD = "threadsUnread";


    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = FIELD_LABEL_NAME, canBeNull = false)
    private String labelName;

    @DatabaseField(columnName = FIELD_LAST_HISTORY_ID, canBeNull = false)
    private long lastHistoryId;

    @DatabaseField(columnName = FIELD_UPDATE_TIME_MILLIS, canBeNull = false)
    private long updateTimeMillis;

    @DatabaseField(columnName = FIELD_MESSAGES_TOTAL, canBeNull = false)
    private int messagesTotal;

    @DatabaseField(columnName = FIELD_MESSAGES_UNREAD, canBeNull = false)
    private int messagesUnread;

    @DatabaseField(columnName = FIELD_THREADS_TOTAL, canBeNull = false)
    private int threadsTotal;

    @DatabaseField(columnName = FIELD_THREADS_UNREAD, canBeNull = false)
    private int threadsUnread;



    public GmailLabelUpdate() {
        // all persisted classes must define a no-arg constructor with at least package visibility
    }

    @Override
    public int hashCode() {
        return (messagesTotal + "-" + messagesUnread + "-" + threadsTotal + "-" + threadsUnread).hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || other.getClass().equals(getClass())) {
            return false;
        }

        GmailLabelUpdate otherGmailLabelUpdate = (GmailLabelUpdate)other;

        return messagesTotal == otherGmailLabelUpdate.messagesTotal
                && messagesUnread == otherGmailLabelUpdate.messagesUnread
                && threadsTotal == otherGmailLabelUpdate.threadsTotal
                && threadsUnread == otherGmailLabelUpdate.threadsUnread
                && lastHistoryId == otherGmailLabelUpdate.lastHistoryId
                && updateTimeMillis == otherGmailLabelUpdate.updateTimeMillis
                && labelName.equals(otherGmailLabelUpdate.labelName);
    }

    ///////////////////


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public long getLastHistoryId() {
        return lastHistoryId;
    }

    public void setLastHistoryId(long lastHistoryId) {
        this.lastHistoryId = lastHistoryId;
    }

    public long getUpdateTimeMillis() {
        return updateTimeMillis;
    }

    public void setUpdateTimeMillis(long updateTimeMillis) {
        this.updateTimeMillis = updateTimeMillis;
    }

    public int getMessagesTotal() {
        return messagesTotal;
    }

    public void setMessagesTotal(int messagesTotal) {
        this.messagesTotal = messagesTotal;
    }

    public int getMessagesUnread() {
        return messagesUnread;
    }

    public void setMessagesUnread(int messagesUnread) {
        this.messagesUnread = messagesUnread;
    }

    public int getThreadsTotal() {
        return threadsTotal;
    }

    public void setThreadsTotal(int threadsTotal) {
        this.threadsTotal = threadsTotal;
    }

    public int getThreadsUnread() {
        return threadsUnread;
    }

    public void setThreadsUnread(int threadsUnread) {
        this.threadsUnread = threadsUnread;
    }
}
