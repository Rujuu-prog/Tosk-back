package com.tosk.app.task.dto;

import com.tosk.app.task.TaskEntity;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UpdateTaskRequestDTO {

  @Size(min = 1, max = 120)
  private String title;

  @Size(max = 10000)
  private String description;

  private LocalDate dueDate;

  private UUID teamId;

  private TaskEntity.Priority priority;

  private TaskEntity.Visibility visibility;
}
