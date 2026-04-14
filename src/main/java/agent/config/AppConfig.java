package agent.config;

import agent.util.ModelConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置类
 */
@Configuration
public class AppConfig {

    @Value("${app.storage.sessions}")
    private String sessionsPath;

    @Value("${app.storage.skills}")
    private String skillsPath;

    @Value("${app.storage.system}")
    private String systemPath;

    @Value("${app.storage.user}")
    private String userPath;

    @Bean
    public ModelConfig modelConfig() {
        return ModelConfig.fromEnv();
    }

    @Bean
    public String skillRepoPath() {
        return skillsPath;
    }

    @Bean
    public String systemRepoPath() {
        return systemPath;
    }

    @Bean
    public String userRepoPath() {
        return userPath;
    }

    @Bean
    public String sessionRepoPath() {
        return sessionsPath;
    }
}
