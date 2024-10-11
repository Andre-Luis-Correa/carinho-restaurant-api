package com.menumaster.restaurant.transcription;

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
import com.google.cloud.vertexai.api.PredictionServiceClient;
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
import java.net.URL;
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
                .setLanguageCode("pt-BR")
                .setModel("default")
                .setEnableAutomaticPunctuation(false)
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

    public String transcribe(String gcsUrl) {
        log.info(gcsUrl);

        // Obtém o arquivo de áudio do bucket do Google Cloud Storage
        Blob blob = storage.get(bucketName, gcsUrl.replace("gs://" + bucketName + "/", ""));
        if (blob == null) {
            System.err.println("Arquivo não encontrado no bucket: " + bucketName);
            return null;
        }

        // Converte o conteúdo do Blob em um InputStream
        try (InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(blob.getContent()))) {
            // Cria a configuração de reconhecimento
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.MP3)
                    .setSampleRateHertz(44100)
                    .setLanguageCode("pt-BR")
                    .setModel("default")
                    .setEnableAutomaticPunctuation(false)
                    .build();

            // Cria o objeto RecognitionAudio a partir do conteúdo do arquivo
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(ByteString.readFrom(inputStream))
                    .build();

            // Faz a transcrição do áudio usando o SpeechClient
            RecognizeResponse response = speechClient.recognize(config, audio);

            // Extrai e retorna a transcrição do áudio
            return response.getResultsList()
                    .stream()
                    .findFirst()
                    .flatMap(result -> result.getAlternativesList().stream().findFirst())
                    .map(SpeechRecognitionAlternative::getTranscript)
                    .orElse("Nenhuma transcrição encontrada.");
        } catch (IOException | ApiException e) {
            e.printStackTrace();
            System.err.println("Erro ao transcrever o áudio: " + e.getMessage());
        }
        return null;
    }
}
