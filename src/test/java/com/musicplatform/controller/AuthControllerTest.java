package com.musicplatform.controller;

import com.musicplatform.dto.UserRegistrationDto;
import com.musicplatform.model.User;
import com.musicplatform.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /login displays login form")
    void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("GET /register displays registration form")
    void testRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("userDto"));
    }

    @Test
    @DisplayName("POST /register with valid parameters creates user and redirects to login")
    void testRegisterSuccess() throws Exception {
        User user = new User("testuser", "test@test.local", "hashedPassword", "ROLE_USER");
        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(user);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "validuser")
                        .param("email", "valid@test.local")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    @DisplayName("POST /register with validation errors re-renders registration view")
    void testRegisterValidationErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "ab") // too short, min is 3
                        .param("email", "not-an-email")
                        .param("password", "123") // too short
                        .param("confirmPassword", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());
    }
}
