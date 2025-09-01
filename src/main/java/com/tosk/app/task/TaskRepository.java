package com.tosk.app.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

  Optional<TaskEntity> findByIdAndDeletedAtIsNull(UUID id);

  @Query("SELECT t FROM TaskEntity t WHERE t.deletedAt IS NULL ORDER BY t.updatedAt DESC")
  Page<TaskEntity> findAllActiveOrderByUpdatedAtDesc(Pageable pageable);

  @Query("SELECT t FROM TaskEntity t WHERE t.userId = :userId AND t.deletedAt IS NULL")
  Page<TaskEntity> findByUserIdAndDeletedAtIsNull(@Param("userId") UUID userId, Pageable pageable);

  @Query(
      "SELECT t FROM TaskEntity t WHERE t.visibility = com.tosk.app.task.TaskEntity$Visibility.public_ AND t.deletedAt IS NULL ORDER BY"
          + " t.updatedAt DESC")
  Page<TaskEntity> findPublicTasksOrderByUpdatedAtDesc(Pageable pageable);

  @Query(
      "SELECT t FROM TaskEntity t WHERE t.teamId = :teamId AND (t.visibility = com.tosk.app.task.TaskEntity$Visibility.team OR t.visibility = com.tosk.app.task.TaskEntity$Visibility.public_)"
          + " AND t.deletedAt IS NULL ORDER BY t.updatedAt DESC")
  Page<TaskEntity> findByTeamIdAndVisibilityInOrderByUpdatedAtDesc(
      @Param("teamId") UUID teamId, Pageable pageable);

  @Query(
      "SELECT t FROM TaskEntity t WHERE (t.title LIKE %:keyword% OR t.description LIKE"
          + " %:keyword%) AND t.deletedAt IS NULL ORDER BY t.updatedAt DESC")
  Page<TaskEntity> findByKeywordAndDeletedAtIsNull(
      @Param("keyword") String keyword, Pageable pageable);

  @Query(
      "SELECT t FROM TaskEntity t WHERE t.priority = :priority AND t.deletedAt IS NULL ORDER BY"
          + " t.updatedAt DESC")
  Page<TaskEntity> findByPriorityAndDeletedAtIsNull(
      @Param("priority") TaskEntity.Priority priority, Pageable pageable);

  @Query(
      "SELECT t FROM TaskEntity t WHERE t.visibility = :visibility AND t.deletedAt IS NULL ORDER"
          + " BY t.updatedAt DESC")
  Page<TaskEntity> findByVisibilityAndDeletedAtIsNull(
      @Param("visibility") TaskEntity.Visibility visibility, Pageable pageable);

  @Query(
      "SELECT t FROM TaskEntity t WHERE t.dueDate >= :startDate AND t.dueDate <= :endDate AND"
          + " t.deletedAt IS NULL ORDER BY t.dueDate ASC")
  Page<TaskEntity> findByDueDateBetweenAndDeletedAtIsNull(
      @Param("startDate") java.time.LocalDate startDate,
      @Param("endDate") java.time.LocalDate endDate,
      Pageable pageable);

  List<TaskEntity> findByUserIdAndDeletedAtIsNull(UUID userId);

  long countByUserIdAndDeletedAtIsNull(UUID userId);

  void deleteById(UUID id);
}
