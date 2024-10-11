package com.menumaster.restaurant.transcription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1p1beta1.*;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vertexai.Transport;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.PredictionServiceClient;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.common.base.Supplier;
import com.google.protobuf.ByteString;
import com.menumaster.restaurant.exception.type.GoogleCredentialsException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.threeten.bp.Duration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@Log4j2
public class TranscriptionService {

    @Value("${gcp.bucket.id}")
    private String bucketName;

    @Value("${gcp.dir.name2}")
    private String gcpDirectoryName;

    private SpeechClient speechClient;

    private Storage storage;
    private final String url;

    public TranscriptionService(@Value("${speech.client.config.file}") String speechClientConfigFilePath, @Value("${gcp.config.file}") String gcpConfigFilePath, @Value("${gemini.api.key}") String apiKey,
                                @Value("${audio.api.url}") String url) {
        this.url = url + apiKey;
        try {
            // Carregar as credenciais do arquivo JSON
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
        System.out.println("Transcribing audio from URI: " + gcsUri);

        boolean exists = storage.get(bucketName, gcsUri.replace("gs://" + bucketName + "/", "")) != null;
        if (!exists) {
            throw new IllegalArgumentException("O arquivo de áudio não foi encontrado no Google Cloud Storage: " + gcsUri);
        }

        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.MP3) // Configuração para MP3
                //.setSampleRateHertz(44100)
                .setSampleRateHertz(16000)
                .setLanguageCode("pt-BR")
                .setModel("default")
                //.setEnableAutomaticPunctuation(true)
                .build();

        RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

        System.out.println("Iniciando a transcrição do áudio...");
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
            log.error("Error while getting transcription results:", e);
            throw new RuntimeException("Transcription failed", e);
        }
    }

    public String transcribeAudioWithGemini(String audioUri) throws IOException {
        try (VertexAI vertexAI = new VertexAI("projeto-esi", "southamerica-east1")) {

            GenerativeModel model = new GenerativeModel("gemini-1.5-flash-002", vertexAI);
            GenerateContentResponse response = model.generateContent(
                    ContentMaker.fromMultiModalData(
                            "Transcribe the audio",
                            PartMaker.fromMimeTypeAndData("audio/mp3", audioUri)
                    ));

            String output = ResponseHandler.getText(response);
            System.out.println(output);

            return output;
        }
    }

    public String transcribeAudioWithGemini2(String audioUri) throws IOException, InterruptedException {
        String endpoint = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s", "AIzaSyBj7D_d69IKHIDnXEkUdcuZ-WCkm8dZxtU");

        // Criar o corpo da requisição JSON para transcrever o áudio
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

        // Montar a requisição HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Criar o cliente HTTP e enviar a requisição
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Retornar a resposta da API como uma String
        return response.body();
    }

    public String uploadAudio(MultipartFile audioFile) {
        String apiKey = "AIzaSyBj7D_d69IKHIDnXEkUdcuZ-WCkm8dZxtU";
        String mimeType = "audio/mpeg"; // Ajuste para o tipo MIME correto para arquivos MP3
        long fileLength = audioFile.getSize();
        String fileName = audioFile.getOriginalFilename();

        try {
            // Definir a URL de upload
            URL url = new URL("https://generativelanguage.googleapis.com/upload/v1beta/files?key=" + apiKey);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("X-Goog-Upload-Command", "start, upload, finalize");
            connection.setRequestProperty("X-Goog-Upload-Header-Content-Length", String.valueOf(fileLength));
            connection.setRequestProperty("X-Goog-Upload-Header-Content-Type", mimeType); // Usando o tipo MIME correto
            connection.setRequestProperty("Content-Type", mimeType); // Usando o tipo MIME correto

            // Escrever os dados do arquivo no corpo da requisição
            String jsonMetadata = String.format("{\"file\": {\"display_name\": \"%s\"}}", fileName);

            try (OutputStream outputStream = connection.getOutputStream()) {
                // Escrever os metadados do arquivo
                outputStream.write(jsonMetadata.getBytes());

                // Escrever o arquivo binário no corpo da requisição
                audioFile.getInputStream().transferTo(outputStream);
                outputStream.flush();
            }

            // Ler a resposta do servidor
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
            // Cria um ObjectMapper para manipular o JSON
            ObjectMapper objectMapper = new ObjectMapper();

            // Converte a string JSON para um objeto JsonNode
            JsonNode rootNode = objectMapper.readTree(jsonString);

            // Navega até o campo "uri" dentro da estrutura do JSON
            JsonNode uriNode = rootNode
                    .path("file")
                    .path("uri");

            // Retorna o URI extraído ou uma mensagem se não encontrado
            return uriNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao processar o JSON.";
        }
    }
}
