package com.musicplatform.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PlaylistDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerUsername;
    private boolean systemPlaylist;
    private int songCount;
    private int totalDurationSeconds;
    private String formattedTotalDuration;
    private LocalDateTime createdAt;

    public PlaylistDto() {
    }

    public PlaylistDto(Long id, String name, String description, Long ownerId, String ownerUsername,
                       boolean systemPlaylist, int songCount, int totalDurationSeconds,
                       String formattedTotalDuration, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.systemPlaylist = systemPlaylist;
        this.songCount = songCount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.formattedTotalDuration = formattedTotalDuration;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public boolean isSystemPlaylist() {
        return systemPlaylist;
    }

    public void setSystemPlaylist(boolean systemPlaylist) {
        this.systemPlaylist = systemPlaylist;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public int getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(int totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public String getFormattedTotalDuration() {
        return formattedTotalDuration;
    }

    public void setFormattedTotalDuration(String formattedTotalDuration) {
        this.formattedTotalDuration = formattedTotalDuration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
