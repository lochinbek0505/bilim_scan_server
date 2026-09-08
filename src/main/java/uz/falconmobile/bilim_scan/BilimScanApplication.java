package uz.falconmobile.bilim_scan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BilimScanApplication {

    public static void main(String[] args) {
        SpringApplication.run(BilimScanApplication.class, args);
    }

}
