package com.zenius.one.modules.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Computes deterministic 64-dimensional normalized feature vectors and cosine similarity
 * for local/offline RAG chunk retrieval without requiring external model runtimes.
 */
final class VectorEmbeddingHelper {

    private static final int DIMENSIONS = 64;

    private VectorEmbeddingHelper() {}

    static List<String> chunkText(String text, int maxChars, int overlapChars) {
        String normalized = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + maxChars);
            chunks.add(normalized.substring(start, end));
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(start + 1, end - overlapChars);
        }
        return chunks;
    }

    static double[] embed(String text) {
        double[] vec = new double[DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vec;
        }
        String clean = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = clean.trim().split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), DIMENSIONS);
            vec[bucket] += 1.0;
            if (token.length() >= 4) {
                for (int i = 0; i <= token.length() - 3; i++) {
                    String tri = token.substring(i, i + 3);
                    vec[Math.floorMod(tri.hashCode(), DIMENSIONS)] += 0.35;
                }
            }
        }
        double norm = 0.0;
        for (double v : vec) {
            norm += v * v;
        }
        if (norm > 0.0) {
            double inv = 1.0 / Math.sqrt(norm);
            for (int i = 0; i < DIMENSIONS; i++) {
                vec[i] *= inv;
            }
        }
        return vec;
    }

    static double cosine(double[] a, double[] b) {
        if (a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }
}

