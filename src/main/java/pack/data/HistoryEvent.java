package pack.data;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 Created by malex on 4/29/2016.
 */

@DatabaseTable(tableName = "gmailHistoryEvents")
public class HistoryEvent {

 public static final String FIELD_HISTORY_ID = "historyId";
 public static final String FIELD_JSON = "json";

 @DatabaseField(generatedId = true)
 private int id;

 @DatabaseField(columnName = FIELD_HISTORY_ID, canBeNull = false)
 private long historyId;

 @DatabaseField(columnName = FIELD_JSON, dataType = DataType.LONG_STRING, canBeNull = false)
 private String json;


 @Override
 public int hashCode() {
  return (historyId + "-" + json).hashCode();
 }

 @Override
 public boolean equals(Object other) { // Do not compare database primary key
  if (other == null || !other.getClass().equals(getClass())) {
   return false;
  }

  HistoryEvent otherGmailLabelUpdate = (HistoryEvent)other;

  return historyId == otherGmailLabelUpdate.historyId
          && json.equals(otherGmailLabelUpdate.json);
 }

 //////////////////////////////////


 public int getId() {
  return id;
 }

 public void setId(int id) {
  this.id = id;
 }

 public long getHistoryId() {
  return historyId;
 }

 public void setHistoryId(long historyId) {
  this.historyId = historyId;
 }

 public String getJson() {
  return json;
 }

 public void setJson(String json) {
  this.json = json;
 }
}
