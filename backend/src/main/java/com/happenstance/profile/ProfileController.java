
package com.happenstance.profile;

import com.happenstance.user.UserAccount;
import com.happenstance.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/profiles/me")
public class ProfileController {

    private static final int MAX_HEADLINE_LENGTH = 120;
    private static final int MAX_BIO_LENGTH = 1000;
    private static final int MAX_LOCATION_LENGTH = 120;
    private static final int MAX_INDUSTRY_LENGTH = 100;
    private static final int MAX_LIST_ITEMS = 30;
    private static final int MAX_LIST_ITEM_LENGTH = 60;

    private static final List<String> VISIBILITIES =
            List.of("PUBLIC", "CONNECTIONS", "PRIVATE");

    private final ProfileRepository profiles;
    private final UserRepository users;

    public ProfileController(
            ProfileRepository profiles,
            UserRepository users) {
        this.profiles = profiles;
        this.users = users;
    }

    @GetMapping
    public ProfileView me(Authentication auth) {
        UserAccount user = getAuthenticatedUser(auth);
        Profile profile = getOrCreateProfile(user);

        return toProfileView(user, profile);
    }

    @PutMapping
    public ProfileView updateMe(
            Authentication auth,
            @RequestBody(required = false) ProfileUpdateRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Profile update data is required."
            );
        }

        UserAccount user = getAuthenticatedUser(auth);
        Profile profile = getOrCreateProfile(user);

        if (request.headline() != null) {
            profile.setHeadline(cleanText(
                    request.headline(),
                    MAX_HEADLINE_LENGTH,
                    "Headline"
            ));
        }

        if (request.bio() != null) {
            profile.setBio(cleanText(
                    request.bio(),
                    MAX_BIO_LENGTH,
                    "Bio"
            ));
        }

        if (request.location() != null) {
            profile.setLocation(cleanText(
                    request.location(),
                    MAX_LOCATION_LENGTH,
                    "Location"
            ));
        }

        if (request.industry() != null) {
            profile.setIndustry(cleanText(
                    request.industry(),
                    MAX_INDUSTRY_LENGTH,
                    "Industry"
            ));
        }

        if (request.skills() != null) {
            profile.setSkills(cleanList(request.skills(), "Skills"));
        }

        if (request.interests() != null) {
            profile.setInterests(
                    cleanList(request.interests(), "Interests")
            );
        }

        if (request.visibility() != null) {
            String visibility = request.visibility()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!VISIBILITIES.contains(visibility)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Visibility must be PUBLIC, CONNECTIONS or PRIVATE."
                );
            }

            profile.setVisibility(visibility);
        }

        profile.setUpdatedAt(Instant.now());

        Profile savedProfile = profiles.save(profile);

        return toProfileView(user, savedProfile);
    }

    private UserAccount getAuthenticatedUser(Authentication auth) {
        if (auth == null || auth.getName() == null
                || auth.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED
            );
        }

        return users.findById(auth.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED
                        )
                );
    }

    private Profile getOrCreateProfile(UserAccount user) {
        return profiles.findByUserId(user.getId())
                .orElseGet(() -> {
                    Profile profile = new Profile();
                    profile.setUserId(user.getId());
                    return profiles.save(profile);
                });
    }

    private String cleanText(
            String value,
            int maxLength,
            String fieldName) {

        String cleaned = value.trim();

        if (cleaned.length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " cannot exceed "
                            + maxLength + " characters."
            );
        }

        return cleaned;
    }

    private List<String> cleanList(
            List<String> values,
            String fieldName) {

        if (values.size() > MAX_LIST_ITEMS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " cannot contain more than "
                            + MAX_LIST_ITEMS + " items."
            );
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> cleanText(
                        value,
                        MAX_LIST_ITEM_LENGTH,
                        fieldName + " item"
                ))
                .map(value -> value.trim())
                .distinct()
                .toList();
    }

    private ProfileView toProfileView(
            UserAccount user,
            Profile profile) {

        return new ProfileView(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                profile.getHeadline(),
                profile.getBio(),
                profile.getLocation(),
                profile.getIndustry(),
                profile.getSkills(),
                profile.getInterests(),
                profile.getVisibility(),
                profile.getUpdatedAt()
        );
    }

    public record ProfileUpdateRequest(
            String headline,
            String bio,
            String location,
            String industry,
            List<String> skills,
            List<String> interests,
            String visibility
    ) {}

    public record ProfileView(
            String userId,
            String email,
            String displayName,
            String headline,
            String bio,
            String location,
            String industry,
            List<String> skills,
            List<String> interests,
            String visibility,
            Instant updatedAt
    ) {}
}
