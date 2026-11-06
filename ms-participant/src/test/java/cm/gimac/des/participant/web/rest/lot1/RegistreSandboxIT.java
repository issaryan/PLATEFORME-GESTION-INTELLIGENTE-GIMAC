package cm.gimac.des.participant.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.participant.AbstractParticipantIT;
import cm.gimac.des.participant.domain.Correspondent;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.SubscribedService;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.CorrespondentFunction;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.Reachability;
import cm.gimac.des.participant.domain.enumeration.SubscriptionState;
import cm.gimac.des.participant.repository.CorrespondentRepository;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.SubscribedServiceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Interface interne du registre lue par la Sandbox GIMAC (US-EXI-21) : contrat de la fiche et de la nomenclature sous
 * un jeton de service, refus de toute écriture d'un compte de service de la sandbox (PostgreSQL par Testcontainers).
 */
@ExtendWith(OutputCaptureExtension.class)
class RegistreSandboxIT extends AbstractParticipantIT {

    @Autowired
    InstitutionRepository institutions;

    @Autowired
    CorrespondentRepository correspondants;

    @Autowired
    SubscribedServiceRepository services;

    @Autowired
    PlatformTransactionManager tm;

    /** Jeton client_credentials d'un compte de service de la sandbox : pas de second facteur, lecture interne seule. */
    static RequestPostProcessor compteDeService(String client) {
        return jwt().jwt(j -> j.subject("service-account-" + client).claim("azp", client)).authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ"));
    }

    Institution institution(String code, InstitutionStatus statut) {
        return institutions.saveAndFlush(
            new Institution()
                .name("Banque fictive " + code)
                .country("CM")
                .category(InstitutionCategory.BANQUE)
                .status(statut)
                .participantCode(code)
                .complianceState(ComplianceState.CONFORME)
                .openLinkCount(0)
                .openSuspenseCount(0)
                .createdAt(Instant.parse("2026-10-22T08:00:00Z"))
        );
    }

    Correspondent correspondant(Institution i, String nom, boolean actif, UUID compte) {
        Correspondent c = new Correspondent()
            .fullName(nom)
            .duty(CorrespondentFunction.MONETIQUE)
            .channels("EMAIL")
            .email(nom.toLowerCase().replace(' ', '.') + "@exemple.cm")
            .reachability(Reachability.JOIGNABLE)
            .onCall247(false)
            .active(actif)
            .userId(compte)
            .institution(i);
        return correspondants.saveAndFlush(c);
    }

    void service(Institution i, String code, SubscriptionState etat) {
        services.saveAndFlush(new SubscribedService().serviceCode(code).subscribedOn(LocalDate.of(2026, 1, 15)).state(etat).institution(i));
    }

    @Test
    @DisplayName("Contrat, critère 1 : fiche de 99001 lue par sbx-espace (statut, version, correspondants actifs, sans contact)")
    void lectureDeLaFiche() throws Exception {
        Institution i = institution("99001", InstitutionStatus.EN_HOMOLOGATION);
        UUID compte = UUID.randomUUID();
        Correspondent actif = correspondant(i, "Awa Ndiaye", true, compte);
        correspondant(i, "Ancien Correspondant", false, null);
        service(i, "MOBILE", SubscriptionState.ACTIF);
        service(i, "CARTE", SubscriptionState.ACTIF);
        service(i, "ANCIEN", SubscriptionState.RESILIE);
        new TransactionTemplate(tm).executeWithoutResult(s -> jdbc.update("insert into institution_version_statut (institution_id, version) values (?, 3)", i.getId()));
        mvc
            .perform(get("/api/v1/interne/institutions/code/99001/fiche").with(compteDeService("sbx-espace")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codeParticipant").value("99001"))
            .andExpect(jsonPath("$.raisonSociale").value("Banque fictive 99001"))
            .andExpect(jsonPath("$.statut").value("EN_HOMOLOGATION"))
            .andExpect(jsonPath("$.versionStatut").value(3))
            .andExpect(jsonPath("$.correspondants.length()").value(1))
            .andExpect(jsonPath("$.correspondants[0].id").value(actif.getId().toString()))
            .andExpect(jsonPath("$.correspondants[0].compteId").value(compte.toString()))
            .andExpect(jsonPath("$.correspondants[0].fonction").value("MONETIQUE"))
            .andExpect(jsonPath("$.correspondants[0].email").doesNotExist())
            .andExpect(jsonPath("$.servicesSouscrits").value(org.hamcrest.Matchers.contains("CARTE", "MOBILE")));
        mvc.perform(get("/api/v1/interne/institutions/code/99999/fiche").with(compteDeService("sbx-espace"))).andExpect(status().isNotFound());
        // D-134 : MS-IAM traduit institution_id (identifiant au registre) en code participant, et inversement.
        mvc.perform(get("/api/v1/interne/institutions/" + i.getId() + "/correspondance").with(compteDeService("iam-service")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.institutionId").value(i.getId().toString()))
            .andExpect(jsonPath("$.codeParticipant").value("99001"));
        mvc.perform(get("/api/v1/interne/institutions/code/99001/correspondance").with(compteDeService("iam-service")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.institutionId").value(i.getId().toString()));
        mvc.perform(get("/api/v1/interne/institutions/" + UUID.randomUUID() + "/correspondance").with(compteDeService("iam-service"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Contrat, critère 2 : nomenclature des services souscrits lue par sbx-referentiel, abonnements actifs, paginée")
    void nomenclatureDesServices() throws Exception {
        Institution a = institution("99001", InstitutionStatus.ACTIF);
        Institution b = institution("99002", InstitutionStatus.ACTIF);
        service(a, "CARTE", SubscriptionState.ACTIF);
        service(b, "CARTE", SubscriptionState.ACTIF);
        service(b, "MOBILE", SubscriptionState.ACTIF);
        service(a, "ANCIEN", SubscriptionState.RESILIE);
        mvc
            .perform(get("/api/v1/interne/nomenclature/services?page=0&taille=1").with(compteDeService("sbx-referentiel")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.contenu[0].code").value("CARTE"))
            .andExpect(jsonPath("$.contenu[0].institutions").value(2));
        mvc
            .perform(get("/api/v1/interne/nomenclature/services").with(compteDeService("sbx-raccordement")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenu.length()").value(2))
            .andExpect(jsonPath("$.taille").value(50));
        mvc.perform(get("/api/v1/interne/nomenclature/services?taille=500").with(compteDeService("sbx-referentiel"))).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Sécurité, critère 3 : le compte de service sbx-espace ne peut pas modifier le statut, refus sans effet et journalisé")
    void tentativeEcriture(CapturedOutput sortie) throws Exception {
        Institution i = institution("99001", InstitutionStatus.ACTIF);
        mvc
            .perform(
                post("/api/v1/institutions/" + i.getId() + "/transitions")
                    .with(compteDeService("sbx-espace"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"vers\":\"SUSPENDU\",\"motif\":\"essai\"}")
            )
            .andExpect(status().isForbidden());
        assertThat(institutions.findById(i.getId()).orElseThrow().getStatus()).isEqualTo(InstitutionStatus.ACTIF);
        assertThat(jdbc.queryForObject("select count(*) from status_transition", Long.class)).isZero();
        assertThat(sortie.getOut()).contains("participant.sandbox-write-denied", "client=sbx-espace", "/api/v1/institutions/" + i.getId() + "/transitions");
        // Un utilisateur sans la permission interne ne lit pas la fiche.
        mvc.perform(get("/api/v1/interne/institutions/code/99001/fiche").with(utilisateur(UUID.randomUUID(), "PAR_INSTITUTION_READ"))).andExpect(status().isForbidden());
    }
}
