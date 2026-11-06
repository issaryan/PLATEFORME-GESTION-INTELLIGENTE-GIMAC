package cm.gimac.des.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.IntegrationTest;
import cm.gimac.des.gateway.security.EmetteurJwtInterne;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * D-134 : émetteur du jeton interne réduit actif seul (gimac.securite.jeton-interne.actif), sans le mode jeton fantôme
 * des routes de la plateforme ; ses clés publiques sont lues sans jeton par les services de la sandbox.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
@TestPropertySource(properties = { "gimac.securite.jeton-interne.actif=true", "gimac.securite.jeton-fantome.actif=false" })
class JwksPasserelleIT {

    @Autowired
    private WebTestClient client;

    @Autowired
    private ObjectProvider<EmetteurJwtInterne> emetteur;

    @Autowired
    private ObjectProvider<cm.gimac.des.gateway.security.PhantomTokenAuthenticationFilter> fantome;

    @Test
    @SuppressWarnings("unchecked")
    void clesPubliquesSansJetonEtModeFantomeInactif() {
        assertThat(emetteur.getIfAvailable()).isNotNull();
        assertThat(fantome.getIfAvailable()).isNull();
        Map<String, Object> jwks = client.get().uri("/api/internal/jwks").exchange().expectStatus().isOk().expectBody(Map.class).returnResult().getResponseBody();
        List<Map<String, Object>> cles = (List<Map<String, Object>>) jwks.get("keys");
        assertThat(cles).isNotEmpty();
        assertThat(cles.get(0)).containsEntry("kid", emetteur.getObject().kid()).containsEntry("kty", "RSA").doesNotContainKey("d");
    }
}
