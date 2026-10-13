package cm.gimac.des.participant.web.rest.lot1;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.participant.application.usecase.InstitutionInterneQueries;
import cm.gimac.des.participant.config.SecurityConfiguration;
import cm.gimac.des.participant.config.TestSecurityConfiguration;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.SubscribedService;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.SubscriptionState;
import cm.gimac.des.participant.web.rest.errors.ParticipantExceptionHandler;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.jhipster.config.JHipsterProperties;

/** Interface interne de lecture d'une institution (PI §0.7) : jeton de service seulement. */
@WebMvcTest(controllers = InstitutionInterneResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, ParticipantExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class InstitutionInterneResourceSecurityTest {

    static final UUID ID = UUID.randomUUID();

    @Autowired
    MockMvc mvc;

    @MockitoBean
    InstitutionInterneQueries queries;

    @Test
    @DisplayName("401 sans jeton, 403 pour un utilisateur même à deux facteurs, 200 pour le jeton de service")
    void acces() throws Exception {
        Institution i = new Institution().name("Banque B").status(InstitutionStatus.ADHERENT).category(InstitutionCategory.BANQUE).country("CM").participantCode("BQ-B-001");
        i.setId(ID);
        SubscribedService s = new SubscribedService().serviceCode("CARTE_INTERBANCAIRE").subscribedOn(LocalDate.of(2026, 9, 1)).state(SubscriptionState.ACTIF);
        when(queries.lire(ID)).thenReturn(new InstitutionInterneQueries.Vue(i, List.of(s)));
        mvc.perform(get("/api/v1/interne/institutions/{id}", ID)).andExpect(status().isUnauthorized());
        mvc
            .perform(
                get("/api/v1/interne/institutions/{id}", ID).with(
                    jwt().jwt(j -> j.claim("acr", "2")).authorities(new SimpleGrantedAuthority("PAR_INSTITUTION_READ"))
                )
            )
            .andExpect(status().isForbidden());
        mvc
            .perform(get("/api/v1/interne/institutions/{id}", ID).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("ADHERENT"))
            .andExpect(jsonPath("$.categorie").value("BANQUE"))
            .andExpect(jsonPath("$.servicesSouscrits[0].code").value("CARTE_INTERBANCAIRE"))
            .andExpect(jsonPath("$.servicesSouscrits[0].depuis").value("2026-09-01"));
    }

    @Test
    @DisplayName("Lecture par code membre (reprise des liaisons, D-107) : jeton de service")
    void parCode() throws Exception {
        Institution i = new Institution().name("Banque B").status(InstitutionStatus.ACTIF).category(InstitutionCategory.BANQUE).country("CM").participantCode("BQ-B-001");
        i.setId(ID);
        when(queries.parCode("BQ-B-001")).thenReturn(new InstitutionInterneQueries.Vue(i, List.of()));
        mvc
            .perform(get("/api/v1/interne/institutions").param("codeParticipant", "BQ-B-001").with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ID.toString()))
            .andExpect(jsonPath("$.codeParticipant").value("BQ-B-001"));
    }

    @Test
    @DisplayName("Institution inconnue : 404 en ProblemDetail")
    void inconnue() throws Exception {
        UUID autre = UUID.randomUUID();
        when(queries.lire(autre)).thenThrow(ParticipantException.notFound("PAR_INSTITUTION_NOT_FOUND", autre));
        mvc
            .perform(get("/api/v1/interne/institutions/{id}", autre).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PAR_INSTITUTION_NOT_FOUND"));
    }
}
