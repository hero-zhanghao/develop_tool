package com.easypay.service.user.security;

import com.easypay.service.user.constant.JWTRequestFilterConstant;
import com.easypay.service.user.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityContextRepository implements ServerSecurityContextRepository {

    @Autowired
    private  AuthenticationManager authenticationManager;

    @Override
    public Mono<Void> save(ServerWebExchange serverWebExchange, SecurityContext securityContext) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange serverWebExchange) {
        final ServerHttpRequest request = serverWebExchange.getRequest();
        final String authorizationHeader = request.getHeaders().getFirst(JWTRequestFilterConstant.AUTHORIZATION);
        if (authorizationHeader != null) {
            final boolean isBearer = authorizationHeader.startsWith(JWTRequestFilterConstant.HEADER_VALUE_BEARER);
            final boolean isToken = authorizationHeader.startsWith(JWTRequestFilterConstant.HEADER_VALUE_TOKEN);
            if (isBearer || isToken) {
                final String authToken = isBearer ? authorizationHeader.substring(7) : authorizationHeader.substring(6);
                Authentication auth = new UsernamePasswordAuthenticationToken(authToken, authToken);
                return  authenticationManager.authenticate(auth).map(SecurityContextImpl::new)
                        .cast(SecurityContext.class)
                        .doOnNext(e -> UserUtils.saveCurrentUser(serverWebExchange,e));
            }
        }
        return Mono.empty();
    }
}
