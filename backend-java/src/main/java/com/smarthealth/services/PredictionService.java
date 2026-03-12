package com.smarthealth.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealth.dto.*;
import com.smarthealth.models.Prediction;
import com.smarthealth.models.User;
import com.smarthealth.repositories.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calls the Python Flask AI service and persists the prediction result.
 */
@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Autowired
    private PredictionRepository predictionRepo;

    public PredictionResponse predict(PredictionRequest req, User patient) {

        // 1. Call Python AI API
        JsonNode aiResult = callAiService(req.getSymptoms());

        // 2. Parse response
        String disease    = aiResult.path("predicted_disease").asText("Unknown");
        double confidence = aiResult.path("confidence").asDouble(0.0);
        String specialty  = aiResult.path("recommended_specialty").asText("General Physician");

        List<AlternativeDisease> alts = new ArrayList<>();
        for (JsonNode alt : aiResult.path("alternatives")) {
            alts.add(new AlternativeDisease(
                    alt.path("disease").asText(),
                    alt.path("probability").asDouble()
            ));
        }

        // 3. Persist to DB
        Prediction saved = Prediction.builder()
                .patient(patient)
                .symptomIds("[]")                               // IDs not resolved in this demo
                .symptomNames(String.join(", ", req.getSymptoms()))
                .predictedDisease(disease)
                .confidenceScore(BigDecimal.valueOf(confidence))
                .alternativeDiseases(aiResult.path("alternatives").toString())
                .recommendedSpecialty(specialty)
                .notes(req.getNotes())
                .build();

        predictionRepo.save(saved);

        // 4. Return DTO
        return new PredictionResponse(
                saved.getId(),
                disease,
                confidence,
                specialty,
                alts,
                req.getSymptoms(),
                saved.getCreatedAt().toString()
        );
    }

    // ── Private helpers ──────────────────────────────────────
    private JsonNode callAiService(List<String> symptoms) {
        try {
            String body = mapper.writeValueAsString(Map.of("symptoms", symptoms));

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/predict-disease"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(httpReq,
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log.error("AI service returned {}: {}", resp.statusCode(), resp.body());
                throw new RuntimeException("AI service error: " + resp.statusCode());
            }

            return mapper.readTree(resp.body());

        } catch (Exception e) {
            log.error("Failed to call AI service: {}", e.getMessage());
            // Return a fallback node so the system doesn't crash
            try {
                return mapper.readTree("""
                    {
                      "predicted_disease": "Unable to diagnose",
                      "confidence": 0.0,
                      "recommended_specialty": "General Physician",
                      "alternatives": []
                    }
                    """);
            } catch (Exception ex) { throw new RuntimeException(ex); }
        }
    }
}
