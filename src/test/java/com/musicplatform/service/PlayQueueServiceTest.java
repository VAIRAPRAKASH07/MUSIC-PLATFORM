package com.musicplatform.service;

import com.musicplatform.dto.PlayQueueStateDto;
import com.musicplatform.model.Playlist;
import com.musicplatform.model.PlaylistSong;
import com.musicplatform.model.Song;
import com.musicplatform.model.User;
import com.musicplatform.repository.PlaylistRepository;
import com.musicplatform.repository.PlaylistSongRepository;
import com.musicplatform.repository.SongRepository;
import com.musicplatform.util.FisherYatesShuffler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayQueueServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistSongRepository playlistSongRepository;

    @Mock
    private SongRepository songRepository;

    private FisherYatesShuffler shuffler;
    private PlayQueueService playQueueService;
    private MockHttpSession session;

    private User user;
    private Playlist playlist;
    private Song songA;
    private Song songB;
    private Song songC;

    @BeforeEach
    void setUp() {
        // Use deterministic seeded Random for testing shuffle
        shuffler = new FisherYatesShuffler(new Random(42L));
        playQueueService = new PlayQueueService(playlistRepository, playlistSongRepository, songRepository, shuffler);
        session = new MockHttpSession();

        user = new User("tester", "test@test.com", "hash", "ROLE_USER");
        user.setId(1L);

        playlist = new Playlist("Party Mix", "Dance tracks", false, user);
        playlist.setId(20L);

        songA = new Song("Track A", "Artist 1", "Album 1", "Pop", 200, 2021, user);
        songA.setId(1L);

        songB = new Song("Track B", "Artist 2", "Album 2", "Rock", 240, 2022, user);
        songB.setId(2L);

        songC = new Song("Track C", "Artist 3", "Album 3", "Jazz", 180, 2023, user);
        songC.setId(3L);
    }

    @Test
    @DisplayName("Play playlist loads tracks into queue in order with index 0")
    void testPlayPlaylistNormal() {
        PlaylistSong ps1 = new PlaylistSong(playlist, songA, 1);
        PlaylistSong ps2 = new PlaylistSong(playlist, songB, 2);
        when(playlistRepository.findById(20L)).thenReturn(Optional.of(playlist));
        when(playlistSongRepository.findByPlaylistWithSongOrderByPositionAsc(playlist)).thenReturn(List.of(ps1, ps2));

        PlayQueueStateDto state = playQueueService.playPlaylist(20L, false, session, user);

        assertNotNull(state);
        assertEquals(2, state.getTotalTracks());
        assertEquals(0, state.getCurrentIndex());
        assertEquals("Track A", state.getCurrentSong().getTitle());
        assertFalse(state.isShuffled());
    }

    @Test
    @DisplayName("Play playlist with shuffle applies Fisher-Yates and marks shuffled=true")
    void testPlayPlaylistShuffle() {
        PlaylistSong ps1 = new PlaylistSong(playlist, songA, 1);
        PlaylistSong ps2 = new PlaylistSong(playlist, songB, 2);
        PlaylistSong ps3 = new PlaylistSong(playlist, songC, 3);
        when(playlistRepository.findById(20L)).thenReturn(Optional.of(playlist));
        when(playlistSongRepository.findByPlaylistWithSongOrderByPositionAsc(playlist)).thenReturn(List.of(ps1, ps2, ps3));

        PlayQueueStateDto state = playQueueService.playPlaylist(20L, true, session, user);

        assertNotNull(state);
        assertEquals(3, state.getTotalTracks());
        assertTrue(state.isShuffled());
        assertNotNull(state.getCurrentSong());
    }

    @Test
    @DisplayName("playSong sets queue to single track and sets index 0")
    void testPlaySong() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(songA));

        PlayQueueStateDto state = playQueueService.playSong(1L, session);

        assertNotNull(state);
        assertEquals(1, state.getTotalTracks());
        assertEquals(0, state.getCurrentIndex());
        assertEquals("Track A", state.getCurrentSong().getTitle());
    }

    @Test
    @DisplayName("next() advances to next track and wraps around at queue end")
    void testNextNavigation() {
        PlaylistSong ps1 = new PlaylistSong(playlist, songA, 1);
        PlaylistSong ps2 = new PlaylistSong(playlist, songB, 2);
        when(playlistRepository.findById(20L)).thenReturn(Optional.of(playlist));
        when(playlistSongRepository.findByPlaylistWithSongOrderByPositionAsc(playlist)).thenReturn(List.of(ps1, ps2));
        when(songRepository.existsById(anyLong())).thenReturn(true);

        playQueueService.playPlaylist(20L, false, session, user);

        // Next moves to track B (index 1)
        PlayQueueStateDto nextState = playQueueService.next(session);
        assertEquals(1, nextState.getCurrentIndex());
        assertEquals("Track B", nextState.getCurrentSong().getTitle());

        // Next wraps around to track A (index 0)
        PlayQueueStateDto wrapState = playQueueService.next(session);
        assertEquals(0, wrapState.getCurrentIndex());
        assertEquals("Track A", wrapState.getCurrentSong().getTitle());
    }

    @Test
    @DisplayName("previous() goes to previous track and wraps around to end when at 0")
    void testPreviousNavigation() {
        PlaylistSong ps1 = new PlaylistSong(playlist, songA, 1);
        PlaylistSong ps2 = new PlaylistSong(playlist, songB, 2);
        when(playlistRepository.findById(20L)).thenReturn(Optional.of(playlist));
        when(playlistSongRepository.findByPlaylistWithSongOrderByPositionAsc(playlist)).thenReturn(List.of(ps1, ps2));
        when(songRepository.existsById(anyLong())).thenReturn(true);

        playQueueService.playPlaylist(20L, false, session, user);

        // Previous from index 0 wraps to track B (index 1)
        PlayQueueStateDto prevState = playQueueService.previous(session);
        assertEquals(1, prevState.getCurrentIndex());
        assertEquals("Track B", prevState.getCurrentSong().getTitle());
    }

    @Test
    @DisplayName("addToQueue appends track to existing queue")
    void testAddToQueue() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(songA));
        when(songRepository.findById(2L)).thenReturn(Optional.of(songB));
        when(songRepository.existsById(anyLong())).thenReturn(true);

        playQueueService.playSong(1L, session);
        PlayQueueStateDto state = playQueueService.addToQueue(2L, session);

        assertEquals(2, state.getTotalTracks());
        assertEquals("Track A", state.getCurrentSong().getTitle());
        assertEquals("Track B", state.getQueue().get(1).getTitle());
    }

    @Test
    @DisplayName("clearQueue empties the queue and resets state")
    void testClearQueue() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(songA));

        playQueueService.playSong(1L, session);
        PlayQueueStateDto state = playQueueService.clearQueue(session);

        assertEquals(0, state.getTotalTracks());
        assertEquals(-1, state.getCurrentIndex());
        assertNull(state.getCurrentSong());
    }

    @Test
    @DisplayName("Deleted songs are cleaned up from queue gracefully")
    void testCleanDeletedSongs() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(songA));
        when(songRepository.findById(2L)).thenReturn(Optional.of(songB));
        when(songRepository.existsById(1L)).thenReturn(true);
        when(songRepository.existsById(2L)).thenReturn(true);

        // Start with 2 songs
        playQueueService.playSong(1L, session);
        playQueueService.addToQueue(2L, session);

        // Simulate song 2 being deleted from database
        when(songRepository.existsById(2L)).thenReturn(false);

        PlayQueueStateDto cleanState = playQueueService.getQueueState(session);

        assertEquals(1, cleanState.getTotalTracks(), "Deleted song must be pruned from session queue");
        assertEquals("Track A", cleanState.getCurrentSong().getTitle());
    }
}
