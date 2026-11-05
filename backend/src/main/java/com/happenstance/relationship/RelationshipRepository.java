
package com.happenstance.relationship;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RelationshipRepository
        extends MongoRepository<Relationship, String> {

    List<Relationship> findBySourceUserIdAndStatus(
            String userId, String status);

    List<Relationship> findByTargetUserIdAndStatus(
            String userId, String status);

    Optional<Relationship> findBySourceUserIdAndTargetUserId(
            String sourceUserId, String targetUserId);

    boolean existsBySourceUserIdAndTargetUserIdAndStatus(
            String sourceUserId, String targetUserId, String status);

    List<Relationship> findByStatus(String status);
}
