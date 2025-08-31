package com.tosk.app.task;

import com.tosk.app.task.dto.CreateTaskRequestDTO;
import com.tosk.app.task.dto.TaskResponseDTO;
import com.tosk.app.task.dto.UpdateTaskRequestDTO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@Validated
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @PostMapping
  public ResponseEntity<TaskResponseDTO> createTask(
      @Valid @RequestBody CreateTaskRequestDTO request) {
    UUID userId = getCurrentUserId();
    TaskResponseDTO task = taskService.createTask(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(task);
  }

  @GetMapping("/{taskId}")
  public ResponseEntity<TaskResponseDTO> getTask(@PathVariable UUID taskId) {
    UUID userId = getCurrentUserId();
    TaskResponseDTO task = taskService.getTaskById(taskId, userId);
    return ResponseEntity.ok(task);
  }

  @GetMapping
  public ResponseEntity<Page<TaskResponseDTO>> getTasks(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) TaskEntity.Priority priority,
      @RequestParam(required = false) TaskEntity.Visibility visibility,
      @RequestParam(required = false) LocalDate dueDateStart,
      @RequestParam(required = false) LocalDate dueDateEnd,
      @RequestParam(required = false) UUID teamId,
      @RequestParam(required = false) Boolean myTasksOnly,
      @PageableDefault(size = 20) Pageable pageable) {

    UUID userId = getCurrentUserId();
    Page<TaskResponseDTO> tasks =
        taskService.getTasks(
            userId,
            keyword,
            priority,
            visibility,
            dueDateStart,
            dueDateEnd,
            teamId,
            myTasksOnly,
            pageable);
    return ResponseEntity.ok(tasks);
  }

  @PutMapping("/{taskId}")
  public ResponseEntity<TaskResponseDTO> updateTask(
      @PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequestDTO request) {
    UUID userId = getCurrentUserId();
    TaskResponseDTO task = taskService.updateTask(taskId, userId, request);
    return ResponseEntity.ok(task);
  }

  @DeleteMapping("/{taskId}")
  public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId) {
    UUID userId = getCurrentUserId();
    taskService.deleteTask(taskId, userId);
    return ResponseEntity.noContent().build();
  }

  private UUID getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("User not authenticated");
    }
    return (UUID) auth.getPrincipal();
  }
}
