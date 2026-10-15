package fr.bluechipit.dvdtheque.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("DvdthequeWebSocketConfiguration - Unit Tests")
class DvdthequeWebSocketConfigurationTest {

    private DvdthequeWebSocketConfiguration config;

    @BeforeEach
    void setUp() {
        config = new DvdthequeWebSocketConfiguration();
        ReflectionTestUtils.setField(config, "stompRelayHost", "localhost");
        ReflectionTestUtils.setField(config, "stompRelayPort", "61613");
        ReflectionTestUtils.setField(config, "activemqUser", "user");
        ReflectionTestUtils.setField(config, "activemqPasswd", "pwd");
        ReflectionTestUtils.setField(config, "stompEndpoint", "/dvdtheque-ws");
    }

    @Test
    @DisplayName("la configuration est active sur les profils attendus")
    void configuration_ShouldDeclareExpectedProfiles() {
        Profile profile = DvdthequeWebSocketConfiguration.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactlyInAnyOrder(
                "local1", "local2", "dev1", "dev2", "prod1", "prod2", "docker");
    }

    @Test
    @DisplayName("configureMessageBroker doit configurer le relais STOMP")
    void configureMessageBroker_ShouldConfigureRelay() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        StompBrokerRelayRegistration relay = mock(StompBrokerRelayRegistration.class);
        given(registry.enableStompBrokerRelay("/queue/", "/topic/")).willReturn(relay);
        given(relay.setRelayHost(anyString())).willReturn(relay);
        given(relay.setRelayPort(anyInt())).willReturn(relay);
        given(relay.setClientLogin(anyString())).willReturn(relay);
        given(relay.setClientPasscode(anyString())).willReturn(relay);

        config.configureMessageBroker(registry);

        verify(registry).enableStompBrokerRelay("/queue/", "/topic/");
        verify(relay).setRelayHost("localhost");
        verify(relay).setRelayPort(61613);
        verify(relay).setClientLogin("user");
        verify(relay).setClientPasscode("pwd");
        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).setPreservePublishOrder(true);
    }

    @Test
    @DisplayName("registerStompEndpoints doit enregistrer l'endpoint SockJS")
    void registerStompEndpoints_ShouldRegisterEndpoint() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration sockJs = mock(SockJsServiceRegistration.class);
        given(registry.addEndpoint("/dvdtheque-ws")).willReturn(registration);
        given(registration.setAllowedOriginPatterns("*")).willReturn(registration);
        given(registration.withSockJS()).willReturn(sockJs);
        given(sockJs.setWebSocketEnabled(true)).willReturn(sockJs);

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/dvdtheque-ws");
        verify(registration).setAllowedOriginPatterns("*");
        verify(registration).withSockJS();
        verify(sockJs).setWebSocketEnabled(true);
    }

    @Test
    @DisplayName("configureMessageConverters doit ajouter un JacksonJsonMessageConverter")
    void configureMessageConverters_ShouldAddJacksonConverter() {
        List<MessageConverter> converters = new ArrayList<>();

        boolean result = config.configureMessageConverters(converters);

        assertThat(result).isTrue();
        assertThat(converters).hasSize(1).first().isInstanceOf(JacksonJsonMessageConverter.class);
    }
}
