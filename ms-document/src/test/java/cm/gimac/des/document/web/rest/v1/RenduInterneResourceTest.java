package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.document.application.port.in.RenduPdfUseCase;
import cm.gimac.des.document.config.SecurityConfiguration;
import cm.gimac.des.document.config.TestSecurityConfiguration;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.web.rest.errors.DocumentExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Rendu PDF interne (D-102, docs/lot1/organigramme.md §4) avec la vraie chaîne de filtres : jeton de service portant
 * INTERNAL_DOCUMENT_RENDER seulement, demande transmise au cas d'utilisation, PDF rendu, erreurs problem+json.
 */
@WebMvcTest(controllers = RenduInterneResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, DocumentExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class RenduInterneResourceTest {

    static final String ROUTE = "/api/v1/interne/rendus/pdf";
    static final String CORPS = "{\"html\":\"<html></html>\",\"piedDePage\":\"<p>pied</p>\",\"format\":\"a3\",\"orientation\":\"PAYSAGE\"}";
    static final byte[] PDF = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RenduPdfUseCase useCase;

    static RequestPostProcessor service(String client, String... permissions) {
        return jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("azp", client).claim("preferred_username", "service-account-" + client))
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @Test
    @DisplayName("Jeton de service INTERNAL_DOCUMENT_RENDER : PDF ; sans jeton 401 ; utilisateur ou autre permission interne : 403")
    void acces() throws Exception {
        when(useCase.rendre(any(), any())).thenReturn(PDF);
        mvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON).content(CORPS)).andExpect(status().isUnauthorized());
        var utilisateur = jwt().jwt(j -> j.subject("agent").claim("acr", "2")).authorities(new SimpleGrantedAuthority("INTERNAL_DOCUMENT_RENDER"), new SimpleGrantedAuthority("PIL_ACTIVITY_OWN"));
        mvc.perform(post(ROUTE).with(utilisateur).contentType(MediaType.APPLICATION_JSON).content(CORPS)).andExpect(status().isForbidden());
        mvc.perform(post(ROUTE).with(service("pilotage-service", "INTERNAL_DOCUMENT_READ")).contentType(MediaType.APPLICATION_JSON).content(CORPS)).andExpect(status().isForbidden());
        mvc
            .perform(post(ROUTE).with(service("iam-service", "INTERNAL_DOCUMENT_RENDER")).contentType(MediaType.APPLICATION_JSON).content(CORPS))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(PDF));
        verify(useCase).rendre(eq(new RenduPdfUseCase.Demande("<html></html>", "<p>pied</p>", RenduPdfUseCase.Format.A3, true)), eq("iam-service"));
    }

    @Test
    @DisplayName("Format ou orientation inconnus : 400 DOC_RENDU_INVALIDE ; moteur indisponible : 503 DOC_RENDU_INDISPONIBLE")
    void erreurs() throws Exception {
        mvc
            .perform(post(ROUTE).with(service("iam-service", "INTERNAL_DOCUMENT_RENDER")).contentType(MediaType.APPLICATION_JSON).content(CORPS.replace("a3", "A5")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DOC_RENDU_INVALIDE"));
        mvc
            .perform(post(ROUTE).with(service("iam-service", "INTERNAL_DOCUMENT_RENDER")).contentType(MediaType.APPLICATION_JSON).content(CORPS.replace("PAYSAGE", "OBLIQUE")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DOC_RENDU_INVALIDE"));
        when(useCase.rendre(any(), any())).thenThrow(DocumentException.indisponible("DOC_RENDU_INDISPONIBLE", "Indisponible"));
        mvc
            .perform(post(ROUTE).with(service("iam-service", "INTERNAL_DOCUMENT_RENDER")).contentType(MediaType.APPLICATION_JSON).content(CORPS.replace(",\"orientation\":\"PAYSAGE\"", "")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("DOC_RENDU_INDISPONIBLE"));
    }

    static final String GABARIT =
        "{\"html\":\"<h2>x</h2>\",\"format\":\"A4\",\"orientation\":\"PORTRAIT\",\"gabarit\":{\"titre\":\"Demande\",\"reference\":\"CHG-1\"," +
        "\"classification\":\"confidentiel\",\"langue\":\"es\",\"edition\":\"2026-11-06T10:30:00+01:00\",\"faits\":[{\"libelle\":\"État\",\"valeur\":\"En cours\"},null]}}";

    @Test
    @DisplayName("Gabarit commun (D-139) : classification, langue, date et données clés transmises ; classification ou date invalides : 400")
    void gabarit() throws Exception {
        when(useCase.rendre(any(), any())).thenReturn(PDF);
        mvc.perform(post(ROUTE).with(service("pilotage-service", "INTERNAL_DOCUMENT_RENDER")).contentType(MediaType.APPLICATION_JSON).content(GABARIT)).andExpect(status().isOk());
        var g = new RenduPdfUseCase.Gabarit(
            "Demande", null, null, "CHG-1", RenduPdfUseCase.Classification.CONFIDENTIEL, "es", java.time.OffsetDateTime.parse("2026-11-06T10:30:00+01:00"), null,
            java.util.List.of(new RenduPdfUseCase.Fait("État", "En cours")), null
        );
        verify(useCase).rendre(eq(new RenduPdfUseCase.Demande("<h2>x</h2>", null, RenduPdfUseCase.Format.A4, false, g)), eq("pilotage-service"));
        for (String faux : java.util.List.of(GABARIT.replace("confidentiel", "secret"), GABARIT.replace("2026-11-06T10:30:00+01:00", "hier"))) {
            mvc
                .perform(post(ROUTE).with(service("pilotage-service", "INTERNAL_DOCUMENT_RENDER")).contentType(MediaType.APPLICATION_JSON).content(faux))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOC_RENDU_INVALIDE"));
        }
        assertThat(RenduInterneResource.gabarit(new RenduInterneResource.GabaritRequest("T", null, null, null, " ", null, " ", null, null, null)))
            .isEqualTo(new RenduPdfUseCase.Gabarit("T", null, null, null, null, null, null, null, java.util.List.of(), null));
        assertThat(RenduInterneResource.gabarit(null)).isNull();
    }
}
