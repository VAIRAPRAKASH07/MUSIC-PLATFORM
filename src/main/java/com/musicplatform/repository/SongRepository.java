package com.musicplatform.repository;

import com.musicplatform.model.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

    @Override
    @EntityGraph(attributePaths = {"createdBy"})
    Page<Song> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"createdBy"})
    Optional<Song> findById(Long id);

    @EntityGraph(attributePaths = {"createdBy"})
    @Query("SELECT s FROM Song s WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(s.artist) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " (s.album IS NOT NULL AND LOWER(s.album) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           " LOWER(s.genre) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:genre IS NULL OR :genre = '' OR LOWER(s.genre) = LOWER(:genre))")
    Page<Song> searchSongs(@Param("query") String query, @Param("genre") String genre, Pageable pageable);

    @Query("SELECT DISTINCT s.genre FROM Song s WHERE s.genre IS NOT NULL ORDER BY s.genre ASC")
    List<String> findDistinctGenres();

    List<Song> findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(String title, String artist);
}
