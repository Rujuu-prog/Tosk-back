package com.tosk.app.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosk.app.task.dto.CreateTaskRequestDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tosk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false"
    })
@Transactional
class TaskControllerIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;

  @Test
  void createTask_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
    CreateTaskRequestDTO request =
        CreateTaskRequestDTO.builder()
            .title("Test Task")
            .description("This is a test task")
            .dueDate(LocalDate.now().plusDays(7))
            .priority(TaskEntity.Priority.MEDIUM)
            .visibility(TaskEntity.Visibility.PRIVATE)
            .build();

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getTasks_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
  }

  @Test
  void getTask_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
    mockMvc
        .perform(get("/api/tasks/550e8400-e29b-41d4-a716-446655440000"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updateTask_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
    mockMvc
        .perform(
            put("/api/tasks/550e8400-e29b-41d4-a716-446655440000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteTask_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
    mockMvc
        .perform(delete("/api/tasks/550e8400-e29b-41d4-a716-446655440000"))
        .andExpect(status().isUnauthorized());
  }
}
