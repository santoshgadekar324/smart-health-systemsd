/**
 * script.js — SmartHealth Core JS
 * - API wrapper (all fetch calls)
 * - Auth token management
 * - Dashboard tab routing
 */

// ─── Config ──────────────────────────────────────────────────
const CONFIG = {
  API_BASE: 'http://localhost:8080/api',
  TOKEN_KEY: 'sh_token',
  USER_KEY: 'sh_user',
};

// ─── Token helpers ───────────────────────────────────────────
const Auth = {
  setToken:  (t) => localStorage.setItem(CONFIG.TOKEN_KEY, t),
  getToken:  ()  => localStorage.getItem(CONFIG.TOKEN_KEY),
  setUser:   (u) => localStorage.setItem(CONFIG.USER_KEY, JSON.stringify(u)),
  getUser:   ()  => {
    const raw = localStorage.getItem(CONFIG.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  clear:     ()  => {
    localStorage.removeItem(CONFIG.TOKEN_KEY);
    localStorage.removeItem(CONFIG.USER_KEY);
  },
  isLoggedIn: () => !!localStorage.getItem(CONFIG.TOKEN_KEY),
};

// ─── Fetch wrapper ───────────────────────────────────────────
async function apiFetch(path, options = {}) {
  const token = Auth.getToken();
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(CONFIG.API_BASE + path, {
    ...options,
    headers,
  });

  // Auto-logout on 401
  if (res.status === 401) {
    Auth.clear();
    window.location.href = 'login.html';
    return;
  }

  return res.json();
}

// ─── API methods ─────────────────────────────────────────────
const API = {
  // Auth
  login: (email, password) =>
    apiFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }).then(res => {
      if (res?.success && res.data?.token) {
        Auth.setToken(res.data.token);
        Auth.setUser(res.data);
      }
      return res;
    }),

  register: (payload) =>
    apiFetch('/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    }).then(res => {
      if (res?.success && res.data?.token) {
        Auth.setToken(res.data.token);
        Auth.setUser(res.data);
      }
      return res;
    }),

  // Patient
  addSymptoms: (symptoms, notes) =>
    apiFetch('/patient/addSymptoms', {
      method: 'POST',
      body: JSON.stringify({ symptoms, notes }),
    }),

  getPredictions: () => apiFetch('/patient/predictions'),

  getAppointments: () => apiFetch('/patient/appointments'),

  bookAppointment: (payload) =>
    apiFetch('/patient/bookAppointment', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  getDoctors: (specialty = '') =>
    apiFetch('/patient/doctors' + (specialty ? `?specialty=${encodeURIComponent(specialty)}` : '')),

  // Doctor
  getDoctorAppointments: () => apiFetch('/doctor/appointments'),

  updateAppointment: (id, payload) =>
    apiFetch(`/doctor/updateAppointment/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  getDoctorProfile: () => apiFetch('/doctor/profile'),

  updateDoctorProfile: (payload) =>
    apiFetch('/doctor/profile', {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  // Admin
  getUsers: () => apiFetch('/admin/manageUsers'),

  toggleUser: (id) =>
    apiFetch(`/admin/manageUsers/${id}/toggle`, { method: 'PUT' }),

  getAdminDoctors: () => apiFetch('/admin/manageDoctors'),

  verifyDoctor: (id) =>
    apiFetch(`/admin/manageDoctors/${id}/verify`, { method: 'PUT' }),

  getAdminAppointments: () => apiFetch('/admin/manageAppointments'),

  getAnalytics: () => apiFetch('/admin/analytics'),
};

// ─── Dashboard Tab Switcher ──────────────────────────────────
function initTabs() {
  const links = document.querySelectorAll('.sidebar-link[data-tab]');
  links.forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const tab = link.dataset.tab;
      switchTab(tab);
    });
  });
}

function switchTab(tabId) {
  // Hide all tabs
  document.querySelectorAll('.dash-tab').forEach(t => t.classList.remove('active'));
  // Deactivate sidebar links
  document.querySelectorAll('.sidebar-link').forEach(l => l.classList.remove('active'));

  // Show target tab
  const target = document.getElementById('tab-' + tabId);
  if (target) target.classList.add('active');

  // Activate sidebar link
  const activeLink = document.querySelector(`.sidebar-link[data-tab="${tabId}"]`);
  if (activeLink) activeLink.classList.add('active');

  // Update topbar title
  const titleEl = document.getElementById('topbarTitle');
  if (titleEl && activeLink) titleEl.textContent = activeLink.textContent.trim();

  // Close sidebar on mobile
  document.getElementById('sidebar')?.classList.remove('open');
}

// ─── Sidebar toggle (mobile) ─────────────────────────────────
function initSidebarToggle() {
  const btn = document.getElementById('sidebarToggle');
  const sidebar = document.getElementById('sidebar');
  if (btn && sidebar) {
    btn.addEventListener('click', () => sidebar.classList.toggle('open'));
  }
}

// ─── Logout ─────────────────────────────────────────────────
function initLogout() {
  const btn = document.getElementById('logoutBtn');
  if (btn) {
    btn.addEventListener('click', () => {
      Auth.clear();
      window.location.href = 'login.html';
    });
  }
}

// ─── User info in topbar ────────────────────────────────────
function initUserInfo() {
  const user = Auth.getUser();
  if (user) {
    const nameEl = document.getElementById('userName');
    const avatarEl = document.getElementById('userAvatar');
    if (nameEl) nameEl.textContent = user.fullName || user.email;
    if (avatarEl) avatarEl.textContent = (user.fullName || 'U')[0].toUpperCase();
  }
}

// ─── Guard: redirect if not logged in ───────────────────────
function requireAuth(expectedRole) {
  if (!Auth.isLoggedIn()) {
    window.location.href = 'login.html';
    return false;
  }
  const user = Auth.getUser();
  if (expectedRole && user?.role !== expectedRole) {
    window.location.href = 'login.html';
    return false;
  }
  return true;
}

// ─── Status badge helper ─────────────────────────────────────
function statusBadge(status) {
  return `<span class="status-badge badge-${status.toLowerCase()}">${status}</span>`;
}

// ─── Date formatter ──────────────────────────────────────────
function fmtDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric'
  });
}

// ─── Init on page load ───────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  initTabs();
  initSidebarToggle();
  initLogout();
  initUserInfo();
});
