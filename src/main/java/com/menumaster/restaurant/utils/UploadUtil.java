package com.menumaster.restaurant.utils;

import com.menumaster.restaurant.exception.type.UploadImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

@Component
public class UploadUtil {

//    private final String imagesPath;
//
//    public UploadUtil(@Value("${dish.images.path}") String imagesPath) {
//        this.imagesPath = imagesPath;
//    }
//
//    public boolean makeImageUpload(MultipartFile image) {
//        boolean successfulUpload = false;
//
//        if(!image.isEmpty()) {
//            String fileName = image.getOriginalFilename();
//
//            try {
//                File dir = new File(imagesPath);
//
//                if(!dir.exists()) {
//                    dir.mkdirs();
//                }
//
//                File uploadedImage = new File(dir.getAbsolutePath() + File.separator + fileName);
//                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(uploadedImage));
//                bufferedOutputStream.write(image.getBytes());
//                bufferedOutputStream.close();
//                successfulUpload = true;
//            } catch (Exception e) {
//                throw new UploadImageException(imagesPath, fileName);
//            }
//        } else {
//            throw new UploadImageException("Falha ao realizar upload de imagem, pois ela está vazia");
//        }
//
//        return successfulUpload;
//    }
}
