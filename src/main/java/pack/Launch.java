package pack;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by User on 12/27/2015.
 */

@Component
public class Launch {

    @PostConstruct
    public void postConstruct() throws Exception {
        System.out.println("Launch class was called");
        GmailQuickstart.launch();
    }

}
