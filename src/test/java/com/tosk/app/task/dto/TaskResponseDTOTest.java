package com.tosk.app.task.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.tosk.app.task.TaskEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskResponseDTOTest {

  @Test
  void from_shouldConvertEntityToDTO() {
    UUID taskId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();
    LocalDate dueDate = LocalDate.now().plusDays(7);
    OffsetDateTime createdAt = OffsetDateTime.now().minusHours(1);
    OffsetDateTime updatedAt = OffsetDateTime.now();

    TaskEntity entity =
        TaskEntity.builder()
            .id(taskId)
            .userId(userId)
            .teamId(teamId)
            .title("Test Task")
            .description("Test Description")
            .dueDate(dueDate)
            .priority(TaskEntity.Priority.HIGH)
            .visibility(TaskEntity.Visibility.TEAM)
            .likeCount(5)
            .commentCount(3)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();

    TaskResponseDTO dto = TaskResponseDTO.from(entity);

    assertThat(dto.getId()).isEqualTo(taskId);
    assertThat(dto.getUserId()).isEqualTo(userId);
    assertThat(dto.getTeamId()).isEqualTo(teamId);
    assertThat(dto.getTitle()).isEqualTo("Test Task");
    assertThat(dto.getDescription()).isEqualTo("Test Description");
    assertThat(dto.getDueDate()).isEqualTo(dueDate);
    assertThat(dto.getPriority()).isEqualTo(TaskEntity.Priority.HIGH);
    assertThat(dto.getVisibility()).isEqualTo(TaskEntity.Visibility.TEAM);
    assertThat(dto.getLikeCount()).isEqualTo(5);
    assertThat(dto.getCommentCount()).isEqualTo(3);
    assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
    assertThat(dto.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void from_withNullFields_shouldHandleNulls() {
    UUID taskId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    TaskEntity entity =
        TaskEntity.builder()
            .id(taskId)
            .userId(userId)
            .teamId(null)
            .title("Minimal Task")
            .description(null)
            .dueDate(null)
            .priority(TaskEntity.Priority.MEDIUM)
            .visibility(TaskEntity.Visibility.PRIVATE)
            .likeCount(0)
            .commentCount(0)
            .build();

    TaskResponseDTO dto = TaskResponseDTO.from(entity);

    assertThat(dto.getId()).isEqualTo(taskId);
    assertThat(dto.getUserId()).isEqualTo(userId);
    assertThat(dto.getTeamId()).isNull();
    assertThat(dto.getTitle()).isEqualTo("Minimal Task");
    assertThat(dto.getDescription()).isNull();
    assertThat(dto.getDueDate()).isNull();
    assertThat(dto.getPriority()).isEqualTo(TaskEntity.Priority.MEDIUM);
    assertThat(dto.getVisibility()).isEqualTo(TaskEntity.Visibility.PRIVATE);
    assertThat(dto.getLikeCount()).isEqualTo(0);
    assertThat(dto.getCommentCount()).isEqualTo(0);
  }

  @Test
  void builder_shouldCreateDTOCorrectly() {
    UUID taskId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    LocalDate dueDate = LocalDate.now().plusDays(5);
    OffsetDateTime now = OffsetDateTime.now();

    TaskResponseDTO dto =
        TaskResponseDTO.builder()
            .id(taskId)
            .userId(userId)
            .title("Built Task")
            .description("Built with builder")
            .dueDate(dueDate)
            .priority(TaskEntity.Priority.LOW)
            .visibility(TaskEntity.Visibility.PUBLIC)
            .likeCount(10)
            .commentCount(2)
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertThat(dto.getId()).isEqualTo(taskId);
    assertThat(dto.getUserId()).isEqualTo(userId);
    assertThat(dto.getTitle()).isEqualTo("Built Task");
    assertThat(dto.getDescription()).isEqualTo("Built with builder");
    assertThat(dto.getDueDate()).isEqualTo(dueDate);
    assertThat(dto.getPriority()).isEqualTo(TaskEntity.Priority.LOW);
    assertThat(dto.getVisibility()).isEqualTo(TaskEntity.Visibility.PUBLIC);
    assertThat(dto.getLikeCount()).isEqualTo(10);
    assertThat(dto.getCommentCount()).isEqualTo(2);
  }
}
