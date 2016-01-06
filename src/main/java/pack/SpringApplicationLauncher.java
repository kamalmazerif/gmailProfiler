package pack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by User on 12/12/2015.
 */

@SpringBootApplication
public class SpringApplicationLauncher {
    public static void main(String[] args) {
        // Must use this object's .class or this class's annotations may have no effect including @ComponentScan!


        SpringApplication app = new SpringApplication(SpringApplicationLauncher.class);
        app.setWebEnvironment(false); //<<<<<<<<<
        ConfigurableApplicationContext ctx = app.run(args);
    }
}
