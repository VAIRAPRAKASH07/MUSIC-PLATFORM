package com.musicplatform.controller;

import com.musicplatform.dto.PlayQueueStateDto;
import com.musicplatform.dto.QueueItemDto;
import com.musicplatform.service.PlayQueueService;
import com.musicplatform.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
class PlayQueueApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayQueueService playQueueService;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser
    @DisplayName("GET /api/queue/state returns current session queue state")
    void testGetQueueState() throws Exception {
        QueueItemDto item = new QueueItemDto(10L, "Track 1", "Artist 1", "Album 1", "Rock", 210, "03:30");
        PlayQueueStateDto state = new PlayQueueStateDto(item, List.of(item), 0, false, null, "Single Track", 1);

        when(playQueueService.getQueueState(any(HttpSession.class))).thenReturn(state);

        mockMvc.perform(get("/api/queue/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentIndex").value(0))
                .andExpect(jsonPath("$.totalTracks").value(1))
                .andExpect(jsonPath("$.currentSong.title").value("Track 1"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/queue/play-song starts song playback directly")
    void testPlaySong() throws Exception {
        QueueItemDto item = new QueueItemDto(50L, "Direct Play", "Solo Artist", "Album", "Pop", 180, "03:00");
        PlayQueueStateDto state = new PlayQueueStateDto(item, List.of(item), 0, false, null, "Single Track", 1);

        when(playQueueService.playSong(eq(50L), any(HttpSession.class))).thenReturn(state);

        mockMvc.perform(post("/api/queue/play-song")
                        .with(csrf())
                        .param("songId", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSong.title").value("Direct Play"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/queue/next advances queue to next track")
    void testNextTrack() throws Exception {
        QueueItemDto item2 = new QueueItemDto(2L, "Track 2", "Artist 2", "Album 2", "Jazz", 240, "04:00");
        PlayQueueStateDto state = new PlayQueueStateDto(item2, List.of(item2), 1, false, 5L, "My Playlist", 2);

        when(playQueueService.next(any(HttpSession.class))).thenReturn(state);

        mockMvc.perform(post("/api/queue/next")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentIndex").value(1))
                .andExpect(jsonPath("$.currentSong.title").value("Track 2"));
    }
}
