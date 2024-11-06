package com.menumaster.restaurant.aiassistant.controller;

import com.menumaster.restaurant.aiassistant.domain.dto.ChatMessageDTO;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatResponseDTO;
import com.menumaster.restaurant.gemini.GeminiService;
import com.menumaster.restaurant.transcription.AudioDTO;
import com.menumaster.restaurant.transcription.TranscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Log4j2
@RestController
@RequestMapping("/ai-assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final GeminiService geminiService;
    private final TranscriptionService transcriptionService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> chatToAiAssistant(@Valid @RequestBody ChatMessageDTO chatMessageDTO) throws IOException, InterruptedException {
        String intent = geminiService.recognizeIntent(chatMessageDTO.userMessage());

        ChatResponseDTO chatResponseDTO = geminiService.processMessageBasedOnIntent(chatMessageDTO, intent);
        return ResponseEntity.ok().body(chatResponseDTO);
    }

    @PostMapping("/transcribe-audio")
    public ResponseEntity<String> transcribeAudioWithGemini(@RequestParam("file") MultipartFile file) throws IOException, InterruptedException {
        String fileUri = transcriptionService.uploadAudio(file);
        String transcriptionResponse = transcriptionService.transcribeAudioWithGemini2(fileUri);

        return ResponseEntity.ok().body(transcriptionResponse);
    }

    @PostMapping("/audio-to-base64")
    public ResponseEntity<String> convertAudioToBase64(@RequestParam("file") MultipartFile file) throws IOException, InterruptedException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Arquivo vazio.");
        }

        byte[] audioBytes = file.getBytes();
        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

        return ResponseEntity.ok(base64Audio);
    }

    @PostMapping("/fix-transcribe")
    public ResponseEntity<String> transcribeFix(@RequestBody AudioDTO audioDTO) throws IOException, InterruptedException {
        log.info("inciando transcrição");
        MultipartFile audio = transcriptionService.convertBase64ToMultipartFile(audioDTO.audioBase64());
        log.info("Transcrição bem sucedida");

        String fileUri = transcriptionService.uploadAudio(audio);
        log.info("trancrição bem sucedid: " + fileUri);
        String transcriptionResponse = transcriptionService.transcribeAudioWithGemini2(fileUri);
        log.info("Se chegou aqui, entrou no transcribeAudioWithGemini2");

        return ResponseEntity.ok().body(transcriptionResponse);
    }
}
