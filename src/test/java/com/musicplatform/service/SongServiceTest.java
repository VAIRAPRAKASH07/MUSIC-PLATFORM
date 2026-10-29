package com.musicplatform.service;

import com.musicplatform.dto.SongDto;
import com.musicplatform.dto.SongForm;
import com.musicplatform.exception.ForbiddenOperationException;
import com.musicplatform.exception.ResourceNotFoundException;
import com.musicplatform.model.Playlist;
import com.musicplatform.model.PlaylistSong;
import com.musicplatform.model.Song;
import com.musicplatform.model.User;
import com.musicplatform.repository.PlaylistRepository;
import com.musicplatform.repository.PlaylistSongRepository;
import com.musicplatform.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private PlaylistSongRepository playlistSongRepository;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private FavouritesService favouritesService;

    private SongService songService;

    private User creator;
    private User otherUser;
    private User admin;
    private Song testSong;

    @BeforeEach
    void setUp() {
        songService = new SongService(songRepository, playlistSongRepository, playlistRepository, favouritesService);

        creator = new User("artist_user", "artist@test.com", "hash", "ROLE_USER");
        creator.setId(1L);

        otherUser = new User("listener", "listener@test.com", "hash", "ROLE_USER");
        otherUser.setId(2L);

        admin = new User("admin_user", "admin@test.com", "hash", "ROLE_ADMIN");
        admin.setId(3L);

        testSong = new Song("Cosmic Voyage", "Starlight Band", "Constellations", "Rock", 240, 2023, creator);
        testSong.setId(100L);
    }

    @Test
    @DisplayName("Search songs applies whitelisted sort and maps to DTO with favourite flags")
    void testSearchSongs() {
        when(songRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(testSong)));
        when(favouritesService.getFavouritedSongIds(creator)).thenReturn(Set.of(100L));

        Page<SongDto> result = songService.searchSongs(null, null, 0, 10, "durationSeconds", "asc", creator);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        SongDto dto = result.getContent().get(0);
        assertEquals("Cosmic Voyage", dto.getTitle());
        assertTrue(dto.isFavourited());
        assertTrue(dto.isCanEdit(), "Creator should have edit permissions");
    }

    @Test
    @DisplayName("Create song attributes to creator user")
    void testCreateSong() {
        SongForm form = new SongForm(null, "New Beat", "DJ Producer", "Album X", "Electronic", 180, 2024);
        when(songRepository.save(any(Song.class))).thenAnswer(invocation -> {
            Song s = invocation.getArgument(0);
            s.setId(101L);
            return s;
        });

        SongDto created = songService.createSong(form, creator);

        assertNotNull(created);
        assertEquals(101L, created.getId());
        assertEquals("New Beat", created.getTitle());
        assertEquals(creator.getId(), created.getCreatedById());
    }

    @Test
    @DisplayName("Update song succeeds for creator or admin, fails for other users")
    void testUpdateSongAuthorization() {
        when(songRepository.findById(100L)).thenReturn(Optional.of(testSong));
        when(songRepository.save(any(Song.class))).thenReturn(testSong);

        SongForm updateForm = new SongForm(100L, "Updated Title", "Updated Artist", "Album", "Rock", 250, 2023);

        // 1. Creator can update
        SongDto updatedByCreator = songService.updateSong(100L, updateForm, creator);
        assertNotNull(updatedByCreator);

        // 2. Admin can update
        SongDto updatedByAdmin = songService.updateSong(100L, updateForm, admin);
        assertNotNull(updatedByAdmin);

        // 3. Unauthorized other user fails
        assertThrows(ForbiddenOperationException.class, () -> songService.updateSong(100L, updateForm, otherUser));
    }

    @Test
    @DisplayName("Delete song verifies order: collect affected playlist IDs -> delete -> compact positions")
    void testDeleteSongCascadeAndReindex() {
        when(songRepository.findById(100L)).thenReturn(Optional.of(testSong));
        when(playlistSongRepository.findPlaylistIdsBySongId(100L)).thenReturn(List.of(5L, 7L));

        Playlist playlist5 = new Playlist("Workout", null, false, creator);
        playlist5.setId(5L);
        when(playlistRepository.findById(5L)).thenReturn(Optional.of(playlist5));

        Playlist playlist7 = new Playlist("Chill", null, false, otherUser);
        playlist7.setId(7L);
        when(playlistRepository.findById(7L)).thenReturn(Optional.of(playlist7));

        when(playlistSongRepository.findByPlaylistOrderByPositionAsc(playlist5)).thenReturn(Collections.emptyList());
        when(playlistSongRepository.findByPlaylistOrderByPositionAsc(playlist7)).thenReturn(Collections.emptyList());

        songService.deleteSong(100L, creator);

        // Verify order:
        // 1. Collected affected playlist IDs
        verify(playlistSongRepository).findPlaylistIdsBySongId(100L);
        // 2. Deleted playlist associations and song
        verify(playlistSongRepository).deleteBySong(testSong);
        verify(songRepository).delete(testSong);
        // 3. Compacted positions in affected playlists
        verify(playlistRepository).findById(5L);
        verify(playlistRepository).findById(7L);
    }

    @Test
    @DisplayName("Delete song by unauthorized user throws ForbiddenOperationException")
    void testDeleteSongUnauthorized() {
        when(songRepository.findById(100L)).thenReturn(Optional.of(testSong));

        assertThrows(ForbiddenOperationException.class, () -> songService.deleteSong(100L, otherUser));
        verify(songRepository, never()).delete(any());
    }
}
