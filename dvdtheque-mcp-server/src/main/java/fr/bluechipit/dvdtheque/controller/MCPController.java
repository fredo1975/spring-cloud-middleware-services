package fr.bluechipit.dvdtheque.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/mcp")
public class MCPController {
    private final ChatClient chatClient;
    private final InMemoryChatMemory chatMemory = new InMemoryChatMemory();

    public MCPController(ChatClient.Builder chat, ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chat.defaultTools(toolCallbackProvider)
                .build();
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        String conversationId = request.getOrDefault("conversationId", "default");

        log.info("Traitement du message: {}", userMessage);
        PromptTemplate pt = new PromptTemplate(userMessage);
        try {
            String response = this.chatClient.prompt(pt.create())
                    .call()
                    .content();

            log.info("Réponse générée: {}", response);

            return ResponseEntity.ok(Map.of(
                    "response", response,
                    "conversationId", conversationId
            ));
        } catch (Exception e) {
            log.error("Erreur lors du traitement du message", e);
            return ResponseEntity.ok(Map.of(
                    "response", "Désolé, une erreur s'est produite: " + e.getMessage(),
                    "conversationId", conversationId
            ));
        }
    }


    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Map<String, String>> clearConversation(@PathVariable String conversationId) {
        chatMemory.clear(conversationId);
        return ResponseEntity.ok(Map.of(
                "message", "Conversation effacée",
                "conversationId", conversationId
        ));
    }
}
