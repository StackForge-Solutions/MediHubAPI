package com.MediHubAPI.security;

import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.Role;
import com.MediHubAPI.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void generateTokenIncludesIdentityRoleAndExpiryClaims() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider();
        String secret = Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded());

        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationMs", 3_600_000L);

        Role adminRole = new Role(ERole.ADMIN);
        Role doctorRole = new Role(ERole.DOCTOR);
        User user = User.builder()
                .id(42L)
                .username("jane.doe")
                .email("jane@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .roles(Set.of(doctorRole, adminRole))
                .build();

        String token = tokenProvider.generateToken(user);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getSubject()).isEqualTo("jane.doe");
        assertThat(claims.get("id", Number.class).longValue()).isEqualTo(42L);
        assertThat(claims.get("name", String.class)).isEqualTo("Jane Doe");
        assertThat(claims.get("email", String.class)).isEqualTo("jane@example.com");
        assertThat(claims.get("roles", java.util.List.class)).containsExactly("ADMIN", "DOCTOR");
        assertThat(claims.getExpiration()).isAfter(new Date());
    }
}
