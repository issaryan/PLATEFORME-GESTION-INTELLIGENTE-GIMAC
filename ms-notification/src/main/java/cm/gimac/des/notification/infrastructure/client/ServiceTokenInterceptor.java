package cm.gimac.des.notification.infrastructure.client;

import cm.gimac.des.notification.infrastructure.security.CorrelationContext;
import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

/** Ajoute le jeton de service et l'identifiant de corrélation aux appels internes (PI §0.7). */
@Component
public class ServiceTokenInterceptor implements ClientHttpRequestInterceptor {

    static final String REGISTRATION = "service-token";

    private final OAuth2AuthorizedClientManager manager;

    public ServiceTokenInterceptor(OAuth2AuthorizedClientManager manager) {
        this.manager = manager;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest req, byte[] body, ClientHttpRequestExecution exec) throws IOException {
        OAuth2AuthorizedClient client = manager.authorize(OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION).principal("service").build());
        if (client == null) {
            throw new IllegalStateException("Jeton de service indisponible (client " + REGISTRATION + ")");
        }
        req.getHeaders().setBearerAuth(client.getAccessToken().getTokenValue());
        req.getHeaders().set(CorrelationContext.HEADER, CorrelationContext.current());
        return exec.execute(req, body);
    }
}
