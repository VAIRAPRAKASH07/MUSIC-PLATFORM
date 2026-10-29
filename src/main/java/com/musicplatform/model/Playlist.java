package com.musicplatform.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "playlists",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_playlist_owner_name", columnNames = {"owner_id", "name"})
    }
)
public class Playlist implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "system_playlist", nullable = false)
    private Boolean systemPlaylist = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Playlist() {
    }

    public Playlist(String name, String description, Boolean systemPlaylist, User owner) {
        this.name = name;
        this.description = description;
        this.systemPlaylist = systemPlaylist != null ? systemPlaylist : false;
        this.owner = owner;
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

    public Boolean getSystemPlaylist() {
        return systemPlaylist;
    }

    public void setSystemPlaylist(Boolean systemPlaylist) {
        this.systemPlaylist = systemPlaylist;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
