package fr.bluechipit.dvdtheque.batch.configuration;

import enums.JmsStatus;
import jakarta.jms.Topic;
import jms.model.JmsStatusMessage;
import model.Film;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JmsMessageSenderTest {

    @Test
    @DisplayName("sendMessage forwards the status message to the topic")
    void sendMessage_ShouldSendToTopic() {
        JmsTemplate jmsTemplate = mock(JmsTemplate.class);
        Topic topic = mock(Topic.class);
        JmsMessageSender sender = new JmsMessageSender();
        sender.setJmsTemplate(jmsTemplate);
        sender.setTopic(topic);
        JmsStatusMessage<Film> message = new JmsStatusMessage<>(JmsStatus.IMPORT_INIT, null, 0L, 0);

        sender.sendMessage(message);

        verify(jmsTemplate).convertAndSend(any(jakarta.jms.Destination.class), any(Object.class));
    }
}
