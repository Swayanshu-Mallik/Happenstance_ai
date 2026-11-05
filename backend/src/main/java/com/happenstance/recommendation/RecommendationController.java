
package com.happenstance.recommendation;

import com.happenstance.profile.Profile;
import com.happenstance.profile.ProfileRepository;
import com.happenstance.user.UserAccount;
import com.happenstance.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public RecommendationController(
            RecommendationService recommendationService,
            UserRepository userRepository,
            ProfileRepository profileRepository) {

        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required."
            );
        }

        if (limit < 1 || limit > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Limit must be between 1 and 50."
            );
        }

        // JWT subject contains the authenticated user's ID.
        String userId = authentication.getName();

        // Retrieve the account using its ID.
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account not found."
                ));

        // Retrieve the authenticated user's profile.
        Profile currentProfile = profileRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User profile not found."
                ));

        // Generate ranked recommendations.
        List<RecommendationResponse> recommendations =
                recommendationService.recommend(currentProfile, limit);

        return ResponseEntity.ok(recommendations);
    }
}
