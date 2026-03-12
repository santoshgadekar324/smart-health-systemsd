package com.smarthealth.repositories;

import com.smarthealth.models.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ─────────────────────────────────────────────────────────────
// UserRepository
// ─────────────────────────────────────────────────────────────
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(User.Role role);
    Page<User> findByRoleAndIsActive(User.Role role, Boolean isActive, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") User.Role role);
}

// ─────────────────────────────────────────────────────────────
// DoctorRepository
// ─────────────────────────────────────────────────────────────
@Repository
interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findBySpecializationIgnoreCase(String specialization);
    List<Doctor> findByIsVerifiedTrue();
    Page<Doctor> findByIsVerifiedTrue(Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE LOWER(d.specialization) LIKE LOWER(CONCAT('%', :q, '%')) AND d.isVerified = true")
    List<Doctor> searchBySpecialization(@Param("q") String query);
}

// ─────────────────────────────────────────────────────────────
// AppointmentRepository
// ─────────────────────────────────────────────────────────────
@Repository
interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Appointment> findByDoctorIdOrderByAppointmentDateAscAppointmentTimeAsc(Long doctorId);
    List<Appointment> findByDoctorIdAndStatus(Long doctorId, Appointment.Status status);
    List<Appointment> findByStatus(Appointment.Status status);
    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTime(Long doctorId, LocalDate date, java.time.LocalTime time);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :status")
    long countByStatus(@Param("status") Appointment.Status status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.appointmentDate = :date")
    long countByDate(@Param("date") LocalDate date);
}

// ─────────────────────────────────────────────────────────────
// PredictionRepository
// ─────────────────────────────────────────────────────────────
@Repository
interface PredictionRepository extends JpaRepository<Prediction, Long> {
    List<Prediction> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    Page<Prediction> findByPatientId(Long patientId, Pageable pageable);

    @Query("SELECT p.predictedDisease, COUNT(p) AS cnt FROM Prediction p GROUP BY p.predictedDisease ORDER BY cnt DESC")
    List<Object[]> findTopDiseases();
}

// ─────────────────────────────────────────────────────────────
// SymptomRepository
// ─────────────────────────────────────────────────────────────
@Repository
interface SymptomRepository extends JpaRepository<Symptom, Long> {
    List<Symptom> findByNameContainingIgnoreCase(String name);
    Optional<Symptom> findByNameIgnoreCase(String name);
}
