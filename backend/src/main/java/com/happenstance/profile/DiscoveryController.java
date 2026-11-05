
package com.happenstance.profile;

import com.happenstance.user.UserAccount;
import com.happenstance.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;

    public DiscoveryController(
            MongoTemplate mongoTemplate,
            UserRepository userRepository) {
        this.mongoTemplate = mongoTemplate;
        this.userRepository = userRepository;
    }

    @GetMapping("/profiles")
    public Page<DiscoveryProfileResponse> discoverProfiles(
            Authentication auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        validateAuthentication(auth);

        if (page < 0 || size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination parameters"
            );
        }

        List<Criteria> filters = new ArrayList<>();

        filters.add(Criteria.where("visibility").is("PUBLIC"));
        filters.add(Criteria.where("userId").ne(auth.getName()));

        if (hasText(keyword)) {
            String pattern = regex(keyword.trim());

            filters.add(new Criteria().orOperator(
                    Criteria.where("headline").regex(pattern, "i"),
                    Criteria.where("bio").regex(pattern, "i"),
                    Criteria.where("industry").regex(pattern, "i"),
                    Criteria.where("location").regex(pattern, "i"),
                    Criteria.where("skills").regex(pattern, "i"),
                    Criteria.where("interests").regex(pattern, "i")
            ));
        }

        if (hasText(skill)) {
            filters.add(
                    Criteria.where("skills")
                            .regex(regex(skill.trim()), "i")
            );
        }

        if (hasText(industry)) {
            filters.add(
                    Criteria.where("industry")
                            .regex(regex(industry.trim()), "i")
            );
        }

        if (hasText(location)) {
            filters.add(
                    Criteria.where("location")
                            .regex(regex(location.trim()), "i")
            );
        }

        Query query = new Query(
                new Criteria().andOperator(
                        filters.toArray(new Criteria[0])
                )
        );

        long total = mongoTemplate.count(query, Profile.class);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        query.with(pageable);

        List<Profile> profiles =
                mongoTemplate.find(query, Profile.class);

        List<DiscoveryProfileResponse> responses =
                enrichProfiles(profiles);

        return new PageImpl<>(responses, pageable, total);
    }

    @GetMapping("/profiles/{userId}")
    public DiscoveryProfileResponse getPublicProfile(
            @PathVariable String userId,
            Authentication auth) {

        validateAuthentication(auth);

        Query query = new Query(
                Criteria.where("userId").is(userId)
                        .and("visibility").is("PUBLIC")
        );

        Profile profile = mongoTemplate.findOne(query, Profile.class);

        if (profile == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Public profile not found"
            );
        }

        return enrichProfile(profile);
    }

    private List<DiscoveryProfileResponse> enrichProfiles(
            List<Profile> profiles) {

        if (profiles == null || profiles.isEmpty()) {
            return List.of();
        }

        List<String> userIds = profiles.stream()
                .map(profile -> profile.getUserId())
                .filter(this::hasText)
                .distinct()
                .toList();

        Map<String, UserAccount> users =
                userRepository.findAllById(userIds)
                        .stream()
                        .collect(Collectors.toMap(
                                user -> user.getId(),
                                Function.identity()
                        ));

        return profiles.stream()
                .map(profile -> {
                    UserAccount user = users.get(profile.getUserId());

                    String displayName = user == null
                            ? null
                            : user.getDisplayName();

                    return DiscoveryProfileResponse.from(
                            profile,
                            displayName
                    );
                })
                .toList();
    }

    private DiscoveryProfileResponse enrichProfile(Profile profile) {

        UserAccount user = userRepository.findById(profile.getUserId())
                .orElse(null);

        String displayName = user == null
                ? null
                : user.getDisplayName();

        return DiscoveryProfileResponse.from(
                profile,
                displayName
        );
    }

    private void validateAuthentication(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String regex(String value) {
        return Pattern.quote(value);
    }
}
