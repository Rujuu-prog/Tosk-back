package com.tosk.app.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tosk.app.comment.CommentEntity;
import com.tosk.app.comment.CommentRepository;
import com.tosk.app.like.dto.LikeResponseDTO;
import com.tosk.app.task.TaskEntity;
import com.tosk.app.task.TaskRepository;
import com.tosk.app.user.UserEntity;
import com.tosk.app.user.UserService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

  @Mock LikeRepository likeRepository;
  @Mock TaskRepository taskRepository;
  @Mock CommentRepository commentRepository;
  @Mock UserService userService;

  @InjectMocks LikeService likeService;

  private UUID userId;
  private UUID taskId;
  private UUID commentId;
  private UserEntity userEntity;
  private TaskEntity taskEntity;
  private CommentEntity commentEntity;
  private LikeEntity likeEntity;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    taskId = UUID.randomUUID();
    commentId = UUID.randomUUID();

    userEntity =
        UserEntity.builder().id(userId).username("testuser").email("testuser@example.com").build();

    taskEntity =
        TaskEntity.builder()
            .id(taskId)
            .title("Test Task")
            .userId(userId)
            .visibility(TaskEntity.Visibility.PUBLIC)
            .likeCount(0)
            .build();

    commentEntity =
        CommentEntity.builder()
            .id(commentId)
            .task(taskEntity)
            .user(userEntity)
            .content("Test comment")
            .likeCount(0)
            .build();

    likeEntity =
        LikeEntity.builder()
            .id(UUID.randomUUID())
            .user(userEntity)
            .targetType(LikeEntity.TargetType.TASK)
            .targetId(taskId)
            .createdAt(LocalDateTime.now())
            .build();
  }

  @Test
  void toggleTaskLike_whenNoExistingLike_shouldCreateLike() {
    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(taskEntity));
    when(userService.getUserById(userId)).thenReturn(userEntity);
    when(likeRepository.findByUserIdAndTargetTypeAndTargetId(
            userId, LikeEntity.TargetType.TASK, taskId))
        .thenReturn(Optional.empty());
    when(likeRepository.save(any(LikeEntity.class))).thenReturn(likeEntity);
    when(taskRepository.save(any(TaskEntity.class))).thenReturn(taskEntity);

    LikeResponseDTO result = likeService.toggleTaskLike(taskId, userId);

    assertThat(result).isNotNull();
    assertThat(result.getTargetType()).isEqualTo(LikeEntity.TargetType.TASK);
    assertThat(result.getTargetId()).isEqualTo(taskId);
    assertThat(result.getUserId()).isEqualTo(userId);

    verify(likeRepository).save(any(LikeEntity.class));
    verify(taskRepository).save(any(TaskEntity.class));
  }

  @Test
  void toggleTaskLike_whenExistingLike_shouldRemoveLike() {
    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(taskEntity));
    when(likeRepository.findByUserIdAndTargetTypeAndTargetId(
            userId, LikeEntity.TargetType.TASK, taskId))
        .thenReturn(Optional.of(likeEntity));

    LikeResponseDTO result = likeService.toggleTaskLike(taskId, userId);

    assertThat(result).isNull();

    verify(likeRepository).delete(likeEntity);
    verify(taskRepository).save(any(TaskEntity.class));
  }

  @Test
  void toggleTaskLike_whenTaskNotFound_shouldThrowException() {
    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> likeService.toggleTaskLike(taskId, userId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Task not found");
  }

  @Test
  void toggleCommentLike_whenNoExistingLike_shouldCreateLike() {
    LikeEntity commentLike =
        LikeEntity.builder()
            .id(UUID.randomUUID())
            .user(userEntity)
            .targetType(LikeEntity.TargetType.COMMENT)
            .targetId(commentId)
            .createdAt(LocalDateTime.now())
            .build();

    when(commentRepository.findByIdAndNotDeleted(commentId)).thenReturn(Optional.of(commentEntity));
    when(userService.getUserById(userId)).thenReturn(userEntity);
    when(likeRepository.findByUserIdAndTargetTypeAndTargetId(
            userId, LikeEntity.TargetType.COMMENT, commentId))
        .thenReturn(Optional.empty());
    when(likeRepository.save(any(LikeEntity.class))).thenReturn(commentLike);
    when(commentRepository.save(any(CommentEntity.class))).thenReturn(commentEntity);

    LikeResponseDTO result = likeService.toggleCommentLike(commentId, userId);

    assertThat(result).isNotNull();
    assertThat(result.getTargetType()).isEqualTo(LikeEntity.TargetType.COMMENT);
    assertThat(result.getTargetId()).isEqualTo(commentId);
    assertThat(result.getUserId()).isEqualTo(userId);

    verify(likeRepository).save(any(LikeEntity.class));
    verify(commentRepository).save(any(CommentEntity.class));
  }

  @Test
  void removeLike_byOwner_shouldDeleteLike() {
    when(likeRepository.findById(likeEntity.getId())).thenReturn(Optional.of(likeEntity));

    likeService.removeLike(likeEntity.getId(), userId);

    verify(likeRepository).delete(likeEntity);
  }

  @Test
  void removeLike_byNonOwner_shouldThrowException() {
    UUID anotherUserId = UUID.randomUUID();
    when(likeRepository.findById(likeEntity.getId())).thenReturn(Optional.of(likeEntity));

    assertThatThrownBy(() -> likeService.removeLike(likeEntity.getId(), anotherUserId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("You can only remove your own likes");

    verify(likeRepository, never()).delete(any(LikeEntity.class));
  }
}
