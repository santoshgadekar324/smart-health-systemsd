package com.smarthealth.dto;

import com.smarthealth.models.User;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// ─── Auth DTOs ────────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank  String fullName;
    @Email @NotBlank String email;
    @NotBlank @Size(min=8) String password;
    String phone;
    User.Role role;
    User.Gender gender;
    LocalDate dateOfBirth;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @Email @NotBlank String email;
    @NotBlank        String password;
}

@Data @AllArgsConstructor
public class AuthResponse {
    String token;
    String email;
    String fullName;
    String role;
    Long   userId;
}

// ─── Symptom / Prediction DTOs ────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor
public class PredictionRequest {
    @NotEmpty List<String> symptoms;
    String   notes;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class PredictionResponse {
    Long   predictionId;
    String predictedDisease;
    Double confidence;
    String recommendedSpecialty;
    List<AlternativeDisease> alternatives;
    List<String> symptomsUsed;
    String createdAt;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class AlternativeDisease {
    String disease;
    Double probability;
}

// ─── Appointment DTOs ─────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor
public class BookAppointmentRequest {
    @NotNull Long      doctorId;
    @NotNull LocalDate appointmentDate;
    @NotNull LocalTime appointmentTime;
    Long   predictionId;
    String reason;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class AppointmentResponse {
    Long   id;
    String patientName;
    String patientEmail;
    String doctorName;
    String specialization;
    String appointmentDate;
    String appointmentTime;
    String status;
    String reason;
    String doctorNotes;
    String createdAt;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateAppointmentRequest {
    @NotBlank String status;       // APPROVED | REJECTED | COMPLETED
    String doctorNotes;
    String rejectionReason;
}

// ─── Doctor DTO ───────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor
public class DoctorProfileRequest {
    @NotBlank String specialization;
    String   licenseNumber;
    Integer  yearsExperience;
    String   hospitalName;
    BigDecimal consultationFee;
    String   bio;
    String   availableDays;
    LocalTime slotStartTime;
    LocalTime slotEndTime;
    Integer  slotDurationMin;
}

// ─── Admin Analytics DTO ─────────────────────────────────────
@Data @AllArgsConstructor
public class AnalyticsResponse {
    long totalUsers;
    long totalPatients;
    long totalDoctors;
    long totalAppointments;
    long pendingAppointments;
    long totalPredictions;
    List<DiseaseCount> topDiseases;
}

@Data @AllArgsConstructor
public class DiseaseCount {
    String disease;
    long   count;
}

// ─── Generic API wrapper ──────────────────────────────────────
@Data @AllArgsConstructor
public class ApiResponse<T> {
    boolean success;
    String  message;
    T       data;

    public static <T> ApiResponse<T> ok(String msg, T data) {
        return new ApiResponse<>(true, msg, data);
    }
    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>(false, msg, null);
    }
}
