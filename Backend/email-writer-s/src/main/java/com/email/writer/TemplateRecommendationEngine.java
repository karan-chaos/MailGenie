package com.email.writer;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise service for extracting context from incoming emails 
 * and recommending the highest scoring templates.
 */
@Service
public class TemplateRecommendationEngine {

    // Simple stop word list to ignore common vocabulary
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "is", "at", "which", "on", "and", "a", "an", "to", "in",
            "of", "that", "this", "it", "with", "as", "for", "from", "by", "or"
    ));

    public static class ScoredTemplate {
        public EmailTemplate template;
        public double score;

        public ScoredTemplate(EmailTemplate template, double score) {
            this.template = template;
            this.score = score;
        }
    }

    /**
     * Given an incoming email content, rank the available templates by contextual relevance.
     * Higher score indicates higher contextual relevance.
     */
    public List<ScoredTemplate> rankTemplates(String inputEmail, List<EmailTemplate> availableTemplates) {
        if (inputEmail == null || inputEmail.trim().isEmpty()) {
            return availableTemplates.stream()
                    .map(t -> new ScoredTemplate(t, 0.0))
                    .collect(Collectors.toList());
        }

        Map<String, Integer> inputCounts = extractWordCounts(inputEmail.toLowerCase());
        
        List<ScoredTemplate> results = new ArrayList<>();
        
        for (EmailTemplate template : availableTemplates) {
            double score = scoreTemplate(inputCounts, template);
            results.add(new ScoredTemplate(template, score));
        }

        // Sort descending by score
        results.sort((t1, t2) -> Double.compare(t2.score, t1.score));
        return results;
    }

    private double scoreTemplate(Map<String, Integer> inputCounts, EmailTemplate template) {
        String templateText = (template.getTitle() + " " + template.getBody()).toLowerCase();
        Map<String, Integer> templateCounts = extractWordCounts(templateText);

        double score = 0.0;
        for (Map.Entry<String, Integer> entry : inputCounts.entrySet()) {
            String word = entry.getKey();
            if (templateCounts.containsKey(word)) {
                // simple TF based increment
                score += Math.sqrt(entry.getValue() * templateCounts.get(word));
            }
        }
        
        // Add contextual boosts for common themes
        if (templateText.contains("meeting") || templateText.contains("call")) {
            if (inputCounts.containsKey("sync") || inputCounts.containsKey("discuss") || inputCounts.containsKey("meet")) {
                score += 5.0;
            }
        }
        
        if (templateText.contains("thanks") || templateText.contains("thank you")) {
            if (inputCounts.containsKey("helpful") || inputCounts.containsKey("great") || inputCounts.containsKey("appreciate")) {
                score += 3.0;
            }
        }

        return score;
    }

    private Map<String, Integer> extractWordCounts(String text) {
        Map<String, Integer> wordCount = new HashMap<>();
        String[] words = text.replaceAll("[^a-zA-Z ]", "").split("\\s+");
        for (String word : words) {
            if (!word.isEmpty() && !STOP_WORDS.contains(word)) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }
        return wordCount;
    }
}
