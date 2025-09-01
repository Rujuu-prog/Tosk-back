package com.tosk.app.like;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tosk.app.like.dto.LikeResponseDTO;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LikeController.class)
class LikeControllerTest {

  @Autowired MockMvc mockMvc;

  @MockBean LikeService likeService;

  @Test
  void likeTask_shouldCreateLike() throws Exception {
    UUID taskId = UUID.randomUUID();
    LikeResponseDTO response =
        LikeResponseDTO.builder()
            .id(UUID.randomUUID())
            .targetType(LikeEntity.TargetType.TASK)
            .targetId(taskId)
            .userId(UUID.randomUUID())
            .build();

    when(likeService.toggleTaskLike(any(UUID.class), any(UUID.class))).thenReturn(response);

    UUID mockUserId = UUID.randomUUID();
    mockMvc
        .perform(
            post("/api/tasks/{taskId}/like", taskId)
                .with(authentication(new MockAuthentication(mockUserId))))
        .andExpect(status().isOk());

    verify(likeService).toggleTaskLike(any(UUID.class), any(UUID.class));
  }

  private static class MockAuthentication
      implements org.springframework.security.core.Authentication {
    private final UUID userId;

    public MockAuthentication(UUID userId) {
      this.userId = userId;
    }

    @Override
    public UUID getPrincipal() {
      return userId;
    }

    @Override
    public boolean isAuthenticated() {
      return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

    @Override
    public String getName() {
      return userId.toString();
    }

    @Override
    public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>
        getAuthorities() {
      return java.util.Collections.emptyList();
    }

    @Override
    public Object getCredentials() {
      return null;
    }

    @Override
    public Object getDetails() {
      return null;
    }
  }

  @Test
  void likeComment_shouldCreateLike() throws Exception {
    UUID commentId = UUID.randomUUID();
    LikeResponseDTO response =
        LikeResponseDTO.builder()
            .id(UUID.randomUUID())
            .targetType(LikeEntity.TargetType.COMMENT)
            .targetId(commentId)
            .userId(UUID.randomUUID())
            .build();

    when(likeService.toggleCommentLike(any(UUID.class), any(UUID.class))).thenReturn(response);

    UUID mockUserId = UUID.randomUUID();
    mockMvc
        .perform(
            post("/api/comments/{commentId}/like", commentId)
                .with(authentication(new MockAuthentication(mockUserId))))
        .andExpect(status().isOk());

    verify(likeService).toggleCommentLike(any(UUID.class), any(UUID.class));
  }

  @Test
  void removeLike_shouldDeleteLike() throws Exception {
    UUID likeId = UUID.randomUUID();

    doNothing().when(likeService).removeLike(any(UUID.class), any(UUID.class));

    UUID mockUserId = UUID.randomUUID();
    mockMvc
        .perform(
            delete("/api/likes/{likeId}", likeId)
                .with(authentication(new MockAuthentication(mockUserId))))
        .andExpect(status().isNoContent());

    verify(likeService).removeLike(any(UUID.class), any(UUID.class));
  }
}
