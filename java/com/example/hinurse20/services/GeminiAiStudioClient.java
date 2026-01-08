package com.example.hinurse20.services;

import android.text.TextUtils;

import com.example.hinurse20.models.SymptomAnalysis;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.Timer;
import java.util.TimerTask;

public class GeminiAiStudioClient {
    public interface CallbackResult {
        void onSuccess(SymptomAnalysis analysis);
        void onError(Exception e);
        void onInfo(String message);
    }

    // Retries for 429/500/503 with backoff and model failover
    private void sendWithRetry(String apiKey, String bodyV1Str, String bodyVbetaStr, String[] urls, int urlIndex, int attempt, Callback finalCallback) {
        if (urlIndex >= urls.length) {
            finalCallback.onFailure(null, new IOException("No available model endpoints"));
            return;
        }
        String url = urls[urlIndex];
        android.util.Log.d("GeminiAiStudioClient", "Calling: " + url + " (attempt=" + (attempt+1) + ")");
        String bodyToSend = url.contains("/v1beta/") ? bodyVbetaStr : bodyV1Str;
        Request req = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", apiKey)
                .post(RequestBody.create(bodyToSend, JSON))
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (attempt < 2) { // retry same URL
                    if (finalCallback instanceof okhttp3.Callback) {}
                    schedule(() -> sendWithRetry(apiKey, bodyV1Str, bodyVbetaStr, urls, urlIndex, attempt + 1, finalCallback), (long) (500 * Math.pow(2, attempt)));
                } else if (urlIndex + 1 < urls.length) { // switch model
                    schedule(() -> sendWithRetry(apiKey, bodyV1Str, bodyVbetaStr, urls, urlIndex + 1, 0, finalCallback), 200);
                } else {
                    finalCallback.onFailure(call, e);
                }
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String res = response.body() != null ? response.body().string() : "";
                int code = response.code();
                if (code == 404 && urlIndex + 1 < urls.length) {
                    // Switch API version (e.g., v1 -> v1beta) for the same model on 404
                    schedule(() -> sendWithRetry(apiKey, bodyV1Str, bodyVbetaStr, urls, urlIndex + 1, 0, finalCallback), 50);
                    return;
                }
                if (code == 429 || code == 500 || code == 503) {
                    try {
                        // Inform UI about transient retry
                        if (finalCallback instanceof com.example.hinurse20.services.GeminiAiStudioClient.InternalUiBridge) {
                            ((InternalUiBridge) finalCallback).info("Model busy, retrying...");
                        }
                    } catch (Throwable ignored) {}
                    if (attempt < 2) {
                        schedule(() -> sendWithRetry(apiKey, bodyV1Str, bodyVbetaStr, urls, urlIndex, attempt + 1, finalCallback), (long) (500 * Math.pow(2, attempt)));
                    } else if (urlIndex + 1 < urls.length) {
                        schedule(() -> sendWithRetry(apiKey, bodyV1Str, bodyVbetaStr, urls, urlIndex + 1, 0, finalCallback), 200);
                    } else {
                        android.util.Log.e("GeminiAiStudioClient", "HTTP " + code + ": " + res);
                        finalCallback.onResponse(call, response.newBuilder().body(okhttp3.ResponseBody.create(res, JSON)).build());
                    }
                    return;
                }
                // For non-retryable responses, pass through to final handler
                response = response.newBuilder().body(okhttp3.ResponseBody.create(res, JSON)).build();
                finalCallback.onResponse(call, response);
            }
        });
    }

    // Bridge callback used to propagate info messages back to UI without changing OkHttp's Callback signature in sendWithRetry
    public static abstract class InternalUiBridge implements Callback {
        public abstract void info(String msg);
    }

    private void schedule(Runnable r, long delayMs) {
        new Timer().schedule(new TimerTask() { @Override public void run() { r.run(); } }, delayMs);
    }

    private void logAvailableModels(String apiKey) {
        try {
            String[] urls = new String[] {
                    "https://generativelanguage.googleapis.com/v1/models",
                    "https://generativelanguage.googleapis.com/v1beta/models"
            };
            for (String u : urls) {
                Request r = new Request.Builder()
                        .url(u)
                        .addHeader("x-goog-api-key", apiKey)
                        .build();
                client.newCall(r).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        android.util.Log.w("GeminiAiStudioClient", "ListModels failed for " + u + ": " + e.getMessage());
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        String res = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            android.util.Log.w("GeminiAiStudioClient", "ListModels HTTP " + response.code() + " for " + u + ": " + res);
                            return;
                        }
                        try {
                            JsonObject root = JsonParser.parseString(res).getAsJsonObject();
                            if (root.has("models")) {
                                root.getAsJsonArray("models").forEach(el -> {
                                    JsonObject m = el.getAsJsonObject();
                                    String name = m.has("name") ? m.get("name").getAsString() : "";
                                    String methods = m.has("supportedGenerationMethods") ? m.get("supportedGenerationMethods").toString() : "[]";
                                    android.util.Log.d("GeminiAiStudioClient", "Model: " + name + " methods=" + methods);
                                });
                            } else {
                                android.util.Log.d("GeminiAiStudioClient", "ListModels response (" + u + "): " + res);
                            }
                        } catch (Exception ex) {
                            android.util.Log.w("GeminiAiStudioClient", "Failed to parse ListModels for " + u + ": " + ex.getMessage());
                        }
                    }
                });
            }
        } catch (Exception ignore) { }
    }

    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public void assess(String apiKey, Integer age, String sex, List<String> symptoms, String onset,
                       Map<String, Object> vitals, List<String> meds, List<String> conditions, String notes,
                       CallbackResult cb) {
        if (apiKey == null || apiKey.isEmpty()) {
            cb.onError(new IllegalArgumentException("Missing API key"));
            return;
        }
        // Log available models in the background for diagnostics
        logAvailableModels(apiKey);
        String primaryUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent";
        String primaryUrlVbeta = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

        String symptomsJoined = symptoms != null ? TextUtils.join(", ", symptoms) : "";
        String prompt = "You are a clinical triage assistant. Do not diagnose; provide triage assessment and safety-first recommendations. " +
                "If any red flags are present, set triage_level to \"emergency\" and list them.\n\n" +
                "Return STRICT JSON with:\n{" +
                "\"assessment\": string,\n" +
                "\"triage_level\": \"emergency\" | \"urgent\" | \"non_urgent\" | \"self_care\",\n" +
                "\"risk_level\": \"high\" | \"medium\" | \"low\",\n" +
                "\"recommendations\": string[],\n" +
                "\"red_flags\": string[],\n" +
                "\"disclaimer\": string\n}\n\n" +
                "Patient:\n" +
                "- Age: " + (age != null ? age : "-") + "\n" +
                "- Sex: " + (sex != null ? sex : "-") + "\n" +
                "- Symptoms: " + symptomsJoined + "\n" +
                "- Onset: " + (onset != null ? onset : "-") + "\n" +
                "- Vitals: " + (vitals != null ? new Gson().toJson(vitals) : "{}") + "\n" +
                "- Medications: " + (meds != null ? TextUtils.join(", ", meds) : "-") + "\n" +
                "- Conditions: " + (conditions != null ? TextUtils.join(", ", conditions) : "-") + "\n" +
                "- Notes: " + (notes != null ? notes : "-");

        // Build contents once (v1 expects a role)
        JsonObject contentsPart = new JsonObject();
        contentsPart.addProperty("text", prompt);
        JsonObject contents = new JsonObject();
        contents.addProperty("role", "user");
        contents.add("parts", new com.google.gson.JsonArray());
        contents.getAsJsonArray("parts").add(contentsPart);

        // Body for v1 (snake_case in generationConfig). Do NOT set response_mime_type (not supported in v1).
        JsonObject genConfigV1 = new JsonObject();
        genConfigV1.addProperty("temperature", 0.2f);
        genConfigV1.addProperty("max_output_tokens", 512);
        JsonObject bodyV1 = new JsonObject();
        bodyV1.add("contents", new com.google.gson.JsonArray());
        bodyV1.getAsJsonArray("contents").add(contents);
        bodyV1.add("generationConfig", genConfigV1);

        // Body for v1beta (camelCase fields)
        JsonObject genConfigVbeta = new JsonObject();
        genConfigVbeta.addProperty("temperature", 0.2f);
        genConfigVbeta.addProperty("maxOutputTokens", 512);
        genConfigVbeta.addProperty("responseMimeType", "application/json");
        JsonObject bodyVbeta = new JsonObject();
        bodyVbeta.add("contents", new com.google.gson.JsonArray());
        bodyVbeta.getAsJsonArray("contents").add(contents);
        bodyVbeta.add("generationConfig", genConfigVbeta);

        // Kick off request with retries (same model across API versions)
        sendWithRetry(apiKey, bodyV1.toString(), bodyVbeta.toString(), new String[]{primaryUrl, primaryUrlVbeta}, 0, 0, new Callback() {
            @Override public void onFailure(Call call, IOException e) { cb.onError(e); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String res = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    android.util.Log.e("GeminiAiStudioClient", "HTTP " + response.code() + ": " + res);
                    cb.onError(new IOException("HTTP " + response.code() + ": " + res));
                    return;
                }
                try {
                    JsonObject root = JsonParser.parseString(res).getAsJsonObject();
                    if (!root.has("candidates") || root.getAsJsonArray("candidates").size() == 0) {
                        throw new IOException("No candidates in response");
                    }
                    JsonObject cand = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
                    if (!cand.has("content") || !cand.getAsJsonObject("content").has("parts") ||
                            cand.getAsJsonObject("content").getAsJsonArray("parts").size() == 0) {
                        throw new IOException("No content parts in response");
                    }
                    String text = cand.getAsJsonObject("content").getAsJsonArray("parts")
                            .get(0).getAsJsonObject().get("text").getAsString();

                    String jsonStr = text != null ? text.trim() : "";
                    if (jsonStr.startsWith("```")) {
                        int firstBrace = jsonStr.indexOf('{');
                        int lastBrace = jsonStr.lastIndexOf('}');
                        if (firstBrace >= 0 && lastBrace > firstBrace) {
                            jsonStr = jsonStr.substring(firstBrace, lastBrace + 1);
                        }
                    }
                    if (!jsonStr.startsWith("{")) {
                        int firstBrace = jsonStr.indexOf('{');
                        int lastBrace = jsonStr.lastIndexOf('}');
                        if (firstBrace >= 0 && lastBrace > firstBrace) {
                            jsonStr = jsonStr.substring(firstBrace, lastBrace + 1);
                        }
                    }
                    JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
                    SymptomAnalysis analysis = new SymptomAnalysis();
                    analysis.setSymptoms(symptoms);
                    analysis.setSeverity(mapRiskToSeverity(obj.has("risk_level") ? obj.get("risk_level").getAsString() : "medium"));
                    String triageLevel = obj.has("triage_level") ? obj.get("triage_level").getAsString() : "non_urgent";
                    analysis.setUrgency(mapTriageToUrgency(triageLevel));
                    analysis.setRecommendation(mapTriageToRecommendation(triageLevel));
                    analysis.setRequiresAppointment(!"self_care".equals(analysis.getRecommendation()));
                    if (obj.has("recommendations")) {
                        java.util.ArrayList<String> recs = new java.util.ArrayList<>();
                        obj.getAsJsonArray("recommendations").forEach(e -> recs.add(e.getAsString()));
                        analysis.setSelfCareTips(TextUtils.join("\n• ", recs));
                    }
                    analysis.setSuggestedTimeframe(getSuggestedTimeframe(analysis.getUrgency()));
                    cb.onSuccess(analysis);
                } catch (Exception ex) { cb.onError(ex); }
            }
        });
    }

    private String mapRiskToSeverity(String risk) {
        if (risk == null) return "moderate";
        switch (risk.toLowerCase()) {
            case "high": return "urgent";
            case "medium": return "moderate";
            default: return "low";
        }
    }

    private String mapTriageToUrgency(String triage) {
        if (triage == null) return "within_week";
        switch (triage.toLowerCase()) {
            case "emergency": return "immediate";
            case "urgent": return "within_24h";
            case "non_urgent": return "within_week";
            default: return "routine";
        }
    }

    private String mapTriageToRecommendation(String triage) {
        if (triage == null) return "self_care";
        switch (triage.toLowerCase()) {
            case "emergency": return "emergency";
            case "urgent": return "urgent_care";
            case "non_urgent": return "schedule_appointment";
            default: return "self_care";
        }
    }

    private String getSuggestedTimeframe(String urgency) {
        if (urgency == null) return "Routine";
        switch (urgency) {
            case "immediate": return "Immediately";
            case "within_24h": return "Within 24 hours";
            case "within_week": return "Within a week";
            default: return "Routine care";
        }
    }
}
