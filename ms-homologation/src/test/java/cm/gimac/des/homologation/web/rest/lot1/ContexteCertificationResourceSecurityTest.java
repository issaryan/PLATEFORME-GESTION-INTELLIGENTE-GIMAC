package cm.gimac.des.homologation.web.rest.lot1;

import static cm.gimac.des.homologation.web.rest.lot1.ParcoursResourceSecurityTest.jeton;
import static cm.gimac.des.homologation.web.rest.lot1.ParcoursResourceSecurityTest.req;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.application.usecase.ParcoursLectures;
import cm.gimac.des.homologation.config.SecurityConfiguration;
import cm.gimac.des.homologation.config.TestSecurityConfiguration;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import cm.gimac.des.homologation.web.rest.errors.HomologationExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.jhipster.config.JHipsterProperties;

/** Interface interne servie à MS-CERTIFICATION : jeton de service INTERNAL_HOMOLOGATION_READ seulement. */
@WebMvcTest(controllers = ContexteCertificationResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, HomologationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class ContexteCertificationResourceSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ParcoursLectures lectures;

    @Test
    @DisplayName("401 sans jeton ; 403 pour un jeton utilisateur, même à deux facteurs ; contexte servi au jeton de service")
    void matrice() throws Exception {
        var p = ParcoursSamples.parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        UUID id = p.id();
        when(lectures.contexteCertification(id)).thenReturn(new ParcoursProjection().contexte(p, "Conducteur de projet 1"));
        String uri = "/api/v1/interne/liaisons/" + id + "/contexte-certification";
        mvc.perform(req(HttpMethod.GET, uri, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, uri, null).with(jeton("2", ParcoursResourceSecurityTest.TOUTES.toArray(String[]::new)))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.GET, uri, null).with(jwt().jwt(j -> j.subject("service-account-certification-service")).authorities(new SimpleGrantedAuthority("INTERNAL_HOMOLOGATION_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.liaisonId").value(id.toString()))
            .andExpect(jsonPath("$.reference").value("LIA-2026-001"))
            .andExpect(jsonPath("$.couvertureCertification.denominateur").value(1))
            .andExpect(jsonPath("$.couvertureConstatee").value(false))
            .andExpect(jsonPath("$.servicesCouverts").isEmpty())
            .andExpect(jsonPath("$.conducteurNom").value("Conducteur de projet 1"));
    }
}
