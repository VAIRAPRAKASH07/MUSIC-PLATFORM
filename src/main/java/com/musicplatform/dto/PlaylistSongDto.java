package com.musicplatform.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PlaylistSongDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long songId;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private Integer durationSeconds;
    private String formattedDuration;
    private Integer releaseYear;
    private Integer position;
    private LocalDateTime addedAt;
    private boolean favourited;

    public PlaylistSongDto() {
    }

    public PlaylistSongDto(Long id, Long songId, String title, String artist, String album,
                           String genre, Integer durationSeconds, String formattedDuration,
                           Integer releaseYear, Integer position, LocalDateTime addedAt, boolean favourited) {
        this.id = id;
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.durationSeconds = durationSeconds;
        this.formattedDuration = formattedDuration;
        this.releaseYear = releaseYear;
        this.position = position;
        this.addedAt = addedAt;
        this.favourited = favourited;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSongId() {
        return songId;
    }

    public void setSongId(Long songId) {
        this.songId = songId;
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

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public boolean isFavourited() {
        return favourited;
    }

    public void setFavourited(boolean favourited) {
        this.favourited = favourited;
    }
}
