package com.tosk.app.like;

import com.tosk.app.like.dto.LikeResponseDTO;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
public class LikeController {

  private final LikeService likeService;

  @PostMapping("/tasks/{taskId}/like")
  public ResponseEntity<LikeResponseDTO> toggleTaskLike(@PathVariable UUID taskId) {
    UUID userId = getCurrentUserId();
    LikeResponseDTO response = likeService.toggleTaskLike(taskId, userId);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/comments/{commentId}/like")
  public ResponseEntity<LikeResponseDTO> toggleCommentLike(@PathVariable UUID commentId) {
    UUID userId = getCurrentUserId();
    LikeResponseDTO response = likeService.toggleCommentLike(commentId, userId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/likes/{likeId}")
  public ResponseEntity<Void> removeLike(@PathVariable UUID likeId) {
    UUID userId = getCurrentUserId();
    likeService.removeLike(likeId, userId);
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
