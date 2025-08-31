package com.tosk.app.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskEntityTest {

  private TaskEntity task;
  private UUID userId;
  private UUID teamId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    teamId = UUID.randomUUID();
  }

  @Test
  void prePersist_shouldSetDefaultValues() {
    task = TaskEntity.builder().userId(userId).title("Test Task").build();

    task.prePersist();

    assertThat(task.getId()).isNotNull();
    assertThat(task.getCreatedAt()).isNotNull();
    assertThat(task.getUpdatedAt()).isNotNull();
    assertThat(task.getLikeCount()).isEqualTo(0);
    assertThat(task.getCommentCount()).isEqualTo(0);
    assertThat(task.getPriority()).isEqualTo(TaskEntity.Priority.MEDIUM);
    assertThat(task.getVisibility()).isEqualTo(TaskEntity.Visibility.PRIVATE);
  }

  @Test
  void preUpdate_shouldUpdateTimestamp() throws InterruptedException {
    task = TaskEntity.builder().id(UUID.randomUUID()).userId(userId).title("Test Task").build();

    OffsetDateTime initialTime = OffsetDateTime.now().minusHours(1);
    task.setUpdatedAt(initialTime);

    Thread.sleep(10);
    task.preUpdate();

    assertThat(task.getUpdatedAt()).isAfter(initialTime);
  }

  @Test
  void builder_shouldCreateTaskWithAllFields() {
    LocalDate dueDate = LocalDate.now().plusDays(7);

    task =
        TaskEntity.builder()
            .userId(userId)
            .teamId(teamId)
            .title("Complete Project")
            .description("Finish the project implementation")
            .dueDate(dueDate)
            .priority(TaskEntity.Priority.HIGH)
            .visibility(TaskEntity.Visibility.TEAM)
            .likeCount(5)
            .commentCount(3)
            .build();

    assertThat(task.getUserId()).isEqualTo(userId);
    assertThat(task.getTeamId()).isEqualTo(teamId);
    assertThat(task.getTitle()).isEqualTo("Complete Project");
    assertThat(task.getDescription()).isEqualTo("Finish the project implementation");
    assertThat(task.getDueDate()).isEqualTo(dueDate);
    assertThat(task.getPriority()).isEqualTo(TaskEntity.Priority.HIGH);
    assertThat(task.getVisibility()).isEqualTo(TaskEntity.Visibility.TEAM);
    assertThat(task.getLikeCount()).isEqualTo(5);
    assertThat(task.getCommentCount()).isEqualTo(3);
  }

  @Test
  void priority_shouldHaveCorrectValues() {
    assertThat(TaskEntity.Priority.LOW).isEqualTo(TaskEntity.Priority.LOW);
    assertThat(TaskEntity.Priority.MEDIUM).isEqualTo(TaskEntity.Priority.MEDIUM);
    assertThat(TaskEntity.Priority.HIGH).isEqualTo(TaskEntity.Priority.HIGH);
  }

  @Test
  void visibility_shouldHaveCorrectValues() {
    assertThat(TaskEntity.Visibility.PRIVATE).isEqualTo(TaskEntity.Visibility.PRIVATE);
    assertThat(TaskEntity.Visibility.TEAM).isEqualTo(TaskEntity.Visibility.TEAM);
    assertThat(TaskEntity.Visibility.PUBLIC).isEqualTo(TaskEntity.Visibility.PUBLIC);
  }
}
