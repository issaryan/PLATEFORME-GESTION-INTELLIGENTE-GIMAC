package cm.gimac.des.participant.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.participant.application.port.in.RepriseCorrespondantsUseCase;
import cm.gimac.des.participant.application.port.in.RepriseCorrespondantsUseCase.Rapport;
import cm.gimac.des.participant.application.port.in.RepriseCorrespondantsUseCase.Resultat;
import cm.gimac.des.participant.config.SecurityConfiguration;
import cm.gimac.des.participant.config.TestSecurityConfiguration;
import cm.gimac.des.participant.web.rest.errors.ParticipantExceptionHandler;
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
 * Reprise des correspondants (D-132) : route interne au jeton de service REF_REPRISE_IMPORT (sans second facteur), route
 * métier au super administrateur à deux facteurs ; simulation par défaut.
 */
@WebMvcTest(controllers = RepriseCorrespondantsResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, ParticipantExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class RepriseCorrespondantsResourceSecurityTest {

    static final String CORPS = "{\"correspondants\":[{\"codeMembre\":\"BQ-1\",\"compteId\":\"0a0b0c0d-0e0f-4a1b-8c2d-3e4f5a6b7c8d\",\"nom\":\"Awa Fictive\"}]}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepriseCorrespondantsUseCase reprise;

    @Test
    @DisplayName("Route interne : 401 sans jeton, 403 sans REF_REPRISE_IMPORT, 200 au compte de service ; simulation par défaut")
    void interne() throws Exception {
        when(reprise.importer(anyList(), eq(true), any(), any())).thenReturn(new Rapport(true, 1, Map.of(Resultat.CREE, 1L), List.of()));
        mvc.perform(post("/api/v1/interne/reprise/correspondants").contentType(MediaType.APPLICATION_JSON).content(CORPS)).andExpect(status().isUnauthorized());
        mvc
            .perform(post("/api/v1/interne/reprise/correspondants").with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ"))).contentType(MediaType.APPLICATION_JSON).content(CORPS))
            .andExpect(status().isForbidden());
        mvc
            .perform(
                post("/api/v1/interne/reprise/correspondants")
                    .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CORPS)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulation").value(true));
        verify(reprise).importer(anyList(), eq(true), any(), any());
    }

    @Test
    @DisplayName("Route métier : second facteur exigé ; simulation à false transmise ; lot vide en 400")
    void metier() throws Exception {
        when(reprise.importer(anyList(), eq(false), eq("Lot 1"), any())).thenReturn(new Rapport(false, 1, Map.of(), List.of()));
        String reel = "{\"simulation\":false,\"motif\":\"Lot 1\"," + CORPS.substring(1);
        mvc
            .perform(post("/api/v1/reprise/correspondants").with(jwt().authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT"))).contentType(MediaType.APPLICATION_JSON).content(reel))
            .andExpect(status().isForbidden());
        mvc
            .perform(
                post("/api/v1/reprise/correspondants")
                    .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("acr", "2")).authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reel)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulation").value(false));
        mvc
            .perform(
                post("/api/v1/reprise/correspondants")
                    .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("acr", "2")).authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"correspondants\":[]}")
            )
            .andExpect(status().isBadRequest());
    }
}
