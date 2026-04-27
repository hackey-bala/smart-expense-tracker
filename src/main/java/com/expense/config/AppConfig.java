package com.expense.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * AppConfig — MainFrame is NO LONGER a Spring bean.
 * It is constructed manually in ExpenseTrackerApplication.main()
 * after the context starts, on the Swing EDT.
 * This avoids HeadlessException during Spring context initialization.
 */
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
