/**
 * Volunteer Hub - Dynamic Role-Based Navigation Bar & Notification Bell
 * Renders consistent header and loads session info from /api/users/me
 */

document.addEventListener('DOMContentLoaded', async () => {
  const navContainer = document.getElementById('app-nav');
  if (!navContainer) return;

  let currentUser = null;
  try {
    currentUser = await apiFetch('/api/users/me');
  } catch (err) {
    // Unauthenticated visitor
    currentUser = null;
  }

  renderNavbar(navContainer, currentUser);

  if (currentUser) {
    loadUnreadNotifications();
  }
});

function renderNavbar(container, user) {
  const currentPath = window.location.pathname;

  let navLinksHtml = '';
  let actionsHtml = '';

  if (user) {
    // Role-specific primary navigation links
    if (user.role === 'VOLUNTEER') {
      navLinksHtml = `
        <li><a href="/volunteer-dashboard.html" class="nav-link ${currentPath.includes('volunteer-dashboard') ? 'active' : ''}">Dashboard</a></li>
        <li><a href="/events.html" class="nav-link ${currentPath.includes('events.html') ? 'active' : ''}">Opportunities</a></li>
        <li><a href="/my-events.html" class="nav-link ${currentPath.includes('my-events') ? 'active' : ''}">My Events</a></li>
        <li><a href="/performance.html" class="nav-link ${currentPath.includes('performance') ? 'active' : ''}">Performance</a></li>
        <li><a href="/certificates.html" class="nav-link ${currentPath.includes('certificates') ? 'active' : ''}">Certificates</a></li>
        <li><a href="/leaderboard.html" class="nav-link ${currentPath.includes('leaderboard') ? 'active' : ''}">Leaderboard</a></li>
      `;
    } else if (user.role === 'NGO') {
      navLinksHtml = `
        <li><a href="/ngo-dashboard.html" class="nav-link ${currentPath.includes('ngo-dashboard') ? 'active' : ''}">Dashboard</a></li>
        <li><a href="/create-event.html" class="nav-link ${currentPath.includes('create-event') ? 'active' : ''}">+ New Event</a></li>
        <li><a href="/attendance-management.html" class="nav-link ${currentPath.includes('attendance') ? 'active' : ''}">Attendance & Ratings</a></li>
        <li><a href="/leaderboard.html" class="nav-link ${currentPath.includes('leaderboard') ? 'active' : ''}">Leaderboard</a></li>
      `;
    } else if (user.role === 'ADMIN') {
      navLinksHtml = `
        <li><a href="/admin-dashboard.html" class="nav-link ${currentPath.includes('admin') ? 'active' : ''}">Admin Panel</a></li>
        <li><a href="/events.html" class="nav-link ${currentPath.includes('events') ? 'active' : ''}">All Events</a></li>
        <li><a href="/leaderboard.html" class="nav-link ${currentPath.includes('leaderboard') ? 'active' : ''}">Leaderboard</a></li>
      `;
    }

    const roleBadgeClass = user.role === 'ADMIN' ? 'badge-danger' : (user.role === 'NGO' ? 'badge-info' : 'badge-success');

    actionsHtml = `
      <a href="/notifications.html" class="notification-bell-btn" title="View Notifications" aria-label="Notifications">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
          <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
        </svg>
        <span id="nav-notification-badge" class="notification-badge" style="display: none;">0</span>
      </a>

      <div style="display: flex; align-items: center; gap: 0.75rem;">
        <a href="/profile.html" class="nav-link" style="display: flex; align-items: center; gap: 0.5rem; font-weight: 600;">
          <span style="max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(user.displayName || user.email)}</span>
          <span class="badge ${roleBadgeClass}">${user.role}</span>
        </a>
        <button id="btn-logout" class="btn btn-secondary btn-sm" title="Log Out">
          Log Out
        </button>
      </div>
    `;
  } else {
    // Public / Unauthenticated visitor navigation
    navLinksHtml = `
      <li><a href="/index.html" class="nav-link ${currentPath === '/' || currentPath.includes('index.html') ? 'active' : ''}">Home</a></li>
      <li><a href="/events.html" class="nav-link ${currentPath.includes('events.html') ? 'active' : ''}">Explore Causes</a></li>
      <li><a href="/leaderboard.html" class="nav-link ${currentPath.includes('leaderboard') ? 'active' : ''}">Leaderboard</a></li>
    `;

    actionsHtml = `
      <a href="/login.html" class="btn btn-secondary btn-sm">Log In</a>
      <a href="/register.html" class="btn btn-primary btn-sm">Get Started</a>
    `;
  }

  container.innerHTML = `
    <div class="navbar-inner">
      <a href="/index.html" class="navbar-brand">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"></path>
        </svg>
        <span>VolunteerHub</span>
        <span class="navbar-brand-badge">Platform</span>
      </a>

      <ul class="navbar-nav">
        ${navLinksHtml}
      </ul>

      <div class="navbar-actions">
        ${actionsHtml}
      </div>
    </div>
  `;

  // Attach logout handler if logged in
  const logoutBtn = document.getElementById('btn-logout');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', async () => {
      try {
        await apiFetch('/api/auth/logout', { method: 'POST' });
        showToast('Logged out successfully', 'success');
        setTimeout(() => {
          window.location.href = '/login.html';
        }, 300);
      } catch (err) {
        showToast('Logout encountered an error', 'danger');
      }
    });
  }
}

async function loadUnreadNotifications() {
  try {
    const data = await apiFetch('/api/notifications/unread-count');
    const badge = document.getElementById('nav-notification-badge');
    if (badge && data.unreadCount > 0) {
      badge.textContent = data.unreadCount > 99 ? '99+' : data.unreadCount;
      badge.style.display = 'flex';
    } else if (badge) {
      badge.style.display = 'none';
    }
  } catch (err) {
    // Fail silently on notification count fetch
  }
}
