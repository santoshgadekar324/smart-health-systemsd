/**
 * doctor.js — Doctor Dashboard Logic
 */

let allAppointments = [];

document.addEventListener('DOMContentLoaded', () => {
  if (!requireAuth('DOCTOR')) return;
  loadDoctorDashboard();
});

async function loadDoctorDashboard() {
  const res = await API.getDoctorAppointments();
  if (!res?.data) return;
  allAppointments = res.data;

  // Stats
  const today = new Date().toISOString().split('T')[0];
  document.getElementById('statTotal').textContent    = allAppointments.length;
  document.getElementById('statPending').textContent  = allAppointments.filter(a => a.status === 'PENDING').length;
  document.getElementById('statApproved').textContent = allAppointments.filter(a => a.status === 'APPROVED').length;
  document.getElementById('statRejected').textContent = allAppointments.filter(a => a.status === 'REJECTED').length;

  // Today's appointments
  const todayAppts = allAppointments.filter(a => a.appointmentDate === today);
  renderAppts(todayAppts, 'todayAppts', true);
  renderAppts(allAppointments, 'allAppts', true);
}

function filterAppointments() {
  const filter = document.getElementById('statusFilter')?.value;
  const filtered = filter
    ? allAppointments.filter(a => a.status === filter)
    : allAppointments;
  renderAppts(filtered, 'allAppts', true);
}

function renderAppts(appts, containerId, withActions) {
  const el = document.getElementById(containerId);
  if (!el) return;

  if (!appts.length) {
    el.innerHTML = `<div class="text-muted py-4 text-center">No appointments.</div>`;
    return;
  }

  el.innerHTML = appts.map(a => `
    <div class="appt-card" id="appt-${a.id}">
      <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
        <div>
          <strong>${a.patientName}</strong>
          <span class="text-muted small ms-2">${a.patientEmail}</span>
        </div>
        ${statusBadge(a.status)}
      </div>
      <div class="text-muted small mt-2">
        <i class="bi bi-calendar3 me-1"></i> ${a.appointmentDate}
        <i class="bi bi-clock ms-3 me-1"></i> ${a.appointmentTime}
      </div>
      ${a.reason ? `<div class="mt-2 small"><strong>Reason:</strong> ${a.reason}</div>` : ''}
      ${withActions && a.status === 'PENDING' ? `
        <div class="mt-3 d-flex gap-2 flex-wrap">
          <button class="btn btn-sm sh-btn-primary" onclick="updateAppt(${a.id}, 'APPROVED')">
            <i class="bi bi-check-lg me-1"></i>Approve
          </button>
          <button class="btn btn-sm" style="background:var(--rose-dim);color:var(--rose);border:none;border-radius:var(--radius-sm);padding:6px 14px;"
            onclick="promptReject(${a.id})">
            <i class="bi bi-x-lg me-1"></i>Reject
          </button>
          <button class="btn btn-sm" style="background:var(--blue-dim);color:var(--blue);border:none;border-radius:var(--radius-sm);padding:6px 14px;"
            onclick="updateAppt(${a.id}, 'COMPLETED')">
            <i class="bi bi-check2-all me-1"></i>Mark Complete
          </button>
        </div>
      ` : ''}
      ${withActions && a.status === 'APPROVED' ? `
        <div class="mt-3">
          <button class="btn btn-sm" style="background:var(--blue-dim);color:var(--blue);border:none;border-radius:var(--radius-sm);padding:6px 14px;"
            onclick="updateAppt(${a.id}, 'COMPLETED')">
            <i class="bi bi-check2-all me-1"></i>Mark Complete
          </button>
        </div>
      ` : ''}
    </div>
  `).join('');
}

async function updateAppt(id, status, extra = {}) {
  const res = await API.updateAppointment(id, { status, ...extra });
  if (res?.success) {
    await loadDoctorDashboard();
  } else {
    alert(res?.message || 'Update failed.');
  }
}

function promptReject(id) {
  const reason = prompt('Reason for rejection (optional):');
  updateAppt(id, 'REJECTED', { rejectionReason: reason || '' });
}

// ─── Profile ─────────────────────────────────────────────────
async function saveProfile() {
  const alertEl = document.getElementById('profileAlert');
  const payload = {
    specialization:   document.getElementById('pSpecialization').value,
    licenseNumber:    document.getElementById('pLicense').value,
    yearsExperience:  parseInt(document.getElementById('pYears').value) || 0,
    hospitalName:     document.getElementById('pHospital').value,
    consultationFee:  parseFloat(document.getElementById('pFee').value) || 0,
    bio:              document.getElementById('pBio').value,
    availableDays:    document.getElementById('pDays').value,
  };

  const res = await API.updateDoctorProfile(payload);
  if (res?.success) {
    alertEl.className = 'alert alert-success';
    alertEl.textContent = 'Profile saved!';
    alertEl.classList.remove('d-none');
  } else {
    alertEl.className = 'alert alert-danger';
    alertEl.textContent = res?.message || 'Save failed.';
    alertEl.classList.remove('d-none');
  }
}

// Load profile when profile tab is clicked
document.addEventListener('DOMContentLoaded', async () => {
  const profileLink = document.querySelector('[data-tab="profile"]');
  if (profileLink) {
    profileLink.addEventListener('click', async () => {
      const res = await API.getDoctorProfile();
      if (res?.data) {
        const d = res.data;
        document.getElementById('pSpecialization').value = d.specialization || '';
        document.getElementById('pLicense').value        = d.licenseNumber || '';
        document.getElementById('pYears').value          = d.yearsExperience || '';
        document.getElementById('pHospital').value       = d.hospitalName || '';
        document.getElementById('pFee').value            = d.consultationFee || '';
        document.getElementById('pBio').value            = d.bio || '';
        document.getElementById('pDays').value           = d.availableDays || '';
      }
    });
  }
});
