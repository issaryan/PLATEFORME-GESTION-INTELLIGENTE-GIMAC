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

import cm.gimac.des.homologation.application.port.in.RepriseCloturesUseCase;
import cm.gimac.des.homologation.application.port.in.RepriseCloturesUseCase.Rapport;
import cm.gimac.des.homologation.application.port.in.RepriseCloturesUseCase.Resultat;
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

/** Clôture sans suite (D-137) : même droit que la reprise, second facteur sur la route métier, simulation par défaut. */
@WebMvcTest(controllers = RepriseCloturesResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, HomologationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class RepriseCloturesResourceSecurityTest {

    static final String CORPS = "{\"motif\":\"Liaison par défaut sans projet\",\"liaisons\":[{\"referenceOrigine\":\"REPRISE-1\"}]}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepriseCloturesUseCase clotures;

    @Test
    @DisplayName("Route interne : 401 sans jeton, 403 sans REF_REPRISE_IMPORT, 200 au compte de service ; simulation par défaut")
    void interne() throws Exception {
        when(clotures.clore(anyList(), eq(true), eq("Liaison par défaut sans projet"), any())).thenReturn(new Rapport(true, 1, Map.of(Resultat.CLOSE, 1L), List.of()));
        mvc.perform(post("/api/v1/interne/reprise/liaisons/clotures").contentType(MediaType.APPLICATION_JSON).content(CORPS)).andExpect(status().isUnauthorized());
        mvc
            .perform(post("/api/v1/interne/reprise/liaisons/clotures").with(jwt().authorities(new SimpleGrantedAuthority("HOM_LIAISON_OPEN"))).contentType(MediaType.APPLICATION_JSON).content(CORPS))
            .andExpect(status().isForbidden());
        mvc
            .perform(
                post("/api/v1/interne/reprise/liaisons/clotures")
                    .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CORPS)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulation").value(true));
        verify(clotures).clore(anyList(), eq(true), eq("Liaison par défaut sans projet"), any());
    }

    @Test
    @DisplayName("Route métier : second facteur exigé ; simulation à false transmise ; lot vide en 400")
    void metier() throws Exception {
        when(clotures.clore(anyList(), eq(false), any(), any())).thenReturn(new Rapport(false, 1, Map.of(), List.of()));
        String reel = "{\"simulation\":false," + CORPS.substring(1);
        mvc
            .perform(post("/api/v1/reprise/liaisons/clotures").with(jwt().authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT"))).contentType(MediaType.APPLICATION_JSON).content(reel))
            .andExpect(status().isForbidden());
        var deuxFacteurs = jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("acr", "2")).authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT"));
        mvc.perform(post("/api/v1/reprise/liaisons/clotures").with(deuxFacteurs).contentType(MediaType.APPLICATION_JSON).content(reel)).andExpect(status().isOk()).andExpect(jsonPath("$.simulation").value(false));
        mvc
            .perform(post("/api/v1/reprise/liaisons/clotures").with(deuxFacteurs).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"x\",\"liaisons\":[]}"))
            .andExpect(status().isBadRequest());
    }
}
