package com.easypay.service.user.security.jwt;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.easypay.service.user.entity.MyUserDetails;
import com.easypay.service.user.repository.JPAAuthTokenRepository;
import com.easypay.service.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OAuthJWT {
    private static final Logger logger = LoggerFactory.getLogger(OAuthJWT.class);

    @Value("${service.security.jwt.secret-key}")
    private String SECRET_KEY;

    @Autowired
    private JPAAuthTokenRepository authTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private UserDetails userDetails;

    public void setUserDetails() {
        // TODO do nothing;
    }

    public UserDetails getUserDetails() {
        return userDetails;
    }

    public Mono<Boolean> validate(String jwtToken) {
        logger.info("jwtToken: {}", jwtToken);
        final Algorithm algorithm = Algorithm.HMAC512(SECRET_KEY);
        final JWTVerifier verifier = JWT.require(algorithm).build();
        final DecodedJWT jwt = verifier.verify(jwtToken);

//			final Date now = DateTime.now().toDate();

//			if(jwt.getIssuedAt() == null) {
//				logger.info("invalid token, no date issued.");
//				return false;
//			}
//
//			if (!jwt.getIssuedAt().before(now)) {
//				logger.warn("token expired");
//				return false;
//			}

        final String key = jwt.getClaim("token").asString();
        if (StrUtil.isBlank(key)) {
            logger.info("invalid token, no token.");
            return Mono.just(false);
        }

        return authTokenRepository.findById(key)
                .flatMap(e -> userRepository.findByIDActive(e.getUserId()))
                .map(user -> {
                    MyUserDetails myUserDetails = new MyUserDetails();
                    myUserDetails.setId(user.getId());
                    myUserDetails.setUserId(user.getUserId());
                    myUserDetails.setUsername(user.getPhoneNumber());
                    myUserDetails.setPassword(user.getLoginPin());
                    myUserDetails.setPhoneNumber(user.getPhoneNumber());
                    myUserDetails.setDateJoined(user.getDateJoined());
                    myUserDetails.setUserType(user.getUserType());
                    myUserDetails.setCompanyId(user.getCompanyId());
                    myUserDetails.setFirstName(user.getFirstName());
                    myUserDetails.setMiddleName(user.getMiddleName());
                    myUserDetails.setLastName(user.getLastName());
                    myUserDetails.setRoleId(user.getRoleId());
                    myUserDetails.setEnabled(true);
                    myUserDetails.setCredentialsNonExpired(true);
                    myUserDetails.setAccountNonLocked(true);
                    myUserDetails.setAccountNonExpired(true);
                    return myUserDetails;
                })
                .doOnNext(myUserDetails -> this.userDetails = myUserDetails)
                .map(e -> true)
                .onErrorResume(Exception.class, e -> Mono.just(false))
                .defaultIfEmpty(false);


//			if (authOpt.isPresent() == false) {
//				logger.info("invalid key from token");
//				return false;
//			}

//			final Long userId = authOpt.get().getUserId();
//
//			final Optional<UserMaster> userOpt = userRepository.findByIDActive(userId);
//			if (userOpt.isPresent() == false) {
//				logger.info("invalid userid");
//				return false;
//			}
//
//
//
//
//			logger.info("Python Token.");
//			return true;
//		} catch (Exception e) {
//			logger.error("validate python token exception \n{}", e.getMessage());
//			return false;
//		}
    }
}
