package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Cadrage (étape 3, D-57, D-58) sur PostgreSQL et Kafka réels : ouverture d'une liaison sur un
 * adhérent par un chef de département qui désigne le conducteur de projet, périmètre dérivé des
 * services souscrits, homologation.liaison-opened.v1 relayé sur homologation.liaison (lu par
 * MS-PARTICIPANT), unicité, refus d'un candidat, redésignation.
 */
class CadrageLiaisonIT extends AbstractHomologationIT {

    static final UUID CHEF_DES = UUID.randomUUID();

    @Autowired
    ParticipantStubAdapter registre;

    static RequestPostProcessor chefDes() {
        return utilisateur(CHEF_DES, "HOM_LIAISON_LIST", "HOM_LIAISON_READ", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE");
    }

    static String ouverture(UUID institution, TypeProjet type, UUID conducteur) {
        return "{\"institutionId\":\"%s\",\"typeProjet\":\"%s\",\"chefDeProjetId\":\"%s\"}".formatted(institution, type, conducteur);
    }

    UUID adherent(String nom, String statut, String... services) {
        UUID id = UUID.randomUUID();
        registre.enregistrer(new InstitutionView(id, nom, statut, "ETABLISSEMENT_PAIEMENT", java.util.Arrays.stream(services).map(s -> new InstitutionView.ServiceSouscrit(s, "2026-09-01")).toList()));
        return id;
    }

    @Test
    @DisplayName("Ouverture sur un adhérent : liaison OUVERTE, périmètre, conducteur, événement relayé vers MS-PARTICIPANT")
    void ouverture() throws Exception {
        fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        UUID ep = adherent("Établissement de paiement Cadrage", "ADHERENT", "PAIEMENT_MOBILE");
        String corps = mvc
            .perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(ouverture(ep, TypeProjet.PAIEMENT_MOBILE, CollaboratorStubAdapter.CONDUCTEUR_1)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.etat").value("OUVERTE"))
            .andExpect(jsonPath("$.phaseCourante").value("CADRAGE"))
            .andExpect(jsonPath("$.chefDeProjetNom").value("Aminata Ngono"))
            .andExpect(jsonPath("$.perimetre").value(org.hamcrest.Matchers.contains("PAIEMENT_MOBILE", "TRANSVERSE")))
            .andExpect(jsonPath("$.actionsPermises[0]").value("DESIGNER_CHEF_DE_PROJET"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String id = JsonPath.read(corps, "$.id");
        assertThat(jdbc.queryForObject("select count(*) from scope_item si join contractual_scope cs on si.scope_id = cs.id where cs.liaison_id = ?::uuid", Integer.class, id)).isEqualTo(2);

        // Le conducteur voit la liaison dans « mes projets ».
        mvc
            .perform(get("/api/v1/liaisons").param("mesProjets", "true").with(utilisateur(CollaboratorStubAdapter.CONDUCTEUR_1, "HOM_LIAISON_LIST")))
            .andExpect(jsonPath("$.contenu[*].id").value(org.hamcrest.Matchers.hasItem(id)));
        mvc
            .perform(get("/api/v1/liaisons").param("mesProjets", "true").with(utilisateur(CollaboratorStubAdapter.CONDUCTEUR_2, "HOM_LIAISON_LIST")))
            .andExpect(jsonPath("$.contenu[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(id))));

        relayerOutboxJusquaEnvoi(id);
        try (var c = consommateur("homologation.liaison")) {
            var recus = KafkaTestUtils.getRecords(c, Duration.ofSeconds(20)).records("homologation.liaison");
            assertThat(recus).anySatisfy(r -> {
                assertThat(r.key()).isEqualTo(id);
                assertThat(JsonPath.<String>read(r.value(), "$.type")).isEqualTo("homologation.liaison-opened.v1");
                assertThat(JsonPath.<String>read(r.value(), "$.data.institutionId")).isEqualTo(ep.toString());
                assertThat(JsonPath.<String>read(r.value(), "$.data.liaisonId")).isEqualTo(id);
            });
        }

        // D-104 : seconde liaison active du même type (lot suivant), avec intitulé et référence d'origine.
        String lot2 = "{\"institutionId\":\"%s\",\"typeProjet\":\"PAIEMENT_MOBILE\",\"chefDeProjetId\":\"%s\",\"intituleProjet\":\"Projet Mobile Cadrage Lot 2\",\"referenceOrigine\":\"202604-CAD-02\"}".formatted(
            ep,
            CollaboratorStubAdapter.CONDUCTEUR_2
        );
        String corpsLot2 = mvc
            .perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(lot2))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.intituleProjet").value("Projet Mobile Cadrage Lot 2"))
            .andExpect(jsonPath("$.referenceOrigine").value("202604-CAD-02"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String idLot2 = JsonPath.read(corpsLot2, "$.id");
        assertThat(idLot2).isNotEqualTo(id);
        mvc
            .perform(get("/api/v1/liaisons").param("institutionId", ep.toString()).param("actives", "true").with(chefDes()))
            .andExpect(jsonPath("$.total").value(2));
        mvc
            .perform(get("/api/v1/liaisons").param("recherche", "cad-02").with(chefDes()))
            .andExpect(jsonPath("$.contenu[*].id").value(org.hamcrest.Matchers.contains(idLot2)));
        // La référence d'origine reste unique ; mal formée, elle est refusée à l'entrée.
        mvc
            .perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(lot2))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_LIAISON_REFERENCE_ORIGINE_DUPLICATE"));
        mvc
            .perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(lot2.replace("202604-CAD-02", "réf ?")))
            .andExpect(status().isBadRequest());

        // Redésignation par un chef de département ; l'ingénieur sans ce droit ne désigne pas.
        String designation = "{\"chefDeProjetId\":\"" + CollaboratorStubAdapter.CONDUCTEUR_2 + "\",\"motif\":\"Réaffectation\"}";
        mvc
            .perform(put("/api/v1/liaisons/{id}/chef-de-projet", id).with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_OPEN")).contentType(MediaType.APPLICATION_JSON).content(designation))
            .andExpect(status().isForbidden());
        mvc
            .perform(put("/api/v1/liaisons/{id}/chef-de-projet", id).with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(designation))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.chefDeProjetNom").value("Paul Essomba"));
        // D-93 : les opérations ouvertes à l'instanciation émettent aussi homologation.operation-activated.v1.
        List<String> types = jdbc.queryForList("select event_type from outbox_event where aggregate_id = ? order by created_at", String.class, id);
        // US-EXI-16 : l'ouverture publie aussi l'état de la liaison pour la sandbox (version 1).
        assertThat(types).filteredOn(t -> !t.startsWith("homologation.operation-")).containsExactlyInAnyOrder(
            "homologation.liaison-opened.v1",
            "homologation.liaison-status-changed.v1",
            "homologation.liaison-lead-designated.v1"
        );
        assertThat(types).contains("homologation.operation-activated.v1");
    }

    @Test
    @DisplayName("Refus : candidat non adhérent, conducteur non habilité, ouverture sans droit de désignation ; rien créé")
    void refus() throws Exception {
        fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        UUID candidat = adherent("Candidat", "CANDIDAT");
        mvc
            .perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(ouverture(candidat, TypeProjet.PAIEMENT_MOBILE, CollaboratorStubAdapter.CONDUCTEUR_1)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE"))
            .andExpect(jsonPath("$.details.statut").value("CANDIDAT"));
        UUID ep = adherent("Adhérent", "ADHERENT");
        mvc
            .perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(ouverture(ep, TypeProjet.PAIEMENT_MOBILE, UUID.randomUUID())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED"));
        mvc
            .perform(
                post("/api/v1/liaisons")
                    .with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_OPEN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ouverture(ep, TypeProjet.PAIEMENT_MOBILE, CollaboratorStubAdapter.CONDUCTEUR_1))
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("HOM_PROJECT_DESIGNATION_FORBIDDEN"));
        assertThat(jdbc.queryForObject("select count(*) from liaison where institution_id in (?::uuid, ?::uuid)", Integer.class, candidat.toString(), ep.toString())).isZero();
        mvc
            .perform(get("/api/v1/liaisons/conducteurs").with(chefDes()))
            .andExpect(jsonPath("$[*].nom").value(org.hamcrest.Matchers.contains("Aminata Ngono", "Paul Essomba")));
    }
}
