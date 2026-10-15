package fr.bluechipit.dvdtheque.batch;

import jakarta.jms.Topic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class BatchApplicationTest {

    private final BatchApplication application = new BatchApplication();

    @Test
    @DisplayName("topic bean targets the topic destination")
    void topic_ShouldReturnActiveMqTopic() throws Exception {
        Topic topic = application.topic();

        assertThat(topic.getTopicName()).isEqualTo("topic");
    }

    @Test
    @DisplayName("jacksonJmsMessageConverter returns a configured jackson converter")
    void jacksonJmsMessageConverter_ShouldReturnJacksonConverter() {
        MessageConverter converter = application.jacksonJmsMessageConverter();

        assertThat(converter).isInstanceOf(MappingJackson2MessageConverter.class);
    }
}
