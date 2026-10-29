package com.musicplatform.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayQueueStateDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private QueueItemDto currentSong;
    private List<QueueItemDto> queue = new ArrayList<>();
    private int currentIndex = -1;
    private boolean shuffled = false;
    private Long playlistId;
    private String playlistName;
    private int totalTracks = 0;

    public PlayQueueStateDto() {
    }

    public PlayQueueStateDto(QueueItemDto currentSong, List<QueueItemDto> queue, int currentIndex,
                             boolean shuffled, Long playlistId, String playlistName, int totalTracks) {
        this.currentSong = currentSong;
        this.queue = queue != null ? queue : new ArrayList<>();
        this.currentIndex = currentIndex;
        this.shuffled = shuffled;
        this.playlistId = playlistId;
        this.playlistName = playlistName;
        this.totalTracks = totalTracks;
    }

    public QueueItemDto getCurrentSong() {
        return currentSong;
    }

    public void setCurrentSong(QueueItemDto currentSong) {
        this.currentSong = currentSong;
    }

    public List<QueueItemDto> getQueue() {
        return queue;
    }

    public void setQueue(List<QueueItemDto> queue) {
        this.queue = queue != null ? queue : new ArrayList<>();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public boolean isShuffled() {
        return shuffled;
    }

    public void setShuffled(boolean shuffled) {
        this.shuffled = shuffled;
    }

    public Long getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(Long playlistId) {
        this.playlistId = playlistId;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public int getTotalTracks() {
        return totalTracks;
    }

    public void setTotalTracks(int totalTracks) {
        this.totalTracks = totalTracks;
    }
}
