package com.musicplatform.dto;

import java.io.Serializable;

public class QueueItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long songId;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private Integer durationSeconds;
    private String formattedDuration;

    public QueueItemDto() {
    }

    public QueueItemDto(Long songId, String title, String artist, String album, String genre, Integer durationSeconds, String formattedDuration) {
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.durationSeconds = durationSeconds;
        this.formattedDuration = formattedDuration;
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
}
