package com.musicplatform.service;

import com.musicplatform.dto.PlaylistSongDto;
import com.musicplatform.exception.ResourceNotFoundException;
import com.musicplatform.model.Playlist;
import com.musicplatform.model.PlaylistSong;
import com.musicplatform.model.Song;
import com.musicplatform.model.User;
import com.musicplatform.repository.PlaylistRepository;
import com.musicplatform.repository.PlaylistSongRepository;
import com.musicplatform.repository.SongRepository;
import com.musicplatform.util.TimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FavouritesService {

    private static final Logger log = LoggerFactory.getLogger(FavouritesService.class);

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;

    public FavouritesService(PlaylistRepository playlistRepository,
                             PlaylistSongRepository playlistSongRepository,
                             SongRepository songRepository) {
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.songRepository = songRepository;
    }

    public Playlist getOrCreateFavouritesPlaylist(User user) {
        return playlistRepository.findByOwnerAndSystemPlaylistTrue(user)
                .orElseGet(() -> {
                    log.warn("System Favourites playlist missing for user id={}, creating now", user.getId());
                    Playlist favourites = new Playlist("Favourites", "Your automatically managed favourite tracks", true, user);
                    return playlistRepository.save(favourites);
                });
    }

    @Transactional
    public boolean toggleFavourite(Long songId, User user) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + songId));

        Playlist favourites = getOrCreateFavouritesPlaylist(user);
        Optional<PlaylistSong> existing = playlistSongRepository.findByPlaylistAndSong(favourites, song);

        if (existing.isPresent()) {
            playlistSongRepository.delete(existing.get());
            log.info("User id={} unfavourited song id={}", user.getId(), songId);
            compactPlaylistPositions(favourites);
            return false;
        } else {
            int maxPos = playlistSongRepository.findMaxPositionByPlaylist(favourites);
            PlaylistSong ps = new PlaylistSong(favourites, song, maxPos + 1);
            playlistSongRepository.save(ps);
            log.info("User id={} favourited song id={}", user.getId(), songId);
            return true;
        }
    }

    public boolean isFavourited(User user, Long songId) {
        if (user == null || songId == null) {
            return false;
        }
        Optional<Playlist> favourites = playlistRepository.findByOwnerAndSystemPlaylistTrue(user);
        if (favourites.isEmpty()) {
            return false;
        }
        Song song = songRepository.findById(songId).orElse(null);
        if (song == null) {
            return false;
        }
        return playlistSongRepository.existsByPlaylistAndSong(favourites.get(), song);
    }

    public Set<Long> getFavouritedSongIds(User user) {
        if (user == null) {
            return Collections.emptySet();
        }
        Optional<Playlist> favourites = playlistRepository.findByOwnerAndSystemPlaylistTrue(user);
        if (favourites.isEmpty()) {
            return Collections.emptySet();
        }
        List<PlaylistSong> songs = playlistSongRepository.findByPlaylistOrderByPositionAsc(favourites.get());
        return songs.stream().map(ps -> ps.getSong().getId()).collect(Collectors.toSet());
    }

    public List<PlaylistSongDto> getFavouritesSongs(User user) {
        Playlist favourites = getOrCreateFavouritesPlaylist(user);
        List<PlaylistSong> songs = playlistSongRepository.findByPlaylistWithSongOrderByPositionAsc(favourites);
        return songs.stream().map(ps -> new PlaylistSongDto(
                ps.getId(),
                ps.getSong().getId(),
                ps.getSong().getTitle(),
                ps.getSong().getArtist(),
                ps.getSong().getAlbum(),
                ps.getSong().getGenre(),
                ps.getSong().getDurationSeconds(),
                TimeFormatter.formatDuration(ps.getSong().getDurationSeconds()),
                ps.getSong().getReleaseYear(),
                ps.getPosition(),
                ps.getAddedAt(),
                true
        )).collect(Collectors.toList());
    }

    @Transactional
    public void compactPlaylistPositions(Playlist playlist) {
        List<PlaylistSong> items = playlistSongRepository.findByPlaylistOrderByPositionAsc(playlist);
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i + 1);
        }
        playlistSongRepository.saveAll(items);
    }
}
