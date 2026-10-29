package com.musicplatform.service;

import com.musicplatform.dto.UserRegistrationDto;
import com.musicplatform.exception.BusinessException;
import com.musicplatform.exception.DuplicateResourceException;
import com.musicplatform.model.Playlist;
import com.musicplatform.model.User;
import com.musicplatform.repository.PlaylistRepository;
import com.musicplatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, playlistRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Registration succeeds: encodes password, assigns ROLE_USER, and auto-provisions Favourites playlist")
    void testRegistrationSuccess() {
        UserRegistrationDto dto = new UserRegistrationDto("newuser", "new@test.com", "Password123!", "Password123!");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedHash");

        User savedUser = new User("newuser", "new@test.com", "encodedHash", "ROLE_USER");
        savedUser.setId(10L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(dto);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("ROLE_USER", result.getRole());

        // Verify Favourites playlist is auto-provisioned
        ArgumentCaptor<Playlist> playlistCaptor = ArgumentCaptor.forClass(Playlist.class);
        verify(playlistRepository).save(playlistCaptor.capture());

        Playlist createdPlaylist = playlistCaptor.getValue();
        assertEquals("Favourites", createdPlaylist.getName());
        assertTrue(createdPlaylist.getSystemPlaylist());
        assertEquals(savedUser, createdPlaylist.getOwner());
    }

    @Test
    @DisplayName("Registration fails when password confirmation does not match")
    void testPasswordMismatch() {
        UserRegistrationDto dto = new UserRegistrationDto("newuser", "new@test.com", "Pass1", "Pass2");

        assertThrows(BusinessException.class, () -> userService.registerUser(dto));
        verify(userRepository, never()).save(any());
        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration fails when username already exists")
    void testDuplicateUsername() {
        UserRegistrationDto dto = new UserRegistrationDto("existinguser", "unique@test.com", "Pass123", "Pass123");
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration fails when email already exists")
    void testDuplicateEmail() {
        UserRegistrationDto dto = new UserRegistrationDto("uniqueuser", "existing@test.com", "Pass123", "Pass123");
        when(userRepository.existsByUsername("uniqueuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByUsername returns user when found")
    void testFindByUsername() {
        User user = new User("alice", "alice@test.com", "hash", "ROLE_USER");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        Optional<User> found = userService.findByUsername("alice");
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getUsername());
    }
}
