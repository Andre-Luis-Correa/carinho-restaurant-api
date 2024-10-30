package com.menumaster.restaurant.aiassistant.controller;

import com.menumaster.restaurant.aiassistant.domain.dto.ChatMessageDTO;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatResponseDTO;
import com.menumaster.restaurant.gemini.GeminiService;
import com.menumaster.restaurant.transcription.TranscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

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
        log.info(intent);

        ChatResponseDTO chatResponseDTO = geminiService.processMessageBasedOnIntent(chatMessageDTO, intent);
        return ResponseEntity.ok().body(chatResponseDTO);
    }

    @PostMapping("/transcribe-audio")
    public ResponseEntity<String> transcribeAudioWithGemini(@RequestParam("file") MultipartFile file) throws IOException, InterruptedException {
        log.info("Fazendo upload de audio!");
        String fileUri = transcriptionService.uploadAudio(file);
        log.info("Nome do arquivo e bytes: " + file.getOriginalFilename());
        log.info("Iniciando a transcrição de audio!");
        String transcriptionResponse = transcriptionService.transcribeAudioWithGemini2(fileUri);
        log.info(transcriptionResponse);
        return ResponseEntity.ok().body(transcriptionResponse);
    }
}
