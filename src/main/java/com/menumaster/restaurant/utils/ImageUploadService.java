package com.menumaster.restaurant.utils;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.menumaster.restaurant.exception.type.ImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
            // Load credentials from the service account JSON file
            this.storage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(gcpConfigFilePath)))
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Google Cloud Storage credentials from JSON file: " + gcpConfigFilePath, e);
        }
    }

    public String uploadImage(MultipartFile file) throws IOException {
        // Generate a unique file name
        String fileName = file.getOriginalFilename();
        String uniqueFileName = gcpDirectoryName + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        // Create BlobId and BlobInfo for the image file to upload
        BlobId blobId = BlobId.of(bucketName, uniqueFileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        // Upload the image file to the bucket
        storage.create(blobInfo, file.getBytes());

        return uniqueFileName;
    }

    public String encodeImageToBase64(String imageName) {
        // Retrieve the image from the bucket using the image name
        Blob blob = storage.get(BlobId.of(bucketName, imageName));
        if (blob == null || !blob.exists()) {
            throw new ImageException("Imagem não encontrada no bucket: " + imageName);
        }

        // Get the image file content as a byte array
        byte[] fileContent = blob.getContent();
        String mimeType = blob.getContentType();

        // Encode the image content to Base64 and return as a data URL
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(fileContent);
    }
}
