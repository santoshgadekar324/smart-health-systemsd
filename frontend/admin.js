/**
 * admin.js — Admin Dashboard Logic
 * Uses Chart.js for visualizations
 */

document.addEventListener('DOMContentLoaded', () => {
  if (!requireAuth('ADMIN')) return;
  loadAdminDashboard();
});

async function loadAdminDashboard() {
  const res = await API.getAnalytics();
  if (!res?.data) return;

  const d = res.data;

  // Stats
  document.getElementById('statUsers').textContent       = d.totalUsers;
  document.getElementById('statDoctors').textContent     = d.totalDoctors;
  document.getElementById('statAppts').textContent       = d.totalAppointments;
  document.getElementById('statPredictions').textContent = d.totalPredictions;

  // Charts
  renderDiseaseChart(d.topDiseases, 'diseaseChart');
  renderApptStatusChart(d, 'apptChart');
  renderDiseaseChart(d.topDiseases, 'bigDiseaseChart', true);

  // Tables
  loadUsers();
  loadDoctors();
  loadAppointments();
}

// ─── Charts ──────────────────────────────────────────────────
function renderDiseaseChart(topDiseases, canvasId, horizontal = false) {
  const el = document.getElementById(canvasId);
  if (!el || !topDiseases?.length) return;

  const labels = topDiseases.map(d => d.disease);
  const data   = topDiseases.map(d => d.count);
  const colors = [
    '#14d9c5','#4c8dff','#9b6dff','#ff5a7e','#f5a623',
    '#30d158','#0fa896','#3a7bd5','#7e57c2','#ef5350'
  ];

  new Chart(el, {
    type: horizontal ? 'bar' : 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Predictions',
        data,
        backgroundColor: colors.map(c => c + '33'),
        borderColor: colors,
        borderWidth: 2,
        borderRadius: 6,
      }]
    },
    options: {
      indexAxis: horizontal ? 'y' : 'x',
      responsive: true,
      plugins: {
        legend: { display: false },
      },
      scales: {
        x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#7a8599' } },
        y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#7a8599' } },
      }
    }
  });
}

function renderApptStatusChart(analytics, canvasId) {
  const el = document.getElementById(canvasId);
  if (!el) return;

  new Chart(el, {
    type: 'doughnut',
    data: {
      labels: ['Pending', 'Approved', 'Completed', 'Rejected'],
      datasets: [{
        data: [
          analytics.pendingAppointments,
          analytics.totalAppointments - analytics.pendingAppointments,
          0, 0   // would need more granular analytics endpoint
        ],
        backgroundColor: [
          'rgba(245,166,35,0.7)',
          'rgba(48,209,88,0.7)',
          'rgba(76,141,255,0.7)',
          'rgba(255,90,126,0.7)',
        ],
        borderColor: ['#f5a623','#30d158','#4c8dff','#ff5a7e'],
        borderWidth: 2,
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          labels: { color: '#7a8599', padding: 16, font: { size: 12 } }
        }
      },
      cutout: '60%',
    }
  });
}

// ─── Users Table ─────────────────────────────────────────────
async function loadUsers() {
  const res = await API.getUsers();
  const tbody = document.getElementById('usersTableBody');
  if (!tbody || !res?.data) return;

  tbody.innerHTML = res.data.map((u, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${u.fullName}</td>
      <td>${u.email}</td>
      <td><span class="status-badge badge-${u.role.toLowerCase()}"
           style="background:var(--blue-dim);color:var(--blue);">${u.role}</span></td>
      <td>${u.isActive
        ? '<span class="status-badge badge-approved">Active</span>'
        : '<span class="status-badge badge-rejected">Inactive</span>'}</td>
      <td>
        <button class="btn btn-sm" style="background:var(--bg-3);color:var(--text-muted);border:1px solid var(--border);border-radius:var(--radius-sm);font-size:12px;"
          onclick="toggleUser(${u.id}, this)">
          ${u.isActive ? 'Deactivate' : 'Activate'}
        </button>
      </td>
    </tr>
  `).join('');
}

async function toggleUser(id, btn) {
  const res = await API.toggleUser(id);
  if (res?.success) {
    loadUsers(); // reload table
  }
}

// ─── Doctors Table ───────────────────────────────────────────
async function loadDoctors() {
  const res = await API.getAdminDoctors();
  const tbody = document.getElementById('doctorsTableBody');
  if (!tbody || !res?.data) return;

  tbody.innerHTML = res.data.map((d, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${d.user?.fullName || '—'}</td>
      <td>${d.specialization}</td>
      <td>${d.hospitalName || '—'}</td>
      <td>${d.isVerified
        ? '<span class="status-badge badge-approved">Verified</span>'
        : '<span class="status-badge badge-pending">Pending</span>'}</td>
      <td>
        ${!d.isVerified ? `
          <button class="btn btn-sm" style="background:var(--green-dim);color:var(--green);border:none;border-radius:var(--radius-sm);font-size:12px;padding:5px 12px;"
            onclick="verifyDoc(${d.id})">
            Verify
          </button>
        ` : '—'}
      </td>
    </tr>
  `).join('');
}

async function verifyDoc(id) {
  const res = await API.verifyDoctor(id);
  if (res?.success) loadDoctors();
}

// ─── Appointments ────────────────────────────────────────────
async function loadAppointments() {
  const res = await API.getAdminAppointments();
  const el  = document.getElementById('adminApptList');
  if (!el || !res?.data) return;

  const appts = res.data;
  if (!appts.length) {
    el.innerHTML = `<div class="text-muted py-4 text-center">No appointments.</div>`;
    return;
  }

  el.innerHTML = appts.map(a => `
    <div class="appt-card">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
        <div>
          <strong>${a.patient?.fullName || '—'}</strong>
          <span class="text-muted small ms-2">→ Dr. ${a.doctor?.user?.fullName || '—'}</span>
        </div>
        ${statusBadge(a.status)}
      </div>
      <div class="text-muted small mt-2">
        <i class="bi bi-calendar3 me-1"></i>${a.appointmentDate}
        <i class="bi bi-clock ms-3 me-1"></i>${a.appointmentTime}
      </div>
    </div>
  `).join('');
}
