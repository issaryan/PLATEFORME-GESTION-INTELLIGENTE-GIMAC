package cm.gimac.des.notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@ConditionalOnProperty(name = "gimac.notification.websocket.actif", havingValue = "true")
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final org.springframework.core.env.Environment env;

    public WebSocketConfig(JwtDecoder jwtDecoder, org.springframework.core.env.Environment env) {
        this.jwtDecoder = jwtDecoder;
        this.env = env;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        String rabbitHost = env.getProperty("RABBITMQ_HOST", System.getenv("RABBITMQ_HOST") != null ? System.getenv("RABBITMQ_HOST") : "");
        if (!rabbitHost.isEmpty()) {
            String rabbitPass = System.getenv("RABBITMQ_WS_PASS") != null ? System.getenv("RABBITMQ_WS_PASS") : "ws_password";
            config.enableStompBrokerRelay("/queue", "/topic")
                  .setRelayHost(rabbitHost)
                  .setRelayPort(61613)
                  .setClientLogin("ws_user")
                  .setClientPasscode(rabbitPass);
        } else {
            config.enableSimpleBroker("/queue", "/topic");
        }
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
               .setAllowedOriginPatterns("*")
               .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor
                    .getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        try {
                            Jwt jwt = jwtDecoder.decode(authHeader.substring(7));
                            String username = jwt.getClaimAsString("preferred_username");
                            if (username == null || username.isBlank()) {
                                username = jwt.getSubject();
                            }
                            final String principalName = username;
                            // Force le principal sur l'accessor mutable puis reconstruit le message
                            // pour garantir la propagation au SimpUserRegistry
                            accessor.setLeaveMutable(true);
                            accessor.setUser(() -> principalName);
                            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
                        } catch (Exception e) {
                            // Token invalide — connexion anonyme tolérée (permettAll /ws/**)
                        }
                    }
                }
                return message;
            }
        });
    }
}
