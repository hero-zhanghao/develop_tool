package com.easypay.service.user.security;

import cn.hutool.core.util.StrUtil;
import com.easypay.service.user.entity.MyUserDetails;
import com.easypay.service.user.repository.cache.JedisRepository;
import com.easypay.service.user.security.jwt.MyJWT;
import com.easypay.service.user.security.jwt.OAuthJWT;
import com.easypay.service.user.service.MyUserDetailService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
@Slf4j
public class AuthenticationManager implements ReactiveAuthenticationManager {

    @Autowired
    private MyJWT jwt;

    @Autowired
    private OAuthJWT jwt2;

    @Autowired
    private MyUserDetailService userDetailsService;

    @Autowired
    private JedisRepository jedis;


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        final String authToken = authentication.getCredentials().toString();
        return Mono.just(authToken)
                .map(e -> getUserId(e))
                .onErrorResume(Exception.class, e -> Mono.just(""))
                .filter(userId -> StrUtil.isNotBlank(userId))
                .flatMap(userId -> userDetailsService.loadUserByUserId(userId))
                .filter(userDetails -> Objects.nonNull(userDetails) && jwt.validateToken(authToken, userDetails))
                .flatMap(userDetails -> {
                            try {
                                final String value = jedis.getData("", authToken);
                                log.info("redis value = {}", value);
                                if (StrUtil.isNotBlank(value)) {
                                    ((MyUserDetails) userDetails).setToken(authToken);
                                    final Authentication usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());
                                    return Mono.just(usernamePasswordAuthenticationToken);
                                }
                            } catch (Exception e) {
                                log.error(e.getMessage());
                            }
                            return Mono.empty();
                        }
                )
                .switchIfEmpty(
                        Mono.just(authToken)
                        .flatMap(e -> {
                            log.info("check if Python Token");
                            return jwt2.validate(e);
                        })
                        .flatMap(b -> {
                            if (b){
                                final Authentication usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                                        jwt2.getUserDetails(), null, jwt2.getUserDetails().getAuthorities());
//                                ReactiveSecurityContextHolder.withAuthentication(usernamePasswordAuthenticationToken);
                                return Mono.just(usernamePasswordAuthenticationToken);
                            }else{
                                return Mono.empty();
                            }
                        })
                )
                .onErrorMap(e -> new BadCredentialsException(e.getMessage()))
                .switchIfEmpty(Mono.error(new BadCredentialsException("need authorization")));


    }

    private String getUserId(String token) {
        Claims claims = jwt.extractAllClaims(token);
        return (String) claims.get("userId");
    }

}
