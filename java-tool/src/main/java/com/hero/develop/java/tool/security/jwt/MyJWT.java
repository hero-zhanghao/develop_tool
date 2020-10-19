package com.easypay.service.user.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.function.Function;

@Component
public class MyJWT {
	
	private static final Logger logger = LoggerFactory.getLogger(MyJWT.class);
	
	@Value("${service.security.jwt.private-key}")
	private String PRIVATE_KEY;
	
	public String extractUsername(String token) {
		return extractClaims(token, Claims::getSubject);
	}
	
	public Date extractExpiration(String token) {
		return extractClaims(token, Claims::getExpiration);
	}
	
	public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}
	
	public Claims extractAllClaims(String token) {
		return Jwts.parser().setSigningKey(PRIVATE_KEY).parseClaimsJws(token).getBody();
	}
	
	private Boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}
	
	public Boolean validateToken(String token, UserDetails userDetails) {
		try {
			final String username = extractUsername(token);
			return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
		} catch(Exception ex) {
			logger.error("i n v a l i d  t o k e n.");
			return false;
		}
	}
	
	public Boolean validateToken(String token) {
		try {
			return !isTokenExpired(token);
		} catch(Exception ex) {
			logger.error("t o k e n  w a s  e x p i r e d.");
			return false;
		}
	}
}
