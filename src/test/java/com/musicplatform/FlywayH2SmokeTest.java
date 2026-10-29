package com.musicplatform;

import com.musicplatform.model.User;
import com.musicplatform.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FlywayH2SmokeTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Random random;

    @Test
    @DisplayName("Smoke Test: Flyway applies V1 schema on H2 MySQL mode without seed")
    void testFlywayV1AppliedOnH2() {
        assertNotNull(flyway, "Flyway bean must be present and initialized");
        MigrationInfo[] applied = flyway.info().applied();
        assertTrue(applied.length > 0, "At least one migration should be applied");

        boolean v1Found = Arrays.stream(applied)
                .anyMatch(info -> "1".equals(info.getVersion().getVersion()) && info.getDescription().contains("init schema"));
        assertTrue(v1Found, "V1__init_schema.sql must be applied in H2 test mode");

        // Requirement E: classpath:db/migration only, no seed in test mode
        boolean seedFound = Arrays.stream(applied)
                .anyMatch(info -> info.getScript() != null && info.getScript().contains("seed"));
        assertFalse(seedFound, "Dev seed migration must NOT be applied in test profile");
    }

    @Test
    @DisplayName("Smoke Test: JPA repositories work against H2 MySQL mode with ddl-auto=validate")
    void testJpaRepositoryInteraction() {
        User testUser = new User("test_runner", "runner@test.local", "hashedPassword", "ROLE_USER");
        User saved = userRepository.save(testUser);
        assertNotNull(saved.getId(), "User should be assigned an auto-increment ID");

        assertTrue(userRepository.findByUsername("test_runner").isPresent());
    }

    @Test
    @DisplayName("Smoke Test: Seeded Random bean is active in test profile (Requirement C)")
    void testSeededRandomBean() {
        assertNotNull(random, "Random bean must be injected");
        Random expected = new Random(42L);
        assertEquals(expected.nextInt(1000), random.nextInt(1000), "Random in test profile must be seeded with 42L");
    }
}
