package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.application.usecase.ParcoursDepot;
import cm.gimac.des.homologation.contrat.SchemaContrat;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * D-133 sur PostgreSQL réel : le périmètre du projet de certification est modifié par son gestionnaire (créateur de la
 * liaison), un super administrateur ou un chargé de l'implémentation des projets de la SNA ; chaque modification est
 * journalisée et publiée ; tout autre acteur est refusé, un test par refus.
 */
class PerimetreLiaisonIT extends AbstractHomologationIT {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final UUID GESTIONNAIRE = UUID.randomUUID();
    static final String[] OUVREUR = { "HOM_LIAISON_LIST", "HOM_LIAISON_READ", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE", "HOM_PERIMETRE_EDIT" };

    @Autowired
    ParticipantStubAdapter registre;

    @Autowired
    ParcoursDepot depot;

    @Autowired
    TransactionTemplate transaction;

    String liaison;

    static String corps(String motif, String... services) {
        StringBuilder s = new StringBuilder("{\"services\":[");
        for (int i = 0; i < services.length; i++) {
            s.append(i == 0 ? "" : ",").append('"').append(services[i]).append('"');
        }
        s.append(']');
        if (motif != null) {
            s.append(",\"motif\":\"").append(motif).append('"');
        }
        return s.append('}').toString();
    }

    @BeforeEach
    void ouvrir() throws Exception {
        fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        UUID institution = UUID.randomUUID();
        registre.enregistrer(
            new InstitutionView(
                institution,
                "Institution 99001",
                "ADHERENT",
                "ETABLISSEMENT_PAIEMENT",
                List.of(new InstitutionView.ServiceSouscrit("PAIEMENT_MOBILE", "2026-09-01"), new InstitutionView.ServiceSouscrit("TRANSFERT_COMPTE_A_COMPTE", "2026-09-01"))
            ),
            "99001"
        );
        String r = mvc
            .perform(
                post("/api/v1/liaisons")
                    .with(utilisateur(GESTIONNAIRE, OUVREUR))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"institutionId\":\"%s\",\"typeProjet\":\"PAIEMENT_MOBILE\",\"chefDeProjetId\":\"%s\"}".formatted(institution, CollaboratorStubAdapter.CONDUCTEUR_1))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        liaison = JsonPath.read(r, "$.id");
    }

    String chemin() {
        return "/api/v1/liaisons/" + liaison + "/perimetre";
    }

    @Test
    @DisplayName("Le créateur est le gestionnaire : périmètre dérivé en version 1, modifiable par lui seul")
    void gestionnaireALOuverture() throws Exception {
        assertThat(jdbc.queryForObject("select gestionnaire_id::text from liaison where id = ?::uuid", String.class, liaison)).isEqualTo(GESTIONNAIRE.toString());
        mvc
            .perform(get(chemin()).with(utilisateur(GESTIONNAIRE, OUVREUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.gestionnaireId").value(GESTIONNAIRE.toString()))
            .andExpect(jsonPath("$.modifiable").value(true))
            .andExpect(jsonPath("$.services.length()").value(3))
            .andExpect(jsonPath("$.proposables").value(org.hamcrest.Matchers.containsInAnyOrder("TRANSVERSE", "PAIEMENT_MOBILE", "TRANSFERT_COMPTE_A_COMPTE")));
        mvc.perform(get(chemin()).with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$.modifiable").value(false)).andExpect(jsonPath("$.proposables.length()").value(0));
    }

    @Test
    @DisplayName("Modification par le gestionnaire : version 2, journal avant et après, parcours et événement conforme")
    void modificationParLeGestionnaire() throws Exception {
        mvc
            .perform(put(chemin()).with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("Lot 1 limité au paiement mobile", "PAIEMENT_MOBILE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.services[?(@.code == 'TRANSFERT_COMPTE_A_COMPTE')].etat").value(org.hamcrest.Matchers.contains("HORS_PERIMETRE")));
        assertThat(jdbc.queryForObject("select inclus from parcours_service where liaison_id = ?::uuid and service_code = 'TRANSFERT_COMPTE_A_COMPTE'", Boolean.class, liaison)).isFalse();
        mvc
            .perform(get(chemin() + "/historique").with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].version").value(2))
            .andExpect(jsonPath("$.contenu[0].auteurId").value(GESTIONNAIRE.toString()))
            .andExpect(jsonPath("$.contenu[0].permissionUtilisee").value("HOM_PERIMETRE_EDIT"))
            .andExpect(jsonPath("$.contenu[0].avant").value(org.hamcrest.Matchers.contains("PAIEMENT_MOBILE", "TRANSFERT_COMPTE_A_COMPTE", "TRANSVERSE")))
            .andExpect(jsonPath("$.contenu[0].apres").value(org.hamcrest.Matchers.contains("PAIEMENT_MOBILE", "TRANSVERSE")))
            .andExpect(jsonPath("$.contenu[0].motif").value("Lot 1 limité au paiement mobile"));
        String charge = jdbc.queryForObject("select payload::text from outbox_event where aggregate_id = ? and event_type = 'homologation.liaison-scope-changed.v1'", String.class, liaison);
        assertThat(SchemaContrat.charger("sandbox/homologation.liaison-scope-changed.v1.schema.json").ecarts(JSON.readTree(charge))).isEmpty();
        assertThat(JsonPath.<String>read(charge, "$.codeParticipant")).isEqualTo("99001");
        assertThat(JsonPath.<List<String>>read(charge, "$.servicesRetires")).containsExactly("TRANSFERT_COMPTE_A_COMPTE");

        // Même périmètre : ni version ni événement nouveaux.
        mvc
            .perform(put(chemin()).with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("Sans changement", "PAIEMENT_MOBILE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(2));
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and event_type = 'homologation.liaison-scope-changed.v1'", Integer.class, liaison)).isEqualTo(1);

        // Retour du service retiré : de nouveau inclus, version 3.
        mvc
            .perform(put(chemin()).with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("Retour du transfert", "PAIEMENT_MOBILE", "TRANSFERT_COMPTE_A_COMPTE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(3));
        assertThat(jdbc.queryForObject("select inclus from parcours_service where liaison_id = ?::uuid and service_code = 'TRANSFERT_COMPTE_A_COMPTE'", Boolean.class, liaison)).isTrue();
    }

    @Test
    @DisplayName("Super administrateur et chargé de l'implémentation SNA : HOM_PERIMETRE_EDIT_TOUS sur tout projet, tracé")
    void editionTous() throws Exception {
        UUID superAdmin = UUID.randomUUID();
        mvc
            .perform(put(chemin()).with(utilisateur(superAdmin, "HOM_LIAISON_READ", "HOM_PERIMETRE_EDIT_TOUS")).contentType(MediaType.APPLICATION_JSON).content(corps("Arbitrage", "TRANSFERT_COMPTE_A_COMPTE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(2));
        assertThat(jdbc.queryForObject("select permission_utilisee from perimetre_modification where liaison_id = ?::uuid", String.class, liaison)).isEqualTo("HOM_PERIMETRE_EDIT_TOUS");
        assertThat(jdbc.queryForObject("select auteur_id::text from perimetre_modification where liaison_id = ?::uuid", String.class, liaison)).isEqualTo(superAdmin.toString());
    }

    @Test
    @DisplayName("Refus : HOM_PERIMETRE_EDIT sans être le gestionnaire du projet (403)")
    void refusAutreGestionnaire() throws Exception {
        mvc
            .perform(put(chemin()).with(utilisateur(UUID.randomUUID(), OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("Tentative", "PAIEMENT_MOBILE")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("HOM_PERIMETRE_GESTIONNAIRE_SEUL"));
        assertThat(jdbc.queryForObject("select count(*) from perimetre_modification where liaison_id = ?::uuid", Integer.class, liaison)).isZero();
    }

    @Test
    @DisplayName("Refus : autre agent du GIMAC sans permission de périmètre, SNA ou conducteur (403)")
    void refusAgentSansPermission() throws Exception {
        mvc
            .perform(put(chemin()).with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_READ", "HOM_OPERATION_VALIDATE", "HOM_OPERATION_ASSIGN")).contentType(MediaType.APPLICATION_JSON).content(corps("Tentative", "PAIEMENT_MOBILE")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Refus : compte d'institution, administrateur ou intégrateur, même porteur d'une permission de périmètre (403)")
    void refusCompteInstitution() throws Exception {
        RequestPostProcessor institution = jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("acr", "2").claim("institution_id", UUID.randomUUID().toString()))
            .authorities(new SimpleGrantedAuthority("HOM_PERIMETRE_EDIT_TOUS"), new SimpleGrantedAuthority("HOM_PORTAL_READ"));
        mvc
            .perform(put(chemin()).with(institution).contentType(MediaType.APPLICATION_JSON).content(corps("Tentative", "PAIEMENT_MOBILE")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("HOM_PERIMETRE_GESTIONNAIRE_SEUL"));
        RequestPostProcessor portail = jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("acr", "2").claim("institution_id", UUID.randomUUID().toString()))
            .authorities(new SimpleGrantedAuthority("HOM_PORTAL_READ"), new SimpleGrantedAuthority("SBX_HAB_ADMINISTRATION"), new SimpleGrantedAuthority("SBX_RAC_ADMINISTRATION"));
        mvc.perform(put(chemin()).with(portail).contentType(MediaType.APPLICATION_JSON).content(corps("Tentative", "PAIEMENT_MOBILE"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Refus : service non souscrit (422), motif absent (422), périmètre vide (422), projet clos (409)")
    void refusMetier() throws Exception {
        mvc
            .perform(put(chemin()).with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("Ajout", "PAIEMENT_MOBILE", "RETRAIT_DAB")))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("HOM_PERIMETRE_SERVICE_NON_SOUSCRIT"));
        mvc
            .perform(put(chemin()).with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps(null, "PAIEMENT_MOBILE")))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("HOM_PERIMETRE_MOTIF_REQUIS"));
        mvc
            .perform(put(chemin()).with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("Vide", "TRANSVERSE")))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("HOM_PERIMETRE_VIDE"));
        transaction.executeWithoutResult(t -> depot.trouver(UUID.fromString(liaison), true).orElseThrow().liaison().setEtat(EtatLiaison.CLOSE));
        mvc
            .perform(put(chemin()).with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("Clos", "PAIEMENT_MOBILE")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_PERIMETRE_LIAISON_CLOSE"));
        assertThat(jdbc.queryForObject("select count(*) from perimetre_modification where liaison_id = ?::uuid", Integer.class, liaison)).isZero();
    }

    @Test
    @DisplayName("D-133 : la référence d'origine de la liaison est publiée pour la sandbox avec son état")
    void referenceOriginePubliee() throws Exception {
        UUID institution = UUID.randomUUID();
        registre.enregistrer(new InstitutionView(institution, "Institution 99002", "ADHERENT", "ETABLISSEMENT_PAIEMENT", List.of(new InstitutionView.ServiceSouscrit("PAIEMENT_MOBILE", "2026-09-01"))), "99002");
        String r = mvc
            .perform(
                post("/api/v1/liaisons")
                    .with(utilisateur(GESTIONNAIRE, OUVREUR))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"institutionId\":\"%s\",\"typeProjet\":\"PAIEMENT_MOBILE\",\"chefDeProjetId\":\"%s\",\"referenceOrigine\":\"BGFI-GB-2510-01\"}".formatted(institution, CollaboratorStubAdapter.CONDUCTEUR_1))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String id = JsonPath.read(r, "$.id");
        String charge = jdbc.queryForObject("select payload::text from outbox_event where aggregate_id = ? and event_type = 'homologation.liaison-status-changed.v1'", String.class, id);
        assertThat(JsonPath.<String>read(charge, "$.referenceOrigine")).isEqualTo("BGFI-GB-2510-01");
        assertThat(SchemaContrat.charger("sandbox/homologation.liaison-status-changed.v1.schema.json").ecarts(JSON.readTree(charge))).isEmpty();
    }

    @Test
    @DisplayName("Liaison inconnue : 404")
    void inconnue() throws Exception {
        mvc.perform(get("/api/v1/liaisons/" + UUID.randomUUID() + "/perimetre").with(utilisateur(GESTIONNAIRE, OUVREUR))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/liaisons/" + UUID.randomUUID() + "/perimetre/historique").with(utilisateur(GESTIONNAIRE, OUVREUR))).andExpect(status().isNotFound());
        mvc
            .perform(put("/api/v1/liaisons/" + UUID.randomUUID() + "/perimetre").with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps("x", "PAIEMENT_MOBILE")))
            .andExpect(status().isNotFound());
    }
}
