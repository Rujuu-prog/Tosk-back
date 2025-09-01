package com.tosk.app.like;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, UUID> {

  @Query(
      "SELECT l FROM LikeEntity l WHERE l.user.id = :userId AND l.targetType = :targetType AND l.targetId = :targetId")
  Optional<LikeEntity> findByUserIdAndTargetTypeAndTargetId(
      @Param("userId") UUID userId,
      @Param("targetType") LikeEntity.TargetType targetType,
      @Param("targetId") UUID targetId);

  @Query(
      "SELECT COUNT(l) FROM LikeEntity l WHERE l.targetType = :targetType AND l.targetId = :targetId")
  Long countByTargetTypeAndTargetId(
      @Param("targetType") LikeEntity.TargetType targetType, @Param("targetId") UUID targetId);
}
