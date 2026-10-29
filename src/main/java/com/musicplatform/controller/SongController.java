package com.musicplatform.controller;

import com.musicplatform.dto.SongDto;
import com.musicplatform.dto.SongForm;
import com.musicplatform.model.User;
import com.musicplatform.service.PlaylistService;
import com.musicplatform.service.SongService;
import com.musicplatform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/songs")
public class SongController {

    private final SongService songService;
    private final UserService userService;
    private final PlaylistService playlistService;

    public SongController(SongService songService, UserService userService, PlaylistService playlistService) {
        this.songService = songService;
        this.userService = userService;
        this.playlistService = playlistService;
    }

    private User resolveCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userService.findByUsername(userDetails.getUsername()).orElse(null);
    }

    @GetMapping
    public String listSongs(@RequestParam(value = "query", required = false) String query,
                            @RequestParam(value = "genre", required = false) String genre,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            @RequestParam(value = "sort", defaultValue = "createdAt") String sortField,
                            @RequestParam(value = "dir", defaultValue = "desc") String sortDir,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        User currentUser = resolveCurrentUser(userDetails);
        Page<SongDto> songsPage = songService.searchSongs(query, genre, page, size, sortField, sortDir, currentUser);

        model.addAttribute("songsPage", songsPage);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("genre", genre != null ? genre : "");
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("genres", songService.getDistinctGenres());

        if (currentUser != null) {
            model.addAttribute("userPlaylists", playlistService.getUserPlaylists(currentUser));
        }

        return "song/list";
    }

    @GetMapping("/new")
    public String newSongForm(Model model) {
        if (!model.containsAttribute("songForm")) {
            model.addAttribute("songForm", new SongForm());
        }
        model.addAttribute("isEdit", false);
        return "song/form";
    }

    @PostMapping({"", "/new"})
    public String createSong(@Valid @ModelAttribute("songForm") SongForm form,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "song/form";
        }
        User currentUser = resolveCurrentUser(userDetails);
        songService.createSong(form, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Song '" + form.getTitle() + "' added successfully!");
        return "redirect:/songs";
    }

    @GetMapping("/{id}")
    public String songDetail(@PathVariable("id") Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        User currentUser = resolveCurrentUser(userDetails);
        SongDto song = songService.getSongDtoById(id, currentUser);
        model.addAttribute("song", song);
        if (currentUser != null) {
            model.addAttribute("userPlaylists", playlistService.getUserPlaylists(currentUser));
        }
        return "song/detail";
    }

    @GetMapping("/{id}/edit")
    public String editSongForm(@PathVariable("id") Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        User currentUser = resolveCurrentUser(userDetails);
        SongDto song = songService.getSongDtoById(id, currentUser);

        if (!model.containsAttribute("songForm")) {
            SongForm form = new SongForm(
                    song.getId(),
                    song.getTitle(),
                    song.getArtist(),
                    song.getAlbum(),
                    song.getGenre(),
                    song.getDurationSeconds(),
                    song.getReleaseYear()
            );
            model.addAttribute("songForm", form);
        }
        model.addAttribute("isEdit", true);
        model.addAttribute("songId", id);
        return "song/form";
    }

    @PostMapping("/{id}/edit")
    public String updateSong(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("songForm") SongForm form,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("songId", id);
            return "song/form";
        }
        User currentUser = resolveCurrentUser(userDetails);
        songService.updateSong(id, form, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Song '" + form.getTitle() + "' updated successfully!");
        return "redirect:/songs";
    }

    @PostMapping("/{id}/delete")
    public String deleteSong(@PathVariable("id") Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        User currentUser = resolveCurrentUser(userDetails);
        songService.deleteSong(id, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Song deleted successfully.");
        return "redirect:/songs";
    }
}
