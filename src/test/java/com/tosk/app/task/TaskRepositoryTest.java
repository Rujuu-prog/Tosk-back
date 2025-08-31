package com.tosk.app.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tosk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.jpa.show-sql=false"
    })
class TaskRepositoryTest {

  @Autowired private TaskRepository taskRepository;

  private UUID userId1;
  private UUID userId2;
  private UUID teamId;
  private TaskEntity task1;
  private TaskEntity task2;
  private TaskEntity task3;

  @BeforeEach
  void setUp() {
    userId1 = UUID.randomUUID();
    userId2 = UUID.randomUUID();
    teamId = UUID.randomUUID();

    task1 =
        TaskEntity.builder()
            .userId(userId1)
            .teamId(teamId)
            .title("High Priority Task")
            .description("Important task description")
            .dueDate(LocalDate.now().plusDays(5))
            .priority(TaskEntity.Priority.HIGH)
            .visibility(TaskEntity.Visibility.PUBLIC)
            .build();
    task1.prePersist();

    task2 =
        TaskEntity.builder()
            .userId(userId1)
            .title("Private Task")
            .description("Private task description")
            .dueDate(LocalDate.now().plusDays(10))
            .priority(TaskEntity.Priority.MEDIUM)
            .visibility(TaskEntity.Visibility.PRIVATE)
            .build();
    task2.prePersist();

    task3 =
        TaskEntity.builder()
            .userId(userId2)
            .teamId(teamId)
            .title("Team Task")
            .description("Team collaboration task")
            .dueDate(LocalDate.now().plusDays(3))
            .priority(TaskEntity.Priority.LOW)
            .visibility(TaskEntity.Visibility.TEAM)
            .build();
    task3.prePersist();

    taskRepository.saveAll(List.of(task1, task2, task3));
  }

  @Test
  void findByIdAndDeletedAtIsNull_existingTask_shouldReturnTask() {
    Optional<TaskEntity> result = taskRepository.findByIdAndDeletedAtIsNull(task1.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getTitle()).isEqualTo("High Priority Task");
  }

  @Test
  void findByIdAndDeletedAtIsNull_deletedTask_shouldReturnEmpty() {
    task1.setDeletedAt(OffsetDateTime.now());
    taskRepository.save(task1);

    Optional<TaskEntity> result = taskRepository.findByIdAndDeletedAtIsNull(task1.getId());

    assertThat(result).isEmpty();
  }

  @Test
  void findAllActiveOrderByUpdatedAtDesc_shouldReturnActiveTasksOrderedByUpdatedAt() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<TaskEntity> result = taskRepository.findAllActiveOrderByUpdatedAtDesc(pageable);

    assertThat(result.getContent()).hasSize(3);
    assertThat(result.getContent()).extracting(TaskEntity::getDeletedAt).containsOnlyNulls();
  }

  @Test
  void findByUserIdAndDeletedAtIsNull_shouldReturnUserTasks() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<TaskEntity> result = taskRepository.findByUserIdAndDeletedAtIsNull(userId1, pageable);

    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent()).allMatch(task -> task.getUserId().equals(userId1));
  }

  @Test
  void findPublicTasksOrderByUpdatedAtDesc_shouldReturnOnlyPublicTasks() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<TaskEntity> result = taskRepository.findPublicTasksOrderByUpdatedAtDesc(pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getVisibility()).isEqualTo(TaskEntity.Visibility.PUBLIC);
  }

  @Test
  void findByTeamIdAndVisibilityInOrderByUpdatedAtDesc_shouldReturnTeamAndPublicTasks() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<TaskEntity> result =
        taskRepository.findByTeamIdAndVisibilityInOrderByUpdatedAtDesc(teamId, pageable);

    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .extracting(TaskEntity::getVisibility)
        .containsExactlyInAnyOrder(TaskEntity.Visibility.PUBLIC, TaskEntity.Visibility.TEAM);
  }

  @Test
  void findByKeywordAndDeletedAtIsNull_shouldReturnMatchingTasks() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<TaskEntity> result =
        taskRepository.findByKeywordAndDeletedAtIsNull("High Priority", pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getTitle()).contains("High Priority");
  }

  @Test
  void findByKeywordAndDeletedAtIsNull_descriptionSearch_shouldReturnMatchingTasks() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<TaskEntity> result =
        taskRepository.findByKeywordAndDeletedAtIsNull("collaboration", pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getDescription()).contains("collaboration");
  }

  @Test
  void findByPriorityAndDeletedAtIsNull_shouldReturnTasksWithSpecificPriority() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<TaskEntity> result =
        taskRepository.findByPriorityAndDeletedAtIsNull(TaskEntity.Priority.HIGH, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getPriority()).isEqualTo(TaskEntity.Priority.HIGH);
  }

  @Test
  void findByVisibilityAndDeletedAtIsNull_shouldReturnTasksWithSpecificVisibility() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<TaskEntity> result =
        taskRepository.findByVisibilityAndDeletedAtIsNull(TaskEntity.Visibility.PRIVATE, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getVisibility()).isEqualTo(TaskEntity.Visibility.PRIVATE);
  }

  @Test
  void findByDueDateBetweenAndDeletedAtIsNull_shouldReturnTasksInDateRange() {
    LocalDate startDate = LocalDate.now().plusDays(1);
    LocalDate endDate = LocalDate.now().plusDays(6);
    Pageable pageable = PageRequest.of(0, 10);

    Page<TaskEntity> result =
        taskRepository.findByDueDateBetweenAndDeletedAtIsNull(startDate, endDate, pageable);

    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .allMatch(
            task ->
                task.getDueDate().isAfter(startDate.minusDays(1))
                    && task.getDueDate().isBefore(endDate.plusDays(1)));
  }

  @Test
  void findByUserIdAndDeletedAtIsNull_listMethod_shouldReturnUserTasksList() {
    List<TaskEntity> result = taskRepository.findByUserIdAndDeletedAtIsNull(userId1);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(task -> task.getUserId().equals(userId1));
  }

  @Test
  void countByUserIdAndDeletedAtIsNull_shouldReturnCorrectCount() {
    long count = taskRepository.countByUserIdAndDeletedAtIsNull(userId1);

    assertThat(count).isEqualTo(2);
  }

  @Test
  void findByKeywordAndDeletedAtIsNull_noMatches_shouldReturnEmptyPage() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<TaskEntity> result =
        taskRepository.findByKeywordAndDeletedAtIsNull("nonexistent", pageable);

    assertThat(result.getContent()).isEmpty();
  }
}
