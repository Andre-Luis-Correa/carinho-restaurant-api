package com.menumaster.restaurant.utils;

import com.menumaster.restaurant.exception.type.ImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Component
public class ImageUtil {

    private final String imagesPath;

    public ImageUtil(@Value("${dish.images.path}") String imagesPath) {
        this.imagesPath = imagesPath;
    }

    public MultipartFile convertBase64ToMultipartFile(String base64Image, String originalFileName) throws IOException {
        String[] parts = base64Image.split(",");
        String imageData = parts.length > 1 ? parts[1] : parts[0];

        byte[] imageBytes = Base64.getDecoder().decode(imageData);

        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        return new MockMultipartFile(uniqueFileName, uniqueFileName, "image/png", new ByteArrayInputStream(imageBytes));
    }

    public String encodeImageToBase64(String imageName) {
        try {
            File imageFile = new File(imagesPath + File.separator + imageName);
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            throw new ImageException("Erro ao codificar a imagem para base64: " + imageName, e);
        }
    }

    public void removeOldImage(String imageName) {
        if (imageName != null && !imageName.isBlank()) {
            try {
                Path imagePath = Paths.get(imagesPath + File.separator + imageName);
                Files.deleteIfExists(imagePath);
            } catch (IOException e) {
                throw new ImageException("Erro ao remover a imagem antiga do prato " + imageName, e);
            }
        }
    }
}