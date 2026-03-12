/**
 * patient.js — Patient Dashboard Logic
 * Handles symptom checker, predictions, appointments
 */

// All 30 symptoms matching the Python model
const ALL_SYMPTOMS = [
  'Fever', 'Cough', 'Headache', 'Fatigue', 'Shortness of Breath',
  'Chest Pain', 'Nausea', 'Vomiting', 'Diarrhea', 'Stomach Pain',
  'Sore Throat', 'Runny Nose', 'Body Aches', 'Chills', 'Rash',
  'Joint Pain', 'Dizziness', 'Swollen Lymph Nodes', 'Loss of Appetite',
  'Night Sweats', 'Blurred Vision', 'Frequent Urination', 'Excessive Thirst',
  'Weight Loss', 'Itching', 'Swelling', 'Back Pain', 'Confusion',
  'Palpitations', 'Sneezing',
];

let selectedSymptoms = new Set();
let lastPrediction   = null;

// ─── Init ────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  if (!requireAuth('PATIENT')) return;

  // Set min date for appointment to today
  const dateInput = document.getElementById('apptDate');
  if (dateInput) dateInput.min = new Date().toISOString().split('T')[0];

  buildSymptomGrid();
  loadOverview();
});

// ─── Symptom Grid ────────────────────────────────────────────
function buildSymptomGrid() {
  const grid = document.getElementById('symptomGrid');
  if (!grid) return;

  grid.innerHTML = ALL_SYMPTOMS.map(sym => `
    <div class="symptom-chip" data-symptom="${sym}" onclick="toggleSymptom(this, '${sym}')">
      <span class="chip-dot"></span>
      ${sym}
    </div>
  `).join('');
}

function toggleSymptom(el, name) {
  if (selectedSymptoms.has(name)) {
    selectedSymptoms.delete(name);
    el.classList.remove('selected');
  } else {
    selectedSymptoms.add(name);
    el.classList.add('selected');
  }
}

// ─── Run AI Prediction ───────────────────────────────────────
async function runPrediction() {
  const alertEl  = document.getElementById('symptomAlert');
  const resultEl = document.getElementById('predictionResult');

  if (selectedSymptoms.size < 1) {
    showInlineAlert(alertEl, 'Please select at least one symptom.', 'warning');
    return;
  }

  const btn     = document.getElementById('predictBtn');
  const spinner = document.getElementById('predictSpinner');
  btn.disabled  = true;
  spinner.classList.remove('d-none');
  alertEl.classList.add('d-none');
  resultEl.classList.add('d-none');

  try {
    const notes = document.getElementById('symptomNotes').value;
    const res = await API.addSymptoms([...selectedSymptoms], notes);

    if (!res.success) {
      showInlineAlert(alertEl, res.message || 'Prediction failed.', 'danger');
      return;
    }

    lastPrediction = res.data;
    displayResult(res.data);
    resultEl.classList.remove('d-none');
    resultEl.scrollIntoView({ behavior: 'smooth', block: 'start' });

  } catch (e) {
    showInlineAlert(alertEl, 'Network error. Is the backend + AI service running?', 'danger');
  } finally {
    btn.disabled = false;
    spinner.classList.add('d-none');
  }
}

function displayResult(pred) {
  document.getElementById('prDisease').textContent  = pred.predictedDisease;
  const confPct = Math.round((pred.confidence || 0) * 100);
  document.getElementById('prConf').textContent     = `${confPct}%`;
  document.getElementById('confFill').style.width   = `${confPct}%`;
  document.getElementById('prSpecialty').textContent = pred.recommendedSpecialty;

  const altsEl = document.getElementById('prAlts');
  if (pred.alternatives && pred.alternatives.length) {
    altsEl.innerHTML = `
      <strong>Other possibilities:</strong>
      ${pred.alternatives.map(a =>
        `<span class="me-3">${a.disease} (${Math.round(a.probability * 100)}%)</span>`
      ).join('')}
    `;
  }
}

// ─── Load Overview ───────────────────────────────────────────
async function loadOverview() {
  const [predsRes, apptsRes] = await Promise.allSettled([
    API.getPredictions(),
    API.getAppointments(),
  ]);

  const preds = predsRes.status === 'fulfilled' && predsRes.value?.data ? predsRes.value.data : [];
  const appts = apptsRes.status === 'fulfilled' && apptsRes.value?.data ? apptsRes.value.data : [];

  // Stats
  document.getElementById('statPredictions').textContent = preds.length;
  document.getElementById('statAppts').textContent       = appts.length;
  document.getElementById('statPending').textContent     = appts.filter(a => a.status === 'PENDING').length;
  document.getElementById('statCompleted').textContent   = appts.filter(a => a.status === 'COMPLETED').length;

  // Recent predictions
  renderPredictions(preds.slice(0, 5), 'recentPredictions');
  renderPredictions(preds, 'predictionHistory');

  // Appointments
  renderAppointments(appts, 'appointmentList');
}

// ─── Renderers ───────────────────────────────────────────────
function renderPredictions(preds, containerId) {
  const el = document.getElementById(containerId);
  if (!el) return;

  if (!preds.length) {
    el.innerHTML = `<div class="text-muted py-4 text-center">No predictions yet.</div>`;
    return;
  }

  el.innerHTML = preds.map(p => `
    <div class="pred-card">
      <div class="d-flex justify-content-between align-items-start">
        <div>
          <strong>${p.predictedDisease}</strong>
          <div class="text-muted small mt-1">${p.symptomNames}</div>
        </div>
        <div class="text-end">
          <div class="text-muted small">${fmtDate(p.createdAt)}</div>
          <div class="mt-1" style="color:var(--teal);font-size:13px">
            Confidence: ${Math.round((p.confidenceScore || 0) * 100)}%
          </div>
        </div>
      </div>
      ${p.recommendedSpecialty ? `
        <div class="mt-2 small" style="color:var(--text-muted)">
          <i class="bi bi-person-badge me-1"></i> ${p.recommendedSpecialty}
        </div>` : ''}
    </div>
  `).join('');
}

function renderAppointments(appts, containerId) {
  const el = document.getElementById(containerId);
  if (!el) return;

  if (!appts.length) {
    el.innerHTML = `<div class="text-muted py-4 text-center">No appointments yet.</div>`;
    return;
  }

  el.innerHTML = appts.map(a => `
    <div class="appt-card">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
        <div>
          <strong>Dr. ${a.doctorName}</strong>
          <span class="text-muted small ms-2">${a.specialization}</span>
        </div>
        ${statusBadge(a.status)}
      </div>
      <div class="text-muted small mt-2">
        <i class="bi bi-calendar3 me-1"></i> ${a.appointmentDate}
        <i class="bi bi-clock ms-3 me-1"></i> ${a.appointmentTime}
      </div>
      ${a.reason ? `<div class="mt-2 small">${a.reason}</div>` : ''}
      ${a.doctorNotes ? `<div class="mt-1 small" style="color:var(--teal)">
        <i class="bi bi-chat-left-text me-1"></i> ${a.doctorNotes}</div>` : ''}
    </div>
  `).join('');
}

// ─── Load Doctors ────────────────────────────────────────────
async function loadDoctors() {
  const specialty = document.getElementById('specialtyFilter')?.value || '';
  const res = await API.getDoctors(specialty);
  const select = document.getElementById('doctorSelect');
  if (!res?.data || !select) return;

  select.innerHTML = '<option value="">— Choose a doctor —</option>' +
    res.data.map(d => `
      <option value="${d.id}">
        Dr. ${d.user?.fullName || 'Unknown'} — ${d.specialization}
        ${d.hospitalName ? `| ${d.hospitalName}` : ''}
      </option>
    `).join('');
}

// ─── Book Appointment ────────────────────────────────────────
async function bookAppointment() {
  const alertEl = document.getElementById('bookAlert');
  const doctorId  = document.getElementById('doctorSelect')?.value;
  const date      = document.getElementById('apptDate')?.value;
  const time      = document.getElementById('apptTime')?.value;
  const reason    = document.getElementById('apptReason')?.value;

  if (!doctorId) { showInlineAlert(alertEl, 'Please select a doctor.', 'warning'); return; }
  if (!date)     { showInlineAlert(alertEl, 'Please choose a date.', 'warning'); return; }
  if (!time)     { showInlineAlert(alertEl, 'Please choose a time.', 'warning'); return; }

  const payload = {
    doctorId: parseInt(doctorId),
    appointmentDate: date,
    appointmentTime: time + ':00',
    reason,
    predictionId: lastPrediction?.predictionId || null,
  };

  const res = await API.bookAppointment(payload);
  if (res?.success) {
    showInlineAlert(alertEl, 'Appointment booked successfully!', 'success');
    loadOverview();
  } else {
    showInlineAlert(alertEl, res?.message || 'Booking failed.', 'danger');
  }
}

// ─── Helpers ─────────────────────────────────────────────────
function showInlineAlert(el, msg, type) {
  if (!el) return;
  el.className = `alert alert-${type}`;
  el.textContent = msg;
  el.classList.remove('d-none');
}

// Load doctors on page init (for book tab)
window.addEventListener('DOMContentLoaded', () => {
  loadDoctors();
});
