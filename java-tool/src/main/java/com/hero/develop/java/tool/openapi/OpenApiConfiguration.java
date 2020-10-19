package com.easypay.service.user.configuration;

import com.easypay.service.user.constant.JWTRequestFilterConstant;
import com.easypay.service.user.constant.PathConstant;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI openAPI(
            @Value("${server.servlet.context-path:/}") String contextPath
    ) {
        return new OpenAPI()
                .addServersItem(new Server().url(contextPath))
                .components(getComponents())
                .addSecurityItem(new SecurityRequirement().addList("Access Token"))
                .info(new Info().title("FORTUNEPAY USER API").version("1.0")
                        .description("https://www.fortunepay.com/"));
    }

    private Components getComponents() {
        SecurityScheme authorizationHeaderSchema = new SecurityScheme()
                .name(JWTRequestFilterConstant.AUTHORIZATION)
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                ;

        return new Components()
                .securitySchemes(ImmutableMap.of("Access Token", authorizationHeaderSchema));
    }

    @Bean
    public GroupedOpenApi serviceOpenApi() {
        String[] paths = {PathConstant.PREFIX + PathConstant.SERVICE + "/**"};
        return GroupedOpenApi.builder().setGroup("service").pathsToMatch(paths)
                .build();
    }

    @Bean
    public GroupedOpenApi settingOpenApi() {
        String[] paths = {PathConstant.PREFIX + PathConstant.SETTING + "/**"};
        return GroupedOpenApi.builder().setGroup("setting").pathsToMatch(paths)
                .build();
    }

}
