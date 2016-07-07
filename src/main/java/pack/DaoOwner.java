package pack;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.springframework.stereotype.Component;
import pack.data.GmailLabelUpdate;
import pack.data.GmailMessage;
import pack.data.HistoryEvent;
import pack.data.Schema;

import javax.annotation.PostConstruct;
import java.sql.SQLException;

/**
 Created by malex on 7/7/2016.
 */

@Component
public class DaoOwner {

 private final static String DATABASE_URL_MEMORY = "jdbc:h2:mem:account";  // Check if table name must be here
 private final static String DATABASE_URL_DISK = "jdbc:h2:db/testdb";

 private ConnectionSource connectionSource;

 // Second generic parameter appears to be wrong, should match the type of ID field
 private Dao<GmailMessage, String> messageDao;
 private Dao<GmailLabelUpdate, String> labelDao;
 private Dao<Schema, String> schemaDao;
 private Dao<HistoryEvent, String> historyDao;

 @PostConstruct
 public void init() throws SQLException { // Throwing on @PostConstruct method will cause application to exit
  System.out.println("DaoOwner postconstruct");

  connectionSource = new JdbcConnectionSource(DATABASE_URL_DISK);
  schemaDao = DaoManager.createDao(connectionSource, Schema.class);
  labelDao = DaoManager.createDao(connectionSource, GmailLabelUpdate.class);
  messageDao = DaoManager.createDao(connectionSource, GmailMessage.class);
  historyDao = DaoManager.createDao(connectionSource, HistoryEvent.class);
 }

 public ConnectionSource getConnectionSource() {
  return connectionSource;
 }

 public Dao<GmailMessage, String> getMessageDao() {
  return messageDao;
 }

 public Dao<GmailLabelUpdate, String> getLabelDao() {
  return labelDao;
 }

 public Dao<Schema, String> getSchemaDao() {
  return schemaDao;
 }

 public Dao<HistoryEvent, String> getHistoryDao() {
  return historyDao;
 }
}
