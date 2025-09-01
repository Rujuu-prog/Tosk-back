package com.tosk.app.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosk.app.auth.dto.SignupRequestDTO;
import com.tosk.app.task.dto.CreateTaskRequestDTO;
import com.tosk.app.task.dto.UpdateTaskRequestDTO;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tosk_auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@Transactional
class TaskControllerFullIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;
  @Autowired TaskRepository taskRepository;

  private Cookie accessCookie;
  private String currentUserId;

  @BeforeEach
  void setUp() throws Exception {
    // Create a user and get authentication cookies
    SignupRequestDTO signupRequest = new SignupRequestDTO();
    signupRequest.setEmail("integration@test.com");
    signupRequest.setUsername("integrationuser");
    signupRequest.setDisplayName("Integration Test User");
    signupRequest.setPassword("SecurePassword123!");

    MvcResult signupResult =
        mockMvc
            .perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(signupRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    // Extract authentication cookies
    Cookie[] cookies = signupResult.getResponse().getCookies();
    for (Cookie cookie : cookies) {
      if ("AT".equals(cookie.getName())) {
        accessCookie = cookie;
        break;
      }
    }

    // Verify authentication works
    if (accessCookie != null) {
      MvcResult meResult =
          mockMvc
              .perform(get("/api/auth/me").cookie(accessCookie))
              .andExpect(status().isOk())
              .andReturn();

      String meResponse = meResult.getResponse().getContentAsString();
      JsonNode meJson = om.readTree(meResponse);
      currentUserId = meJson.get("id").asText();
    }

    assertThat(accessCookie).isNotNull();
    assertThat(currentUserId).isNotNull();
  }

  @Test
  void fullTaskLifecycle_createReadUpdateDelete_shouldWorkCorrectly() throws Exception {
    // 1. Create a task
    CreateTaskRequestDTO createRequest =
        CreateTaskRequestDTO.builder()
            .title("Integration Test Task")
            .description("This task is created in integration test")
            .dueDate(LocalDate.now().plusDays(7))
            .priority(TaskEntity.Priority.HIGH)
            .visibility(TaskEntity.Visibility.PRIVATE)
            .build();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/tasks")
                    .cookie(accessCookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Integration Test Task"))
            .andExpect(jsonPath("$.description").value("This task is created in integration test"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.visibility").value("PRIVATE"))
            .andExpect(jsonPath("$.userId").value(currentUserId))
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    JsonNode createJson = om.readTree(createResponse);
    String taskId = createJson.get("id").asText();

    // 2. Read the created task
    mockMvc
        .perform(get("/api/tasks/" + taskId).cookie(accessCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(taskId))
        .andExpect(jsonPath("$.title").value("Integration Test Task"))
        .andExpect(jsonPath("$.userId").value(currentUserId));

    // 3. Update the task
    UpdateTaskRequestDTO updateRequest =
        UpdateTaskRequestDTO.builder()
            .title("Updated Integration Test Task")
            .description("This task has been updated")
            .priority(TaskEntity.Priority.MEDIUM)
            .build();

    mockMvc
        .perform(
            put("/api/tasks/" + taskId)
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Integration Test Task"))
        .andExpect(jsonPath("$.description").value("This task has been updated"))
        .andExpect(jsonPath("$.priority").value("MEDIUM"));

    // 4. List tasks (should include our task)
    mockMvc
        .perform(get("/api/tasks").cookie(accessCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[?(@.id == '" + taskId + "')]").exists());

    // 5. List my tasks only
    mockMvc
        .perform(get("/api/tasks").cookie(accessCookie).param("myTasksOnly", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[?(@.id == '" + taskId + "')]").exists());

    // 6. Delete the task
    mockMvc
        .perform(delete("/api/tasks/" + taskId).cookie(accessCookie))
        .andExpect(status().isNoContent());

    // 7. Verify task is deleted (soft delete)
    mockMvc
        .perform(get("/api/tasks/" + taskId).cookie(accessCookie))
        .andExpect(status().isBadRequest()); // Should return error as task is soft deleted
  }

  @Test
  void createTask_withTeamVisibility_shouldRequireTeamId() throws Exception {
    CreateTaskRequestDTO request =
        CreateTaskRequestDTO.builder()
            .title("Team Task")
            .description("Task for team")
            .priority(TaskEntity.Priority.MEDIUM)
            .visibility(TaskEntity.Visibility.TEAM)
            .teamId(null) // Missing team ID
            .build();

    mockMvc
        .perform(
            post("/api/tasks")
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createTask_withValidTeamId_shouldSucceed() throws Exception {
    UUID teamId = UUID.randomUUID(); // In real scenario, this would be an existing team

    CreateTaskRequestDTO request =
        CreateTaskRequestDTO.builder()
            .title("Team Task")
            .description("Task for team")
            .priority(TaskEntity.Priority.MEDIUM)
            .visibility(TaskEntity.Visibility.TEAM)
            .teamId(teamId)
            .build();

    mockMvc
        .perform(
            post("/api/tasks")
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Team Task"))
        .andExpect(jsonPath("$.visibility").value("TEAM"))
        .andExpect(jsonPath("$.teamId").value(teamId.toString()));
  }

  @Test
  void createTask_withPublicVisibility_shouldSucceed() throws Exception {
    CreateTaskRequestDTO request =
        CreateTaskRequestDTO.builder()
            .title("Public Task")
            .description("This is a public task")
            .priority(TaskEntity.Priority.LOW)
            .visibility(TaskEntity.Visibility.PUBLIC)
            .build();

    mockMvc
        .perform(
            post("/api/tasks")
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Public Task"))
        .andExpect(jsonPath("$.visibility").value("PUBLIC"));
  }

  @Test
  void updateTask_byNonOwner_shouldFail() throws Exception {
    // Create a task
    CreateTaskRequestDTO createRequest =
        CreateTaskRequestDTO.builder()
            .title("Original Task")
            .priority(TaskEntity.Priority.LOW)
            .visibility(TaskEntity.Visibility.PRIVATE)
            .build();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/tasks")
                    .cookie(accessCookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    String taskId = om.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

    // Create another user
    SignupRequestDTO anotherUser = new SignupRequestDTO();
    anotherUser.setEmail("another@test.com");
    anotherUser.setUsername("anotheruser");
    anotherUser.setDisplayName("Another User");
    anotherUser.setPassword("AnotherPassword123!");

    MvcResult anotherUserResult =
        mockMvc
            .perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(anotherUser)))
            .andExpect(status().isCreated())
            .andReturn();

    Cookie anotherUserCookie = null;
    Cookie[] anotherUserCookies = anotherUserResult.getResponse().getCookies();
    for (Cookie cookie : anotherUserCookies) {
      if ("AT".equals(cookie.getName())) {
        anotherUserCookie = cookie;
        break;
      }
    }

    // Try to update the task with another user's credentials
    UpdateTaskRequestDTO updateRequest =
        UpdateTaskRequestDTO.builder().title("Hacked Task").build();

    mockMvc
        .perform(
            put("/api/tasks/" + taskId)
                .cookie(anotherUserCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(updateRequest)))
        .andExpect(status().isBadRequest()); // Should fail due to access control
  }

  @Test
  void taskFiltering_byPriorityAndVisibility_shouldWork() throws Exception {
    // Create tasks with different priorities and visibilities
    CreateTaskRequestDTO highPriorityTask =
        CreateTaskRequestDTO.builder()
            .title("High Priority Task")
            .priority(TaskEntity.Priority.HIGH)
            .visibility(TaskEntity.Visibility.PRIVATE)
            .build();

    CreateTaskRequestDTO lowPriorityTask =
        CreateTaskRequestDTO.builder()
            .title("Low Priority Task")
            .priority(TaskEntity.Priority.LOW)
            .visibility(TaskEntity.Visibility.PUBLIC)
            .build();

    // Create the tasks
    mockMvc
        .perform(
            post("/api/tasks")
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(highPriorityTask)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/tasks")
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(lowPriorityTask)))
        .andExpect(status().isCreated());

    // Filter by high priority
    mockMvc
        .perform(get("/api/tasks").cookie(accessCookie).param("priority", "HIGH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[?(@.priority == 'HIGH')]").exists());

    // Filter by public visibility
    mockMvc
        .perform(get("/api/tasks").cookie(accessCookie).param("visibility", "PUBLIC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[?(@.visibility == 'PUBLIC')]").exists());
  }

  @Test
  void taskSearch_byKeyword_shouldWork() throws Exception {
    // Create a task with searchable content
    CreateTaskRequestDTO searchableTask =
        CreateTaskRequestDTO.builder()
            .title("Searchable Integration Test")
            .description("This task contains unique searchable keywords")
            .priority(TaskEntity.Priority.MEDIUM)
            .visibility(TaskEntity.Visibility.PRIVATE)
            .build();

    mockMvc
        .perform(
            post("/api/tasks")
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(searchableTask)))
        .andExpect(status().isCreated());

    // Search by title keyword
    mockMvc
        .perform(get("/api/tasks").cookie(accessCookie).param("keyword", "Searchable"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[?(@.title =~ /.*Searchable.*/)]").exists());

    // Search by description keyword
    mockMvc
        .perform(get("/api/tasks").cookie(accessCookie).param("keyword", "unique"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[?(@.description =~ /.*unique.*/)]").exists());
  }
}
