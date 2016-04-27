package pack.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.Gmail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by User on 12/27/2015.
 */
public class GmailApiService {

    final static String projectId = "gmail-test-2015-12";



        /** Application name. */
        private static final String APPLICATION_NAME =
                "Gmail API Java Quickstart";

        /** Directory to store user credentials for this application. */
        private static final java.io.File DATA_STORE_DIR = new java.io.File(
                System.getProperty("user.home"), ".credentials/gmail-java-quickstart");

        /** Global instance of the {@link FileDataStoreFactory}. */
        private static FileDataStoreFactory DATA_STORE_FACTORY;

        /** Global instance of the JSON factory. */
        private static final JsonFactory JSON_FACTORY =
                JacksonFactory.getDefaultInstance();

        /** Global instance of the HTTP transport. */
        private static HttpTransport HTTP_TRANSPORT;

        /** Global instance of the scopes required by this quickstart. */
        private static final List<String> SCOPES =
                Arrays.asList(GmailScopes.MAIL_GOOGLE_COM,
                        GmailScopes.GMAIL_LABELS);

        static {
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        }

        /**
         * Creates an authorized Credential object.
         * @return an authorized Credential object.
         * @throws IOException
         */
        public static Credential authorize() throws IOException {
            // Load client secrets.
            InputStream in =
                    GmailApiService.class.getResourceAsStream("/client_secret.json");
            GoogleClientSecrets clientSecrets =
                    GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow =
                    new GoogleAuthorizationCodeFlow.Builder(
                            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                            .setDataStoreFactory(DATA_STORE_FACTORY)
                            .setAccessType("offline")
                            .build();
            Credential credential = new AuthorizationCodeInstalledApp(
                    flow, new LocalServerReceiver()).authorize("user");
            System.out.println(
                    "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
            return credential;
        }

        /**
         * Build and return an authorized Gmail client service.
         * @return an authorized Gmail client service
         * @throws IOException
         */
        public static Gmail getGmailService() throws IOException {
            Credential credential = authorize();
            return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }

        public static void listLabels() throws IOException {
            // Build a new authorized API client service.
            Gmail service = getGmailService();

            // Print the labels in the user's account.
            String userId = "me";
            ListLabelsResponse listResponse =
                    service.users().labels().list(userId).execute();
            List<Label> labels = listResponse.getLabels();

            if (labels.size() == 0) {
                System.out.println("No labels found.");
            } else {
                System.out.println("Labels:");
                for (Label label : labels) {
                    System.out.printf("- %s\n", label.getName());
                }
            }
        }

    public static void setWatch() throws IOException {
        // Build a new authorized API client service.
        Gmail service = getGmailService();

        // Print the labels in the user's account.
        String userId = "me";
        WatchRequest watchRequest = new WatchRequest();
        watchRequest.setTopicName("projects/" + projectId + "/topics/mytopic");
        // watchRequest.setLabelIds(); // Should set this to inbox...
        final Gmail.Users.Watch watch = service.users().watch(userId, watchRequest);
        final WatchResponse watchResponse = watch.execute();

        System.out.println("Watch response expiration:" + watchResponse.getExpiration());

    }


    public static Message getMessageByIdFromApi(String messageId) throws IOException {
        // Build a new authorized API client service.
        Gmail service = getGmailService();

        final Message message = service.users().messages().get("me", messageId).execute();
        final String snippet = message.getSnippet();
        System.out.println("Got details for messageId:" + messageId + " snippet: " + snippet);
        return message;
    }

    public static void getHistory(String historyId) throws IOException {
        Gmail service = getGmailService();
        ListHistoryResponse response = service.users().history().list("me").setStartHistoryId(new BigInteger(historyId)).execute();
        System.out.println(response);
    }

    public static Label getLabelInfo(String labelId) throws IOException {
        // Build a new authorized API client service.
        Gmail service = getGmailService();

        Label label = service.users().labels().get("me", labelId).execute();
        System.out.println("Got label:" );
        System.out.println(label);
        return label;
    }

//    public static void scanAllMessagesWithLabel(String labelId) throws IOException, InterruptedException {
//        // Build a new authorized API client service.
//        Gmail service = getGmailService();
//
//        final String userId = "me";
//        ListMessagesResponse response = service.users().messages().list(userId)
//                .setLabelIds(Arrays.asList(labelId)).execute();
//
//        List<Message> messagesOverall = new ArrayList<Message>();
//
//
//
//        HashMap<String, Message> messageMap = new HashMap<>();
//
//
//
//
//        while (messagesOnPage != null) {
//            List<Message> messagesOnPage = response.getMessages();
//            for (Message nextMessage : messagesOnPage) {
//                if (messageMap.containsKey(nextMessage.getId())) {
//                    System.out.println("Duplicate message id: " + nextMessage.getId());
//                } else {
//                    messageMap.put(nextMessage.getId(), nextMessage);
//                }
//            }
//
//            messagesOverall.addAll(messagesOnPage);
//            System.out.println("Added " + messagesOnPage.size() + " messages, " + messagesOverall.size() + " messages total, Est. results size: " + response.getResultSizeEstimate());
//            if (response.getNextPageToken() != null) {
//                String pageToken = response.getNextPageToken();
//                response = service.users().messages().list(userId).setLabelIds(Arrays.asList(labelId))
//                        .setPageToken(pageToken).execute();
//            } else {
//                break;
//            }
//        }
//
//        System.out.println("Message map size: " + messageMap.size());
//
//        for (Message message : messagesOverall) {
//            System.out.println(message.toPrettyString());
//            Thread.currentThread().sleep(1000);
//        }
//    }

    public static List<Message> scanAllMessagesWithLabel(String labelId) throws IOException, InterruptedException {
        // Build a new authorized API client service.
        Gmail service = getGmailService();

        final String userId = "me";
        ListMessagesResponse response = service.users().messages().list(userId)
                .setLabelIds(Arrays.asList(labelId)).execute();

        List<Message> messagesOverall = new ArrayList<Message>();
        HashMap<String, Message> messageMap = new HashMap<>();




        while (response.getMessages() != null) {

            for (Message nextMessage : response.getMessages()) {
                if (nextMessage.getHistoryId() != null) {
                    System.out.println("Found history ID: " + nextMessage.getHistoryId());
                }
                if (messageMap.containsKey(nextMessage.getId())) {
                    System.out.println("Duplicate message id: " + nextMessage.getId());
                } else {
                    messageMap.put(nextMessage.getId(), nextMessage);
                }
            }

            messagesOverall.addAll(response.getMessages());
            System.out.println("Added " + response.getMessages().size() + " messages, " + messagesOverall.size() + " messages total, Est. results size: " + response.getResultSizeEstimate());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().messages().list(userId).setLabelIds(Arrays.asList(labelId))
                        .setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        System.out.println("Message map size: " + messageMap.size());

        return messagesOverall;
    }

    // Should be separated into fetching the history ID (this class)
    // And doing something with it (controller class)
    public static List<History> getMessageHistoryFrom(String startHistoryId) throws IOException {
        // Build a new authorized API client service.
        Gmail service = getGmailService();


        BigInteger startHistoryIdBigInteger = BigInteger.valueOf(Long.valueOf(startHistoryId));
        final Gmail.Users.History.List request = service.users().history().list("me")
                .setStartHistoryId(startHistoryIdBigInteger);

        List<History> histories = new ArrayList<History>();
        try {
            ListHistoryResponse response = request.execute();

            while (response.getHistory() != null) {
                histories.addAll(response.getHistory());
                if (response.getNextPageToken() != null) {
                    String pageToken = response.getNextPageToken();
                    System.out.println("Getting next page, token: " + pageToken);
                    response = service.users().history().list("me").setPageToken(pageToken)
                            .setStartHistoryId(startHistoryIdBigInteger).execute();
                } else {
                    System.out.println("Response has no next page");
                    break;
                }
            }

        } catch (GoogleJsonResponseException e) {
            System.out.println("---");
            System.out.println(e.getClass().getCanonicalName() + " occurred when accessing history info for history id: " + startHistoryIdBigInteger + ", details: " + e.getDetails().toString());
        }

        System.out.println("Done collecting histories");
        return histories;
    }

    public static boolean isHistoryIdValid(String historyId) throws IOException {
        // Build a new authorized API client service.
        Gmail service = getGmailService();

        BigInteger startHistoryIdBigInteger = BigInteger.valueOf(Long.valueOf(historyId));
        final Gmail.Users.History.List request = service.users().history().list("me")
                .setStartHistoryId(startHistoryIdBigInteger);
        try {
            ListHistoryResponse response = request.execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getDetails().getCode() == 404) {
                return false;
            }

            throw e; // Not sure what is wrong, so rethrow
        }
        return true;
    }


}
