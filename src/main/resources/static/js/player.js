/**
 * Simulated Audio Player with auto-advance, session rehydration, and queue management
 */

const Player = (function () {
    let currentState = null;
    let isPlaying = false;
    let elapsedSeconds = 0;
    let timerInterval = null;

    // DOM Elements
    let trackTitleEl, trackArtistEl, trackCoverEl;
    let playPauseBtn, playPauseIcon;
    let prevBtn, nextBtn, shuffleBtn;
    let currentTimeEl, totalTimeEl, progressFillEl, progressBarEl;
    let queueListEl, queueBadgeEl;

    function init() {
        // Cache DOM elements
        trackTitleEl = document.getElementById('playerTrackTitle');
        trackArtistEl = document.getElementById('playerTrackArtist');
        trackCoverEl = document.getElementById('playerTrackCover');
        playPauseBtn = document.getElementById('playerPlayPauseBtn');
        playPauseIcon = document.getElementById('playerPlayPauseIcon');
        prevBtn = document.getElementById('playerPrevBtn');
        nextBtn = document.getElementById('playerNextBtn');
        shuffleBtn = document.getElementById('playerShuffleBtn');
        currentTimeEl = document.getElementById('playerCurrentTime');
        totalTimeEl = document.getElementById('playerTotalTime');
        progressFillEl = document.getElementById('playerProgressFill');
        progressBarEl = document.getElementById('playerProgressBar');
        queueListEl = document.getElementById('queueList');
        queueBadgeEl = document.getElementById('queueBadge');

        // Restore saved elapsed time and playback state across full page reloads
        const savedElapsed = sessionStorage.getItem('player_elapsed_seconds');
        if (savedElapsed !== null) {
            elapsedSeconds = parseInt(savedElapsed, 10) || 0;
        }
        const savedPlaying = sessionStorage.getItem('player_is_playing');
        isPlaying = savedPlaying === 'true';

        bindEvents();
        rehydrateState();
    }

    function bindEvents() {
        if (playPauseBtn) {
            playPauseBtn.addEventListener('click', togglePlayPause);
        }
        if (prevBtn) {
            prevBtn.addEventListener('click', previousTrack);
        }
        if (nextBtn) {
            nextBtn.addEventListener('click', nextTrack);
        }
        if (progressBarEl) {
            progressBarEl.addEventListener('click', seekProgress);
        }

        // Global delegation for playing songs or playlists from list buttons
        document.addEventListener('click', function (e) {
            const playSongBtn = e.target.closest('.btn-play-song');
            if (playSongBtn) {
                e.preventDefault();
                const songId = playSongBtn.getAttribute('data-song-id');
                if (songId) playSong(songId);
                return;
            }

            const addQueueBtn = e.target.closest('.btn-add-queue');
            if (addQueueBtn) {
                e.preventDefault();
                const songId = addQueueBtn.getAttribute('data-song-id');
                if (songId) addToQueue(songId);
                return;
            }

            const playPlaylistBtn = e.target.closest('.btn-play-playlist');
            if (playPlaylistBtn) {
                e.preventDefault();
                const playlistId = playPlaylistBtn.getAttribute('data-playlist-id');
                const shuffle = playPlaylistBtn.getAttribute('data-shuffle') === 'true';
                if (playlistId) playPlaylist(playlistId, shuffle);
            }
        });
    }

    async function rehydrateState() {
        try {
            const res = await MusicApp.authFetch('/api/queue/state');
            if (res.ok) {
                const state = await res.json();
                applyState(state);
                if (isPlaying && currentState && currentState.currentSong) {
                    startTimer();
                } else {
                    pauseTimer();
                }
            }
        } catch (e) {
            console.warn('Could not rehydrate player queue state:', e);
        }
    }

    function applyState(state) {
        currentState = state;
        const current = state.currentSong;

        if (current) {
            if (trackTitleEl) trackTitleEl.textContent = current.title;
            if (trackArtistEl) trackArtistEl.textContent = current.artist;
            if (totalTimeEl) totalTimeEl.textContent = current.formattedDuration || MusicApp.formatTime(current.durationSeconds);

            // Cap elapsed seconds if needed
            if (elapsedSeconds > current.durationSeconds) {
                elapsedSeconds = 0;
            }
            updateProgressDisplay();
        } else {
            if (trackTitleEl) trackTitleEl.textContent = 'No track selected';
            if (trackArtistEl) trackArtistEl.textContent = 'Choose a song to play';
            if (totalTimeEl) totalTimeEl.textContent = '0:00';
            if (currentTimeEl) currentTimeEl.textContent = '0:00';
            if (progressFillEl) progressFillEl.style.width = '0%';
            elapsedSeconds = 0;
            pauseTimer();
        }

        if (queueBadgeEl) {
            queueBadgeEl.textContent = state.totalTracks || 0;
        }

        renderQueueDrawer(state);
    }

    function togglePlayPause() {
        if (!currentState || !currentState.currentSong) {
            MusicApp.showToast('Select a song or playlist to play', 'info');
            return;
        }

        if (isPlaying) {
            pauseTimer();
            MusicApp.showToast('Playback paused', 'info');
        } else {
            startTimer();
            MusicApp.showToast('Playback started', 'info');
        }
    }

    function startTimer() {
        isPlaying = true;
        sessionStorage.setItem('player_is_playing', 'true');
        if (playPauseIcon) {
            playPauseIcon.className = 'bi bi-pause-fill';
        }

        clearInterval(timerInterval);
        timerInterval = setInterval(() => {
            if (!currentState || !currentState.currentSong) {
                pauseTimer();
                return;
            }

            elapsedSeconds++;
            sessionStorage.setItem('player_elapsed_seconds', elapsedSeconds.toString());
            updateProgressDisplay();

            // Auto-advance when track reaches duration
            if (elapsedSeconds >= currentState.currentSong.durationSeconds) {
                nextTrack();
            }
        }, 1000);
    }

    function pauseTimer() {
        isPlaying = false;
        sessionStorage.setItem('player_is_playing', 'false');
        if (playPauseIcon) {
            playPauseIcon.className = 'bi bi-play-fill';
        }
        clearInterval(timerInterval);
    }

    function updateProgressDisplay() {
        if (!currentState || !currentState.currentSong) return;
        const total = currentState.currentSong.durationSeconds || 1;
        const pct = Math.min(100, Math.max(0, (elapsedSeconds / total) * 100));

        if (currentTimeEl) currentTimeEl.textContent = MusicApp.formatTime(elapsedSeconds);
        if (progressFillEl) progressFillEl.style.width = pct + '%';
    }

    function seekProgress(e) {
        if (!currentState || !currentState.currentSong || !progressBarEl) return;
        const rect = progressBarEl.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        const width = rect.width;
        const percentage = Math.max(0, Math.min(1, clickX / width));

        elapsedSeconds = Math.floor(percentage * currentState.currentSong.durationSeconds);
        sessionStorage.setItem('player_elapsed_seconds', elapsedSeconds.toString());
        updateProgressDisplay();
    }

    async function playSong(songId) {
        try {
            const res = await MusicApp.authFetch(`/api/queue/play-song?songId=${songId}`, { method: 'POST' });
            if (res.ok) {
                const state = await res.json();
                elapsedSeconds = 0;
                sessionStorage.setItem('player_elapsed_seconds', '0');
                applyState(state);
                startTimer();
                MusicApp.showToast(`Playing "${state.currentSong.title}"`, 'success');
            }
        } catch (e) {
            MusicApp.showToast('Could not play track', 'danger');
        }
    }

    async function playPlaylist(playlistId, shuffle) {
        try {
            const res = await MusicApp.authFetch(`/api/queue/play-playlist?playlistId=${playlistId}&shuffle=${shuffle}`, { method: 'POST' });
            if (res.ok) {
                const state = await res.json();
                if (state.totalTracks === 0) {
                    MusicApp.showToast('This playlist is empty', 'warning');
                    return;
                }
                elapsedSeconds = 0;
                sessionStorage.setItem('player_elapsed_seconds', '0');
                applyState(state);
                startTimer();
                const modeMsg = shuffle ? ' (Shuffled)' : '';
                MusicApp.showToast(`Playing playlist "${state.playlistName}"${modeMsg}`, 'success');
            }
        } catch (e) {
            MusicApp.showToast('Could not play playlist', 'danger');
        }
    }

    async function nextTrack() {
        try {
            const res = await MusicApp.authFetch('/api/queue/next', { method: 'POST' });
            if (res.ok) {
                const state = await res.json();
                elapsedSeconds = 0;
                sessionStorage.setItem('player_elapsed_seconds', '0');
                applyState(state);
                if (isPlaying) startTimer();
            }
        } catch (e) {
            console.error('Error advancing to next track:', e);
        }
    }

    async function previousTrack() {
        try {
            const res = await MusicApp.authFetch('/api/queue/prev', { method: 'POST' });
            if (res.ok) {
                const state = await res.json();
                elapsedSeconds = 0;
                sessionStorage.setItem('player_elapsed_seconds', '0');
                applyState(state);
                if (isPlaying) startTimer();
            }
        } catch (e) {
            console.error('Error going to previous track:', e);
        }
    }

    async function addToQueue(songId) {
        try {
            const res = await MusicApp.authFetch(`/api/queue/add?songId=${songId}`, { method: 'POST' });
            if (res.ok) {
                const state = await res.json();
                applyState(state);
                MusicApp.showToast('Song added to play queue', 'success');
            }
        } catch (e) {
            MusicApp.showToast('Could not add song to queue', 'danger');
        }
    }

    async function removeFromQueue(index) {
        try {
            const res = await MusicApp.authFetch(`/api/queue/items/${index}`, { method: 'DELETE' });
            if (res.ok) {
                const state = await res.json();
                applyState(state);
                MusicApp.showToast('Removed from queue', 'info');
            }
        } catch (e) {
            MusicApp.showToast('Could not remove song', 'danger');
        }
    }

    async function clearQueue() {
        try {
            const res = await MusicApp.authFetch('/api/queue/clear', { method: 'POST' });
            if (res.ok) {
                const state = await res.json();
                elapsedSeconds = 0;
                sessionStorage.setItem('player_elapsed_seconds', '0');
                applyState(state);
                pauseTimer();
                MusicApp.showToast('Queue cleared', 'info');
            }
        } catch (e) {
            MusicApp.showToast('Could not clear queue', 'danger');
        }
    }

    function renderQueueDrawer(state) {
        if (!queueListEl) return;
        const queue = state.queue || [];

        if (queue.length === 0) {
            queueListEl.innerHTML = `
                <div class="text-center py-5 text-muted">
                    <i class="bi bi-music-note-list fs-1 mb-2"></i>
                    <p>The queue is empty.</p>
                </div>
            `;
            return;
        }

        let html = '';
        queue.forEach((item, idx) => {
            const isActive = idx === state.currentIndex;
            html += `
                <div class="queue-item ${isActive ? 'active' : ''}">
                    <div class="d-flex align-items-center gap-2 overflow-hidden">
                        <span class="text-muted small">${idx + 1}.</span>
                        <div class="text-truncate">
                            <div class="fw-semibold text-truncate small ${isActive ? 'text-success' : 'text-light'}">${item.title}</div>
                            <div class="text-muted small text-truncate">${item.artist}</div>
                        </div>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <span class="text-muted small">${item.formattedDuration}</span>
                        <button class="btn btn-sm btn-link text-danger p-0" onclick="Player.removeFromQueue(${idx})" title="Remove">
                            <i class="bi bi-x-circle"></i>
                        </button>
                    </div>
                </div>
            `;
        });

        queueListEl.innerHTML = html;
    }

    return {
        init,
        playSong,
        playPlaylist,
        addToQueue,
        removeFromQueue,
        clearQueue
    };
})();

document.addEventListener('DOMContentLoaded', function () {
    Player.init();
});
