package org.quizly.quizly.configuration;

import jakarta.servlet.DispatcherType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.jwt.JwtAuthenticationFilter;
import org.quizly.quizly.jwt.error.JwtAuthenticationEntryPoint;
import org.quizly.quizly.oauth.CookieAuthorizationRequestRepository;
import org.quizly.quizly.oauth.CustomAuthorizationRequestResolver;
import org.quizly.quizly.oauth.OAuth2LoginSuccessHandler;
import org.quizly.quizly.oauth.service.OAuth2LoginUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final OAuth2LoginUserService oAuth2LoginUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final CookieAuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Value("${custom.oauth2.allowed-redirect-origins}")
    private List<String> allowedRedirectOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.cors(cors -> cors.configurationSource(corsConfigurationSource))

            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth
                    .authorizationRequestResolver(
                        new CustomAuthorizationRequestResolver(clientRegistrationRepository, allowedRedirectOrigins))
                    .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
                    .userService(oAuth2LoginUserService))
                .successHandler(oAuth2LoginSuccessHandler)
            )

            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/faqs",
                    "/",
                    "/docs",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/auth/reissue",
                    "/quizzes/guest/**",
                    "/quizzes/{quizId}/answer/guest",
                    "/actuator/health"
                ).permitAll()
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                .anyRequest().authenticated());

        return http.build();
    }
}