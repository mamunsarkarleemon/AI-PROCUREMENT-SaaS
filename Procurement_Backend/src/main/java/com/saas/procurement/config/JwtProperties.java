package com.saas.procurement.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.jwt.* in application.properties / config/local.properties.
 *
 * @param secret         HMAC-SHA256 signing secret, at least 32 bytes. Never commit it.
 * @param issuer         value of the "iss" claim; tokens from any other issuer are rejected
 * @param audience       value of the "aud" claim; tokens for any other audience are rejected
 * @param accessTokenTtl lifetime of an access token (keep it short)
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, String audience, Duration accessTokenTtl) {
}
