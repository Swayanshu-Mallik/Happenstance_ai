package com.happenstance.recommendation;

import com.happenstance.profile.Profile;
import com.happenstance.profile.ProfileRepository;
import com.happenstance.relationship.Relationship;
import com.happenstance.relationship.RelationshipRepository;
import com.happenstance.user.UserAccount;
import com.happenstance.user.UserRepository;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final String ACCEPTED = "ACCEPTED";
    private static final String PUBLIC = "PUBLIC";

    private final ProfileRepository profileRepository;
    private final RelationshipRepository relationshipRepository;
    private final UserRepository userRepository;

    public RecommendationService(
            ProfileRepository profileRepository,
            RelationshipRepository relationshipRepository,
            UserRepository userRepository) {

        this.profileRepository = profileRepository;
        this.relationshipRepository = relationshipRepository;
        this.userRepository = userRepository;
    }

    public List<RecommendationResponse> recommend(
            Profile currentProfile, int requestedLimit) {

        if (currentProfile == null || currentProfile.getUserId() == null) {
            throw new IllegalArgumentException(
                    "Current user profile is required.");
        }

        int limit = Math.max(1, Math.min(requestedLimit, 50));
        String currentUserId = currentProfile.getUserId();

        List<Relationship> acceptedRelationships =
                relationshipRepository.findByStatus(ACCEPTED);

        Map<String, Set<String>> graph =
                buildGraph(acceptedRelationships);

        Set<String> currentConnections = graph.getOrDefault(
                currentUserId, Collections.emptySet());

        List<Profile> candidates = profileRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(profile -> profile.getUserId() != null)
                .filter(profile -> !profile.getUserId()
                        .equals(currentUserId))
                .filter(profile -> PUBLIC.equalsIgnoreCase(
                        safe(profile.getVisibility())))
                .filter(profile -> !currentConnections.contains(
                        profile.getUserId()))
                .toList();

        // Fetch all candidate accounts together to avoid N+1 queries.
        Set<String> candidateUserIds = candidates.stream()
                .map(profile -> Objects.requireNonNull(profile).getUserId())
                .collect(Collectors.toSet());

        Map<String, UserAccount> accountsById =
                userRepository.findAllById(candidateUserIds).stream()
                        .collect(Collectors.toMap(
                                account -> account.getId(),
                                Function.identity(),
                                (first, second) -> first));

        return candidates.stream()
                .map(candidate -> {
                    UserAccount account = accountsById.get(
                            candidate.getUserId());

                    String displayName = account == null
                            ? null
                            : account.getDisplayName();

                    return scoreProfile(
                            candidate,
                            displayName,
                            currentProfile,
                            currentConnections,
                            graph);
                })
                .filter(result -> result.score() > 0)
                .sorted(
                        Comparator.<RecommendationResponse>comparingInt(
                                response -> response.score())
                                .reversed()
                                .thenComparing(
                                        (RecommendationResponse response) ->
                                                response.userId()))
                .limit(limit)
                .toList();
    }

    private Map<String, Set<String>> buildGraph(
            List<Relationship> relationships) {

        Map<String, Set<String>> graph = new HashMap<>();

        for (Relationship relationship : relationships) {
            if (relationship.getSourceUserId() == null
                    || relationship.getTargetUserId() == null) {
                continue;
            }

            String source = relationship.getSourceUserId();
            String target = relationship.getTargetUserId();

            graph.computeIfAbsent(source, key -> new HashSet<>())
                    .add(target);

            graph.computeIfAbsent(target, key -> new HashSet<>())
                    .add(source);
        }

        return graph;
    }

    private RecommendationResponse scoreProfile(
            Profile candidate,
            String displayName,
            Profile currentProfile,
            Set<String> currentConnections,
            Map<String, Set<String>> graph) {

        int score = 0;
        List<String> reasons = new ArrayList<>();

        Set<String> sharedSkills = intersection(
                candidate.getSkills(),
                currentProfile.getSkills());

        Set<String> sharedInterests = intersection(
                candidate.getInterests(),
                currentProfile.getInterests());

        if (!sharedSkills.isEmpty()) {
            score += Math.min(sharedSkills.size(), 6) * 5;
            reasons.add("Shared skills: "
                    + String.join(", ", sharedSkills));
        }

        if (!sharedInterests.isEmpty()) {
            score += Math.min(sharedInterests.size(), 5) * 4;
            reasons.add("Shared interests: "
                    + String.join(", ", sharedInterests));
        }

        if (sameValue(candidate.getIndustry(),
                currentProfile.getIndustry())) {
            score += 15;
            reasons.add("Works in the same industry");
        }

        if (sameValue(candidate.getLocation(),
                currentProfile.getLocation())) {
            score += 10;
            reasons.add("Based in the same location");
        }

        Set<String> candidateConnections = graph.getOrDefault(
                candidate.getUserId(), Collections.emptySet());

        Set<String> mutualConnections = new HashSet<>(
                currentConnections);

        mutualConnections.retainAll(candidateConnections);

        if (!mutualConnections.isEmpty()) {
            score += Math.min(mutualConnections.size(), 3) * 8;
            reasons.add(mutualConnections.size()
                    + " mutual connection"
                    + (mutualConnections.size() == 1 ? "" : "s"));
        }

        return new RecommendationResponse(
                candidate.getUserId(),
                displayName,
                candidate.getHeadline(),
                candidate.getBio(),
                candidate.getLocation(),
                candidate.getIndustry(),
                safeList(candidate.getSkills()),
                safeList(candidate.getInterests()),
                Math.min(score, 99),
                reasons
        );
    }

    private Set<String> intersection(
            List<String> first,
            List<String> second) {

        Set<String> secondValues = safeList(second).stream()
                .map(this::normalize)
                .collect(Collectors.toSet());

        return safeList(first).stream()
                .filter(value -> secondValues.contains(normalize(value)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean sameValue(String first, String second) {
        return first != null
                && second != null
                && !first.isBlank()
                && first.trim().equalsIgnoreCase(second.trim());
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}