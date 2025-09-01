package com.tosk.app.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tosk.app.comment.dto.CommentResponseDTO;
import com.tosk.app.comment.dto.CreateCommentRequestDTO;
import com.tosk.app.comment.dto.UpdateCommentRequestDTO;
import com.tosk.app.task.TaskEntity;
import com.tosk.app.task.TaskService;
import com.tosk.app.user.UserEntity;
import com.tosk.app.user.UserService;
import java.time.LocalDateTime;
import java.util.List;
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
class CommentServiceTest {

  @Mock CommentRepository commentRepository;
  @Mock TaskService taskService;
  @Mock UserService userService;

  @InjectMocks CommentService commentService;

  private UUID userId;
  private UserEntity userEntity;
  private TaskEntity taskEntity;
  private CommentEntity commentEntity;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    UUID taskId = UUID.randomUUID();

    userEntity =
        UserEntity.builder().id(userId).username("testuser").email("testuser@example.com").build();

    taskEntity =
        TaskEntity.builder()
            .id(taskId)
            .title("Test Task")
            .userId(userId)
            .visibility(TaskEntity.Visibility.PUBLIC)
            .build();

    commentEntity =
        CommentEntity.builder()
            .id(UUID.randomUUID())
            .task(taskEntity)
            .user(userEntity)
            .content("Test comment")
            .likeCount(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
  }

  @Test
  void createComment_withValidRequest_shouldCreateComment() {
    CreateCommentRequestDTO request = new CreateCommentRequestDTO();
    request.setContent("This is a test comment");

    when(taskService.getTaskEntityById(any(UUID.class), any(UUID.class))).thenReturn(taskEntity);
    when(userService.getUserById(any(UUID.class))).thenReturn(userEntity);
    when(commentRepository.save(any(CommentEntity.class))).thenReturn(commentEntity);

    CommentResponseDTO result = commentService.createComment(taskEntity.getId(), request, userId);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEqualTo(commentEntity.getContent());
    assertThat(result.getTaskId()).isEqualTo(taskEntity.getId());
    assertThat(result.getUserId()).isEqualTo(userEntity.getId());

    verify(commentRepository).save(any(CommentEntity.class));
  }

  @Test
  void getTaskComments_shouldReturnComments() {
    Page<CommentEntity> page = new PageImpl<>(List.of(commentEntity));

    when(taskService.getTaskEntityById(any(UUID.class), any(UUID.class))).thenReturn(taskEntity);
    when(commentRepository.findByTaskIdAndNotDeleted(any(UUID.class), any(Pageable.class)))
        .thenReturn(page);

    Page<CommentResponseDTO> result =
        commentService.getTaskComments(taskEntity.getId(), Pageable.unpaged(), userId);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getContent()).isEqualTo("Test comment");

    verify(taskService).getTaskEntityById(any(UUID.class), any(UUID.class));
    verify(commentRepository).findByTaskIdAndNotDeleted(any(UUID.class), any(Pageable.class));
  }

  @Test
  void updateComment_byOwner_shouldUpdateComment() {
    UpdateCommentRequestDTO request = new UpdateCommentRequestDTO();
    request.setContent("Updated comment");

    CommentEntity updatedComment =
        CommentEntity.builder()
            .id(commentEntity.getId())
            .task(taskEntity)
            .user(userEntity)
            .content("Updated comment")
            .likeCount(0)
            .createdAt(commentEntity.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .build();

    when(commentRepository.findByIdAndNotDeleted(any(UUID.class)))
        .thenReturn(Optional.of(commentEntity));
    when(commentRepository.save(any(CommentEntity.class))).thenReturn(updatedComment);

    CommentResponseDTO result =
        commentService.updateComment(commentEntity.getId(), request, userId);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEqualTo("Updated comment");

    verify(commentRepository).save(any(CommentEntity.class));
  }

  @Test
  void updateComment_byNonOwner_shouldThrowException() {
    UUID anotherUserId = UUID.randomUUID();

    UpdateCommentRequestDTO request = new UpdateCommentRequestDTO();
    request.setContent("Updated comment");

    when(commentRepository.findByIdAndNotDeleted(any(UUID.class)))
        .thenReturn(Optional.of(commentEntity));

    assertThatThrownBy(
            () -> commentService.updateComment(commentEntity.getId(), request, anotherUserId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("You can only edit your own comments");

    verify(commentRepository, never()).save(any(CommentEntity.class));
  }

  @Test
  void deleteComment_byOwner_shouldDeleteComment() {
    when(commentRepository.findByIdAndNotDeleted(any(UUID.class)))
        .thenReturn(Optional.of(commentEntity));
    when(commentRepository.save(any(CommentEntity.class))).thenReturn(commentEntity);

    commentService.deleteComment(commentEntity.getId(), userId);

    verify(commentRepository).save(any(CommentEntity.class));
  }

  @Test
  void deleteComment_byNonOwner_shouldThrowException() {
    UUID anotherUserId = UUID.randomUUID();

    when(commentRepository.findByIdAndNotDeleted(any(UUID.class)))
        .thenReturn(Optional.of(commentEntity));

    assertThatThrownBy(() -> commentService.deleteComment(commentEntity.getId(), anotherUserId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("You can only delete your own comments");

    verify(commentRepository, never()).save(any(CommentEntity.class));
  }
}
