package com.musicplatform.dto;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class SongSortField {

    public static final String TITLE = "title";
    public static final String ARTIST = "artist";
    public static final String ALBUM = "album";
    public static final String GENRE = "genre";
    public static final String DURATION = "durationSeconds";
    public static final String RELEASE_YEAR = "releaseYear";
    public static final String CREATED_AT = "createdAt";

    private static final Set<String> ALLOWED_FIELDS = new HashSet<>(Arrays.asList(
            TITLE, ARTIST, ALBUM, GENRE, DURATION, RELEASE_YEAR, CREATED_AT
    ));

    private SongSortField() {
    }

    public static boolean isValid(String field) {
        return field != null && ALLOWED_FIELDS.contains(field.trim());
    }

    public static String sanitize(String field) {
        if (field == null || !ALLOWED_FIELDS.contains(field.trim())) {
            return CREATED_AT;
        }
        return field.trim();
    }
}
