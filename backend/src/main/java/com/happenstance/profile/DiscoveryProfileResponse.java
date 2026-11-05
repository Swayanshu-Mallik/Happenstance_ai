
package com.happenstance.profile;

import java.time.Instant;
import java.util.List;

public class DiscoveryProfileResponse {

    private String userId;
    private String displayName;
    private String headline;
    private String bio;
    private String location;
    private String industry;
    private List<String> skills;
    private List<String> interests;
    private Instant updatedAt;

    // New constructor with display name
    public DiscoveryProfileResponse(
            String userId,
            String displayName,
            String headline,
            String bio,
            String location,
            String industry,
            List<String> skills,
            List<String> interests,
            Instant updatedAt) {

        this.userId = userId;
        this.displayName = displayName;
        this.headline = headline;
        this.bio = bio;
        this.location = location;
        this.industry = industry;
        this.skills = skills;
        this.interests = interests;
        this.updatedAt = updatedAt;
    }

    // Backward-compatible constructor
    public DiscoveryProfileResponse(
            String userId,
            String headline,
            String bio,
            String location,
            String industry,
            List<String> skills,
            List<String> interests,
            Instant updatedAt) {

        this(
                userId,
                null,
                headline,
                bio,
                location,
                industry,
                skills,
                interests,
                updatedAt
        );
    }

    // Existing mapping method retained
    public static DiscoveryProfileResponse from(Profile profile) {
        return from(profile, null);
    }

    // Mapping method with display name
    public static DiscoveryProfileResponse from(
            Profile profile,
            String displayName) {

        return new DiscoveryProfileResponse(
                profile.getUserId(),
                displayName,
                profile.getHeadline(),
                profile.getBio(),
                profile.getLocation(),
                profile.getIndustry(),
                profile.getSkills(),
                profile.getInterests(),
                profile.getUpdatedAt()
        );
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHeadline() {
        return headline;
    }

    public String getBio() {
        return bio;
    }

    public String getLocation() {
        return location;
    }

    public String getIndustry() {
        return industry;
    }

    public List<String> getSkills() {
        return skills;
    }

    public List<String> getInterests() {
        return interests;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
