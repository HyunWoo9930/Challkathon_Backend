package challkahthon.backend.hihigh.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import challkahthon.backend.hihigh.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

	private final SecretKey jwtSecret;

	@Value("${spring.jwt.access.expiration}")
	private long jwtExpirationInMs;

	@Value("${spring.jwt.refresh.expiration}")
	private long refreshExpirationInMs;

	@Value("${spring.jwt.secret:mySecretKey123456789012345678901234567890}")
	private String secretKey;

	public JwtTokenProvider() {
		this.jwtSecret = Keys.hmacShaKeyFor("HiHighSecretKey123456789012345678901234567890HiHighSecretKey".getBytes());
	}

	public String generateAccessToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

		return Jwts.builder()
			.setSubject(user.getLoginId())
			.setIssuedAt(now)
			.setExpiration(expiryDate)
			.claim("tokenType", "access")
			.claim("userId", user.getId())
			.claim("username", user.getName())
			.signWith(jwtSecret)
			.compact();
	}

	public String generateToken(User user) {
		return generateAccessToken(user);
	}

	public String generateRefreshToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + refreshExpirationInMs);

		return Jwts.builder()
			.setSubject(user.getLoginId())
			.setIssuedAt(now)
			.setExpiration(expiryDate)
			.claim("tokenType", "refresh")
			.claim("userId", user.getId())
			.signWith(jwtSecret)
			.compact();
	}

	public boolean isRefreshToken(String token) {
		try {
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(jwtSecret)
				.build()
				.parseClaimsJws(token)
				.getBody();

			return "refresh".equals(claims.get("tokenType"));
		} catch (Exception ex) {
			return false;
		}
	}

	public String getUserIdFromJWT(String token) {
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(jwtSecret)
			.build()
			.parseClaimsJws(token)
			.getBody();

		return claims.getSubject();
	}

	public boolean validateToken(String authToken) {
		try {
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(jwtSecret)
				.build()
				.parseClaimsJws(authToken)
				.getBody();
			
			Date expiration = claims.getExpiration();
			if (expiration.before(new Date())) {
				return false;
			}
			
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public Claims getTokenClaims(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(jwtSecret)
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	public Date getTokenIssuedAt(String token) {
		Claims claims = getTokenClaims(token);
		return claims.getIssuedAt();
	}

	public Date getTokenExpiration(String token) {
		Claims claims = getTokenClaims(token);
		return claims.getExpiration();
	}

	public String getTokenType(String token) {
		Claims claims = getTokenClaims(token);
		return (String)claims.get("tokenType");
	}

	public boolean isTokenExpired(String token) {
		try {
			Date expiration = getTokenExpiration(token);
			return expiration.before(new Date());
		} catch (Exception ex) {
			return true;
		}
	}

	public Long getUserIdFromToken(String token) {
		try {
			Claims claims = getTokenClaims(token);
			Object userIdObj = claims.get("userId");
			if (userIdObj instanceof Integer) {
				return ((Integer) userIdObj).longValue();
			} else if (userIdObj instanceof Long) {
				return (Long) userIdObj;
			}
			return null;
		} catch (Exception ex) {
			return null;
		}
	}

	public String getUsernameFromToken(String token) {
		try {
			Claims claims = getTokenClaims(token);
			return (String) claims.get("username");
		} catch (Exception ex) {
			return null;
		}
	}
}
