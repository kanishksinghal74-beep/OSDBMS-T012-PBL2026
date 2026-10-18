package com.smartwarehouse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TabNetClient — HTTP Client for TabNet AI Order Scheduling & Demand Prediction.
 *
 * Connects to the local Python TabNet service (Flask / FastAPI) to obtain:
 * - Predicted Demand
 * - Stockout Risk Level
 * - Recommended Scheduling Algorithm (PRIORITY, SJF, ROUND ROBIN, FCFS)
 * - Priority Ranking (1 = Highest to 4 = Lowest)
 *
 * Features:
 * 1. Fully backward-compatible raw predict(...) method.
 * 2. Strongly-typed predictStructured(...) with automatic JSON extraction (zero external dependencies).
 * 3. Transparent fallback heuristics when the AI service is offline.
 * 4. Fast health check ping() / isOnline().
 */
public class TabNetClient {

    public static final String DEFAULT_ENDPOINT = "http://127.0.0.1:8000/predict";

    private final String endpoint;
    private final HttpClient client;

    // =========================================================
    // 1. DATA MODEL: PREDICTION RESULT
    // =========================================================

    public static class PredictionResult {
        private final double predictedDemand;
        private final String stockoutRisk;
        private final String recommendedAlgorithm;
        private final String reason;
        private final int priority;
        private final boolean fallback;
        private final String rawResponse;

        public PredictionResult(
                double predictedDemand,
                String stockoutRisk,
                String recommendedAlgorithm,
                String reason,
                int priority,
                boolean fallback,
                String rawResponse
        ) {
            this.predictedDemand = predictedDemand;
            this.stockoutRisk = stockoutRisk != null ? stockoutRisk.toUpperCase(Locale.US) : "MODERATE";
            this.recommendedAlgorithm = recommendedAlgorithm != null ? recommendedAlgorithm.toUpperCase(Locale.US) : "FCFS";
            this.reason = reason != null ? reason : "Standard order processing";
            this.priority = priority;
            this.fallback = fallback;
            this.rawResponse = rawResponse != null ? rawResponse : "";
        }

        public double getPredictedDemand() {
            return predictedDemand;
        }

        public String getStockoutRisk() {
            return stockoutRisk;
        }

        public String getRecommendedAlgorithm() {
            return recommendedAlgorithm;
        }

        public String getReason() {
            return reason;
        }

        public int getPriority() {
            return priority;
        }

        public boolean isFallback() {
            return fallback;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        public String getPriorityLabel() {
            return switch (priority) {
                case 1 -> "Priority 1 (Urgent / High Demand)";
                case 2 -> "Priority 2 (High / Short Burst)";
                case 3 -> "Priority 3 (Medium / Balanced)";
                default -> "Priority 4 (Standard FCFS)";
            };
        }

        @Override
        public String toString() {
            return String.format(
                    "PredictionResult[Demand=%.2f, Risk=%s, Algo=%s, Priority=%d, Fallback=%b]",
                    predictedDemand, stockoutRisk, recommendedAlgorithm, priority, fallback
            );
        }
    }

    // =========================================================
    // 2. CONSTRUCTORS
    // =========================================================

    public TabNetClient() {
        this(DEFAULT_ENDPOINT);
    }

    public TabNetClient(String endpoint) {
        this.endpoint = (endpoint != null && !endpoint.isBlank()) ? endpoint : DEFAULT_ENDPOINT;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public String getEndpoint() {
        return endpoint;
    }

    // =========================================================
    // 3. HEALTH CHECK / PING
    // =========================================================

    /**
     * Checks if the TabNet AI service is reachable.
     * Returns true if server responds, false otherwise.
     */
    public boolean isOnline() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(2))
                    .header("Accept", "application/json")
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> resp = client.send(request, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() > 0;
        } catch (Exception e) {
            // Also try a quick dummy POST if HEAD is not implemented by Flask
            try {
                String dummyJson = "{\"stock\":10,\"demand\":1.0,\"orders\":1,\"queue_length\":1}";
                HttpRequest postReq = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(2))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(dummyJson))
                        .build();

                HttpResponse<Void> postResp = client.send(postReq, HttpResponse.BodyHandlers.discarding());
                return postResp.statusCode() >= 200 && postResp.statusCode() < 500;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    // =========================================================
    // 4. ORIGINAL PREDICT (BACKWARD COMPATIBLE)
    // =========================================================

    /**
     * Sends raw prediction request to TabNet server and returns response string.
     */
    public String predict(int stock, double demand, int orders, int queue) throws Exception {
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }
        if (demand < 0) {
            throw new IllegalArgumentException("Demand cannot be negative.");
        }
        if (orders < 0) {
            throw new IllegalArgumentException("Orders cannot be negative.");
        }
        if (queue < 0) {
            throw new IllegalArgumentException("Queue length cannot be negative.");
        }

        // Use Locale.US to guarantee dot (.) as decimal separator in JSON
        String json = String.format(
                Locale.US,
                "{\"stock\":%d,\"demand\":%.2f,\"orders\":%d,\"queue_length\":%d}",
                stock, demand, orders, queue
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            return "TabNet API Error\nHTTP Status: " + response.statusCode() + "\nResponse: " + response.body();
        } catch (java.net.http.HttpTimeoutException e) {
            return "TabNet API Timeout\nThe AI server did not respond within the expected time.";
        } catch (java.net.ConnectException e) {
            return "TabNet API Connection Failed\nMake sure the TabNet Python server is running at:\n" + endpoint;
        }
    }

    // =========================================================
    // 5. STRUCTURED PREDICTION WITH TRANSPARENT FALLBACK
    // =========================================================

    /**
     * Obtains structured AI recommendation. If the TabNet service is unavailable,
     * falls back gracefully to deterministic warehouse heuristics.
     */
    public PredictionResult predictStructured(int stock, double demand, int orders, int queue) {
        int safeStock = Math.max(0, stock);
        double safeDemand = Math.max(0.0, demand);
        int safeOrders = Math.max(0, orders);
        int safeQueue = Math.max(0, queue);

        try {
            String rawJson = predict(safeStock, safeDemand, safeOrders, safeQueue);

            // Verify if rawJson is a valid JSON object
            if (rawJson != null && rawJson.trim().startsWith("{")) {
                double predictedDemand = extractDouble(rawJson, "predicted_demand", safeDemand);
                String risk = extractString(rawJson, "stockout_risk", "MODERATE");
                String algorithm = extractString(rawJson, "recommended_algorithm", "FCFS");
                String reason = extractString(rawJson, "reason", "TabNet AI Model Recommendation");

                int priority = mapAlgorithmToPriority(algorithm, risk);

                return new PredictionResult(
                        predictedDemand,
                        risk,
                        algorithm,
                        reason,
                        priority,
                        false,
                        rawJson
                );
            }

            // If response is an error message, compute transparent fallback
            return computeFallback(safeStock, safeDemand, safeOrders, safeQueue, rawJson);

        } catch (Exception e) {
            return computeFallback(safeStock, safeDemand, safeOrders, safeQueue, e.getMessage());
        }
    }

    // =========================================================
    // 6. ALGORITHM TO PRIORITY MAPPING
    // =========================================================

    /**
     * Maps AI recommendation algorithms to OS priority levels:
     * Priority 1 = PRIORITY (Critical / High Surge)
     * Priority 2 = SJF (Shortest Job First / Low Stock Buffer)
     * Priority 3 = ROUND ROBIN (Balanced load across warehouse)
     * Priority 4 = FCFS (Standard First-Come First-Served)
     */
    public static int mapAlgorithmToPriority(String algorithm, String risk) {
        if (algorithm == null) algorithm = "FCFS";
        String algo = algorithm.toUpperCase(Locale.US).trim();

        if ("CRITICAL".equalsIgnoreCase(risk)) {
            return 1;
        }

        return switch (algo) {
            case "PRIORITY" -> 1;
            case "SJF" -> 2;
            case "ROUND ROBIN", "RR" -> 3;
            case "FCFS" -> 4;
            default -> 4;
        };
    }

    // =========================================================
    // 7. DETERMINISTIC TRANSPARENT FALLBACK
    // =========================================================

    /**
     * Transparent rule-based fallback when the AI model is offline.
     * Prompt Section 6: Clearly labeled as fallback, NOT fake AI predictions.
     */
    private PredictionResult computeFallback(
            int stock,
            double demand,
            int orders,
            int queue,
            String failureReason
    ) {
        String risk;
        String algorithm;
        int priority;
        String reason;
        double predictedDemand = demand > 0 ? demand * 1.05 : Math.max(1.0, orders * 1.2);

        if (stock <= 0 || orders > stock) {
            risk = "CRITICAL";
            algorithm = "PRIORITY";
            priority = 1;
            reason = "[FALLBACK] Critical stock shortage. Immediate priority dispatch required.";
        } else if (stock < (demand * 2) || queue > 6) {
            risk = "HIGH";
            algorithm = "SJF";
            priority = 2;
            reason = "[FALLBACK] Low stock buffer or high backlog. Prioritizing smaller orders first.";
        } else if (queue > 2) {
            risk = "MODERATE";
            algorithm = "ROUND ROBIN";
            priority = 3;
            reason = "[FALLBACK] Moderate queue load. Balanced time-slice robot distribution.";
        } else {
            risk = "LOW";
            algorithm = "FCFS";
            priority = 4;
            reason = "[FALLBACK] Standard queue. Processing orders in First-Come, First-Served order.";
        }

        String raw = "{\"fallback\": true, \"reason\": \"" + reason + "\", \"note\": \"" +
                (failureReason != null ? failureReason.replace("\"", "'") : "Offline") + "\"}";

        return new PredictionResult(
                predictedDemand,
                risk,
                algorithm,
                reason,
                priority,
                true,
                raw
        );
    }

    // =========================================================
    // 8. ZERO-DEPENDENCY REGEX JSON EXTRACTORS
    // =========================================================

    private static String extractString(String json, String key, String defaultValue) {
        try {
            Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
            Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }

    private static double extractDouble(String json, String key, double defaultValue) {
        try {
            Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
            Matcher m = p.matcher(json);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }
}