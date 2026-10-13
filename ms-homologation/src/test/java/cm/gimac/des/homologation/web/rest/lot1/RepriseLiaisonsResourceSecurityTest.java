package cm.gimac.des.homologation.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.Rapport;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.Resultat;
import cm.gimac.des.homologation.config.SecurityConfiguration;
import cm.gimac.des.homologation.config.TestSecurityConfiguration;
import cm.gimac.des.homologation.web.rest.errors.HomologationExceptionHandler;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.jhipster.config.JHipsterProperties;

/**
 * Reprise des liaisons (D-107) : route interne au jeton de service REF_REPRISE_IMPORT (sans second facteur), route
 * métier au super administrateur à deux facteurs ; simulation par défaut.
 */
@WebMvcTest(controllers = RepriseLiaisonsResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, HomologationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class RepriseLiaisonsResourceSecurityTest {

    static final String CORPS = "{\"liaisons\":[{\"referenceOrigine\":\"R-1\",\"codeMembre\":\"BQ-1\",\"typeProjet\":\"PAIEMENT_MOBILE\"}]}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepriseLiaisonsUseCase reprise;

    @Test
    @DisplayName("Route interne : 401 sans jeton, 403 sans REF_REPRISE_IMPORT, 200 au compte de service ; simulation par défaut")
    void interne() throws Exception {
        when(reprise.importer(anyList(), eq(true), any())).thenReturn(new Rapport(true, 1, Map.of(Resultat.CREEE, 1L), List.of()));
        mvc.perform(post("/api/v1/interne/reprise/liaisons").contentType(MediaType.APPLICATION_JSON).content(CORPS)).andExpect(status().isUnauthorized());
        mvc
            .perform(post("/api/v1/interne/reprise/liaisons").with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_HOMOLOGATION_READ"))).contentType(MediaType.APPLICATION_JSON).content(CORPS))
            .andExpect(status().isForbidden());
        mvc
            .perform(
                post("/api/v1/interne/reprise/liaisons")
                    .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CORPS)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulation").value(true));
        verify(reprise).importer(anyList(), eq(true), any());
    }

    @Test
    @DisplayName("Route métier : second facteur exigé ; simulation à false transmise ; lot vide en 400")
    void metier() throws Exception {
        when(reprise.importer(anyList(), eq(false), any())).thenReturn(new Rapport(false, 1, Map.of(), List.of()));
        String reel = "{\"simulation\":false," + CORPS.substring(1);
        mvc
            .perform(post("/api/v1/reprise/liaisons").with(jwt().authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT"))).contentType(MediaType.APPLICATION_JSON).content(reel))
            .andExpect(status().isForbidden());
        mvc
            .perform(
                post("/api/v1/reprise/liaisons")
                    .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("acr", "2")).authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reel)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulation").value(false));
        mvc
            .perform(
                post("/api/v1/reprise/liaisons")
                    .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("acr", "2")).authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"liaisons\":[]}")
            )
            .andExpect(status().isBadRequest());
    }
}
