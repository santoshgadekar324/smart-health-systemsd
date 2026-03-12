package com.smarthealth.models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(name = "symptom_ids", columnDefinition = "TEXT", nullable = false)
    private String symptomIds;            // JSON array e.g. "[1,3,7]"

    @Column(name = "symptom_names", columnDefinition = "TEXT", nullable = false)
    private String symptomNames;          // "Fever, Cough, Headache"

    @Column(name = "predicted_disease", nullable = false, length = 150)
    private String predictedDisease;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "alternative_diseases", columnDefinition = "TEXT")
    private String alternativeDiseases;   // JSON array

    @Column(name = "recommended_specialty", length = 100)
    private String recommendedSpecialty;

    @Column(name = "ai_model_version", length = 20)
    @Builder.Default
    private String aiModelVersion = "1.0";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
