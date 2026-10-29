package com.musicplatform.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SongDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private Integer durationSeconds;
    private String formattedDuration;
    private Integer releaseYear;
    private LocalDateTime createdAt;
    private Long createdById;
    private String createdByUsername;
    private boolean canEdit;
    private boolean favourited;

    public SongDto() {
    }

    public SongDto(Long id, String title, String artist, String album, String genre,
                   Integer durationSeconds, String formattedDuration, Integer releaseYear,
                   LocalDateTime createdAt, Long createdById, String createdByUsername,
                   boolean canEdit, boolean favourited) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.durationSeconds = durationSeconds;
        this.formattedDuration = formattedDuration;
        this.releaseYear = releaseYear;
        this.createdAt = createdAt;
        this.createdById = createdById;
        this.createdByUsername = createdByUsername;
        this.canEdit = canEdit;
        this.favourited = favourited;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getFormattedDuration() {
        return formattedDuration;
    }

    public void setFormattedDuration(String formattedDuration) {
        this.formattedDuration = formattedDuration;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isFavourited() {
        return favourited;
    }

    public void setFavourited(boolean favourited) {
        this.favourited = favourited;
    }
}
