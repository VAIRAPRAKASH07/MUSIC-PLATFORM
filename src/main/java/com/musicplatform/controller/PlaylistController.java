package com.musicplatform.controller;

import com.musicplatform.dto.PlaylistDto;
import com.musicplatform.dto.PlaylistForm;
import com.musicplatform.dto.PlaylistSongDto;
import com.musicplatform.model.User;
import com.musicplatform.service.FavouritesService;
import com.musicplatform.service.PlaylistService;
import com.musicplatform.service.SongService;
import com.musicplatform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final FavouritesService favouritesService;
    private final UserService userService;
    private final SongService songService;

    public PlaylistController(PlaylistService playlistService,
                              FavouritesService favouritesService,
                              UserService userService,
                              SongService songService) {
        this.playlistService = playlistService;
        this.favouritesService = favouritesService;
        this.userService = userService;
        this.songService = songService;
    }

    private User resolveCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userService.getByUsername(userDetails.getUsername());
    }

    @GetMapping
    public String listPlaylists(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = resolveCurrentUser(userDetails);
        List<PlaylistDto> customPlaylists = playlistService.getUserPlaylists(currentUser);
        PlaylistDto favourites = playlistService.getFavouritesPlaylistDto(currentUser);

        model.addAttribute("customPlaylists", customPlaylists);
        model.addAttribute("favouritesPlaylist", favourites);
        return "playlist/list";
    }

    @GetMapping("/favourites")
    public String viewFavourites(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = resolveCurrentUser(userDetails);
        PlaylistDto favourites = playlistService.getFavouritesPlaylistDto(currentUser);
        List<PlaylistSongDto> songs = favouritesService.getFavouritesSongs(currentUser);

        model.addAttribute("playlist", favourites);
        model.addAttribute("playlistSongs", songs);
        model.addAttribute("isFavourites", true);
        model.addAttribute("userPlaylists", playlistService.getUserPlaylists(currentUser));
        return "playlist/favourites";
    }

    @GetMapping("/new")
    public String newPlaylistForm(Model model) {
        if (!model.containsAttribute("playlistForm")) {
            model.addAttribute("playlistForm", new PlaylistForm());
        }
        model.addAttribute("isEdit", false);
        return "playlist/form";
    }

    @PostMapping("/new")
    public String createPlaylist(@Valid @ModelAttribute("playlistForm") PlaylistForm form,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "playlist/form";
        }
        User currentUser = resolveCurrentUser(userDetails);
        PlaylistDto created = playlistService.createPlaylist(form, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Playlist '" + created.getName() + "' created successfully!");
        return "redirect:/playlists/" + created.getId();
    }

    @GetMapping("/{id}")
    public String viewPlaylist(@PathVariable("id") Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        User currentUser = resolveCurrentUser(userDetails);
        PlaylistDto playlist = playlistService.getPlaylistDto(id, currentUser);
        List<PlaylistSongDto> songs = playlistService.getPlaylistSongs(id, currentUser);

        model.addAttribute("playlist", playlist);
        model.addAttribute("playlistSongs", songs);
        model.addAttribute("isFavourites", playlist.isSystemPlaylist());
        model.addAttribute("userPlaylists", playlistService.getUserPlaylists(currentUser));
        return "playlist/detail";
    }

    @GetMapping("/{id}/edit")
    public String editPlaylistForm(@PathVariable("id") Long id,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {
        User currentUser = resolveCurrentUser(userDetails);
        PlaylistDto playlist = playlistService.getPlaylistDto(id, currentUser);

        if (!model.containsAttribute("playlistForm")) {
            PlaylistForm form = new PlaylistForm(playlist.getId(), playlist.getName(), playlist.getDescription());
            model.addAttribute("playlistForm", form);
        }
        model.addAttribute("isEdit", true);
        model.addAttribute("playlistId", id);
        return "playlist/form";
    }

    @PostMapping("/{id}/edit")
    public String updatePlaylist(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("playlistForm") PlaylistForm form,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("playlistId", id);
            return "playlist/form";
        }
        User currentUser = resolveCurrentUser(userDetails);
        playlistService.updatePlaylist(id, form, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Playlist updated successfully!");
        return "redirect:/playlists/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deletePlaylist(@PathVariable("id") Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = resolveCurrentUser(userDetails);
        playlistService.deletePlaylist(id, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Playlist deleted successfully.");
        return "redirect:/playlists";
    }

    @PostMapping("/{id}/songs/add")
    public String addSongToPlaylist(@PathVariable("id") Long id,
                                    @RequestParam("songId") Long songId,
                                    @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = resolveCurrentUser(userDetails);
        playlistService.addSongToPlaylist(id, songId, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Song added to playlist!");
        return "redirect:" + (redirectUrl != null && !redirectUrl.isBlank() ? redirectUrl : "/playlists/" + id);
    }

    @PostMapping("/{id}/songs/{songId}/remove")
    public String removeSongFromPlaylist(@PathVariable("id") Long id,
                                         @PathVariable("songId") Long songId,
                                         @AuthenticationPrincipal UserDetails userDetails,
                                         RedirectAttributes redirectAttributes) {
        User currentUser = resolveCurrentUser(userDetails);
        playlistService.removeSongFromPlaylist(id, songId, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Song removed from playlist.");
        return "redirect:/playlists/" + id;
    }

    @PostMapping("/{id}/songs/{songId}/move")
    public String moveSongPosition(@PathVariable("id") Long id,
                                   @PathVariable("songId") Long songId,
                                   @RequestParam("direction") String direction,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = resolveCurrentUser(userDetails);
        boolean moveUp = "up".equalsIgnoreCase(direction);
        playlistService.reorderSong(id, songId, moveUp, currentUser);
        return "redirect:/playlists/" + id;
    }
}
