package com.musicplatform.service;

import com.musicplatform.dto.UserRegistrationDto;
import com.musicplatform.exception.BusinessException;
import com.musicplatform.exception.DuplicateResourceException;
import com.musicplatform.exception.ResourceNotFoundException;
import com.musicplatform.model.Playlist;
import com.musicplatform.model.User;
import com.musicplatform.repository.PlaylistRepository;
import com.musicplatform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PlaylistRepository playlistRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.playlistRepository = playlistRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        if (userRepository.existsByUsername(dto.getUsername().trim())) {
            throw new DuplicateResourceException("Username '" + dto.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("Email '" + dto.getEmail() + "' is already registered");
        }

        // Requirement B: Registration always assigns ROLE_USER; role is never bound from forms.
        User user = new User(
                dto.getUsername().trim(),
                dto.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(dto.getPassword()),
                "ROLE_USER"
        );
        User savedUser = userRepository.save(user);
        log.info("Registered new user: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        // Favourites Rule: On registration, auto-create a system playlist named 'Favourites' for the user.
        Playlist favourites = new Playlist(
                "Favourites",
                "Your automatically managed favourite tracks",
                true,
                savedUser
        );
        playlistRepository.save(favourites);
        log.info("Auto-provisioned system Favourites playlist for user id={}", savedUser.getId());

        return savedUser;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}
