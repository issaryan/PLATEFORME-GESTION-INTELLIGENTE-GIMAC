package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import cm.gimac.des.homologation.infrastructure.messaging.consumer.StatutsInstitutionConsumer;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * HOM-06 et republication (D-122) sur PostgreSQL : participant.status-changed.v1 gèle puis rétablit les liaisons de
 * l'institution, chaque changement publié avec une version d'état supérieure ; doublon et événement périmé sans effet ;
 * route interne de republication, idempotente, réservée au jeton portant INTERNAL_HOMOLOGATION_REPUBLISH.
 */
class GelLiaisonsSandboxIT extends AbstractHomologationIT {

    @Autowired
    ParticipantStubAdapter registre;

    @Autowired
    StatutsInstitutionConsumer consommateur;

    UUID institution;
    String liaison;

    void ouvrirLiaison() throws Exception {
        fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        institution = UUID.randomUUID();
        registre.enregistrer(new InstitutionView(institution, "Institution 99001", "ADHERENT", "ETABLISSEMENT_PAIEMENT", List.of(new InstitutionView.ServiceSouscrit("PAIEMENT_MOBILE", "2026-09-01"))), "99001");
        String corps = mvc
            .perform(
                post("/api/v1/liaisons")
                    .with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_READ", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"institutionId\":\"%s\",\"typeProjet\":\"PAIEMENT_MOBILE\",\"chefDeProjetId\":\"%s\"}".formatted(institution, CollaboratorStubAdapter.CONDUCTEUR_1))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        liaison = JsonPath.read(corps, "$.id");
    }

    void statut(String id, String statut, Integer version) {
        String v = version == null ? "" : ",\"versionStatutRegistre\":" + version;
        consommateur.recevoir(
            "{\"id\":\"%s\",\"type\":\"participant.status-changed.v1\",\"data\":{\"institutionId\":\"%s\",\"statut\":\"%s\",\"motif\":\"Décision du comité\",\"dateEffet\":\"2026-10-22T08:00:00Z\"%s}}".formatted(id, institution, statut, v)
        );
    }

    String etat() {
        return jdbc.queryForObject("select etat from liaison where id = ?::uuid", String.class, liaison);
    }

    List<String> charges() {
        return jdbc.queryForList(
            "select payload::text from outbox_event where aggregate_id = ? and event_type = 'homologation.liaison-status-changed.v1' order by created_at",
            String.class,
            liaison
        );
    }

    @Test
    @DisplayName("HOM-06 : suspension puis retour de l'institution, liaison gelée puis rétablie, versions 2 et 3")
    void gelEtRetablissement() throws Exception {
        ouvrirLiaison();
        String avant = etat();
        statut(UUID.randomUUID().toString(), "SUSPENDU", 4);
        assertThat(etat()).isEqualTo("GELEE");
        assertThat(jdbc.queryForObject("select motif_etat from liaison where id = ?::uuid", String.class, liaison)).isEqualTo("Liaison gelée : institution SUSPENDU au registre (Décision du comité)");
        assertThat(charges()).hasSize(2);
        assertThat(JsonPath.<String>read(charges().get(1), "$.statut")).isEqualTo("GELEE");
        assertThat(JsonPath.<Integer>read(charges().get(1), "$.versionEtat")).isEqualTo(2);

        // Doublon (même identifiant) et événement périmé (version inférieure) : sans effet.
        String doublon = UUID.randomUUID().toString();
        statut(doublon, "ACTIF", 3);
        statut(doublon, "ACTIF", 3);
        assertThat(etat()).isEqualTo("GELEE");
        assertThat(charges()).hasSize(2);

        statut(UUID.randomUUID().toString(), "ACTIF", 5);
        assertThat(etat()).isEqualTo(avant);
        assertThat(jdbc.queryForObject("select etat_avant_gel::text from liaison where id = ?::uuid", String.class, liaison)).isNull();
        assertThat(JsonPath.<String>read(charges().get(2), "$.statut")).isEqualTo("OUVERTE");
        assertThat(JsonPath.<Integer>read(charges().get(2), "$.versionEtat")).isEqualTo(3);
    }

    @Test
    @DisplayName("Republication : route interne idempotente, même version d'état, refusée sans la permission dédiée")
    void republication() throws Exception {
        ouvrirLiaison();
        var service = jwt().jwt(j -> j.subject("service-account-exploitation")).authorities(new SimpleGrantedAuthority("INTERNAL_HOMOLOGATION_REPUBLISH"));
        mvc
            .perform(post("/api/v1/interne/sandbox/liaisons/republication").with(service))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.liaisons").value(1))
            .andExpect(jsonPath("$.republiees").value(1))
            .andExpect(jsonPath("$.nouvelles").value(0));
        mvc.perform(post("/api/v1/interne/sandbox/liaisons/republication").with(service)).andExpect(status().isOk()).andExpect(jsonPath("$.nouvelles").value(0));
        assertThat(charges()).hasSize(3);
        assertThat(charges().stream().map(c -> JsonPath.<Integer>read(c, "$.versionEtat"))).containsOnly(1);

        mvc
            .perform(post("/api/v1/interne/sandbox/liaisons/republication").with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_HOMOLOGATION_READ"))))
            .andExpect(status().isForbidden());
        mvc
            .perform(post("/api/v1/interne/sandbox/liaisons/republication").with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_OPEN", "SBX_LEC_ADMINISTRATION")))
            .andExpect(status().isForbidden());
        assertThat(charges()).hasSize(3);
    }
}
