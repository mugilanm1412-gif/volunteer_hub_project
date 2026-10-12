/**
 * Volunteer Hub - Centralized API Client & Fetch Utility
 * Features:
 *  - Automatic extraction and injection of XSRF-TOKEN cookie in X-XSRF-TOKEN header
 *  - Automatic JSON parsing and error normalization
 *  - Seamless redirection to login.html on HTTP 401 Unauthorized
 *  - Toast alert messaging system
 */

/**
 * Extracts a cookie value by name (used for XSRF-TOKEN)
 */
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop().split(';').shift();
  }
  return null;
}

/**
 * Global apiFetch helper
 * @param {string} endpoint - Path such as '/api/auth/login'
 * @param {object} options - Fetch options (method, headers, body)
 * @returns {Promise<any>}
 */
async function apiFetch(endpoint, options = {}) {
  const defaultHeaders = {
    'Accept': 'application/json'
  };

  const method = (options.method || 'GET').toUpperCase();

  // If sending a body and Content-Type not explicitly specified, default to application/json
  if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
    defaultHeaders['Content-Type'] = 'application/json';
    options.body = JSON.stringify(options.body);
  }

  // Inject CSRF header for state-mutating HTTP methods
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    const xsrfToken = getCookie('XSRF-TOKEN');
    if (xsrfToken) {
      defaultHeaders['X-XSRF-TOKEN'] = decodeURIComponent(xsrfToken);
    }
  }

  const mergedHeaders = {
    ...defaultHeaders,
    ...(options.headers || {})
  };

  try {
    const response = await fetch(endpoint, {
      ...options,
      headers: mergedHeaders,
      credentials: 'same-origin' // Ensures JSESSIONID cookie is exchanged
    });

    // Handle 401 Unauthorized (Session expired or unauthenticated)
    if (response.status === 401) {
      // Avoid redirect loops if checking status from login/register pages
      const currentPath = window.location.pathname;
      if (!currentPath.endsWith('login.html') &&
          !currentPath.endsWith('register.html') &&
          !currentPath.endsWith('forgot-password.html') &&
          !currentPath.endsWith('reset-password.html') &&
          !currentPath.endsWith('index.html') &&
          currentPath !== '/') {
        window.location.href = '/login.html?expired=true';
      }
    }

    // Attempt to parse JSON response body
    const contentType = response.headers.get('content-type');
    let responseData = null;
    if (contentType && contentType.includes('application/json')) {
      responseData = await response.json();
    } else {
      const text = await response.text();
      responseData = text ? { message: text } : {};
    }

    if (!response.ok) {
      const error = new Error(responseData.message || responseData.error || `Request failed with status ${response.status}`);
      error.status = response.status;
      error.data = responseData;
      throw error;
    }

    return responseData;
  } catch (err) {
    throw err;
  }
}

/**
 * Display toast notification at top-right of screen
 * @param {string} message
 * @param {'success'|'danger'|'warning'|'info'} type
 */
function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast alert alert-${type}`;
  toast.style.margin = '0';
  toast.textContent = message;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 200ms ease-out';
    setTimeout(() => toast.remove(), 250);
  }, 4000);
}

/**
 * Prevents XSS by sanitizing text for innerHTML usage where textContent is not directly suitable
 */
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
