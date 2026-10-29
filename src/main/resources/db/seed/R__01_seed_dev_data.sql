-- =============================================================================
-- R__01_seed_dev_data.sql
-- Repeatable, idempotent dev-only seed migration.
-- Contains:
-- 1. demo_user (Password123!) with ROLE_USER
-- 2. admin (Password123!) with ROLE_ADMIN
-- 3. Favourites system playlist for demo_user
-- 4. 28+ diverse sample songs across Rock, Synthwave, Jazz, Lo-Fi, Classical, etc.
-- 5. Seed songs in demo_user's Favourites playlist
-- =============================================================================

-- 1. Create demo_user (password: Password123!)
INSERT INTO users (id, username, email, password_hash, role)
VALUES (
    1,
    'demo_user',
    'demo@musicplatform.local',
    '$2a$10$BEkHPt9.QsL8KRD74oq5M.FR6ZCRWdQgrbMchSjVi1OLKl2i8X/lK',
    'ROLE_USER'
)
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    role = VALUES(role);

-- 2. Create dev admin user (password: Password123!)
INSERT INTO users (id, username, email, password_hash, role)
VALUES (
    2,
    'admin',
    'admin@musicplatform.local',
    '$2a$10$BEkHPt9.QsL8KRD74oq5M.FR6ZCRWdQgrbMchSjVi1OLKl2i8X/lK',
    'ROLE_ADMIN'
)
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    role = VALUES(role);

-- 3. System 'Favourites' playlist for demo_user
INSERT INTO playlists (id, name, description, owner_id, system_playlist)
VALUES (
    1,
    'Favourites',
    'Your automatically managed favourite tracks',
    1,
    TRUE
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    owner_id = VALUES(owner_id),
    system_playlist = VALUES(system_playlist);

-- 4. 28 Realistic sample songs across multiple genres
INSERT INTO songs (id, title, artist, album, genre, duration_seconds, release_year, created_by)
VALUES
(1, 'Midnight City Lights', 'Neon Horizon', 'Synthwave Dreams', 'Synthwave', 245, 2021, 2),
(2, 'Starlight Echoes', 'Astral Project', 'Celestial Odyssey', 'Ambient', 198, 2022, 2),
(3, 'Coffee & Rain', 'Lo-Fi Chillers', 'Sunday Morning Beats', 'Lo-Fi', 154, 2023, 2),
(4, 'Autumn Leaves in Paris', 'Miles Quartet', 'Blue Note Sessions', 'Jazz', 312, 1998, 2),
(5, 'Thunderbolt', 'Iron Vanguard', 'Heavy Voltage', 'Rock', 265, 2019, 2),
(6, 'Digital Horizon', 'Cyber Pulse', 'Matrix Velocity', 'Electronic', 220, 2023, 2),
(7, 'Moonlight Sonata (Remastered)', 'Ludwig Ensemble', 'Classical Masterworks', 'Classical', 345, 2015, 2),
(8, 'Streets of Harlem', 'King Groove', 'The Golden Microphone', 'Hip-Hop', 210, 2020, 2),
(9, 'Sunset Boulevard', 'Pacific Drive', 'Californian Sunset', 'Pop', 188, 2022, 2),
(10, 'Velvet Soul', 'Ruby Chambers', 'Silk & Smoke', 'R&B', 234, 2021, 2),
(11, 'Delta Crossroads', 'Blind River Muddy', 'Acoustic Blues Vol. 1', 'Blues', 276, 2017, 2),
(12, 'Electric Euphoria', 'Hyperion Flux', 'Club Anthems 2024', 'EDM', 195, 2024, 2),
(13, 'Breeze of the Andes', 'Flute of Dawn', 'World Echoes', 'Folk', 240, 2018, 2),
(14, 'Cosmic Reverie', 'Quantum Leap', 'Space Exploration', 'Ambient', 310, 2023, 2),
(15, 'Symphony of the Brave', 'Northern Philharmonic', 'Epic Orchestral', 'Classical', 420, 2016, 2),
(16, 'Shadow Dancer', 'Dark Mirage', 'After Dark', 'Synthwave', 215, 2022, 2),
(17, 'Soul Kitchen', 'The Groovers', 'Funky Town Sessions', 'Funk', 230, 2020, 2),
(18, 'Crystal Waterfall', 'Zen Oasis', 'Meditation Harmony', 'Lo-Fi', 160, 2024, 2),
(19, 'Desert Highway', 'Cactus Canyon', 'Route 66 Reverie', 'Rock', 285, 2018, 2),
(20, 'Tokyo Metro Glow', 'Shibuya Sound', 'Neon Underground', 'Electronic', 205, 2023, 2),
(21, 'Golden Hour Memories', 'The Melancholics', 'Past Lives EP', 'Indie', 228, 2021, 2),
(22, 'Rooftop Serenade', 'Smooth Trio', 'Late Night Cocktail', 'Jazz', 264, 2019, 2),
(23, 'Bassline Protocol', 'Sub Zero', 'Subterranean Frequency', 'Drum & Bass', 178, 2023, 2),
(24, 'Rhythm of the Rain', 'Acoustic Traveler', 'Campfire Stories', 'Folk', 192, 2020, 2),
(25, 'Blinding Velocity', 'Viper Drive', 'Turbo Overdrive', 'Synthwave', 248, 2024, 2),
(26, 'Havana Sunset', 'Tropicana Beat', 'Latin Grooves', 'Latin', 215, 2021, 2),
(27, 'Silent Reflections', 'Echo Garden', 'Solitude', 'Ambient', 320, 2022, 2),
(28, 'Rise of the Phoenix', 'Valhalla Flame', 'Odin Calls', 'Metal', 295, 2020, 2)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    artist = VALUES(artist),
    album = VALUES(album),
    genre = VALUES(genre),
    duration_seconds = VALUES(duration_seconds),
    release_year = VALUES(release_year),
    created_by = VALUES(created_by);

-- 5. Seed some favourites for demo_user
INSERT INTO playlist_songs (id, playlist_id, song_id, position)
VALUES
(1, 1, 1, 1),
(2, 1, 3, 2),
(3, 1, 4, 3),
(4, 1, 9, 4),
(5, 1, 16, 5)
ON DUPLICATE KEY UPDATE
    playlist_id = VALUES(playlist_id),
    song_id = VALUES(song_id),
    position = VALUES(position);
