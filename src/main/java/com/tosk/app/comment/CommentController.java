package com.tosk.app.comment;

import com.tosk.app.comment.dto.CommentResponseDTO;
import com.tosk.app.comment.dto.CreateCommentRequestDTO;
import com.tosk.app.comment.dto.UpdateCommentRequestDTO;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
public class CommentController {

  private final CommentService commentService;

  @PostMapping("/tasks/{taskId}/comments")
  public ResponseEntity<CommentResponseDTO> createComment(
      @PathVariable UUID taskId, @Valid @RequestBody CreateCommentRequestDTO request) {

    UUID userId = getCurrentUserId();
    CommentResponseDTO response = commentService.createComment(taskId, request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/tasks/{taskId}/comments")
  public ResponseEntity<Page<CommentResponseDTO>> getTaskComments(
      @PathVariable UUID taskId, @PageableDefault(size = 20) Pageable pageable) {

    UUID userId = getCurrentUserId();
    Page<CommentResponseDTO> comments = commentService.getTaskComments(taskId, pageable, userId);
    return ResponseEntity.ok(comments);
  }

  @PutMapping("/comments/{commentId}")
  public ResponseEntity<CommentResponseDTO> updateComment(
      @PathVariable UUID commentId, @Valid @RequestBody UpdateCommentRequestDTO request) {

    UUID userId = getCurrentUserId();
    CommentResponseDTO response = commentService.updateComment(commentId, request, userId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {

    UUID userId = getCurrentUserId();
    commentService.deleteComment(commentId, userId);
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
