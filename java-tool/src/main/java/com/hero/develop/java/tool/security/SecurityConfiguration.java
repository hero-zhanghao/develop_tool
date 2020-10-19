package com.easypay.service.user.configuration;

import com.easypay.service.user.security.AuthenticationManager;
import com.easypay.service.user.security.SecurityContextRepository;
import com.easypay.service.user.service.MyUserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
@Configuration
public class SecurityConfiguration {

//    @Autowired
//    private JWTRequestFilter jwtRequestFilter;

    @Autowired
    private MyUserDetailService myUserDetailService;


    @Autowired
    private SecurityContextRepository securityContextRepository;

    @Autowired
    private AuthenticationManager authenticationManager;


    public static Boolean IS_LOCAL;

    @Value("${environment.local:false}")
    public void setIsLocal(Boolean isLocal) {
        SecurityConfiguration.IS_LOCAL = isLocal;
    }


    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .authorizeExchange(
                        auth ->
                                addLocalMatcher(
                                        auth.pathMatchers("/actuator/health").permitAll()
                                                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                                                .pathMatchers(
                                                        "/v3/api-docs/**",
                                                        "/configuration/ui",
                                                        "/swagger-resources/**",
                                                        "/configuration/security",
                                                        "/swagger-ui.html",
                                                        "/webjars/**"
                                                ).permitAll()

                                )
                                        .anyExchange().authenticated()
                )
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)
//                .addFilterBefore(jwtRequestFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }


    private ServerHttpSecurity.AuthorizeExchangeSpec addLocalMatcher(ServerHttpSecurity.AuthorizeExchangeSpec authorizeExchangeSpec) {
        return SecurityConfiguration.IS_LOCAL ? authorizeExchangeSpec.pathMatchers("/**").permitAll() : authorizeExchangeSpec;
    }


}
