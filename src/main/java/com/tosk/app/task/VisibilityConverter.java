package com.tosk.app.task;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class VisibilityConverter implements AttributeConverter<TaskEntity.Visibility, String> {

  @Override
  public String convertToDatabaseColumn(TaskEntity.Visibility visibility) {
    if (visibility == null) {
      return null;
    }
    return visibility.toValue();
  }

  @Override
  public TaskEntity.Visibility convertToEntityAttribute(String value) {
    if (value == null) {
      return null;
    }
    return TaskEntity.Visibility.fromString(value);
  }
}