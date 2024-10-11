package com.menumaster.restaurant.transcription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1p1beta1.*;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.menumaster.restaurant.exception.type.GoogleCredentialsException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Log4j2
public class TranscriptionService {

    @Value("${gcp.bucket.id}")
    private String bucketName;

    @Value("${gcp.dir.audios.name}")
    private String gcpDirectoryName;

    private SpeechClient speechClient;

    private Storage storage;

    private final String geminiAudioUrl;
    private final String geminiUploadUrl;

    public TranscriptionService(@Value("${speech.client.config.file}") String speechClientConfigFilePath,
                                @Value("${gcp.config.file}") String gcpConfigFilePath,
                                @Value("${gemini.api.key}") String apiKey,
                                @Value("${gemini.audio.api.url}") String geminiAudioUrl,
                                @Value("${gemini.upload.api.url}") String geminiUploadUrl) {

        this.geminiAudioUrl = geminiAudioUrl + apiKey;
        this.geminiUploadUrl = geminiUploadUrl + apiKey;

        try {
            Credentials speechClientCredentials = ServiceAccountCredentials.fromStream(new FileInputStream(speechClientConfigFilePath));

            this.speechClient = SpeechClient.create(SpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> speechClientCredentials)
                    .build());
            this.storage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(gcpConfigFilePath)))
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new GoogleCredentialsException("Falha ao carregar as credenciais do google a partir do arquivo json: " + gcpConfigFilePath);
        }
    }

    public String uploadFileToGCS(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String fullPath = gcpDirectoryName + "/" + fileName;

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fullPath).build();
        storage.create(blobInfo, file.getBytes());

        return "gs://" + bucketName + "/" + fullPath;
    }

    public String transcribeAudio(String gcsUri) throws InterruptedException, ExecutionException {
        boolean exists = storage.get(bucketName, gcsUri.replace("gs://" + bucketName + "/", "")) != null;

        if (!exists) {
            throw new IllegalArgumentException("O arquivo de áudio não foi encontrado no Google Cloud Storage: " + gcsUri);
        }

        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.MP3)
                //.setSampleRateHertz(44100)
                .setSampleRateHertz(16000)
                .setLanguageCode("pt-BR")
                .setModel("default")
                .setEnableAutomaticPunctuation(true)
                .build();

        RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

        OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response = speechClient
                .longRunningRecognizeAsync(config, audio);

        while(!response.isDone()) {
            Thread.sleep(1000);
        }

        try {
            List<SpeechRecognitionResult> speechResults = response.get().getResultsList();

            StringBuilder transcription = new StringBuilder();
            for (SpeechRecognitionResult result : speechResults) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcription.append(alternative.getTranscript());

            }

            return transcription.toString();

        } catch (ExecutionException e) {
            throw new RuntimeException("Transcription failed", e);
        }
    }

    public String transcribeAudioWithGemini2(String audioUri) throws IOException, InterruptedException {
        String requestBody = String.format("""
                {
                    "contents": {
                        "role": "user",
                        "parts": [
                            {
                                "fileData": {
                                    "mimeType": "audio/mp3",
                                    "fileUri": "%s"
                                }
                            },
                            {
                                "text": "Transcribe this audio."
                            }
                        ]
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

        return response.body();
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
            e.printStackTrace();
            return "Erro ao enviar o arquivo: " + e.getMessage();
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
            e.printStackTrace();
            return "Erro ao processar o JSON.";
        }
    }
}
