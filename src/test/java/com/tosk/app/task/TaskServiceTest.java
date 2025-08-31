package com.tosk.app.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tosk.app.task.dto.CreateTaskRequestDTO;
import com.tosk.app.task.dto.TaskResponseDTO;
import com.tosk.app.task.dto.UpdateTaskRequestDTO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock private TaskRepository taskRepository;

  @InjectMocks private TaskService taskService;

  private UUID userId;
  private UUID teamId;
  private UUID taskId;
  private TaskEntity mockTask;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    teamId = UUID.randomUUID();
    taskId = UUID.randomUUID();

    mockTask =
        TaskEntity.builder()
            .id(taskId)
            .userId(userId)
            .teamId(teamId)
            .title("Test Task")
            .description("Test Description")
            .priority(TaskEntity.Priority.MEDIUM)
            .visibility(TaskEntity.Visibility.PRIVATE)
            .likeCount(0)
            .commentCount(0)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
  }

  @Test
  void createTask_shouldCreateAndReturnTask() {
    CreateTaskRequestDTO request =
        CreateTaskRequestDTO.builder()
            .title("New Task")
            .description("Task Description")
            .dueDate(LocalDate.now().plusDays(7))
            .teamId(teamId)
            .priority(TaskEntity.Priority.HIGH)
            .visibility(TaskEntity.Visibility.TEAM)
            .build();

    when(taskRepository.save(any(TaskEntity.class))).thenReturn(mockTask);

    TaskResponseDTO result = taskService.createTask(userId, request);

    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(userId);
    verify(taskRepository).save(any(TaskEntity.class));
  }

  @Test
  void createTask_withTeamVisibilityButNoTeamId_shouldThrowException() {
    CreateTaskRequestDTO request =
        CreateTaskRequestDTO.builder()
            .title("New Task")
            .priority(TaskEntity.Priority.MEDIUM)
            .visibility(TaskEntity.Visibility.TEAM)
            .teamId(null)
            .build();

    assertThatThrownBy(() -> taskService.createTask(userId, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Team ID is required for team visibility");

    verify(taskRepository, never()).save(any(TaskEntity.class));
  }

  @Test
  void getTaskById_existingTask_shouldReturnTask() {
    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(mockTask));

    TaskResponseDTO result = taskService.getTaskById(taskId, userId);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(taskId);
    assertThat(result.getTitle()).isEqualTo("Test Task");
  }

  @Test
  void getTaskById_nonExistingTask_shouldThrowException() {
    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.getTaskById(taskId, userId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Task not found");
  }

  @Test
  void getTaskById_privateTaskByNonOwner_shouldThrowException() {
    UUID anotherUserId = UUID.randomUUID();
    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(mockTask));

    assertThatThrownBy(() -> taskService.getTaskById(taskId, anotherUserId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Access denied");
  }

  @Test
  void updateTask_validUpdate_shouldUpdateAndReturnTask() {
    UpdateTaskRequestDTO request =
        UpdateTaskRequestDTO.builder()
            .title("Updated Task")
            .description("Updated Description")
            .priority(TaskEntity.Priority.HIGH)
            .build();

    TaskEntity updatedTask =
        mockTask.toBuilder()
            .title("Updated Task")
            .description("Updated Description")
            .priority(TaskEntity.Priority.HIGH)
            .build();

    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(mockTask));
    when(taskRepository.save(any(TaskEntity.class))).thenReturn(updatedTask);

    TaskResponseDTO result = taskService.updateTask(taskId, userId, request);

    assertThat(result.getTitle()).isEqualTo("Updated Task");
    assertThat(result.getDescription()).isEqualTo("Updated Description");
    assertThat(result.getPriority()).isEqualTo(TaskEntity.Priority.HIGH);
  }

  @Test
  void updateTask_byNonOwner_shouldThrowException() {
    UUID anotherUserId = UUID.randomUUID();
    UpdateTaskRequestDTO request = UpdateTaskRequestDTO.builder().title("Updated Task").build();

    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(mockTask));

    assertThatThrownBy(() -> taskService.updateTask(taskId, anotherUserId, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Access denied");
  }

  @Test
  void deleteTask_byOwner_shouldSoftDeleteTask() {
    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(mockTask));
    when(taskRepository.save(any(TaskEntity.class))).thenReturn(mockTask);

    taskService.deleteTask(taskId, userId);

    verify(taskRepository).save(any(TaskEntity.class));
  }

  @Test
  void deleteTask_byNonOwner_shouldThrowException() {
    UUID anotherUserId = UUID.randomUUID();
    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(mockTask));

    assertThatThrownBy(() -> taskService.deleteTask(taskId, anotherUserId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Access denied");
  }

  @Test
  void getTasks_myTasksOnly_shouldReturnUserTasks() {
    Page<TaskEntity> mockPage = new PageImpl<>(java.util.List.of(mockTask));
    when(taskRepository.findByUserIdAndDeletedAtIsNull(eq(userId), any(Pageable.class)))
        .thenReturn(mockPage);

    Page<TaskResponseDTO> result =
        taskService.getTasks(userId, null, null, null, null, null, null, true, Pageable.unpaged());

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getUserId()).isEqualTo(userId);
  }

  @Test
  void getTasks_withKeyword_shouldReturnFilteredTasks() {
    Page<TaskEntity> mockPage = new PageImpl<>(java.util.List.of(mockTask));
    when(taskRepository.findByKeywordAndDeletedAtIsNull(eq("test"), any(Pageable.class)))
        .thenReturn(mockPage);

    Page<TaskResponseDTO> result =
        taskService.getTasks(
            userId, "test", null, null, null, null, null, false, Pageable.unpaged());

    assertThat(result.getContent()).hasSize(1);
    verify(taskRepository).findByKeywordAndDeletedAtIsNull(eq("test"), any(Pageable.class));
  }

  @Test
  void getTasks_withPriority_shouldReturnFilteredTasks() {
    Page<TaskEntity> mockPage = new PageImpl<>(java.util.List.of(mockTask));
    when(taskRepository.findByPriorityAndDeletedAtIsNull(
            eq(TaskEntity.Priority.HIGH), any(Pageable.class)))
        .thenReturn(mockPage);

    Page<TaskResponseDTO> result =
        taskService.getTasks(
            userId,
            null,
            TaskEntity.Priority.HIGH,
            null,
            null,
            null,
            null,
            false,
            Pageable.unpaged());

    assertThat(result.getContent()).hasSize(1);
    verify(taskRepository)
        .findByPriorityAndDeletedAtIsNull(eq(TaskEntity.Priority.HIGH), any(Pageable.class));
  }
}
