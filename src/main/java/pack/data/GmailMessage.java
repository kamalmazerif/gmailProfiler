package pack.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by User on 1/3/2016.
 */

@DatabaseTable(tableName = Schema.TABLE_NAME_GMAIL_MESSAGES)
public class GmailMessage {

    public static final String FIELD_MESSAGE_ID = "messageId";
    public static final String FIELD_THREAD_ID = "threadId";
    public static final String FIELD_HEADER_FROM = "headerFrom";
    public static final String FIELD_INTERNAL_DATE = "internalDate";


    @DatabaseField(generatedId = true)
    private int id;

    // MessageId and ThreadId always provided in Summary view
    @DatabaseField(columnName = FIELD_MESSAGE_ID, canBeNull = false)
    private String messageId;

    // MessageId and ThreadId always provided in Summary view
    @DatabaseField(columnName = FIELD_THREAD_ID, canBeNull = false)
    private String threadId;

    @DatabaseField(columnName = FIELD_HEADER_FROM, canBeNull = true)
    private String headerFrom;

    @DatabaseField(columnName = FIELD_INTERNAL_DATE, canBeNull = true)
    private Long internalDate;

    GmailMessage() {
        // all persisted classes must define a no-arg constructor with at least package visibility
    }

    public GmailMessage(String messageId, String threadId) {
        this.messageId = messageId;
        this.threadId = threadId;
    }

    @Override
    public int hashCode() {
        return ("" + messageId
                + "-" + threadId
                + "-" + headerFrom
                + "-" + internalDate
        ).hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !other.getClass().equals(getClass())) {
            return false;
        }
        return messageId.equals(((GmailMessage) other).messageId)
                && threadId.equals(((GmailMessage) other).threadId)
                && headerFrom.equals(((GmailMessage) other).headerFrom)
                && internalDate.equals(((GmailMessage) other).internalDate);
    }

    //////////////////////////////////////////////////

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getHeaderFrom() {
        return headerFrom;
    }

    public void setHeaderFrom(String headerFrom) {
        this.headerFrom = headerFrom;
    }

    public void setInternalDate(Long internalDate) {
        this.internalDate = internalDate;
    }

    public Long getInternalDate() {
        return internalDate;
    }
}
