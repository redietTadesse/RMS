package com.rms.config;

import com.rms.service.TimeTravelService;
import jakarta.validation.ClockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ValidationConfig {

    @Bean
    public ClockProvider virtualClockProvider(TimeTravelService timeTravelService) {
        return () -> {
            ZoneId zone = ZoneId.systemDefault();
            return Clock.fixed(
                    timeTravelService.getCurrentDate().atStartOfDay(zone).toInstant(),
                    zone
            );
        };
    }

    @Primary
    @Bean(name = "defaultValidator")
    public LocalValidatorFactoryBean defaultValidator(ClockProvider clockProvider) {
        return new LocalValidatorFactoryBean() {
            @Override
            protected void postProcessConfiguration(jakarta.validation.Configuration<?> configuration) {
                configuration.clockProvider(clockProvider);
            }
        };
    }
}
