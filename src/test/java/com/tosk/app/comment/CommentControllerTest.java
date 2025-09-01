package com.tosk.app.comment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosk.app.comment.dto.CommentResponseDTO;
import com.tosk.app.comment.dto.CreateCommentRequestDTO;
import com.tosk.app.comment.dto.UpdateCommentRequestDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockBean CommentService commentService;

  @Test
  @WithMockUser
  void createComment_withValidRequest_shouldCreateComment() throws Exception {
    UUID taskId = UUID.randomUUID();
    CreateCommentRequestDTO request = new CreateCommentRequestDTO();
    request.setContent("This is a test comment");

    CommentResponseDTO response =
        CommentResponseDTO.builder()
            .id(UUID.randomUUID())
            .taskId(taskId)
            .userId(UUID.randomUUID())
            .parentCommentId(null)
            .content("This is a test comment")
            .likeCount(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    when(commentService.createComment(
            any(UUID.class), any(CreateCommentRequestDTO.class), any(UUID.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/tasks/{taskId}/comments", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("This is a test comment"))
        .andExpect(jsonPath("$.taskId").value(taskId.toString()));

    verify(commentService)
        .createComment(any(UUID.class), any(CreateCommentRequestDTO.class), any(UUID.class));
  }

  @Test
  @WithMockUser
  void getTaskComments_shouldReturnComments() throws Exception {
    UUID taskId = UUID.randomUUID();
    CommentResponseDTO comment =
        CommentResponseDTO.builder()
            .id(UUID.randomUUID())
            .taskId(taskId)
            .userId(UUID.randomUUID())
            .content("Test comment")
            .likeCount(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    Page<CommentResponseDTO> page = new PageImpl<>(List.of(comment));
    when(commentService.getTaskComments(any(UUID.class), any(Pageable.class), any(UUID.class)))
        .thenReturn(page);

    mockMvc
        .perform(get("/api/tasks/{taskId}/comments", taskId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].content").value("Test comment"));

    verify(commentService).getTaskComments(any(UUID.class), any(Pageable.class), any(UUID.class));
  }

  @Test
  @WithMockUser
  void updateComment_withValidRequest_shouldUpdateComment() throws Exception {
    UUID commentId = UUID.randomUUID();
    UpdateCommentRequestDTO request = new UpdateCommentRequestDTO();
    request.setContent("Updated comment");

    CommentResponseDTO response =
        CommentResponseDTO.builder().id(commentId).content("Updated comment").build();

    when(commentService.updateComment(
            any(UUID.class), any(UpdateCommentRequestDTO.class), any(UUID.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            put("/api/comments/{commentId}", commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("Updated comment"));

    verify(commentService)
        .updateComment(any(UUID.class), any(UpdateCommentRequestDTO.class), any(UUID.class));
  }

  @Test
  @WithMockUser
  void deleteComment_shouldDeleteComment() throws Exception {
    UUID commentId = UUID.randomUUID();

    mockMvc
        .perform(delete("/api/comments/{commentId}", commentId))
        .andExpect(status().isNoContent());

    verify(commentService).deleteComment(any(UUID.class), any(UUID.class));
  }
}
