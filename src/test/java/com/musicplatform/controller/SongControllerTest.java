package com.musicplatform.controller;

import com.musicplatform.dto.SongDto;
import com.musicplatform.dto.SongForm;
import com.musicplatform.model.User;
import com.musicplatform.service.PlaylistService;
import com.musicplatform.service.SongService;
import com.musicplatform.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SongControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SongService songService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PlaylistService playlistService;

    @Test
    @WithMockUser(username = "musicfan")
    @DisplayName("GET /songs renders catalog list view with pagination model")
    void testListSongs() throws Exception {
        User user = new User("musicfan", "fan@music.local", "hash", "ROLE_USER");
        user.setId(1L);

        SongDto songDto = new SongDto(
                100L, "Sample Track", "Sample Artist", "Sample Album",
                "Rock", 240, "04:00", 2023,
                LocalDateTime.now(), 1L, "musicfan", true, false
        );
        var page = new PageImpl<>(List.of(songDto), PageRequest.of(0, 10), 1);

        when(userService.findByUsername("musicfan")).thenReturn(Optional.of(user));
        when(songService.searchSongs(any(), any(), anyInt(), anyInt(), anyString(), anyString(), any(User.class)))
                .thenReturn(page);
        when(songService.getDistinctGenres()).thenReturn(List.of("Rock", "Pop"));
        when(playlistService.getUserPlaylists(user)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/songs"))
                .andExpect(status().isOk())
                .andExpect(view().name("song/list"))
                .andExpect(model().attributeExists("songsPage"))
                .andExpect(model().attributeExists("genres"));
    }

    @Test
    @WithMockUser(username = "musicfan")
    @DisplayName("GET /songs/new renders song creation form")
    void testNewSongForm() throws Exception {
        mockMvc.perform(get("/songs/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("song/form"))
                .andExpect(model().attributeExists("songForm"));
    }

    @Test
    @WithMockUser(username = "musicfan")
    @DisplayName("POST /songs creates new song and redirects to /songs")
    void testCreateSong() throws Exception {
        User user = new User("musicfan", "fan@music.local", "hash", "ROLE_USER");
        when(userService.findByUsername("musicfan")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/songs")
                        .with(csrf())
                        .param("title", "Hotel California")
                        .param("artist", "Eagles")
                        .param("album", "Hotel California")
                        .param("genre", "Rock")
                        .param("durationSeconds", "390")
                        .param("releaseYear", "1976"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/songs"));

        verify(songService).createSong(any(SongForm.class), eq(user));
    }
}
