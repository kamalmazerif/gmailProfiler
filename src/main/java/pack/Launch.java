package pack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pack.controller.GmailController;
import pack.service.GmailQuickstart;

import javax.annotation.PostConstruct;

/**
 * Created by User on 12/27/2015.
 */

@Component
public class Launch {

    @Autowired
    private GmailController gmailController;

    @PostConstruct
    public void postConstruct() throws Exception {
        // Confirmed this gets run


        //GmailQuickstart.listLabels();
        //GmailQuickstart.setWatch();


        //PubSubClient.createTopic(); // Already done
        //PubSubClient.createSubscription();
        //PubSubClient.setTopicPolicy();

        //GmailQuickstart.getMessageInfo("7209405991688");
        //GmailQuickstart.getLabelInfo("INBOX");
        //GmailQuickstart.scanAllMessagesWithLabel("INBOX");
        //PubSubClient.doPull();

        // 4706615
        // 4706653
        //GmailQuickstart.showMessageHistory("4706615");

        gmailController.resyncInbox();
    }

}
