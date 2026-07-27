package org.quizly.quizly.configuration;

import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.logging.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@RequiredArgsConstructor
public class LoggingConfig {

    private final TraceIdFilter traceIdFilter;

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(traceIdFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
