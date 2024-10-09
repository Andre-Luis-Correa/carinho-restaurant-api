package com.menumaster.restaurant.utils;

import com.menumaster.restaurant.exception.type.UploadImageException;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class UploadUtil {

    public static boolean makeImageUpload(MultipartFile image) {
        boolean successfulUpload = false;

        if(!image.isEmpty()) {
            String fileName = image.getOriginalFilename();
            String uploadFolderPath = "C:\\Users\\andre\\IdeaProjects\\restaurant\\src\\main\\resources\\images\\dishimages";
            try {
                File dir = new File(uploadFolderPath);

                if(!dir.exists()) {
                    dir.mkdirs();
                }

                File uploadedImage = new File(dir.getAbsolutePath() + File.separator + fileName);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(uploadedImage));
                bufferedOutputStream.write(image.getBytes());
                bufferedOutputStream.close();
                successfulUpload = true;
            } catch (Exception e) {
                throw new UploadImageException(uploadFolderPath, fileName);
            }
        } else {
            throw new UploadImageException("Falha ao realizar upload de imagem, pois ela está vazia");
        }

        return successfulUpload;
    }
}
