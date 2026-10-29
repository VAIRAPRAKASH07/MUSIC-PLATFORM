package com.musicplatform.controller;

import com.musicplatform.dto.PlayQueueStateDto;
import com.musicplatform.model.User;
import com.musicplatform.service.PlayQueueService;
import com.musicplatform.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queue")
public class PlayQueueApiController {

    private final PlayQueueService playQueueService;
    private final UserService userService;

    public PlayQueueApiController(PlayQueueService playQueueService, UserService userService) {
        this.playQueueService = playQueueService;
        this.userService = userService;
    }

    private User resolveCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userService.findByUsername(userDetails.getUsername()).orElse(null);
    }

    @GetMapping("/state")
    public ResponseEntity<PlayQueueStateDto> getQueueState(HttpSession session) {
        return ResponseEntity.ok(playQueueService.getQueueState(session));
    }

    @PostMapping("/play-playlist")
    public ResponseEntity<PlayQueueStateDto> playPlaylist(@RequestParam("playlistId") Long playlistId,
                                                          @RequestParam(value = "shuffle", defaultValue = "false") boolean shuffle,
                                                          @AuthenticationPrincipal UserDetails userDetails,
                                                          HttpSession session) {
        User currentUser = resolveCurrentUser(userDetails);
        PlayQueueStateDto state = playQueueService.playPlaylist(playlistId, shuffle, session, currentUser);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/play-song")
    public ResponseEntity<PlayQueueStateDto> playSong(@RequestParam("songId") Long songId,
                                                      HttpSession session) {
        PlayQueueStateDto state = playQueueService.playSong(songId, session);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/next")
    public ResponseEntity<PlayQueueStateDto> next(HttpSession session) {
        return ResponseEntity.ok(playQueueService.next(session));
    }

    @PostMapping("/prev")
    public ResponseEntity<PlayQueueStateDto> previous(HttpSession session) {
        return ResponseEntity.ok(playQueueService.previous(session));
    }

    @PostMapping("/add")
    public ResponseEntity<PlayQueueStateDto> addToQueue(@RequestParam("songId") Long songId,
                                                        HttpSession session) {
        return ResponseEntity.ok(playQueueService.addToQueue(songId, session));
    }

    @DeleteMapping("/items/{index}")
    public ResponseEntity<PlayQueueStateDto> removeFromQueue(@PathVariable("index") int index,
                                                             HttpSession session) {
        return ResponseEntity.ok(playQueueService.removeFromQueue(index, session));
    }

    @PostMapping("/clear")
    public ResponseEntity<PlayQueueStateDto> clearQueue(HttpSession session) {
        return ResponseEntity.ok(playQueueService.clearQueue(session));
    }
}
