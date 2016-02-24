package pack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pack.controller.GmailController;
import pack.service.GmailApiService;

import javax.annotation.PostConstruct;

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
        // Confirmed this gets run


        //GmailApiService.listLabels();
        //GmailApiService.setWatch();


        //PubSubClient.createTopic(); // Already done
        //PubSubClient.createSubscription();
        //PubSubClient.setTopicPolicy();

        gmailController.resyncInbox(); // Update inbox

        //GmailApiService.getMessageInfoFromApi("7209405991688");
        //GmailApiService.getLabelInfo("INBOX");
        //GmailApiService.scanAllMessagesWithLabel("INBOX");
        //PubSubClient.doPull();

        // 4706615
        // 4706653

        // 4755298
        // 4755302
        // "4755298"
        gmailController.testUpdateMessageHistory();



//        List<GmailLabelUpdate> labelInfo = gmailController.getLabelInfo();
//        for (GmailLabelUpdate label : labelInfo) {
//            label.getLastHistoryId();
//        }
//        System.out.println(labelInfo);

    }

}
