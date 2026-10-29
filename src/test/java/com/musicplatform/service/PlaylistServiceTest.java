package com.musicplatform.service;

import com.musicplatform.dto.PlaylistDto;
import com.musicplatform.dto.PlaylistForm;
import com.musicplatform.exception.BusinessException;
import com.musicplatform.exception.DuplicateResourceException;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistSongRepository playlistSongRepository;

    @Mock
    private SongRepository songRepository;

    @Mock
    private FavouritesService favouritesService;

    private PlaylistService playlistService;

    private User owner;
    private User otherUser;
    private Playlist customPlaylist;
    private Playlist systemFavourites;
    private Song song1;
    private Song song2;

    @BeforeEach
    void setUp() {
        playlistService = new PlaylistService(playlistRepository, playlistSongRepository, songRepository, favouritesService);

        owner = new User("musicfan", "fan@test.com", "hash", "ROLE_USER");
        owner.setId(1L);

        otherUser = new User("intruder", "intruder@test.com", "hash", "ROLE_USER");
        otherUser.setId(2L);

        customPlaylist = new Playlist("Road Trip", "Upbeat tracks", false, owner);
        customPlaylist.setId(10L);

        systemFavourites = new Playlist("Favourites", "System favourites", true, owner);
        systemFavourites.setId(1L);

        song1 = new Song("Highway Star", "Deep Purple", "Machine Head", "Rock", 365, 1972, owner);
        song1.setId(101L);

        song2 = new Song("Speed King", "Deep Purple", "In Rock", "Rock", 350, 1970, owner);
        song2.setId(102L);
    }

    @Test
    @DisplayName("Create playlist blocks creation of custom playlist named 'Favourites'")
    void testCreatePlaylistBlocksFavouritesName() {
        PlaylistForm form = new PlaylistForm(null, "Favourites", "Sneaky favourites");
        assertThrows(BusinessException.class, () -> playlistService.createPlaylist(form, owner));

        PlaylistForm lowerForm = new PlaylistForm(null, "favourites", "lowercase test");
        assertThrows(BusinessException.class, () -> playlistService.createPlaylist(lowerForm, owner));

        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create playlist blocks duplicate playlist name for same user")
    void testCreatePlaylistBlocksDuplicateName() {
        when(playlistRepository.existsByOwnerAndName(owner, "Road Trip")).thenReturn(true);

        PlaylistForm form = new PlaylistForm(null, "Road Trip", "Duplicate");
        assertThrows(DuplicateResourceException.class, () -> playlistService.createPlaylist(form, owner));
    }

    @Test
    @DisplayName("Update playlist succeeds for custom playlist, blocks renaming system playlist")
    void testUpdatePlaylist() {
        when(playlistRepository.findById(10L)).thenReturn(Optional.of(customPlaylist));
        when(playlistRepository.save(any(Playlist.class))).thenReturn(customPlaylist);

        PlaylistForm updateForm = new PlaylistForm(10L, "Highway Cruising", "New desc");
        PlaylistDto updated = playlistService.updatePlaylist(10L, updateForm, owner);

        assertNotNull(updated);
        assertEquals("Highway Cruising", customPlaylist.getName());

        // Test updating system playlist
        when(playlistRepository.findById(1L)).thenReturn(Optional.of(systemFavourites));
        assertThrows(ForbiddenOperationException.class, () -> playlistService.updatePlaylist(1L, updateForm, owner));
    }

    @Test
    @DisplayName("Delete playlist succeeds for custom playlist, blocks deleting system playlist")
    void testDeletePlaylist() {
        when(playlistRepository.findById(10L)).thenReturn(Optional.of(customPlaylist));

        playlistService.deletePlaylist(10L, owner);
        verify(playlistRepository).delete(customPlaylist);

        // Test deleting system playlist
        when(playlistRepository.findById(1L)).thenReturn(Optional.of(systemFavourites));
        assertThrows(ForbiddenOperationException.class, () -> playlistService.deletePlaylist(1L, owner));
    }

    @Test
    @DisplayName("Accessing someone else's playlist throws ForbiddenOperationException")
    void testAccessControlOtherUser() {
        when(playlistRepository.findById(10L)).thenReturn(Optional.of(customPlaylist));

        assertThrows(ForbiddenOperationException.class, () -> playlistService.getPlaylistDto(10L, otherUser));
    }

    @Test
    @DisplayName("Add song to playlist appends to end and prevents duplicate song additions")
    void testAddSongToPlaylist() {
        when(playlistRepository.findById(10L)).thenReturn(Optional.of(customPlaylist));
        when(songRepository.findById(101L)).thenReturn(Optional.of(song1));
        when(playlistSongRepository.existsByPlaylistAndSong(customPlaylist, song1)).thenReturn(false);
        when(playlistSongRepository.findMaxPositionByPlaylist(customPlaylist)).thenReturn(2);

        playlistService.addSongToPlaylist(10L, 101L, owner);

        verify(playlistSongRepository).save(argThat(ps ->
                ps.getPosition() == 3 &&
                ps.getPlaylist().equals(customPlaylist) &&
                ps.getSong().equals(song1)
        ));

        // Adding duplicate throws DuplicateResourceException
        when(playlistSongRepository.existsByPlaylistAndSong(customPlaylist, song1)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> playlistService.addSongToPlaylist(10L, 101L, owner));
    }

    @Test
    @DisplayName("Reorder song swaps position with adjacent neighbor")
    void testReorderSong() {
        when(playlistRepository.findById(10L)).thenReturn(Optional.of(customPlaylist));
        when(songRepository.findById(102L)).thenReturn(Optional.of(song2));

        PlaylistSong ps1 = new PlaylistSong(customPlaylist, song1, 1);
        PlaylistSong ps2 = new PlaylistSong(customPlaylist, song2, 2);
        List<PlaylistSong> items = new ArrayList<>(List.of(ps1, ps2));

        when(playlistSongRepository.findByPlaylistOrderByPositionAsc(customPlaylist)).thenReturn(items);

        // Move song 2 UP
        playlistService.reorderSong(10L, 102L, true, owner);

        assertEquals(2, ps1.getPosition());
        assertEquals(1, ps2.getPosition());
        verify(playlistSongRepository).save(ps1);
        verify(playlistSongRepository).save(ps2);
    }

    @Test
    @DisplayName("getUserPlaylists returns user custom playlists mapped to DTO")
    void testGetUserPlaylists() {
        when(playlistRepository.findByOwnerAndSystemPlaylistFalseOrderByNameAsc(owner))
                .thenReturn(List.of(customPlaylist));
        when(playlistSongRepository.findByPlaylistOrderByPositionAsc(customPlaylist))
                .thenReturn(Collections.emptyList());

        var result = playlistService.getUserPlaylists(owner);
        assertEquals(1, result.size());
        assertEquals("Road Trip", result.get(0).getName());
    }

    @Test
    @DisplayName("getPlaylistSongs returns ordered songs with favourite flags")
    void testGetPlaylistSongs() {
        when(playlistRepository.findById(10L)).thenReturn(Optional.of(customPlaylist));
        PlaylistSong ps1 = new PlaylistSong(customPlaylist, song1, 1);
        when(playlistSongRepository.findByPlaylistWithSongOrderByPositionAsc(customPlaylist))
                .thenReturn(List.of(ps1));
        when(favouritesService.getFavouritedSongIds(owner)).thenReturn(Set.of(101L));

        var songs = playlistService.getPlaylistSongs(10L, owner);
        assertEquals(1, songs.size());
        assertEquals("Highway Star", songs.get(0).getTitle());
        assertTrue(songs.get(0).isFavourited());
    }

    @Test
    @DisplayName("removeSongFromPlaylist deletes song and compacts remaining positions")
    void testRemoveSongFromPlaylist() {
        when(playlistRepository.findById(10L)).thenReturn(Optional.of(customPlaylist));
        when(songRepository.findById(101L)).thenReturn(Optional.of(song1));
        PlaylistSong ps1 = new PlaylistSong(customPlaylist, song1, 1);
        when(playlistSongRepository.findByPlaylistAndSong(customPlaylist, song1)).thenReturn(Optional.of(ps1));
        when(playlistSongRepository.findByPlaylistOrderByPositionAsc(customPlaylist)).thenReturn(Collections.emptyList());

        playlistService.removeSongFromPlaylist(10L, 101L, owner);
        verify(playlistSongRepository).delete(ps1);
        verify(playlistSongRepository).saveAll(any());
    }
}
