package com.menumaster.restaurant.image;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.menumaster.restaurant.exception.type.GoogleCredentialsException;
import com.menumaster.restaurant.exception.type.ImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Service
public class ImageUploadService {


    @Value("${gcp.bucket.id}")
    private String bucketName;

    @Value("${gcp.dir.name}")
    private String gcpDirectoryName;

    private Storage storage;

    public ImageUploadService(@Value("${gcp.config.file}") String gcpConfigFilePath) {
        try {
            this.storage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(gcpConfigFilePath)))
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new GoogleCredentialsException("Falha ao carregar as credenciais do google a partir do arquivo json: " + gcpConfigFilePath);
        }
    }

    public String uploadImage(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String uniqueFileName = gcpDirectoryName + "/" + UUID.randomUUID() + "-" + fileName;

        BlobId blobId = BlobId.of(bucketName, uniqueFileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        storage.create(blobInfo, file.getBytes());

        return uniqueFileName;
    }

    public String encodeImageToBase64(String imageName) {
        Blob blob = storage.get(BlobId.of(bucketName, imageName));
        if (blob == null || !blob.exists()) {
            throw new ImageException("Imagem não encontrada no bucket: " + imageName);
        }

        byte[] fileContent = blob.getContent();
        String mimeType = blob.getContentType();

        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(fileContent);
    }

    public MultipartFile convertBase64ToMultipartFile(String base64Image, String originalFileName) throws IOException {
        String[] parts = base64Image.split(",");
        String imageData = parts.length > 1 ? parts[1] : parts[0];

        byte[] imageBytes = Base64.getDecoder().decode(imageData);

        String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;

        return new MockMultipartFile(uniqueFileName, uniqueFileName, "image/png", new ByteArrayInputStream(imageBytes));
    }

    public void removeOldImage(String imageName) {
        if (imageName != null && !imageName.isBlank()) {
            try {
                BlobId blobId = BlobId.of(bucketName, imageName);

                boolean deleted = storage.delete(blobId);

                if (!deleted) {
                    throw new ImageException("Failed to delete the old image: " + imageName + " from the bucket.");
                }
            } catch (StorageException e) {
                throw new ImageException("Erro ao remover a imagem antiga do prato " + imageName, e);
            }
        }
    }
}
