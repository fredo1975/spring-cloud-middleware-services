package fr.bluechipit.dvdtheque.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MCPControllerTest {
    @Mock
    ChatClient.Builder builder;

    @Mock
    ToolCallbackProvider toolCallbackProvider;

    @Mock
    ChatClient chatClient;

    @BeforeEach
    void setUp() {
        when(builder.defaultTools(toolCallbackProvider)).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
    }

    @Test
    void constructor_invokesBuilderDefaultToolsAndBuild() {
        new MCPController(builder, toolCallbackProvider);
        verify(builder).defaultTools(toolCallbackProvider);
        verify(builder).build();
        verifyNoMoreInteractions(builder);
    }

    @Test
    void chat_returnsGeneratedResponse_whenChatClientSucceeds() throws Exception {
        // Prépare une proxy dynamique pour simuler prompt().call().content() -> "réponse test"
        Method promptMethod = null;
        for (Method m : ChatClient.class.getMethods()) {
            if ("prompt".equals(m.getName()) && m.getParameterCount() == 1) {
                promptMethod = m;
                break;
            }
        }
        assertNotNull(promptMethod, "Méthode prompt introuvable sur ChatClient");

        Class<?> promptReturnType = promptMethod.getReturnType();

        Object promptProxy = Proxy.newProxyInstance(
                promptReturnType.getClassLoader(),
                new Class<?>[]{promptReturnType},
                (proxy, method, args) -> {
                    if ("call".equals(method.getName())) {
                        Class<?> callReturnType = method.getReturnType();
                        return Proxy.newProxyInstance(
                                callReturnType.getClassLoader(),
                                new Class<?>[]{callReturnType},
                                (p2, m2, a2) -> {
                                    if ("content".equals(m2.getName())) {
                                        return "réponse test";
                                    }
                                    return null;
                                }
                        );
                    }
                    return null;
                }
        );

        doReturn(promptProxy).when(chatClient).prompt(anyString());

        MCPController controller = new MCPController(builder, toolCallbackProvider);

        ResponseEntity<Map<String, String>> resp = controller.chat(Map.of(
                "message", "bonjour",
                "conversationId", "conv1"
        ));

        assertEquals(200, resp.getStatusCodeValue());
        Map<String, String> body = resp.getBody();
        assertNotNull(body);
        assertEquals("réponse test", body.get("response"));
        assertEquals("conv1", body.get("conversationId"));
    }

    @Test
    void chat_returnsErrorMessage_whenChatClientThrows() {
        doThrow(new RuntimeException("boom")).when(chatClient).prompt(anyString());

        MCPController controller = new MCPController(builder, toolCallbackProvider);

        ResponseEntity<Map<String, String>> resp = controller.chat(Map.of(
                "message", "bonjour",
                "conversationId", "conv2"
        ));

        assertEquals(200, resp.getStatusCodeValue());
        Map<String, String> body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.get("response").contains("Désolé, une erreur s'est produite"));
        assertEquals("conv2", body.get("conversationId"));
    }

    @Test
    void clearConversation_returnsConfirmationMessage() {
        MCPController controller = new MCPController(builder, toolCallbackProvider);

        ResponseEntity<Map<String, String>> resp = controller.clearConversation("conv3");

        assertEquals(200, resp.getStatusCodeValue());
        Map<String, String> body = resp.getBody();
        assertNotNull(body);
        assertEquals("Conversation effacée", body.get("message"));
        assertEquals("conv3", body.get("conversationId"));
    }
}
