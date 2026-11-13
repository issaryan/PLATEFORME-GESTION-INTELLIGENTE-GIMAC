package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.contrat.SchemaContrat;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * D-145 sur PostgreSQL réel : le gestionnaire du projet définit le périmètre à la création de la liaison, parmi les
 * services proposables ; la définition est validée, journalisée en version 1 et publiée à la sandbox par
 * homologation.liaison-opened.v1.
 */
class PerimetreCreationIT extends AbstractHomologationIT {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final UUID GESTIONNAIRE = UUID.randomUUID();
    static final String[] OUVREUR = { "HOM_LIAISON_LIST", "HOM_LIAISON_READ", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE", "HOM_PERIMETRE_EDIT" };

    @Autowired
    ParticipantStubAdapter registre;

    UUID institution;

    @BeforeEach
    void registre() {
        fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        institution = UUID.randomUUID();
        registre.enregistrer(
            new InstitutionView(
                institution,
                "Institution 99002",
                "ADHERENT",
                "ETABLISSEMENT_PAIEMENT",
                List.of(new InstitutionView.ServiceSouscrit("W2W", "2026-09-01"), new InstitutionView.ServiceSouscrit("ISO.W2W", "2026-09-01"), new InstitutionView.ServiceSouscrit("CRT.NET", "2026-09-01"))
            ),
            "99002"
        );
    }

    String corps(String perimetre) {
        return "{\"institutionId\":\"%s\",\"typeProjet\":\"PAIEMENT_MOBILE\",\"chefDeProjetId\":\"%s\"%s}".formatted(
            institution,
            CollaboratorStubAdapter.CONDUCTEUR_1,
            perimetre == null ? "" : ",\"perimetre\":" + perimetre
        );
    }

    @Test
    @DisplayName("Services proposables : souscrits, TRANSVERSE exclu, interface de la sandbox d'après le préfixe")
    void proposables() throws Exception {
        mvc
            .perform(get("/api/v1/liaisons/perimetre-proposable").param("institutionId", institution.toString()).param("typeProjet", "PAIEMENT_MOBILE").with(utilisateur(GESTIONNAIRE, OUVREUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.souscrits").value(true))
            .andExpect(jsonPath("$.services[*].code").value(Matchers.contains("W2W", "ISO.W2W", "CRT.NET")))
            .andExpect(jsonPath("$.services[*].interfaceSandbox").value(Matchers.contains("MOBILE", "ISO20022", "CARTE")));
        mvc
            .perform(get("/api/v1/liaisons/perimetre-proposable").param("institutionId", institution.toString()).param("typeProjet", "PAIEMENT_MOBILE").with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_READ")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Création avec périmètre : services retenus seuls, version 1 journalisée, événement conforme, puis version 2")
    void creationAvecPerimetre() throws Exception {
        String r = mvc
            .perform(post("/api/v1/liaisons").with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("[\"ISO.W2W\",\"W2W\"]")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String liaison = JsonPath.read(r, "$.id");
        mvc
            .perform(get("/api/v1/liaisons/" + liaison + "/perimetre").with(utilisateur(GESTIONNAIRE, OUVREUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.services[*].code").value(Matchers.containsInAnyOrder("TRANSVERSE", "W2W", "ISO.W2W")));
        mvc
            .perform(get("/api/v1/liaisons/" + liaison + "/perimetre/historique").with(utilisateur(GESTIONNAIRE, OUVREUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].version").value(1))
            .andExpect(jsonPath("$.contenu[0].auteurId").value(GESTIONNAIRE.toString()))
            .andExpect(jsonPath("$.contenu[0].permissionUtilisee").value("HOM_LIAISON_OPEN"))
            .andExpect(jsonPath("$.contenu[0].avant").value(Matchers.contains("TRANSVERSE", "W2W", "ISO.W2W", "CRT.NET")))
            .andExpect(jsonPath("$.contenu[0].apres").value(Matchers.contains("TRANSVERSE", "W2W", "ISO.W2W")));
        String charge = jdbc.queryForObject("select payload::text from outbox_event where aggregate_id = ? and event_type = 'homologation.liaison-opened.v1'", String.class, liaison);
        assertThat(SchemaContrat.charger("sandbox/homologation.liaison-opened.v1.schema.json").ecarts(JSON.readTree(charge))).isEmpty();
        assertThat(JsonPath.<List<String>>read(charge, "$.perimetre")).containsExactly("TRANSVERSE", "W2W", "ISO.W2W");
        assertThat(JsonPath.<String>read(charge, "$.codeParticipant")).isEqualTo("99002");

        // Modification ultérieure par le gestionnaire : version 2, à la suite de la version 1 de la création.
        mvc
            .perform(
                put("/api/v1/liaisons/" + liaison + "/perimetre")
                    .with(utilisateur(GESTIONNAIRE, OUVREUR))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"services\":[\"W2W\",\"ISO.W2W\",\"CRT.NET\"],\"motif\":\"Carte ajoutée au projet\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("Création sans périmètre (reprise, automate) : dérivation des services souscrits, sans ligne de journal")
    void creationSansPerimetre() throws Exception {
        String r = mvc
            .perform(post("/api/v1/liaisons").with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps(null)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String liaison = JsonPath.read(r, "$.id");
        assertThat(jdbc.queryForObject("select count(*) from perimetre_modification where liaison_id = ?::uuid", Integer.class, liaison)).isZero();
        mvc.perform(get("/api/v1/liaisons/" + liaison + "/perimetre").with(utilisateur(GESTIONNAIRE, OUVREUR))).andExpect(jsonPath("$.services.length()").value(4));
    }

    @Test
    @DisplayName("Refus : service non souscrit (422), périmètre sans service à certifier (422), liste vide (400)")
    void refus() throws Exception {
        mvc
            .perform(post("/api/v1/liaisons").with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("[\"W2W\",\"BILL\"]")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_PERIMETRE_SERVICE_NON_SOUSCRIT"));
        mvc
            .perform(post("/api/v1/liaisons").with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("[\"TRANSVERSE\"]")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_PERIMETRE_VIDE"));
        mvc.perform(post("/api/v1/liaisons").with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("[]"))).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from liaison where institution_id = ?::uuid", Integer.class, institution.toString())).isZero();
    }
}
