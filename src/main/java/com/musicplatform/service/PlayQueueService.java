package com.musicplatform.service;

import com.musicplatform.dto.PlayQueueStateDto;
import com.musicplatform.dto.QueueItemDto;
import com.musicplatform.exception.ResourceNotFoundException;
import com.musicplatform.model.Playlist;
import com.musicplatform.model.PlaylistSong;
import com.musicplatform.model.Song;
import com.musicplatform.model.User;
import com.musicplatform.repository.PlaylistRepository;
import com.musicplatform.repository.PlaylistSongRepository;
import com.musicplatform.repository.SongRepository;
import com.musicplatform.util.FisherYatesShuffler;
import com.musicplatform.util.TimeFormatter;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PlayQueueService {

    private static final Logger log = LoggerFactory.getLogger(PlayQueueService.class);
    public static final String SESSION_QUEUE_KEY = "PLAY_QUEUE_STATE";

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;
    private final FisherYatesShuffler shuffler;

    public PlayQueueService(PlaylistRepository playlistRepository,
                            PlaylistSongRepository playlistSongRepository,
                            SongRepository songRepository,
                            FisherYatesShuffler shuffler) {
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.songRepository = songRepository;
        this.shuffler = shuffler;
    }

    public PlayQueueStateDto getQueueState(HttpSession session) {
        PlayQueueStateDto state = (PlayQueueStateDto) session.getAttribute(SESSION_QUEUE_KEY);
        if (state == null) {
            state = new PlayQueueStateDto();
            session.setAttribute(SESSION_QUEUE_KEY, state);
            return state;
        }

        // Requirement H: Handle queued songs that were deleted in the meantime
        cleanDeletedSongs(state);
        session.setAttribute(SESSION_QUEUE_KEY, state);
        return state;
    }

    public PlayQueueStateDto playPlaylist(Long playlistId, boolean shuffle, HttpSession session, User currentUser) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + playlistId));

        List<PlaylistSong> playlistSongs = playlistSongRepository.findByPlaylistWithSongOrderByPositionAsc(playlist);
        if (playlistSongs.isEmpty()) {
            PlayQueueStateDto emptyState = new PlayQueueStateDto(null, new ArrayList<>(), -1, shuffle, playlist.getId(), playlist.getName(), 0);
            session.setAttribute(SESSION_QUEUE_KEY, emptyState);
            return emptyState;
        }

        List<QueueItemDto> items = playlistSongs.stream()
                .filter(ps -> ps.getSong() != null)
                .map(ps -> toQueueItem(ps.getSong()))
                .collect(Collectors.toList());

        List<QueueItemDto> finalQueue = shuffle ? shuffler.shuffle(items) : items;
        QueueItemDto currentSong = !finalQueue.isEmpty() ? finalQueue.get(0) : null;

        PlayQueueStateDto state = new PlayQueueStateDto(
                currentSong,
                finalQueue,
                currentSong != null ? 0 : -1,
                shuffle,
                playlist.getId(),
                playlist.getName(),
                finalQueue.size()
        );

        session.setAttribute(SESSION_QUEUE_KEY, state);
        log.info("Started playing playlist id={}, tracks={}, shuffle={}", playlistId, finalQueue.size(), shuffle);
        return state;
    }

    public PlayQueueStateDto playSong(Long songId, HttpSession session) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + songId));

        QueueItemDto item = toQueueItem(song);
        List<QueueItemDto> queue = new ArrayList<>();
        queue.add(item);

        PlayQueueStateDto state = new PlayQueueStateDto(
                item,
                queue,
                0,
                false,
                null,
                "Single Track",
                1
        );

        session.setAttribute(SESSION_QUEUE_KEY, state);
        log.info("Directly playing song id={}, title='{}'", songId, song.getTitle());
        return state;
    }

    public PlayQueueStateDto next(HttpSession session) {
        PlayQueueStateDto state = getQueueState(session);
        List<QueueItemDto> queue = state.getQueue();

        if (queue.isEmpty()) {
            state.setCurrentIndex(-1);
            state.setCurrentSong(null);
            return state;
        }

        int nextIndex = (state.getCurrentIndex() + 1) % queue.size();
        state.setCurrentIndex(nextIndex);
        state.setCurrentSong(queue.get(nextIndex));
        session.setAttribute(SESSION_QUEUE_KEY, state);
        return state;
    }

    public PlayQueueStateDto previous(HttpSession session) {
        PlayQueueStateDto state = getQueueState(session);
        List<QueueItemDto> queue = state.getQueue();

        if (queue.isEmpty()) {
            state.setCurrentIndex(-1);
            state.setCurrentSong(null);
            return state;
        }

        int prevIndex = (state.getCurrentIndex() - 1 + queue.size()) % queue.size();
        state.setCurrentIndex(prevIndex);
        state.setCurrentSong(queue.get(prevIndex));
        session.setAttribute(SESSION_QUEUE_KEY, state);
        return state;
    }

    public PlayQueueStateDto addToQueue(Long songId, HttpSession session) {
        PlayQueueStateDto state = getQueueState(session);
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + songId));

        QueueItemDto item = toQueueItem(song);
        List<QueueItemDto> queue = state.getQueue();
        queue.add(item);
        state.setTotalTracks(queue.size());

        if (state.getCurrentSong() == null) {
            state.setCurrentIndex(0);
            state.setCurrentSong(item);
        }

        session.setAttribute(SESSION_QUEUE_KEY, state);
        log.info("Added song id={} to session queue. Queue size now: {}", songId, queue.size());
        return state;
    }

    public PlayQueueStateDto removeFromQueue(int index, HttpSession session) {
        PlayQueueStateDto state = getQueueState(session);
        List<QueueItemDto> queue = state.getQueue();

        if (index >= 0 && index < queue.size()) {
            queue.remove(index);
            state.setTotalTracks(queue.size());

            if (queue.isEmpty()) {
                state.setCurrentIndex(-1);
                state.setCurrentSong(null);
            } else if (index == state.getCurrentIndex()) {
                int newIndex = Math.min(index, queue.size() - 1);
                state.setCurrentIndex(newIndex);
                state.setCurrentSong(queue.get(newIndex));
            } else if (index < state.getCurrentIndex()) {
                state.setCurrentIndex(state.getCurrentIndex() - 1);
            }
        }

        session.setAttribute(SESSION_QUEUE_KEY, state);
        return state;
    }

    public PlayQueueStateDto clearQueue(HttpSession session) {
        PlayQueueStateDto state = new PlayQueueStateDto();
        session.setAttribute(SESSION_QUEUE_KEY, state);
        log.info("Cleared session play queue");
        return state;
    }

    private void cleanDeletedSongs(PlayQueueStateDto state) {
        List<QueueItemDto> queue = state.getQueue();
        if (queue.isEmpty()) {
            return;
        }

        List<QueueItemDto> validQueue = new ArrayList<>();
        for (QueueItemDto item : queue) {
            if (songRepository.existsById(item.getSongId())) {
                validQueue.add(item);
            }
        }

        if (validQueue.size() != queue.size()) {
            log.info("Removed {} deleted songs from active session queue", queue.size() - validQueue.size());
            state.setQueue(validQueue);
            state.setTotalTracks(validQueue.size());

            if (validQueue.isEmpty()) {
                state.setCurrentIndex(-1);
                state.setCurrentSong(null);
            } else if (state.getCurrentIndex() >= validQueue.size()) {
                state.setCurrentIndex(0);
                state.setCurrentSong(validQueue.get(0));
            } else {
                state.setCurrentSong(validQueue.get(state.getCurrentIndex()));
            }
        }
    }

    private QueueItemDto toQueueItem(Song song) {
        return new QueueItemDto(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getAlbum(),
                song.getGenre(),
                song.getDurationSeconds(),
                TimeFormatter.formatDuration(song.getDurationSeconds())
        );
    }
}
