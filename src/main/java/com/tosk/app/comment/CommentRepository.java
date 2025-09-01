package com.tosk.app.comment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

  @Query(
      "SELECT c FROM CommentEntity c WHERE c.task.id = :taskId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
  Page<CommentEntity> findByTaskIdAndNotDeleted(@Param("taskId") UUID taskId, Pageable pageable);

  @Query("SELECT c FROM CommentEntity c WHERE c.id = :commentId AND c.deletedAt IS NULL")
  Optional<CommentEntity> findByIdAndNotDeleted(@Param("commentId") UUID commentId);

  @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.task.id = :taskId AND c.deletedAt IS NULL")
  Long countByTaskIdAndNotDeleted(@Param("taskId") UUID taskId);
}
