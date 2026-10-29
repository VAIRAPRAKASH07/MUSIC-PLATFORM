/**
 * AJAX Favourites Heart Toggle (Zero Page Reload)
 */

document.addEventListener('DOMContentLoaded', function () {
    document.addEventListener('click', async function (event) {
        const heartBtn = event.target.closest('.btn-heart');
        if (!heartBtn) return;

        event.preventDefault();
        event.stopPropagation();

        const songId = heartBtn.getAttribute('data-song-id');
        if (!songId) return;

        const icon = heartBtn.querySelector('i');
        const currentlyFavourited = heartBtn.classList.contains('active');

        // Optimistic UI update
        heartBtn.classList.toggle('active');
        if (icon) {
            icon.className = currentlyFavourited ? 'bi bi-heart' : 'bi bi-heart-fill';
        }

        try {
            const response = await MusicApp.authFetch(`/api/favourites/toggle?songId=${songId}`, {
                method: 'POST'
            });

            if (!response.ok) {
                throw new Error('Failed to update favourite status');
            }

            const data = await response.json();
            // Sync confirmed state
            if (data.favourited) {
                heartBtn.classList.add('active');
                if (icon) icon.className = 'bi bi-heart-fill';
                MusicApp.showToast('Added to your Favourites', 'success');
            } else {
                heartBtn.classList.remove('active');
                if (icon) icon.className = 'bi bi-heart';
                MusicApp.showToast('Removed from your Favourites', 'info');

                // If currently on the /playlists/favourites page, remove row
                if (window.location.pathname.includes('/playlists/favourites')) {
                    const row = heartBtn.closest('tr');
                    if (row) {
                        row.style.opacity = '0';
                        setTimeout(() => row.remove(), 250);
                    }
                }
            }
        } catch (error) {
            console.error('Error toggling favourite:', error);
            // Rollback optimistic update
            heartBtn.classList.toggle('active', currentlyFavourited);
            if (icon) {
                icon.className = currentlyFavourited ? 'bi bi-heart-fill' : 'bi bi-heart';
            }
            MusicApp.showToast('Could not update favourite. Please try again.', 'danger');
        }
    });
});
