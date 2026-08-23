package com.email.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates/recommend")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TemplateRecommendationController {

    private final TemplateRecommendationEngine recommendationEngine;
    private final EmailTemplateRepository templateRepository;
    private final TelemetryService telemetryService;

    @PostMapping
    public ResponseEntity<List<TemplateRecommendationEngine.ScoredTemplate>> getRecommendations(@RequestBody Map<String, String> requestBody) {
        long startTime = System.currentTimeMillis();
        String context = requestBody.get("emailContent");
        
        try {
            List<EmailTemplate> allTemplates = templateRepository.findAll();
            List<TemplateRecommendationEngine.ScoredTemplate> recommendations = recommendationEngine.rankTemplates(context, allTemplates);
            
            // Assuming we take top 5
            List<TemplateRecommendationEngine.ScoredTemplate> topResults = recommendations.size() > 5 
                    ? recommendations.subList(0, 5) 
                    : recommendations;
            
            long duration = System.currentTimeMillis() - startTime;
            telemetryService.logTelemetry("internal-engine", "/templates/recommend", duration, "SUCCESS", null, context != null ? context.length() : 0);
            
            return ResponseEntity.ok(topResults);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            telemetryService.logTelemetry("internal-engine", "/templates/recommend", duration, "ERROR", e.getMessage(), context != null ? context.length() : 0);
            throw new RuntimeException("Failed to generate recommendations", e);
        }
    }
}
