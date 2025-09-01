package com.tosk.app.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "tasks")
public class TaskEntity {

  @Id
  @Column(name = "id", nullable = false, columnDefinition = "uuid")
  private UUID id;

  @Column(name = "author_id", nullable = false, columnDefinition = "uuid")
  private UUID userId;

  @Column(name = "team_id", columnDefinition = "uuid")
  private UUID teamId;

  @Column(name = "title", nullable = false, length = 120)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Convert(converter = PriorityConverter.class)
  @Column(name = "priority", nullable = false, columnDefinition = "task_priority")
  @Builder.Default
  private Priority priority = Priority.medium;

  @Convert(converter = VisibilityConverter.class)
  @Column(name = "visibility", nullable = false, columnDefinition = "task_visibility")
  @Builder.Default
  private Visibility visibility = Visibility.private_;

  @Column(name = "like_count", nullable = false)
  @Builder.Default
  private Integer likeCount = 0;

  @Column(name = "comment_count", nullable = false)
  @Builder.Default
  private Integer commentCount = 0;

  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  public enum Priority {
    low,
    medium,
    high;

    @JsonCreator
    public static Priority fromString(String value) {
      return switch (value.toLowerCase()) {
        case "low" -> low;
        case "medium" -> medium;
        case "high" -> high;
        default -> throw new IllegalArgumentException("Unknown priority: " + value);
      };
    }

    @JsonValue
    public String toValue() {
      return name();
    }
  }

  public enum Visibility {
    private_,
    team,
    public_;

    @JsonCreator
    public static Visibility fromString(String value) {
      return switch (value.toLowerCase()) {
        case "private" -> private_;
        case "team" -> team;
        case "public" -> public_;
        default -> throw new IllegalArgumentException("Unknown visibility: " + value);
      };
    }

    @JsonValue
    public String toValue() {
      return switch (this) {
        case private_ -> "private";
        case team -> "team";
        case public_ -> "public";
      };
    }
  }

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
    if (likeCount == null) {
      likeCount = 0;
    }
    if (commentCount == null) {
      commentCount = 0;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
