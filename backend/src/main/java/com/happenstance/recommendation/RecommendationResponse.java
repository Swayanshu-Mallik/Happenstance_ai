package com.happenstance.recommendation;

import java.util.List;

public record RecommendationResponse(
        String userId,
        String displayName,
        String headline,
        String bio,
        String location,
        String industry,
        List<String> skills,
        List<String> interests,
        int score,
        List<String> reasons
) {
}