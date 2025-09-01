package com.tosk.app.comment;

import com.tosk.app.comment.dto.CommentResponseDTO;
import com.tosk.app.comment.dto.CreateCommentRequestDTO;
import com.tosk.app.comment.dto.UpdateCommentRequestDTO;
import com.tosk.app.task.TaskEntity;
import com.tosk.app.task.TaskService;
import com.tosk.app.user.UserEntity;
import com.tosk.app.user.UserService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

  private final CommentRepository commentRepository;
  private final TaskService taskService;
  private final UserService userService;

  public CommentResponseDTO createComment(
      UUID taskId, CreateCommentRequestDTO request, UUID userId) {
    TaskEntity task = taskService.getTaskEntityById(taskId, userId);
    UserEntity user = userService.getUserById(userId);

    CommentEntity parentComment = null;
    if (request.getParentCommentId() != null) {
      parentComment =
          commentRepository
              .findByIdAndNotDeleted(request.getParentCommentId())
              .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));

      if (!parentComment.getTask().getId().equals(taskId)) {
        throw new IllegalArgumentException("Parent comment does not belong to this task");
      }
    }

    CommentEntity comment =
        CommentEntity.builder()
            .task(task)
            .user(user)
            .parentComment(parentComment)
            .content(request.getContent())
            .likeCount(0)
            .build();

    CommentEntity savedComment = commentRepository.save(comment);
    return mapToResponseDTO(savedComment);
  }

  @Transactional(readOnly = true)
  public Page<CommentResponseDTO> getTaskComments(UUID taskId, Pageable pageable, UUID userId) {
    taskService.getTaskEntityById(taskId, userId);

    Page<CommentEntity> comments = commentRepository.findByTaskIdAndNotDeleted(taskId, pageable);
    return comments.map(this::mapToResponseDTO);
  }

  public CommentResponseDTO updateComment(
      UUID commentId, UpdateCommentRequestDTO request, UUID userId) {
    CommentEntity comment =
        commentRepository
            .findByIdAndNotDeleted(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

    if (!comment.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You can only edit your own comments");
    }

    comment.setContent(request.getContent());
    CommentEntity updatedComment = commentRepository.save(comment);
    return mapToResponseDTO(updatedComment);
  }

  public void deleteComment(UUID commentId, UUID userId) {
    CommentEntity comment =
        commentRepository
            .findByIdAndNotDeleted(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

    if (!comment.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You can only delete your own comments");
    }

    comment.setDeletedAt(LocalDateTime.now());
    commentRepository.save(comment);
  }

  private CommentResponseDTO mapToResponseDTO(CommentEntity comment) {
    return CommentResponseDTO.builder()
        .id(comment.getId())
        .taskId(comment.getTask().getId())
        .userId(comment.getUser().getId())
        .parentCommentId(
            comment.getParentComment() != null ? comment.getParentComment().getId() : null)
        .content(comment.getContent())
        .likeCount(comment.getLikeCount())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .authorUsername(comment.getUser().getUsername())
        .authorDisplayName(comment.getUser().getDisplayName())
        .authorAvatarUrl(comment.getUser().getAvatarUrl())
        .build();
  }
}
