package com.fedstack.spending.auth.token;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(JwtAuthProperties.class)
public class JwtTokenConfiguration {
	@Bean
	KeyPair jwtKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("RSA key generation is unavailable", exception);
		}
	}

	@Bean
	JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {
		RSAPublicKey publicKey = (RSAPublicKey) jwtKeyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) jwtKeyPair.getPrivate();
		RSAKey rsaKey = new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(UUID.randomUUID().toString())
				.algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
				.build();
		return new NimbusJwtEncoder((jwkSelector, context) -> jwkSelector.select(new JWKSet(rsaKey)));
	}

	@Bean
	@Primary
	JwtDecoder jwtDecoder(KeyPair jwtKeyPair, JwtAuthProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) jwtKeyPair.getPublic())
				.signatureAlgorithm(SignatureAlgorithm.RS256)
				.build();
		decoder.setJwtValidator(jwtValidator(properties, TokenPurpose.ACCESS));
		return decoder;
	}

	@Bean
	@Qualifier("refreshJwtDecoder")
	JwtDecoder refreshJwtDecoder(KeyPair jwtKeyPair, JwtAuthProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) jwtKeyPair.getPublic())
				.signatureAlgorithm(SignatureAlgorithm.RS256)
				.build();
		decoder.setJwtValidator(jwtValidator(properties, TokenPurpose.REFRESH));
		return decoder;
	}

	private static OAuth2TokenValidator<Jwt> jwtValidator(JwtAuthProperties properties, TokenPurpose purpose) {
		return new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtIssuerValidator(properties.getIssuer()),
				new JwtAudienceValidator(properties.getAudience()),
				new JwtRequiredClaimsValidator(),
				new JwtPurposeValidator(purpose)
		);
	}
}
