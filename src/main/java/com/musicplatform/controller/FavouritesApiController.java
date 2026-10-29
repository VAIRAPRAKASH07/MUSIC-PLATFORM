package com.musicplatform.controller;

import com.musicplatform.model.User;
import com.musicplatform.service.FavouritesService;
import com.musicplatform.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/favourites")
public class FavouritesApiController {

    private final FavouritesService favouritesService;
    private final UserService userService;

    public FavouritesApiController(FavouritesService favouritesService, UserService userService) {
        this.favouritesService = favouritesService;
        this.userService = userService;
    }

    private User resolveCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userService.getByUsername(userDetails.getUsername());
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleFavourite(@RequestParam("songId") Long songId,
                                                               @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = resolveCurrentUser(userDetails);
        boolean favourited = favouritesService.toggleFavourite(songId, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("songId", songId);
        response.put("favourited", favourited);
        response.put("message", favourited ? "Added to favourites" : "Removed from favourites");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFavouriteStatus(@RequestParam("songId") Long songId,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = resolveCurrentUser(userDetails);
        boolean favourited = favouritesService.isFavourited(currentUser, songId);

        Map<String, Object> response = new HashMap<>();
        response.put("songId", songId);
        response.put("favourited", favourited);
        return ResponseEntity.ok(response);
    }
}
