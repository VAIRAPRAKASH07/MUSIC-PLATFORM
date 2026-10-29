package com.musicplatform.service;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavouritesServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistSongRepository playlistSongRepository;

    @Mock
    private SongRepository songRepository;

    private FavouritesService favouritesService;

    private User user;
    private Playlist favouritesPlaylist;
    private Song testSong;

    @BeforeEach
    void setUp() {
        favouritesService = new FavouritesService(playlistRepository, playlistSongRepository, songRepository);

        user = new User("tester", "test@music.com", "hash", "ROLE_USER");
        user.setId(1L);

        favouritesPlaylist = new Playlist("Favourites", "System favourites", true, user);
        favouritesPlaylist.setId(10L);

        testSong = new Song("Bohemian Rhapsody", "Queen", "A Night at the Opera", "Rock", 354, 1975, user);
        testSong.setId(500L);
    }

    @Test
    @DisplayName("Toggle favourite when not present adds track to Favourites playlist and returns true")
    void testToggleFavouriteAdd() {
        when(songRepository.findById(500L)).thenReturn(Optional.of(testSong));
        when(playlistRepository.findByOwnerAndSystemPlaylistTrue(user)).thenReturn(Optional.of(favouritesPlaylist));
        when(playlistSongRepository.findByPlaylistAndSong(favouritesPlaylist, testSong)).thenReturn(Optional.empty());
        when(playlistSongRepository.findMaxPositionByPlaylist(favouritesPlaylist)).thenReturn(4);

        boolean result = favouritesService.toggleFavourite(500L, user);

        assertTrue(result, "Adding favourite should return true");
        verify(playlistSongRepository).save(argThat(ps ->
                ps.getPlaylist().equals(favouritesPlaylist) &&
                ps.getSong().equals(testSong) &&
                ps.getPosition() == 5
        ));
    }

    @Test
    @DisplayName("Toggle favourite when already present removes track, compacts positions, and returns false")
    void testToggleFavouriteRemove() {
        PlaylistSong existingPs = new PlaylistSong(favouritesPlaylist, testSong, 3);
        when(songRepository.findById(500L)).thenReturn(Optional.of(testSong));
        when(playlistRepository.findByOwnerAndSystemPlaylistTrue(user)).thenReturn(Optional.of(favouritesPlaylist));
        when(playlistSongRepository.findByPlaylistAndSong(favouritesPlaylist, testSong)).thenReturn(Optional.of(existingPs));
        when(playlistSongRepository.findByPlaylistOrderByPositionAsc(favouritesPlaylist)).thenReturn(new ArrayList<>());

        boolean result = favouritesService.toggleFavourite(500L, user);

        assertFalse(result, "Removing favourite should return false");
        verify(playlistSongRepository).delete(existingPs);
        verify(playlistSongRepository).findByPlaylistOrderByPositionAsc(favouritesPlaylist);
    }

    @Test
    @DisplayName("isFavourited correctly returns true when song exists in Favourites playlist")
    void testIsFavourited() {
        when(playlistRepository.findByOwnerAndSystemPlaylistTrue(user)).thenReturn(Optional.of(favouritesPlaylist));
        when(songRepository.findById(500L)).thenReturn(Optional.of(testSong));
        when(playlistSongRepository.existsByPlaylistAndSong(favouritesPlaylist, testSong)).thenReturn(true);

        assertTrue(favouritesService.isFavourited(user, 500L));
    }

    @Test
    @DisplayName("getFavouritedSongIds returns set of all song IDs in favourites")
    void testGetFavouritedSongIds() {
        PlaylistSong ps1 = new PlaylistSong(favouritesPlaylist, testSong, 1);
        when(playlistRepository.findByOwnerAndSystemPlaylistTrue(user)).thenReturn(Optional.of(favouritesPlaylist));
        when(playlistSongRepository.findByPlaylistOrderByPositionAsc(favouritesPlaylist)).thenReturn(List.of(ps1));

        Set<Long> ids = favouritesService.getFavouritedSongIds(user);
        assertEquals(1, ids.size());
        assertTrue(ids.contains(500L));
    }

    @Test
    @DisplayName("getOrCreateFavouritesPlaylist creates a new system playlist if one is not present")
    void testGetOrCreateFavouritesPlaylistWhenMissing() {
        when(playlistRepository.findByOwnerAndSystemPlaylistTrue(user)).thenReturn(Optional.empty());
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(inv -> inv.getArgument(0));

        Playlist created = favouritesService.getOrCreateFavouritesPlaylist(user);
        assertNotNull(created);
        assertEquals("Favourites", created.getName());
        assertTrue(created.getSystemPlaylist());
    }

    @Test
    @DisplayName("getFavouritesSongs returns mapped PlaylistSongDto list")
    void testGetFavouritesSongs() {
        PlaylistSong ps1 = new PlaylistSong(favouritesPlaylist, testSong, 1);
        ps1.setId(99L);
        when(playlistRepository.findByOwnerAndSystemPlaylistTrue(user)).thenReturn(Optional.of(favouritesPlaylist));
        when(playlistSongRepository.findByPlaylistWithSongOrderByPositionAsc(favouritesPlaylist)).thenReturn(List.of(ps1));

        var songs = favouritesService.getFavouritesSongs(user);
        assertEquals(1, songs.size());
        assertEquals("Bohemian Rhapsody", songs.get(0).getTitle());
    }

    @Test
    @DisplayName("isFavourited returns false when user or song is null")
    void testIsFavouritedNullInputs() {
        assertFalse(favouritesService.isFavourited(null, 500L));
        assertFalse(favouritesService.isFavourited(user, null));
    }
}
