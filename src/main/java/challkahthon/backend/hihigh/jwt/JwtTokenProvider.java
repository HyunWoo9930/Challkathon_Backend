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

	private final SecretKey jwtSecret = Keys.secretKeyFor(SignatureAlgorithm.HS512);

	@Value("${spring.jwt.access.expiration}")
	private long jwtExpirationInMs;

	@Value("${spring.jwt.refresh.expiration}")
	private long refreshExpirationInMs;

	/**
	 * Generate an access token for the given user
	 * @param user The user for whom to generate the token
	 * @return The generated access token
	 */
	public String generateAccessToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

		return Jwts.builder()
			.setSubject(user.getName())
			.setIssuedAt(now)
			.setExpiration(expiryDate)
			.claim("tokenType", "access")
			.signWith(jwtSecret)
			.compact();
	}

	/**
	 * Generate a token for the given user (for backward compatibility)
	 * @param user The user for whom to generate the token
	 * @return The generated access token
	 */
	public String generateToken(User user) {
		return generateAccessToken(user);
	}

	/**
	 * Generate a refresh token for the given user
	 * @param user The user for whom to generate the token
	 * @return The generated refresh token
	 */
	public String generateRefreshToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + refreshExpirationInMs);

		return Jwts.builder()
			.setSubject(user.getName())
			.setIssuedAt(now)
			.setExpiration(expiryDate)
			.claim("tokenType", "refresh")
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
			Jwts.parserBuilder()
				.setSigningKey(jwtSecret)
				.build()
				.parseClaimsJws(authToken);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
}
