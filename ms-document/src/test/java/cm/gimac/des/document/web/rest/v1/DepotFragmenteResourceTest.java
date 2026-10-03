package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Demandeur;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Etat;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Ouverture;
import cm.gimac.des.document.config.SecurityConfiguration;
import cm.gimac.des.document.config.TestSecurityConfiguration;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.enumeration.EtatDepotFragmente;
import cm.gimac.des.document.web.rest.errors.DocumentExceptionHandler;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Routes des dépôts fragmentés (D-74, docs/lot1/depots-fragmentes.md §3) avec la vraie chaîne de
 * filtres : 401 sans jeton ; 403 sans second facteur ; 403 sans l'une des permissions de dépôt ni
 * COL_MESSAGE_SEND ; accès avec chacune ; représentations JSON et codes de statut du contrat ;
 * erreurs en ProblemDetail. Cas d'utilisation simulé.
 */
@WebMvcTest(controllers = DepotFragmenteResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, DocumentExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class DepotFragmenteResourceTest {

    static final UUID ID = UUID.randomUUID();
    static final UUID DOCUMENT = UUID.randomUUID();
    static final String SHA = "ab".repeat(32);
    static final String DEPOTS = "/api/v1/depots";
    static final String[] PERMISSIONS = {
        "PAR_ADHESION_INSTRUCT",
        "HOM_EVIDENCE_SUBMIT",
        "HOM_STEP_MANAGE",
        "CER_CHANGE_RECORD_LINK",
        "HOM_PORTAL_SUBMIT",
        "DOC_DEPOSIT",
        "COL_MESSAGE_SEND",
    };
    static final String OUVERTURE =
        "{\"nomFichier\":\"atelier.mp4\",\"typeMime\":\"video/mp4\",\"taille\":4294967296,\"finalite\":\"MESSAGERIE\",\"reference\":\"conversation/c1\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DepotFragmenteUseCase useCase;

    static Etat etat(EtatDepotFragmente e) {
        boolean disponible = e == EtatDepotFragmente.DISPONIBLE;
        return new Etat(
            ID,
            e,
            "atelier.mp4",
            "video/mp4",
            4_294_967_296L,
            16_777_216L,
            256,
            List.of(1, 2, 3),
            disponible ? DOCUMENT : null,
            disponible ? SHA : null,
            disponible,
            e == EtatDepotFragmente.REJETE ? "DOC_PAN_DETECTED" : null,
            e == EtatDepotFragmente.OUVERT ? Instant.parse("2026-10-03T08:00:00Z") : null
        );
    }

    @BeforeEach
    void init() {
        when(useCase.ouvrir(any(), any())).thenReturn(etat(EtatDepotFragmente.OUVERT));
        when(useCase.etat(eq(ID), any())).thenReturn(etat(EtatDepotFragmente.DISPONIBLE));
        when(useCase.terminer(eq(ID), any())).thenReturn(etat(EtatDepotFragmente.EN_VERIFICATION));
    }

    static RequestPostProcessor jeton(String acr, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject("agent-ddrm").claim("institution_id", "inst-1");
                if (acr != null) {
                    j.claim("acr", acr);
                }
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    static MockHttpServletRequestBuilder ouverture() {
        return post(DEPOTS).contentType(MediaType.APPLICATION_JSON).content(OUVERTURE);
    }

    static MockHttpServletRequestBuilder partie(byte[] octets) {
        return put(DEPOTS + "/" + ID + "/parties/2").contentType(MediaType.APPLICATION_OCTET_STREAM).content(octets);
    }

    static Stream<MockHttpServletRequestBuilder> routes() {
        return Stream.of(ouverture(), partie(new byte[] { 1 }), get(DEPOTS + "/" + ID), post(DEPOTS + "/" + ID + "/fin"), delete(DEPOTS + "/" + ID));
    }

    @Test
    @DisplayName("Sans jeton : 401 sur chaque route")
    void sansJeton() throws Exception {
        for (MockHttpServletRequestBuilder r : routes().toList()) {
            mvc.perform(r).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("D-61 : sans second facteur (acr absent ou 1), 403 sur chaque route, même avec toutes les permissions")
    void sansDoubleFacteur() throws Exception {
        for (MockHttpServletRequestBuilder r : routes().toList()) {
            mvc.perform(r.with(jeton(null, PERMISSIONS))).andExpect(status().isForbidden());
        }
        for (MockHttpServletRequestBuilder r : routes().toList()) {
            mvc.perform(r.with(jeton("1", PERMISSIONS))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Sans permission de dépôt ni COL_MESSAGE_SEND : 403 sur chaque route")
    void sansPermission() throws Exception {
        for (MockHttpServletRequestBuilder r : routes().toList()) {
            mvc.perform(r.with(jeton("2", "COL_MESSAGE_READ", "INTERNAL_DOCUMENT_READ"))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(useCase);
    }

    @ParameterizedTest
    @ValueSource(
        strings = { "PAR_ADHESION_INSTRUCT", "HOM_EVIDENCE_SUBMIT", "HOM_STEP_MANAGE", "CER_CHANGE_RECORD_LINK", "HOM_PORTAL_SUBMIT", "DOC_DEPOSIT", "COL_MESSAGE_SEND" }
    )
    @DisplayName("Ouverture avec l'une des permissions : 201 Depot ; demandeur = sujet, institution et permissions du jeton")
    void ouvertureAvecPermission(String permission) throws Exception {
        mvc
            .perform(ouverture().with(jeton("2", permission)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ID.toString()))
            .andExpect(jsonPath("$.etat").value("OUVERT"))
            .andExpect(jsonPath("$.nomFichier").value("atelier.mp4"))
            .andExpect(jsonPath("$.typeMime").value("video/mp4"))
            .andExpect(jsonPath("$.taille").value(4_294_967_296L))
            .andExpect(jsonPath("$.taillePartie").value(16_777_216))
            .andExpect(jsonPath("$.nombreParties").value(256))
            .andExpect(jsonPath("$.partiesRecues").value(org.hamcrest.Matchers.contains(1, 2, 3)))
            .andExpect(jsonPath("$.dedoublonne").value(false))
            .andExpect(jsonPath("$.expireLe").value("2026-10-03T08:00:00Z"))
            .andExpect(jsonPath("$.documentId").doesNotExist())
            .andExpect(jsonPath("$.sha256").doesNotExist())
            .andExpect(jsonPath("$.motif").doesNotExist());

        ArgumentCaptor<Ouverture> ouverture = ArgumentCaptor.forClass(Ouverture.class);
        ArgumentCaptor<Demandeur> demandeur = ArgumentCaptor.forClass(Demandeur.class);
        verify(useCase).ouvrir(ouverture.capture(), demandeur.capture());
        assertThat(ouverture.getValue()).isEqualTo(new Ouverture("atelier.mp4", "video/mp4", 4_294_967_296L, "MESSAGERIE", "conversation/c1"));
        assertThat(demandeur.getValue()).isEqualTo(new Demandeur("agent-ddrm", "inst-1", Set.of(permission)));
    }

    @Test
    @DisplayName("Partie : octets bruts transmis en flux avec leur longueur, 204 ; autre type de contenu, 415")
    void partie() throws Exception {
        byte[] octets = { 4, 8, 15, 16, 23, 42 };
        byte[][] recus = new byte[1][];
        doAnswer(i -> {
            recus[0] = ((InputStream) i.getArgument(2)).readAllBytes();
            return null;
        })
            .when(useCase)
            .recevoirPartie(eq(ID), eq(2), any(), eq(6L), eq("agent-ddrm"));
        mvc.perform(partie(octets).with(jeton("2", "COL_MESSAGE_SEND"))).andExpect(status().isNoContent());
        assertThat(recus[0]).isEqualTo(octets);

        mvc
            .perform(put(DEPOTS + "/" + ID + "/parties/2").contentType(MediaType.TEXT_PLAIN).content("x").with(jeton("2", "COL_MESSAGE_SEND")))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("État : 200 Depot DISPONIBLE avec ticket, empreinte et déduplication ; fin : 202 EN_VERIFICATION ; abandon : 204")
    void etatFinAbandon() throws Exception {
        mvc
            .perform(get(DEPOTS + "/" + ID).with(jeton("2", "DOC_DEPOSIT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("DISPONIBLE"))
            .andExpect(jsonPath("$.documentId").value(DOCUMENT.toString()))
            .andExpect(jsonPath("$.sha256").value(SHA))
            .andExpect(jsonPath("$.dedoublonne").value(true))
            .andExpect(jsonPath("$.expireLe").doesNotExist());
        mvc
            .perform(post(DEPOTS + "/" + ID + "/fin").with(jeton("2", "DOC_DEPOSIT")))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.etat").value("EN_VERIFICATION"));
        mvc.perform(delete(DEPOTS + "/" + ID).with(jeton("2", "DOC_DEPOSIT"))).andExpect(status().isNoContent());
        verify(useCase).etat(ID, "agent-ddrm");
        verify(useCase).terminer(ID, "agent-ddrm");
        verify(useCase).abandonner(ID, "agent-ddrm");
    }

    @Test
    @DisplayName("Rejet : motif restitué")
    void rejet() throws Exception {
        when(useCase.etat(eq(ID), any())).thenReturn(etat(EtatDepotFragmente.REJETE));
        mvc
            .perform(get(DEPOTS + "/" + ID).with(jeton("2", "DOC_DEPOSIT")))
            .andExpect(jsonPath("$.etat").value("REJETE"))
            .andExpect(jsonPath("$.motif").value("DOC_PAN_DETECTED"))
            .andExpect(jsonPath("$.documentId").doesNotExist());
    }

    @Test
    @DisplayName("Erreurs en ProblemDetail : 404 dépôt d'un autre compte, 400 taille de partie, 409 dépôt incomplet (details.manquantes), 403 finalité")
    void erreurs() throws Exception {
        when(useCase.etat(eq(ID), any())).thenThrow(DocumentException.depotIntrouvable(ID));
        mvc
            .perform(get(DEPOTS + "/" + ID).with(jeton("2", "DOC_DEPOSIT")).header("X-Correlation-Id", "corr-7"))
            .andExpect(status().isNotFound())
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.code").value("DOC_UPLOAD_NOT_FOUND"))
            .andExpect(jsonPath("$.correlationId").value("corr-7"));

        doThrow(DocumentException.invalide("DOC_PART_SIZE", "Taille", Map.of("numero", 2, "attendue", 16_777_216L, "recue", 1L)))
            .when(useCase)
            .recevoirPartie(any(), anyInt(), any(), anyLong(), any());
        mvc
            .perform(partie(new byte[] { 1 }).with(jeton("2", "DOC_DEPOSIT")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DOC_PART_SIZE"))
            .andExpect(jsonPath("$.details.attendue").value(16_777_216));

        when(useCase.terminer(eq(ID), any())).thenThrow(
            DocumentException.conflit("DOC_UPLOAD_INCOMPLETE", "Incomplet", Map.of("manquantes", List.of(4, 7)))
        );
        mvc
            .perform(post(DEPOTS + "/" + ID + "/fin").with(jeton("2", "DOC_DEPOSIT")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DOC_UPLOAD_INCOMPLETE"))
            .andExpect(jsonPath("$.details.manquantes").value(org.hamcrest.Matchers.contains(4, 7)));

        when(useCase.ouvrir(any(), any())).thenThrow(DocumentException.interdit("DOC_PURPOSE_FORBIDDEN", "Finalité"));
        mvc.perform(ouverture().with(jeton("2", "DOC_DEPOSIT"))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("DOC_PURPOSE_FORBIDDEN"));
    }

    @Test
    @DisplayName("Jeton sans sujet : 403 DOC_USER_TOKEN_REQUIRED")
    void sansSujet() throws Exception {
        mvc
            .perform(ouverture().with(jwt().jwt(j -> j.subject(" ").claim("acr", "2")).authorities(new SimpleGrantedAuthority("DOC_DEPOSIT"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DOC_USER_TOKEN_REQUIRED"));
    }
}
