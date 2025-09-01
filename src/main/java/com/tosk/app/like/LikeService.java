package com.tosk.app.like;

import com.tosk.app.comment.CommentEntity;
import com.tosk.app.comment.CommentRepository;
import com.tosk.app.like.dto.LikeResponseDTO;
import com.tosk.app.task.TaskEntity;
import com.tosk.app.task.TaskRepository;
import com.tosk.app.user.UserEntity;
import com.tosk.app.user.UserService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeService {

  private final LikeRepository likeRepository;
  private final TaskRepository taskRepository;
  private final CommentRepository commentRepository;
  private final UserService userService;

  public LikeResponseDTO toggleTaskLike(UUID taskId, UUID userId) {
    TaskEntity task =
        taskRepository
            .findByIdAndDeletedAtIsNull(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    Optional<LikeEntity> existingLike =
        likeRepository.findByUserIdAndTargetTypeAndTargetId(
            userId, LikeEntity.TargetType.TASK, taskId);

    if (existingLike.isPresent()) {
      // Unlike: 既存のいいねを削除
      likeRepository.delete(existingLike.get());
      task.setLikeCount(Math.max(0, task.getLikeCount() - 1));
      taskRepository.save(task);
      return null; // いいねを削除した場合はnullを返す
    } else {
      // Like: 新しいいいねを作成
      UserEntity user = userService.getUserById(userId);

      LikeEntity like =
          LikeEntity.builder()
              .user(user)
              .targetType(LikeEntity.TargetType.TASK)
              .targetId(taskId)
              .build();

      LikeEntity savedLike = likeRepository.save(like);
      task.setLikeCount(task.getLikeCount() + 1);
      taskRepository.save(task);

      return mapToResponseDTO(savedLike);
    }
  }

  public LikeResponseDTO toggleCommentLike(UUID commentId, UUID userId) {
    CommentEntity comment =
        commentRepository
            .findByIdAndNotDeleted(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

    Optional<LikeEntity> existingLike =
        likeRepository.findByUserIdAndTargetTypeAndTargetId(
            userId, LikeEntity.TargetType.COMMENT, commentId);

    if (existingLike.isPresent()) {
      // Unlike: 既存のいいねを削除
      likeRepository.delete(existingLike.get());
      comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
      commentRepository.save(comment);
      return null; // いいねを削除した場合はnullを返す
    } else {
      // Like: 新しいいいねを作成
      UserEntity user = userService.getUserById(userId);

      LikeEntity like =
          LikeEntity.builder()
              .user(user)
              .targetType(LikeEntity.TargetType.COMMENT)
              .targetId(commentId)
              .build();

      LikeEntity savedLike = likeRepository.save(like);
      comment.setLikeCount(comment.getLikeCount() + 1);
      commentRepository.save(comment);

      return mapToResponseDTO(savedLike);
    }
  }

  public void removeLike(UUID likeId, UUID userId) {
    LikeEntity like =
        likeRepository
            .findById(likeId)
            .orElseThrow(() -> new IllegalArgumentException("Like not found: " + likeId));

    if (!like.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You can only remove your own likes");
    }

    // カウントを減らす
    if (like.getTargetType() == LikeEntity.TargetType.TASK) {
      TaskEntity task = taskRepository.findByIdAndDeletedAtIsNull(like.getTargetId()).orElse(null);
      if (task != null) {
        task.setLikeCount(Math.max(0, task.getLikeCount() - 1));
        taskRepository.save(task);
      }
    } else if (like.getTargetType() == LikeEntity.TargetType.COMMENT) {
      CommentEntity comment =
          commentRepository.findByIdAndNotDeleted(like.getTargetId()).orElse(null);
      if (comment != null) {
        comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        commentRepository.save(comment);
      }
    }

    likeRepository.delete(like);
  }

  private LikeResponseDTO mapToResponseDTO(LikeEntity like) {
    return LikeResponseDTO.builder()
        .id(like.getId())
        .userId(like.getUser().getId())
        .targetType(like.getTargetType())
        .targetId(like.getTargetId())
        .createdAt(like.getCreatedAt())
        .build();
  }
}
