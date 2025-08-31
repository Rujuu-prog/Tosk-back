package com.tosk.app.task;

import com.tosk.app.task.dto.CreateTaskRequestDTO;
import com.tosk.app.task.dto.TaskResponseDTO;
import com.tosk.app.task.dto.UpdateTaskRequestDTO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {

  private final TaskRepository taskRepository;

  public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Transactional
  public TaskResponseDTO createTask(UUID userId, CreateTaskRequestDTO request) {
    validateTaskVisibility(request.getVisibility(), request.getTeamId());

    TaskEntity task =
        TaskEntity.builder()
            .userId(userId)
            .title(request.getTitle())
            .description(request.getDescription())
            .dueDate(request.getDueDate())
            .teamId(request.getTeamId())
            .priority(request.getPriority())
            .visibility(request.getVisibility())
            .build();

    TaskEntity saved = taskRepository.save(task);
    return TaskResponseDTO.from(saved);
  }

  public TaskResponseDTO getTaskById(UUID taskId, UUID requestUserId) {
    TaskEntity task =
        taskRepository
            .findByIdAndDeletedAtIsNull(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    if (!canViewTask(task, requestUserId)) {
      throw new IllegalArgumentException("Access denied to task: " + taskId);
    }

    return TaskResponseDTO.from(task);
  }

  public Page<TaskResponseDTO> getTasks(
      UUID requestUserId,
      String keyword,
      TaskEntity.Priority priority,
      TaskEntity.Visibility visibility,
      LocalDate dueDateStart,
      LocalDate dueDateEnd,
      UUID teamId,
      Boolean myTasksOnly,
      Pageable pageable) {

    Page<TaskEntity> tasks;

    if (Boolean.TRUE.equals(myTasksOnly)) {
      tasks = taskRepository.findByUserIdAndDeletedAtIsNull(requestUserId, pageable);
    } else if (keyword != null && !keyword.trim().isEmpty()) {
      tasks = taskRepository.findByKeywordAndDeletedAtIsNull(keyword, pageable);
    } else if (priority != null) {
      tasks = taskRepository.findByPriorityAndDeletedAtIsNull(priority, pageable);
    } else if (visibility != null) {
      tasks = taskRepository.findByVisibilityAndDeletedAtIsNull(visibility, pageable);
    } else if (dueDateStart != null && dueDateEnd != null) {
      tasks =
          taskRepository.findByDueDateBetweenAndDeletedAtIsNull(dueDateStart, dueDateEnd, pageable);
    } else if (teamId != null) {
      tasks = taskRepository.findByTeamIdAndVisibilityInOrderByUpdatedAtDesc(teamId, pageable);
    } else {
      tasks = taskRepository.findAllActiveOrderByUpdatedAtDesc(pageable);
    }

    return tasks.map(TaskResponseDTO::from);
  }

  @Transactional
  public TaskResponseDTO updateTask(UUID taskId, UUID requestUserId, UpdateTaskRequestDTO request) {
    TaskEntity task =
        taskRepository
            .findByIdAndDeletedAtIsNull(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    if (!canEditTask(task, requestUserId)) {
      throw new IllegalArgumentException("Access denied to edit task: " + taskId);
    }

    if (request.getTitle() != null) {
      task.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      task.setDescription(request.getDescription());
    }
    if (request.getDueDate() != null) {
      task.setDueDate(request.getDueDate());
    }
    if (request.getTeamId() != null) {
      task.setTeamId(request.getTeamId());
    }
    if (request.getPriority() != null) {
      task.setPriority(request.getPriority());
    }
    if (request.getVisibility() != null) {
      validateTaskVisibility(request.getVisibility(), task.getTeamId());
      task.setVisibility(request.getVisibility());
    }

    TaskEntity updated = taskRepository.save(task);
    return TaskResponseDTO.from(updated);
  }

  @Transactional
  public void deleteTask(UUID taskId, UUID requestUserId) {
    TaskEntity task =
        taskRepository
            .findByIdAndDeletedAtIsNull(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    if (!canDeleteTask(task, requestUserId)) {
      throw new IllegalArgumentException("Access denied to delete task: " + taskId);
    }

    task.setDeletedAt(OffsetDateTime.now());
    taskRepository.save(task);
  }

  private boolean canViewTask(TaskEntity task, UUID requestUserId) {
    return switch (task.getVisibility()) {
      case PRIVATE -> task.getUserId().equals(requestUserId);
      case TEAM -> task.getUserId().equals(requestUserId)
          || isTeamMember(task.getTeamId(), requestUserId);
      case PUBLIC -> true;
    };
  }

  private boolean canEditTask(TaskEntity task, UUID requestUserId) {
    if (task.getUserId().equals(requestUserId)) {
      return true;
    }
    return task.getVisibility() == TaskEntity.Visibility.TEAM
        && isTeamLeader(task.getTeamId(), requestUserId);
  }

  private boolean canDeleteTask(TaskEntity task, UUID requestUserId) {
    return task.getUserId().equals(requestUserId)
        || (task.getVisibility() == TaskEntity.Visibility.TEAM
            && isTeamLeader(task.getTeamId(), requestUserId));
  }

  private boolean isTeamMember(UUID teamId, UUID userId) {
    return true;
  }

  private boolean isTeamLeader(UUID teamId, UUID userId) {
    return true;
  }

  private void validateTaskVisibility(TaskEntity.Visibility visibility, UUID teamId) {
    if (visibility == TaskEntity.Visibility.TEAM && teamId == null) {
      throw new IllegalArgumentException("Team ID is required for team visibility");
    }
  }
}
