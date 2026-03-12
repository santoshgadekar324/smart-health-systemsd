package com.smarthealth.controllers;

import com.smarthealth.dto.*;
import com.smarthealth.models.*;
import com.smarthealth.repositories.*;
import com.smarthealth.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

// ═════════════════════════════════════════════════════════════
// DOCTOR CONTROLLER
// ═════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/doctor")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
class DoctorController {

    @Autowired private AppointmentRepository appointmentRepo;
    @Autowired private DoctorRepository doctorRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtils jwtUtils;

    /** GET /api/doctor/appointments */
    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointments(
            @RequestHeader("Authorization") String authHeader) {
        Doctor doctor = getDoctor(authHeader);
        List<AppointmentResponse> list = appointmentRepo
                .findByDoctorIdOrderByAppointmentDateAscAppointmentTimeAsc(doctor.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Appointments.", list));
    }

    /** PUT /api/doctor/updateAppointment/{id} */
    @PutMapping("/updateAppointment/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentRequest req,
            @RequestHeader("Authorization") String authHeader) {

        Doctor doctor = getDoctor(authHeader);
        Appointment appt = appointmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appt.getDoctor().getId().equals(doctor.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Forbidden"));
        }

        appt.setStatus(Appointment.Status.valueOf(req.getStatus()));
        if (req.getDoctorNotes() != null)     appt.setDoctorNotes(req.getDoctorNotes());
        if (req.getRejectionReason() != null) appt.setRejectionReason(req.getRejectionReason());

        appointmentRepo.save(appt);
        return ResponseEntity.ok(ApiResponse.ok("Appointment updated.", toDto(appt)));
    }

    /** GET /api/doctor/profile */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Doctor>> profile(
            @RequestHeader("Authorization") String authHeader) {
        Doctor doctor = getDoctor(authHeader);
        doctor.getUser().setPassword(null);
        return ResponseEntity.ok(ApiResponse.ok("Doctor profile.", doctor));
    }

    /** PUT /api/doctor/profile */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Doctor>> updateProfile(
            @Valid @RequestBody DoctorProfileRequest req,
            @RequestHeader("Authorization") String authHeader) {

        Doctor doctor = getDoctor(authHeader);
        doctor.setSpecialization(req.getSpecialization());
        doctor.setLicenseNumber(req.getLicenseNumber());
        doctor.setYearsExperience(req.getYearsExperience());
        doctor.setHospitalName(req.getHospitalName());
        doctor.setConsultationFee(req.getConsultationFee());
        doctor.setBio(req.getBio());
        doctor.setAvailableDays(req.getAvailableDays());
        doctor.setSlotStartTime(req.getSlotStartTime());
        doctor.setSlotEndTime(req.getSlotEndTime());
        doctor.setSlotDurationMin(req.getSlotDurationMin());
        doctorRepo.save(doctor);
        doctor.getUser().setPassword(null);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated.", doctor));
    }

    private Doctor getDoctor(String header) {
        String email = jwtUtils.getEmailFromToken(header.substring(7));
        User user = userRepo.findByEmail(email).orElseThrow();
        return doctorRepo.findByUserId(user.getId()).orElseThrow();
    }

    private AppointmentResponse toDto(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getFullName(),
                a.getPatient().getEmail(),
                a.getDoctor().getUser().getFullName(),
                a.getDoctor().getSpecialization(),
                a.getAppointmentDate().toString(),
                a.getAppointmentTime().toString(),
                a.getStatus().name(),
                a.getReason(), a.getDoctorNotes(),
                a.getCreatedAt().toString()
        );
    }
}

// ═════════════════════════════════════════════════════════════
// ADMIN CONTROLLER
// ═════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
class AdminController {

    @Autowired private UserRepository userRepo;
    @Autowired private DoctorRepository doctorRepo;
    @Autowired private AppointmentRepository appointmentRepo;
    @Autowired private PredictionRepository predictionRepo;

    /** GET /api/admin/manageUsers */
    @GetMapping("/manageUsers")
    public ResponseEntity<ApiResponse<List<User>>> manageUsers() {
        List<User> users = userRepo.findAll();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(ApiResponse.ok("All users.", users));
    }

    /** PUT /api/admin/manageUsers/{id}/toggle */
    @PutMapping("/manageUsers/{id}/toggle")
    public ResponseEntity<ApiResponse<String>> toggleUser(@PathVariable Long id) {
        User user = userRepo.findById(id).orElseThrow();
        user.setIsActive(!user.getIsActive());
        userRepo.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User status toggled.",
                user.getIsActive() ? "ACTIVE" : "DEACTIVATED"));
    }

    /** GET /api/admin/manageDoctors */
    @GetMapping("/manageDoctors")
    public ResponseEntity<ApiResponse<List<Doctor>>> manageDoctors() {
        List<Doctor> docs = doctorRepo.findAll();
        docs.forEach(d -> d.getUser().setPassword(null));
        return ResponseEntity.ok(ApiResponse.ok("All doctors.", docs));
    }

    /** POST /api/admin/manageDoctors/register — create a doctor account */
    @PostMapping("/manageDoctors/register")
    public ResponseEntity<ApiResponse<Doctor>> registerDoctor(
            @RequestBody RegisterDoctorRequest req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(User.Role.DOCTOR);
        userRepo.save(user);

        Doctor doc = Doctor.builder()
                .user(user)
                .specialization(req.specialization())
                .licenseNumber(req.licenseNumber())
                .yearsExperience(req.yearsExperience())
                .hospitalName(req.hospitalName())
                .isVerified(false)
                .build();
        doctorRepo.save(doc);
        doc.getUser().setPassword(null);
        return ResponseEntity.ok(ApiResponse.ok("Doctor registered.", doc));
    }

    /** PUT /api/admin/manageDoctors/{id}/verify */
    @PutMapping("/manageDoctors/{id}/verify")
    public ResponseEntity<ApiResponse<String>> verifyDoctor(@PathVariable Long id) {
        Doctor doc = doctorRepo.findById(id).orElseThrow();
        doc.setIsVerified(true);
        doctorRepo.save(doc);
        return ResponseEntity.ok(ApiResponse.ok("Doctor verified.", "VERIFIED"));
    }

    /** GET /api/admin/manageAppointments */
    @GetMapping("/manageAppointments")
    public ResponseEntity<ApiResponse<List<Appointment>>> manageAppts() {
        return ResponseEntity.ok(ApiResponse.ok("All appointments.", appointmentRepo.findAll()));
    }

    /** GET /api/admin/analytics */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> analytics() {
        long totalUsers   = userRepo.count();
        long patients     = userRepo.countByRole(User.Role.PATIENT);
        long doctors      = userRepo.countByRole(User.Role.DOCTOR);
        long appointments = appointmentRepo.count();
        long pending      = appointmentRepo.countByStatus(Appointment.Status.PENDING);
        long predictions  = predictionRepo.count();

        List<DiseaseCount> topDiseases = predictionRepo.findTopDiseases()
                .stream()
                .map(row -> new DiseaseCount((String) row[0], (Long) row[1]))
                .limit(10).collect(Collectors.toList());

        AnalyticsResponse data = new AnalyticsResponse(
                totalUsers, patients, doctors, appointments, pending, predictions, topDiseases
        );
        return ResponseEntity.ok(ApiResponse.ok("Analytics.", data));
    }

    record RegisterDoctorRequest(Long userId, String specialization,
                                  String licenseNumber, Integer yearsExperience,
                                  String hospitalName) {}
}
