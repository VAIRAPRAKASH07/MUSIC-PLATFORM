package com.musicplatform.service;

import com.musicplatform.dto.PlaylistDto;
import com.musicplatform.dto.PlaylistForm;
import com.musicplatform.dto.PlaylistSongDto;
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
import com.musicplatform.util.TimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistService.class);

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;
    private final FavouritesService favouritesService;

    public PlaylistService(PlaylistRepository playlistRepository,
                           PlaylistSongRepository playlistSongRepository,
                           SongRepository songRepository,
                           FavouritesService favouritesService) {
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.songRepository = songRepository;
        this.favouritesService = favouritesService;
    }

    public List<PlaylistDto> getUserPlaylists(User owner) {
        List<Playlist> playlists = playlistRepository.findByOwnerAndSystemPlaylistFalseOrderByNameAsc(owner);
        return playlists.stream().map(this::toDto).collect(Collectors.toList());
    }

    public PlaylistDto getFavouritesPlaylistDto(User owner) {
        Playlist favourites = favouritesService.getOrCreateFavouritesPlaylist(owner);
        return toDto(favourites);
    }

    public PlaylistDto getPlaylistDto(Long id, User currentUser) {
        Playlist playlist = getAndVerifyOwner(id, currentUser);
        return toDto(playlist);
    }

    public List<PlaylistSongDto> getPlaylistSongs(Long id, User currentUser) {
        Playlist playlist = getAndVerifyOwner(id, currentUser);
        List<PlaylistSong> items = playlistSongRepository.findByPlaylistWithSongOrderByPositionAsc(playlist);
        Set<Long> favouritedIds = favouritesService.getFavouritedSongIds(currentUser);

        return items.stream().map(ps -> new PlaylistSongDto(
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
                favouritedIds.contains(ps.getSong().getId())
        )).collect(Collectors.toList());
    }

    @Transactional
    public PlaylistDto createPlaylist(PlaylistForm form, User owner) {
        String name = form.getName().trim();
        if ("Favourites".equalsIgnoreCase(name)) {
            throw new BusinessException("A custom playlist named 'Favourites' cannot be created");
        }

        if (playlistRepository.existsByOwnerAndName(owner, name)) {
            throw new DuplicateResourceException("You already have a playlist named '" + name + "'");
        }

        Playlist playlist = new Playlist(
                name,
                form.getDescription() != null ? form.getDescription().trim() : null,
                false,
                owner
        );
        Playlist saved = playlistRepository.save(playlist);
        log.info("Created playlist id={} for user={}", saved.getId(), owner.getUsername());
        return toDto(saved);
    }

    @Transactional
    public PlaylistDto updatePlaylist(Long id, PlaylistForm form, User currentUser) {
        Playlist playlist = getAndVerifyOwner(id, currentUser);

        if (Boolean.TRUE.equals(playlist.getSystemPlaylist())) {
            throw new ForbiddenOperationException("The system Favourites playlist cannot be renamed");
        }

        String newName = form.getName().trim();
        if ("Favourites".equalsIgnoreCase(newName)) {
            throw new BusinessException("A custom playlist cannot be named 'Favourites'");
        }

        if (!playlist.getName().equalsIgnoreCase(newName) && playlistRepository.existsByOwnerAndName(currentUser, newName)) {
            throw new DuplicateResourceException("You already have a playlist named '" + newName + "'");
        }

        playlist.setName(newName);
        playlist.setDescription(form.getDescription() != null ? form.getDescription().trim() : null);
        Playlist updated = playlistRepository.save(playlist);
        log.info("Updated playlist id={} by user={}", updated.getId(), currentUser.getUsername());
        return toDto(updated);
    }

    @Transactional
    public void deletePlaylist(Long id, User currentUser) {
        Playlist playlist = getAndVerifyOwner(id, currentUser);

        if (Boolean.TRUE.equals(playlist.getSystemPlaylist())) {
            throw new ForbiddenOperationException("The system Favourites playlist cannot be deleted");
        }

        playlistRepository.delete(playlist);
        log.info("Deleted playlist id={} by user={}", id, currentUser.getUsername());
    }

    @Transactional
    public void addSongToPlaylist(Long playlistId, Long songId, User currentUser) {
        Playlist playlist = getAndVerifyOwner(playlistId, currentUser);
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + songId));

        if (playlistSongRepository.existsByPlaylistAndSong(playlist, song)) {
            throw new DuplicateResourceException("This song is already in the playlist");
        }

        int maxPos = playlistSongRepository.findMaxPositionByPlaylist(playlist);
        PlaylistSong ps = new PlaylistSong(playlist, song, maxPos + 1);
        playlistSongRepository.save(ps);
        log.info("Added song id={} to playlist id={} at position={}", songId, playlistId, maxPos + 1);
    }

    @Transactional
    public void removeSongFromPlaylist(Long playlistId, Long songId, User currentUser) {
        Playlist playlist = getAndVerifyOwner(playlistId, currentUser);
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + songId));

        PlaylistSong ps = playlistSongRepository.findByPlaylistAndSong(playlist, song)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found in this playlist"));

        playlistSongRepository.delete(ps);
        compactPositions(playlist);
        log.info("Removed song id={} from playlist id={}", songId, playlistId);
    }

    @Transactional
    public void reorderSong(Long playlistId, Long songId, boolean moveUp, User currentUser) {
        Playlist playlist = getAndVerifyOwner(playlistId, currentUser);
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + songId));

        List<PlaylistSong> items = playlistSongRepository.findByPlaylistOrderByPositionAsc(playlist);
        int targetIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getSong().getId().equals(song.getId())) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            throw new ResourceNotFoundException("Song is not part of this playlist");
        }

        int swapIndex = moveUp ? targetIndex - 1 : targetIndex + 1;
        if (swapIndex < 0 || swapIndex >= items.size()) {
            return; // Already at the boundary
        }

        PlaylistSong currentItem = items.get(targetIndex);
        PlaylistSong neighborItem = items.get(swapIndex);

        int tempPos = currentItem.getPosition();
        currentItem.setPosition(neighborItem.getPosition());
        neighborItem.setPosition(tempPos);

        playlistSongRepository.save(currentItem);
        playlistSongRepository.save(neighborItem);
        log.info("Swapped positions in playlist id={} between song id={} and song id={}", playlistId, currentItem.getSong().getId(), neighborItem.getSong().getId());
    }

    public Playlist getAndVerifyOwner(Long id, User currentUser) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + id));

        boolean isOwner = currentUser != null && playlist.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && "ROLE_ADMIN".equals(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("You do not have permission to access this playlist");
        }
        return playlist;
    }

    @Transactional
    public void compactPositions(Playlist playlist) {
        List<PlaylistSong> items = playlistSongRepository.findByPlaylistOrderByPositionAsc(playlist);
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i + 1);
        }
        playlistSongRepository.saveAll(items);
    }

    private PlaylistDto toDto(Playlist playlist) {
        List<PlaylistSong> items = playlistSongRepository.findByPlaylistOrderByPositionAsc(playlist);
        int totalSeconds = items.stream()
                .filter(ps -> ps.getSong() != null)
                .mapToInt(ps -> ps.getSong().getDurationSeconds())
                .sum();

        return new PlaylistDto(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                playlist.getOwner() != null ? playlist.getOwner().getId() : null,
                playlist.getOwner() != null ? playlist.getOwner().getUsername() : "System",
                Boolean.TRUE.equals(playlist.getSystemPlaylist()),
                items.size(),
                totalSeconds,
                TimeFormatter.formatDuration(totalSeconds),
                playlist.getCreatedAt()
        );
    }
}
