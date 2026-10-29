/**
 * Music Platform - Global Helper Utilities & CSRF Fetch
 */

const MusicApp = (function () {
    // Read CSRF token and header from meta tags
    function getCsrfInfo() {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        return {
            token: tokenMeta ? tokenMeta.getAttribute('content') : '',
            header: headerMeta ? headerMeta.getAttribute('content') : 'X-CSRF-TOKEN'
        };
    }

    // Authenticated Fetch with CSRF header injection
    async function authFetch(url, options = {}) {
        const csrf = getCsrfInfo();
        const headers = options.headers ? { ...options.headers } : {};

        if (csrf.token && !headers[csrf.header]) {
            headers[csrf.header] = csrf.token;
        }

        const mergedOptions = {
            ...options,
            headers
        };

        const response = await fetch(url, mergedOptions);
        if (response.status === 401) {
            showToast('Authentication required. Please sign in.', 'warning');
            window.location.href = '/login';
            throw new Error('Unauthorized');
        }
        return response;
    }

    // Toast notification display
    function showToast(message, type = 'info') {
        let container = document.getElementById('toastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainer';
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(container);
        }

        const bgClass = type === 'success' ? 'bg-success text-white' :
                        type === 'danger' || type === 'error' ? 'bg-danger text-white' :
                        type === 'warning' ? 'bg-warning text-dark' : 'bg-dark text-white border border-secondary';

        const toastEl = document.createElement('div');
        toastEl.className = `toast align-items-center ${bgClass} border-0 shadow-lg mb-2`;
        toastEl.setAttribute('role', 'alert');
        toastEl.setAttribute('aria-live', 'assertive');
        toastEl.setAttribute('aria-atomic', 'true');

        toastEl.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        `;

        container.appendChild(toastEl);
        if (window.bootstrap && window.bootstrap.Toast) {
            const bsToast = new bootstrap.Toast(toastEl, { delay: 3500 });
            bsToast.show();
            toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
        } else {
            setTimeout(() => toastEl.remove(), 3500);
        }
    }

    // Format seconds into MM:SS
    function formatTime(seconds) {
        if (!seconds || isNaN(seconds) || seconds < 0) return '0:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
    }

    return {
        authFetch,
        showToast,
        formatTime
    };
})();
