package uz.falconmobile.bilim_scan.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bootstrap.admin")
public class AdminBootstrapProperties {
    private boolean enabled;
    private String username;
    private String password;
}
