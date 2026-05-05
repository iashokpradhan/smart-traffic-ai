package com.smarttraffic.service;

import com.smarttraffic.model.VisionResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
public class GeminiVisionService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    public VisionResult detectVehicles(byte[] imageBytes, String mimeType, int fallbackCount) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Gemini API Key is missing. Using fallback.");
            return new VisionResult(fallbackCount, false, false);
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            JSONObject inlineData = new JSONObject();
            inlineData.put("mimeType", mimeType != null ? mimeType : "image/jpeg");
            inlineData.put("data", base64Image);

            JSONObject part1 = new JSONObject();
            part1.put("inlineData", inlineData);

            JSONObject part2 = new JSONObject();
            part2.put("text", "Analyze this traffic image. Respond ONLY with a valid JSON object strictly matching this format: {\"vehicleCount\": number, \"hasEmergency\": boolean, \"hasAccident\": boolean}. 'hasEmergency' is true if there is an ambulance, fire truck, or police car. 'hasAccident' is true if there is a crash, flipped car, or major debris. Do not include markdown formatting or backticks like ```json.");

            JSONArray parts = new JSONArray();
            parts.put(part1);
            parts.put(part2);

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String text = jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
                        
                try {
                    String cleanText = text.replace("```json", "").replace("```", "").trim();
                    JSONObject resultJson = new JSONObject(cleanText);
                    int count = resultJson.optInt("vehicleCount", fallbackCount);
                    boolean hasEmergency = resultJson.optBoolean("hasEmergency", false);
                    boolean hasAccident = resultJson.optBoolean("hasAccident", false);
                    return new VisionResult(count, hasEmergency, hasAccident);
                } catch (Exception e) {
                    System.err.println("Could not parse JSON from AI response: " + text);
                }
            } else {
                System.err.println("AI request failed: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Exception in GeminiVisionService: " + e.getMessage());
        }
        return new VisionResult(fallbackCount, false, false);
    }
}
