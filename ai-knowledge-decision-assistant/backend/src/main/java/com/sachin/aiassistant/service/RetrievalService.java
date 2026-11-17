package com.sachin.aiassistant.service;

import com.sachin.aiassistant.entity.DocumentChunk;
import com.sachin.aiassistant.repository.DocumentChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RetrievalService {
    private static final Set<String> STOP_WORDS = Set.of("the","a","an","is","are","am","was","were","to","of","for","in","on","at","and","or","i","we","you","my","our","what","how","when","where","which","who","can","could","would","should","do","does","did","it","this","that","with","from","be","as","by","about","tell","me","please");
    private final DocumentChunkRepository chunks;
    private final int maxChunks;
    private final int minimumScore;
    public RetrievalService(DocumentChunkRepository chunks,
                            @Value("${app.retrieval.max-chunks:6}") int maxChunks,
                            @Value("${app.retrieval.minimum-score:1}") int minimumScore) {
        this.chunks=chunks; this.maxChunks=maxChunks; this.minimumScore=minimumScore;
    }

    public List<RetrievedPassage> retrieve(UUID organizationId, String question) {
        Set<String> queryTerms = terms(question);
        if (queryTerms.isEmpty()) return List.of();
        String normalizedQuestion = question.toLowerCase(Locale.ROOT);
        return chunks.findAllForOrganization(organizationId).stream()
                .map(chunk -> score(chunk, queryTerms, normalizedQuestion))
                .filter(p -> p.score() >= minimumScore)
                .sorted(Comparator.comparingInt(RetrievedPassage::score).reversed())
                .limit(maxChunks)
                .toList();
    }

    private RetrievedPassage score(DocumentChunk chunk, Set<String> queryTerms, String question) {
        String text = chunk.getContent().toLowerCase(Locale.ROOT);
        Set<String> contentTerms = terms(text);
        int overlap = 0;
        for (String term : queryTerms) if (contentTerms.contains(term)) overlap++;
        int score = overlap * 3;
        if (text.contains(question)) score += 10;
        if (queryTerms.size() > 1) {
            long phraseHits = queryTerms.stream().filter(text::contains).count();
            if (phraseHits >= Math.min(3, queryTerms.size())) score += 2;
        }
        return new RetrievedPassage(chunk, score);
    }

    private Set<String> terms(String value) {
        return Pattern.compile("[a-z0-9]{2,}").matcher(value.toLowerCase(Locale.ROOT)).results()
                .map(m -> m.group()).filter(t -> !STOP_WORDS.contains(t)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public record RetrievedPassage(DocumentChunk chunk, int score) {}
}
