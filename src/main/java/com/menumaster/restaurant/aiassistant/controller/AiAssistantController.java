package com.menumaster.restaurant.aiassistant.controller;

import com.menumaster.restaurant.aiassistant.domain.dto.ChatMessageDTO;
import com.menumaster.restaurant.aiassistant.domain.dto.ChatResponseDTO;
import com.menumaster.restaurant.gemini.GeminiService;
import com.menumaster.restaurant.transcription.TranscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

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
}
