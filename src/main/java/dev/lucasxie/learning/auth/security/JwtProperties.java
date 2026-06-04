package dev.lucasxie.learning.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

	private String secret;

	private String issuer = "learning-platform";

	private long accessTokenExpirationMinutes = 30;

	private long refreshTokenExpirationDays = 7;
}
