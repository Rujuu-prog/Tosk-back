package com.tosk.app.task;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PriorityConverter implements AttributeConverter<TaskEntity.Priority, String> {

  @Override
  public String convertToDatabaseColumn(TaskEntity.Priority priority) {
    if (priority == null) {
      return null;
    }
    return priority.name();
  }

  @Override
  public TaskEntity.Priority convertToEntityAttribute(String value) {
    if (value == null) {
      return null;
    }
    return TaskEntity.Priority.valueOf(value);
  }
}