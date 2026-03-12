package com.smarthealth.controllers;

import com.smarthealth.dto.*;
import com.smarthealth.models.User;
import com.smarthealth.repositories.UserRepository;
import com.smarthealth.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints – register and login.
 * No authentication required on these routes.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired private AuthenticationManager authManager;
    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtUtils jwtUtils;

    /** POST /api/auth/register */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest req) {

        // 1. Check email uniqueness
        if (userRepo.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email already registered."));
        }

        // 2. Default role to PATIENT if not specified
        User.Role role = req.getRole() != null ? req.getRole() : User.Role.PATIENT;

        // 3. Build & persist user
        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(encoder.encode(req.getPassword()))   // BCrypt
                .phone(req.getPhone())
                .role(role)
                .gender(req.getGender())
                .dateOfBirth(req.getDateOfBirth())
                .isActive(true)
                .build();

        userRepo.save(user);

        // 4. Issue JWT
        String token = jwtUtils.generateTokenFromEmail(user.getEmail());
        AuthResponse body = new AuthResponse(
                token, user.getEmail(), user.getFullName(),
                user.getRole().name(), user.getId()
        );

        return ResponseEntity.ok(ApiResponse.ok("Registration successful.", body));
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {

        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );

            String token = jwtUtils.generateToken(auth);

            User user = userRepo.findByEmail(req.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getIsActive()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Account is deactivated."));
            }

            AuthResponse body = new AuthResponse(
                    token, user.getEmail(), user.getFullName(),
                    user.getRole().name(), user.getId()
            );
            return ResponseEntity.ok(ApiResponse.ok("Login successful.", body));

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Invalid email or password."));
        }
    }

    /** GET /api/auth/me — return current user info from JWT */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> me(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String email = jwtUtils.getEmailFromToken(token);
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(null); // never expose password hash
        return ResponseEntity.ok(ApiResponse.ok("User info.", user));
    }
}
