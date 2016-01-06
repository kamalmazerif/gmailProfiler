package pack.service;

/**
 * Created by User on 12/28/2015.
 */

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
        import com.google.api.client.googleapis.util.Utils;
        import com.google.api.client.http.HttpRequestInitializer;
        import com.google.api.client.http.HttpTransport;
        import com.google.api.client.json.JsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.pubsub.Pubsub;
        import com.google.api.services.pubsub.PubsubScopes;
import com.google.api.services.pubsub.model.*;
import com.google.common.base.Preconditions;
import com.google.api.services.pubsub.Pubsub;
import pack.RetryHttpInitializerWrapper;

import java.io.IOException;

        import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Create a Pubsub client using portable credentials.
 */
public class PubSubClient {

    final static String projectId = "gmail-test-2015-12";
    final static String subscriptionName = "projects/" + projectId + "/subscriptions/" + "testSubscription";



    private static final String APPLICATION_NAME =
            "Gmail API Java Quickstart";

    // Default factory method.
    public static Pubsub createPubsubClient() throws IOException {
        HttpTransport defaultTransport = Utils.getDefaultTransport();
        JsonFactory defaultJsonFactory = Utils.getDefaultJsonFactory();
        return createPubsubClient(defaultTransport, defaultJsonFactory);
    }

    // A factory method that allows you to use your own HttpTransport
    // and JsonFactory.
    public static Pubsub createPubsubClient(HttpTransport httpTransport,
                                            JsonFactory jsonFactory) throws IOException {
        Preconditions.checkNotNull(httpTransport);
        Preconditions.checkNotNull(jsonFactory);
        GoogleCredential credential = GoogleCredential.getApplicationDefault(httpTransport, jsonFactory);
        // In some cases, you need to add the scope explicitly.
        if (credential.createScopedRequired()) {
            credential = credential.createScoped(PubsubScopes.all());
        }
        // Please use custom HttpRequestInitializer for automatic
        // retry upon failures.  We provide a simple reference
        // implementation in the "Retry Handling" section.
        HttpRequestInitializer initializer =
                new RetryHttpInitializerWrapper(credential);
        return new Pubsub.Builder(httpTransport, jsonFactory, initializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }




    public static void createTopic() throws IOException {
        Pubsub pubsub = createPubsubClient();
        Pubsub.Projects projects = pubsub.projects();
        Pubsub.Projects.Topics topics = projects.topics();

        Pubsub.Projects.Topics.Create create = topics.create("projects/" + projectId + "/topics/mytopic", new Topic());
        Topic newTopic = create.execute();
        System.out.println("Created topic: " + newTopic.getName());
    }

    public static void createSubscription() throws IOException {
        Pubsub pubsub = createPubsubClient();

        Subscription subscription = new Subscription();
        subscription.setTopic("projects/gmail-test-2015-12/topics/mytopic");
        subscription.setAckDeadlineSeconds(10);

        Subscription newSubscription = pubsub.projects().subscriptions()
                .create(subscriptionName, subscription)
                .execute();
        System.out.println("Created subscription: " + newSubscription.getName());

    }

    public static void setTopicPolicy() throws IOException {


        Binding binding = new Binding();
        binding.setRole("roles/pubsub.publisher");

        binding.setMembers(Arrays.asList("serviceAccount:gmail-api-push@system.gserviceaccount.com"));
        Policy policy = new Policy();
        policy.setBindings(Arrays.asList(binding));


        SetIamPolicyRequest setIamPolicyRequest = new SetIamPolicyRequest();
        setIamPolicyRequest.setPolicy(policy);

        Pubsub pubsub = createPubsubClient();
        Pubsub.Projects projects = pubsub.projects();
        Pubsub.Projects.Topics topics = projects.topics();
        final Policy returnPolicy = topics.setIamPolicy("projects/" + projectId + "/topics/mytopic", setIamPolicyRequest).execute();

        System.out.println("SetIamPolicy returned Policy: " + returnPolicy.getEtag());
        System.out.println("SetIamPolicy returned Policy: " + returnPolicy.toString());


    }


    public static void doPull() throws IOException {
        Pubsub pubsub = createPubsubClient();
        String userId = "me";

        PullRequest pullRequest = new PullRequest().setReturnImmediately(true)
                .setMaxMessages(2);

        PullResponse pullResponse = pubsub
                .projects()
                .subscriptions()
                .pull(subscriptionName, pullRequest).execute();

        List<String> ackIds = new ArrayList<>();

        List<ReceivedMessage> receivedMessages =
                pullResponse.getReceivedMessages();

        if (receivedMessages == null) {
            System.out.println("No new messages");
            return;
        }

        for (ReceivedMessage receivedMessage : receivedMessages) {
            PubsubMessage pubsubMessage = receivedMessage.getMessage();
            if (pubsubMessage != null) {
                System.out.print("Message Id: " + pubsubMessage.getMessageId());
                System.out.println(
                        new String(pubsubMessage.decodeData(), "UTF-8"));
            }
            ackIds.add(receivedMessage.getAckId());
        }

        boolean acknowledge = false;
        if (acknowledge) {
            AcknowledgeRequest ackRequest =
                    new AcknowledgeRequest().setAckIds(ackIds);
            Empty emptyResponse = pubsub.projects().subscriptions()
                    .acknowledge(subscriptionName, ackRequest).execute();
            System.out.println("Messages acknowledged: " + ackIds);
            System.out.println("Got \"empty\" response: " + emptyResponse);
        }

    }



}
