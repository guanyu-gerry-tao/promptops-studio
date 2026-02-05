package com.promptops.platformapi.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.platformapi.config.WebMvcConfig;
import com.promptops.platformapi.dto.ProjectRequest;
import com.promptops.platformapi.entity.Project;
import com.promptops.platformapi.exception.BusinessException;
import com.promptops.platformapi.exception.GlobalExceptionHandler;
import com.promptops.platformapi.interceptor.JwtAuthInterceptor;
import com.promptops.platformapi.service.ProjectService;
import com.promptops.platformapi.util.JwtUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ProjectController tests using MockMvc.
 *
 * Since /projects/** requires JWT authentication, we need to mock
 * the JwtUtil to let requests pass through the interceptor.
 */
@WebMvcTest({ProjectController.class, GlobalExceptionHandler.class,
        WebMvcConfig.class, JwtAuthInterceptor.class})
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private static final String FAKE_TOKEN = "Bearer fake.jwt.token";

    @BeforeEach
    void setUp() {
        // Make the interceptor accept our fake token
        when(jwtUtil.getUserIdFromToken("fake.jwt.token")).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken("fake.jwt.token")).thenReturn("testuser");
    }

    @Test
    void create_success() throws Exception {
        Project mockProject = new Project();
        mockProject.setId(1L);
        mockProject.setName("My Project");
        mockProject.setDescription("A cool project");
        mockProject.setOwnerId(1L);

        when(projectService.create("My Project", "A cool project", 1L))
                .thenReturn(mockProject);

        ProjectRequest request = new ProjectRequest();
        request.setName("My Project");
        request.setDescription("A cool project");

        mockMvc.perform(post("/projects")
                        .header("Authorization", FAKE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Project"))
                .andExpect(jsonPath("$.ownerId").value(1));
    }

    @Test
    void create_fail_blankName() throws Exception {
        ProjectRequest request = new ProjectRequest();
        request.setName("");  // blank!

        mockMvc.perform(post("/projects")
                        .header("Authorization", FAKE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_fail_noToken() throws Exception {
        ProjectRequest request = new ProjectRequest();
        request.setName("My Project");

        // No Authorization header → interceptor returns 401
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_success() throws Exception {
        Project p1 = new Project();
        p1.setId(1L);
        p1.setName("Project A");
        Project p2 = new Project();
        p2.setId(2L);
        p2.setName("Project B");

        when(projectService.findByOwnerId(1L)).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/projects")
                        .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Project A"))
                .andExpect(jsonPath("$[1].name").value("Project B"));
    }

    @Test
    void get_success() throws Exception {
        Project mockProject = new Project();
        mockProject.setId(1L);
        mockProject.setName("My Project");

        when(projectService.findById(1L)).thenReturn(mockProject);

        mockMvc.perform(get("/projects/1")
                        .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Project"));
    }

    @Test
    void get_fail_notFound() throws Exception {
        when(projectService.findById(999L))
                .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "Project not found"));

        mockMvc.perform(get("/projects/999")
                        .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void update_success() throws Exception {
        Project updated = new Project();
        updated.setId(1L);
        updated.setName("New Name");
        updated.setDescription("New Desc");

        when(projectService.update(1L, "New Name", "New Desc")).thenReturn(updated);

        ProjectRequest request = new ProjectRequest();
        request.setName("New Name");
        request.setDescription("New Desc");

        mockMvc.perform(put("/projects/1")
                        .header("Authorization", FAKE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void delete_success() throws Exception {
        doNothing().when(projectService).delete(1L);

        mockMvc.perform(delete("/projects/1")
                        .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void delete_fail_notFound() throws Exception {
        doThrow(new BusinessException(HttpStatus.NOT_FOUND, "Project not found"))
                .when(projectService).delete(999L);

        mockMvc.perform(delete("/projects/999")
                        .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }
}
