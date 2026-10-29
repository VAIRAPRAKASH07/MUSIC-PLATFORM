package com.musicplatform.service;

import com.musicplatform.dto.SongDto;
import com.musicplatform.dto.SongForm;
import com.musicplatform.dto.SongSortField;
import com.musicplatform.exception.ForbiddenOperationException;
import com.musicplatform.exception.ResourceNotFoundException;
import com.musicplatform.model.Playlist;
import com.musicplatform.model.Song;
import com.musicplatform.model.User;
import com.musicplatform.repository.PlaylistRepository;
import com.musicplatform.repository.PlaylistSongRepository;
import com.musicplatform.repository.SongRepository;
import com.musicplatform.util.TimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SongService {

    private static final Logger log = LoggerFactory.getLogger(SongService.class);

    private final SongRepository songRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final PlaylistRepository playlistRepository;
    private final FavouritesService favouritesService;

    public SongService(SongRepository songRepository,
                       PlaylistSongRepository playlistSongRepository,
                       PlaylistRepository playlistRepository,
                       FavouritesService favouritesService) {
        this.songRepository = songRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.playlistRepository = playlistRepository;
        this.favouritesService = favouritesService;
    }

    public Page<SongDto> searchSongs(String query, String genre, int page, int size,
                                     String sortField, String sortDir, User currentUser) {
        String sanitizedField = SongSortField.sanitize(sortField);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, sanitizedField));

        Page<Song> songPage;
        if ((query != null && !query.trim().isEmpty()) || (genre != null && !genre.trim().isEmpty())) {
            songPage = songRepository.searchSongs(
                    query != null ? query.trim() : null,
                    genre != null && !genre.trim().isEmpty() ? genre.trim() : null,
                    pageable
            );
        } else {
            songPage = songRepository.findAll(pageable);
        }

        Set<Long> favouritedSongIds = currentUser != null ? favouritesService.getFavouritedSongIds(currentUser) : Set.of();

        return songPage.map(song -> toDto(song, currentUser, favouritedSongIds.contains(song.getId())));
    }

    public SongDto getSongDtoById(Long id, User currentUser) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + id));
        boolean favourited = currentUser != null && favouritesService.isFavourited(currentUser, id);
        return toDto(song, currentUser, favourited);
    }

    public Song getSongEntityById(Long id) {
        return songRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + id));
    }

    @Transactional
    public SongDto createSong(SongForm form, User creator) {
        Song song = new Song(
                form.getTitle().trim(),
                form.getArtist().trim(),
                form.getAlbum() != null ? form.getAlbum().trim() : null,
                form.getGenre().trim(),
                form.getDurationSeconds(),
                form.getReleaseYear(),
                creator
        );
        Song saved = songRepository.save(song);
        log.info("Created new song id={} by user={}", saved.getId(), creator.getUsername());
        return toDto(saved, creator, false);
    }

    @Transactional
    public SongDto updateSong(Long id, SongForm form, User currentUser) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + id));

        // Authorization check: Only creator or ADMIN can edit
        verifySongPermission(song, currentUser, "edit");

        song.setTitle(form.getTitle().trim());
        song.setArtist(form.getArtist().trim());
        song.setAlbum(form.getAlbum() != null ? form.getAlbum().trim() : null);
        song.setGenre(form.getGenre().trim());
        song.setDurationSeconds(form.getDurationSeconds());
        song.setReleaseYear(form.getReleaseYear());

        Song updated = songRepository.save(song);
        log.info("Updated song id={} by user={}", updated.getId(), currentUser.getUsername());
        boolean favourited = favouritesService.isFavourited(currentUser, id);
        return toDto(updated, currentUser, favourited);
    }

    @Transactional
    public void deleteSong(Long id, User currentUser) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + id));

        // Authorization check: Only creator or ADMIN can delete
        verifySongPermission(song, currentUser, "delete");

        // Requirement I: song deletion order is collect affected playlist IDs -> delete -> compact positions
        List<Long> affectedPlaylistIds = playlistSongRepository.findPlaylistIdsBySongId(id);
        log.info("Deleting song id={}; found {} affected playlists for reindexing", id, affectedPlaylistIds.size());

        playlistSongRepository.deleteBySong(song);
        songRepository.delete(song);

        // Reindex remaining songs in each affected playlist
        for (Long playlistId : affectedPlaylistIds) {
            playlistRepository.findById(playlistId).ifPresent(this::compactPlaylistPositions);
        }
        log.info("Song id={} deleted successfully and affected playlists compacted", id);
    }

    private void compactPlaylistPositions(Playlist playlist) {
        List<com.musicplatform.model.PlaylistSong> items = playlistSongRepository.findByPlaylistOrderByPositionAsc(playlist);
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i + 1);
        }
        playlistSongRepository.saveAll(items);
    }

    public List<String> getDistinctGenres() {
        return songRepository.findDistinctGenres();
    }

    private void verifySongPermission(Song song, User currentUser, String action) {
        if (currentUser == null) {
            throw new ForbiddenOperationException("Authentication required to " + action + " songs");
        }
        boolean isCreator = song.getCreatedBy() != null && song.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = "ROLE_ADMIN".equals(currentUser.getRole());
        if (!isCreator && !isAdmin) {
            throw new ForbiddenOperationException("Only the song creator or an administrator can " + action + " this song");
        }
    }

    private SongDto toDto(Song song, User currentUser, boolean favourited) {
        boolean canEdit = currentUser != null && (
                "ROLE_ADMIN".equals(currentUser.getRole()) ||
                (song.getCreatedBy() != null && song.getCreatedBy().getId().equals(currentUser.getId()))
        );

        return new SongDto(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getAlbum(),
                song.getGenre(),
                song.getDurationSeconds(),
                TimeFormatter.formatDuration(song.getDurationSeconds()),
                song.getReleaseYear(),
                song.getCreatedAt(),
                song.getCreatedBy() != null ? song.getCreatedBy().getId() : null,
                song.getCreatedBy() != null ? song.getCreatedBy().getUsername() : "System",
                canEdit,
                favourited
        );
    }
}
