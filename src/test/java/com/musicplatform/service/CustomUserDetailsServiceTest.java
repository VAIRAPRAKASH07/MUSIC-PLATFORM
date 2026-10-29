package com.musicplatform.service;

import com.musicplatform.model.User;
import com.musicplatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("rockstar", "rock@music.local", "hashedPass", "ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername returns UserDetails when user exists")
    void testLoadUserByUsernameSuccess() {
        when(userRepository.findByUsername("rockstar")).thenReturn(Optional.of(sampleUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("rockstar");

        assertNotNull(userDetails);
        assertEquals("rockstar", userDetails.getUsername());
        assertEquals("hashedPass", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException when user does not exist")
    void testLoadUserByUsernameNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("ghost")
        );
    }
}
