package cm.gimac.des.collaboration.web.rest.messagerie;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.collaboration.application.port.in.ConsulterConversationInterneUseCase;
import cm.gimac.des.collaboration.application.port.in.ConsulterConversationInterneUseCase.ConversationInterne;
import cm.gimac.des.collaboration.config.SecurityConfiguration;
import cm.gimac.des.collaboration.config.TestSecurityConfiguration;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.web.rest.errors.CollaborationExceptionHandler;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Route interne GET /api/v1/interne/conversations/{id} (D-80) avec la vraie chaîne de filtres : jeton de
 * service portant INTERNAL_COLLABORATION_READ seulement ; forme { id, type, nom, membres } ; 404 inconnue.
 */
@WebMvcTest(controllers = ConversationInterneResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, CollaborationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class ConversationInterneResourceTest {

    static final UUID GROUPE = UUID.randomUUID();
    static final UUID DIRECTE = UUID.randomUUID();
    static final UUID INCONNUE = UUID.randomUUID();

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ConsulterConversationInterneUseCase conversations;

    @BeforeEach
    void init() {
        when(conversations.conversationInterne(GROUPE)).thenReturn(new ConversationInterne(GROUPE, "GROUPE", "Groupe Cadrage L-12", List.of("paul", "aminata")));
        when(conversations.conversationInterne(DIRECTE)).thenReturn(new ConversationInterne(DIRECTE, "DIRECTE", null, List.of("paul", "serge")));
        when(conversations.conversationInterne(INCONNUE)).thenThrow(CollaborationException.introuvable("COL_CONVERSATION_NOT_FOUND", INCONNUE));
    }

    static RequestPostProcessor service(String... permissions) {
        return jwt()
            .jwt(j -> j.subject("sa-notification").claim("azp", "notification-service").claim("preferred_username", "service-account-notification-service"))
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @Test
    @DisplayName("401 sans jeton ; 403 sans INTERNAL_COLLABORATION_READ (jeton utilisateur à deux facteurs compris)")
    void securite() throws Exception {
        String uri = "/api/v1/interne/conversations/" + GROUPE;
        mvc.perform(get(uri)).andExpect(status().isUnauthorized());
        mvc.perform(get(uri).with(service("INTERNAL_IAM_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(get(uri).with(jwt().jwt(j -> j.subject("paul").claim("acr", "2")).authorities(new SimpleGrantedAuthority("COL_MESSAGE_READ"))))
            .andExpect(status().isForbidden());
        mvc.perform(get(uri).with(service("INTERNAL_COLLABORATION_READ"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Forme : groupe nommé, directe sans nom, membres en identifiants ; inconnue : 404 COL_CONVERSATION_NOT_FOUND")
    void formes() throws Exception {
        mvc
            .perform(get("/api/v1/interne/conversations/" + GROUPE).with(service("INTERNAL_COLLABORATION_READ")))
            .andExpect(jsonPath("$.id").value(GROUPE.toString()))
            .andExpect(jsonPath("$.type").value("GROUPE"))
            .andExpect(jsonPath("$.nom").value("Groupe Cadrage L-12"))
            .andExpect(jsonPath("$.membres.length()").value(2))
            .andExpect(jsonPath("$.membres[0]").value("paul"));
        mvc
            .perform(get("/api/v1/interne/conversations/" + DIRECTE).with(service("INTERNAL_COLLABORATION_READ")))
            .andExpect(jsonPath("$.type").value("DIRECTE"))
            .andExpect(jsonPath("$.nom").doesNotExist());
        mvc
            .perform(get("/api/v1/interne/conversations/" + INCONNUE).with(service("INTERNAL_COLLABORATION_READ")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COL_CONVERSATION_NOT_FOUND"));
    }
}
