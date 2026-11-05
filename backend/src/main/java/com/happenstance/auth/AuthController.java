
package com.happenstance.auth;

import com.happenstance.profile.Profile;
import com.happenstance.profile.ProfileRepository;
import com.happenstance.user.UserAccount;
import com.happenstance.user.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.crypto.SecretKey;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final SecretKey key;
    private final long expiry;

    public AuthController(
            UserRepository users,
            ProfileRepository profiles,
            PasswordEncoder encoder,
            @Value("${happenstance.jwt.secret}") String secret,
            @Value("${happenstance.jwt.expiration-ms}") long expiry) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET must be configured.");
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must contain at least 32 bytes.");
        }

        if (expiry <= 0) {
            throw new IllegalStateException(
                    "JWT expiration must be greater than zero.");
        }

        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiry = expiry;
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
    }

    public record RegisterRequest(
            @NotBlank
            @Size(min = 2, max = 80)
            String displayName,

            @NotBlank
            @Email
            @Size(max = 254)
            String email,

            @NotBlank
            @Size(min = 12, max = 72)
            String password
    ) {}

    public record LoginRequest(
            @NotBlank
            @Email
            @Size(max = 254)
            String email,

            @NotBlank
            String password
    ) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(
            @Valid @RequestBody RegisterRequest request) {

        String displayName = request.displayName().trim();
        String email = normalizeEmail(request.email());
        String password = request.password();

        if (displayName.length() < 2 || displayName.length() > 80) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Display name must contain 2 to 80 characters.");
        }

        // BCrypt only processes up to 72 UTF-8 bytes.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes.");
        }

        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered.");
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(encoder.encode(password));

        try {
            user = users.save(user);
        } catch (DuplicateKeyException exception) {
            // Handles simultaneous registration attempts.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered.");
        }

        Profile profile = new Profile();
        profile.setUserId(user.getId());
        profiles.save(profile);

        return tokenResponse(user);
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody LoginRequest request) {

        String email = normalizeEmail(request.email());

        UserAccount user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password."));

        String passwordHash = user.getPasswordHash();

        if (passwordHash == null
                || !encoder.matches(request.password(), passwordHash)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password.");
        }

        return tokenResponse(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> tokenResponse(UserAccount user) {

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expiry);

        String token = Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();

        return Map.of(
                "tokenType", "Bearer",
                "accessToken", token,
                "expiresInMs", expiry,
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "displayName", user.getDisplayName() == null
                                ? ""
                                : user.getDisplayName()
                )
        );
    }
}
