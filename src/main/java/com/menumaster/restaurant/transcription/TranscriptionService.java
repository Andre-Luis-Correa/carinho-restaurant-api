package com.menumaster.restaurant.transcription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.menumaster.restaurant.exception.type.AudioTranscriptionException;
import com.menumaster.restaurant.exception.type.ExtractingJsonDataException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class TranscriptionService {

    private final String geminiAudioUrl;
    private final String geminiUploadUrl;

    public TranscriptionService(@Value("${gemini.api.key}") String apiKey,
                                @Value("${gemini.audio.api.url}") String geminiAudioUrl,
                                @Value("${gemini.upload.api.url}") String geminiUploadUrl) {

        this.geminiAudioUrl = geminiAudioUrl + apiKey;
        this.geminiUploadUrl = geminiUploadUrl + apiKey;
    }

    public String transcribeAudioWithGemini2(String audioUri) throws IOException, InterruptedException {
        String requestBody = String.format("""
            {
                "contents": [
                    {
                        "role": "user",
                        "parts": [
                            {
                                "fileData": {
                                    "mimeType": "audio/mpeg",
                                    "fileUri": "%s"
                                }
                            },
                            {
                                "text": "Transcreva esse audio"
                            }
                        ]
                    }
                ],
                "generationConfig": {
                    "temperature": 1,
                    "topK": 64,
                    "topP": 0.95,
                    "maxOutputTokens": 8192,
                    "responseMimeType": "text/plain"
                }
            }
            """, audioUri);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(geminiAudioUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return extractTextFromJson(response.body());
    }

    public static String extractTextFromJson(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode rootNode = objectMapper.readTree(response);

            JsonNode textNode = rootNode
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");
            return textNode.asText();
        } catch (Exception e) {
            throw new ExtractingJsonDataException("text");
        }
    }

    public String uploadAudio(MultipartFile audioFile) {
        String mimeType = "audio/mpeg";
        long fileLength = audioFile.getSize();
        String fileName = audioFile.getOriginalFilename();

        try {
            URL url = new URL(geminiUploadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("X-Goog-Upload-Command", "start, upload, finalize");
            connection.setRequestProperty("X-Goog-Upload-Header-Content-Length", String.valueOf(fileLength));
            connection.setRequestProperty("X-Goog-Upload-Header-Content-Type", mimeType);
            connection.setRequestProperty("Content-Type", mimeType);

            String jsonMetadata = String.format("{\"file\": {\"display_name\": \"%s\"}}", fileName);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(jsonMetadata.getBytes());
                audioFile.getInputStream().transferTo(outputStream);
                outputStream.flush();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return extractFileUriFromJson(response.toString());

        } catch (Exception e) {
            throw new AudioTranscriptionException();
        }
    }

    public static String extractFileUriFromJson(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode rootNode = objectMapper.readTree(jsonString);

            JsonNode uriNode = rootNode
                    .path("file")
                    .path("uri");

            return uriNode.asText();
        } catch (Exception e) {
            throw new ExtractingJsonDataException("uri");
        }
    }

}