package com.smarthealth.controllers;

import com.smarthealth.dto.*;
import com.smarthealth.models.*;
import com.smarthealth.repositories.*;
import com.smarthealth.security.JwtUtils;
import com.smarthealth.services.PredictionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Patient-facing endpoints:
 *   addSymptoms / getPrediction / bookAppointment / history
 */
@RestController
@RequestMapping("/patient")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
public class PatientController {

    @Autowired private PredictionService predictionService;
    @Autowired private AppointmentRepository appointmentRepo;
    @Autowired private PredictionRepository predictionRepo;
    @Autowired private DoctorRepository doctorRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtils jwtUtils;

    /** POST /api/patient/addSymptoms   → calls Python AI, saves prediction */
    @PostMapping("/addSymptoms")
    public ResponseEntity<ApiResponse<PredictionResponse>> addSymptoms(
            @Valid @RequestBody PredictionRequest req,
            @RequestHeader("Authorization") String authHeader) {

        User patient = getUser(authHeader);
        PredictionResponse result = predictionService.predict(req, patient);
        return ResponseEntity.ok(ApiResponse.ok("Prediction complete.", result));
    }

    /** GET /api/patient/getPrediction/{id} */
    @GetMapping("/getPrediction/{id}")
    public ResponseEntity<ApiResponse<Prediction>> getPrediction(@PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        User patient = getUser(authHeader);
        Prediction pred = predictionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Prediction not found"));
        if (!pred.getPatient().getId().equals(patient.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Forbidden"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Prediction found.", pred));
    }

    /** GET /api/patient/predictions */
    @GetMapping("/predictions")
    public ResponseEntity<ApiResponse<List<Prediction>>> myPredictions(
            @RequestHeader("Authorization") String authHeader) {
        User patient = getUser(authHeader);
        List<Prediction> preds = predictionRepo.findByPatientIdOrderByCreatedAtDesc(patient.getId());
        return ResponseEntity.ok(ApiResponse.ok("Prediction history.", preds));
    }

    /** POST /api/patient/bookAppointment */
    @PostMapping("/bookAppointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> book(
            @Valid @RequestBody BookAppointmentRequest req,
            @RequestHeader("Authorization") String authHeader) {

        User patient = getUser(authHeader);

        Doctor doctor = doctorRepo.findById(req.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        // Check slot availability
        boolean slotTaken = appointmentRepo.existsByDoctorIdAndAppointmentDateAndAppointmentTime(
                doctor.getId(), req.getAppointmentDate(), req.getAppointmentTime());
        if (slotTaken) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Slot already booked."));
        }

        Prediction prediction = null;
        if (req.getPredictionId() != null) {
            prediction = predictionRepo.findById(req.getPredictionId()).orElse(null);
        }

        Appointment appt = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .prediction(prediction)
                .appointmentDate(req.getAppointmentDate())
                .appointmentTime(req.getAppointmentTime())
                .reason(req.getReason())
                .status(Appointment.Status.PENDING)
                .build();

        appointmentRepo.save(appt);

        return ResponseEntity.ok(ApiResponse.ok("Appointment booked.", toDto(appt)));
    }

    /** GET /api/patient/appointments */
    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> myAppointments(
            @RequestHeader("Authorization") String authHeader) {
        User patient = getUser(authHeader);
        List<AppointmentResponse> list = appointmentRepo
                .findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Appointments.", list));
    }

    /** GET /api/patient/doctors?specialty=Cardiologist */
    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<Doctor>>> findDoctors(
            @RequestParam(required = false) String specialty) {
        List<Doctor> doctors = specialty != null
                ? doctorRepo.searchBySpecialization(specialty)
                : doctorRepo.findByIsVerifiedTrue();
        doctors.forEach(d -> d.getUser().setPassword(null));
        return ResponseEntity.ok(ApiResponse.ok("Doctors.", doctors));
    }

    // ── Helpers ──────────────────────────────────────────────
    private User getUser(String authHeader) {
        String email = jwtUtils.getEmailFromToken(authHeader.substring(7));
        return userRepo.findByEmail(email).orElseThrow();
    }

    private AppointmentResponse toDto(Appointment a) {
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getFullName(),
                a.getPatient().getEmail(),
                a.getDoctor().getUser().getFullName(),
                a.getDoctor().getSpecialization(),
                a.getAppointmentDate().format(df),
                a.getAppointmentTime().format(tf),
                a.getStatus().name(),
                a.getReason(),
                a.getDoctorNotes(),
                a.getCreatedAt().toString()
        );
    }
}
