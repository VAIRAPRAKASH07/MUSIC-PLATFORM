package com.musicplatform.controller;

import com.musicplatform.model.User;
import com.musicplatform.service.FavouritesService;
import com.musicplatform.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavouritesApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavouritesService favouritesService;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("Unauthenticated request to /api/favourites/toggle returns HTTP 401 JSON")
    void testUnauthenticatedReturns401Json() throws Exception {
        mockMvc.perform(post("/api/favourites/toggle")
                        .with(csrf())
                        .param("songId", "100"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @WithMockUser(username = "listener")
    @DisplayName("Authenticated POST /api/favourites/toggle toggles favourite status and returns JSON")
    void testAuthenticatedToggleFavourite() throws Exception {
        User user = new User("listener", "listener@test.local", "hash", "ROLE_USER");
        user.setId(10L);

        when(userService.getByUsername("listener")).thenReturn(user);
        when(favouritesService.toggleFavourite(eq(100L), any(User.class))).thenReturn(true);

        mockMvc.perform(post("/api/favourites/toggle")
                        .with(csrf())
                        .param("songId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.songId").value(100))
                .andExpect(jsonPath("$.favourited").value(true))
                .andExpect(jsonPath("$.message").value("Added to favourites"));
    }

    @Test
    @WithMockUser(username = "listener")
    @DisplayName("Authenticated GET /api/favourites/status returns current favourite state")
    void testAuthenticatedFavouriteStatus() throws Exception {
        User user = new User("listener", "listener@test.local", "hash", "ROLE_USER");
        when(userService.getByUsername("listener")).thenReturn(user);
        when(favouritesService.isFavourited(any(User.class), eq(100L))).thenReturn(true);

        mockMvc.perform(get("/api/favourites/status")
                        .param("songId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.songId").value(100))
                .andExpect(jsonPath("$.favourited").value(true));
    }
}
