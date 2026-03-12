package com.smarthealth.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ─────────────────────────────────────────────────────────────
// Symptom.java — master catalogue of symptoms
// ─────────────────────────────────────────────────────────────
@Entity
@Table(name = "symptoms")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class Symptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "body_part", length = 50)
    private String bodyPart;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private Severity severity = Severity.MILD;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum Severity { MILD, MODERATE, SEVERE }
}

// ─────────────────────────────────────────────────────────────
// MedicalHistory.java
// ─────────────────────────────────────────────────────────────
@Entity
@Table(name = "medical_history")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class MedicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(length = 200)
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String prescription;

    @Column(name = "lab_results", columnDefinition = "TEXT")
    private String labResults;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "chronic_conditions", columnDefinition = "TEXT")
    private String chronicConditions;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
