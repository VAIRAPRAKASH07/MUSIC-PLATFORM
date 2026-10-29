package com.musicplatform.repository;

import com.musicplatform.model.Playlist;
import com.musicplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    List<Playlist> findByOwnerOrderByNameAsc(User owner);

    List<Playlist> findByOwnerAndSystemPlaylistFalseOrderByNameAsc(User owner);

    Optional<Playlist> findByOwnerAndSystemPlaylistTrue(User owner);

    boolean existsByOwnerAndName(User owner, String name);

    boolean existsByOwnerAndNameIgnoreCase(User owner, String name);

    @Query("SELECT p FROM Playlist p LEFT JOIN FETCH p.owner WHERE p.id = :id")
    Optional<Playlist> findByIdWithOwner(@Param("id") Long id);
}
