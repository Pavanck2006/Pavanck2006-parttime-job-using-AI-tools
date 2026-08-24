package com.parttimejob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parttimejob.dto.auth.LoginRequest;
import com.parttimejob.dto.auth.RegisterRequest;
import com.parttimejob.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully login as Admin")
    void testAdminLogin() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@parttimejob.com")
                .password("Admin@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Should register a new Student successfully")
    void testStudentRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Rahul Verma")
                .email("student.rahul" + System.currentTimeMillis() + "@gmail.com")
                .password("Rahul@123")
                .phone("+919888877777")
                .role(Role.ROLE_STUDENT)
                .collegeName("PES University")
                .preferredArea("Banashankari")
                .skills("Food Server, Cleanliness")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("ROLE_STUDENT"));
    }

    @Test
    @DisplayName("Should reject invalid credentials with 401")
    void testInvalidLogin() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@parttimejob.com")
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
