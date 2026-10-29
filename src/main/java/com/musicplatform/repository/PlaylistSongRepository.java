package com.musicplatform.repository;

import com.musicplatform.model.Playlist;
import com.musicplatform.model.PlaylistSong;
import com.musicplatform.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {

    List<PlaylistSong> findByPlaylistOrderByPositionAsc(Playlist playlist);

    @Query("SELECT ps FROM PlaylistSong ps JOIN FETCH ps.song s JOIN FETCH s.createdBy WHERE ps.playlist = :playlist ORDER BY ps.position ASC")
    List<PlaylistSong> findByPlaylistWithSongOrderByPositionAsc(@Param("playlist") Playlist playlist);

    Optional<PlaylistSong> findByPlaylistAndSong(Playlist playlist, Song song);

    boolean existsByPlaylistAndSong(Playlist playlist, Song song);

    @Query("SELECT COALESCE(MAX(ps.position), 0) FROM PlaylistSong ps WHERE ps.playlist = :playlist")
    int findMaxPositionByPlaylist(@Param("playlist") Playlist playlist);

    @Query("SELECT DISTINCT ps.playlist.id FROM PlaylistSong ps WHERE ps.song.id = :songId")
    List<Long> findPlaylistIdsBySongId(@Param("songId") Long songId);

    void deleteByPlaylistAndSong(Playlist playlist, Song song);

    void deleteBySong(Song song);
}
