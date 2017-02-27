package pack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pack.controller.GmailController;
import pack.data.GmailLabelUpdate;
import pack.service.GmailApiService;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by User on 12/27/2015.
 */

@Component
public class Launch {

    public static final String SCHEMA_APP_NAME = "GMAIL_MAILBOX_STATS";

    @Autowired
    private GmailController gmailController;

    @PostConstruct
    public void postConstruct() throws Exception {



        //GmailApiService.listLabels();
        //GmailApiService.setWatch();


        //PubSubClient.createTopic(); // Already done
        //PubSubClient.createSubscription();
        //PubSubClient.setTopicPolicy();


        gmailController.resyncInbox(); // Update inbox
        gmailController.printLargestSenderInfo(6); // Prepares and displays statistics

        //GmailApiService.getMessageByIdFromApi("7209405991688");
//        GmailApiService.getLabelInfo("INBOX");
//        GmailApiService.getAllMessagesForLabel("INBOX");

//        PubSubClient.doPull();

        // 4706615
        // 4706653
        // 4755298
        // 4755302
        // "4755298"

//        gmailController.testCheckMessageHistory(4846068);

        //gmailController.findNextValidHistoryId("4755298"); // old test

        //gmailController.findNextValidHistoryId("4846847"); // just expired
//        gmailController.findNextValidHistoryId("4846068");

        //gmailController.updateTasksForHistory();
//        gmailController.updateTasksForHistory("" + 4846068);
//        gmailController.updateTasksForHistory("" + 4927834);


//        List<GmailLabelUpdate> labelInfo = gmailController.getLabelInfo();
//        for (GmailLabelUpdate label : labelInfo) {
//            label.getLastHistoryId();
//        }
//        System.out.println(labelInfo);

    }



}
