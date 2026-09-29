package cm.gimac.des.participant.cucumber.stepdefs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.participant.AbstractParticipantIT;
import cm.gimac.des.participant.RelaisOutboxDeTest;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Et;
import io.cucumber.java.fr.Quand;
import io.cucumber.java.fr.Étantdonné;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Scénarios Gherkin de la DDRM (PAR-01 à PAR-03, D-57), exécutés par MockMvc sur l'application complète. */
public class DdrmStepDefs extends StepDefs {

    /**
     * Permissions des profils Keycloak utiles à ces scénarios, reprises de
     * infra/keycloak/generer-realm-gimac.py (le jeton simulé porte les permissions, pas le rôle).
     */
    private static final Map<String, List<String>> PROFILS = Map.of(
        "CHARGE_DDRM",
        List.of("PAR_INSTITUTION_READ", "PAR_INSTITUTION_CREATE", "PAR_INSTITUTION_EDIT", "PAR_ADHESION_INSTRUCT", "PAR_TRANSITION_REQUEST"),
        "CHEF_DDRM",
        List.of("PAR_INSTITUTION_READ", "PAR_INSTITUTION_EDIT", "PAR_ADHESION_INSTRUCT", "PAR_TRANSITION_REQUEST", "PAR_TRANSITION_APPROVE"),
        "SNA",
        List.of("PAR_INSTITUTION_READ", "HOM_LIAISON_READ")
    );

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final RelaisOutboxDeTest relais;

    private final UUID chargeId = UUID.randomUUID();
    private final UUID chefId = UUID.randomUUID();
    private RequestPostProcessor agent;
    private String institutionId;
    private String transitionId;

    public DdrmStepDefs(MockMvc mvc, JdbcTemplate jdbc, RelaisOutboxDeTest relais) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.relais = relais;
    }

    private RequestPostProcessor profil(String nom, UUID id) {
        List<String> permissions = PROFILS.get(nom);
        assertThat(permissions).as("profil %s", nom).isNotNull();
        return AbstractParticipantIT.utilisateur(id, permissions.toArray(String[]::new));
    }

    private RequestPostProcessor charge() {
        return profil("CHARGE_DDRM", chargeId);
    }

    private RequestPostProcessor chef() {
        return profil("CHEF_DDRM", chefId);
    }

    private static String unique(String prefixe) {
        return prefixe + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // ------------------------------------------------------------------ PAR-01

    @Étantdonné("un agent authentifié à deux facteurs avec le profil {string}")
    public void agentAvecProfil(String nom) {
        agent = profil(nom, nom.equals("CHEF_DDRM") ? chefId : chargeId);
    }

    @Quand("il enregistre la prise de contact de {string} de catégorie {string}")
    public void enregistrePriseDeContact(String nom, String categorie) throws Exception {
        enregistrer(agent, nom, categorie, null);
    }

    @Alors("la fiche est créée à l'état {string} avec son dossier de pièces attendues")
    public void ficheCreee(String etat) throws Exception {
        actions.andExpect(status().isCreated()).andExpect(jsonPath("$.statut").value(etat));
        institutionId = JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.id");
        mvc
            .perform(get("/api/v1/institutions/{id}", institutionId).with(agent))
            .andExpect(jsonPath("$.pieces[*].etat").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ATTENDUE"))));
    }

    @Et("l'événement {string} est émis sur le sujet {string}")
    public void evenementEmis(String type, String sujet) throws Exception {
        assertThat(
            jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and event_type = ?", Integer.class, institutionId, type)
        ).isEqualTo(1);
        relais.relayerJusquaEnvoi(institutionId);
        assertThat(relais.lire(sujet, institutionId)).anySatisfy(r -> {
            assertThat(JsonPath.<String>read(r.value(), "$.specversion")).isEqualTo("1.0");
            assertThat(JsonPath.<String>read(r.value(), "$.source")).isNotBlank();
        });
    }

    @Étantdonné("une institution enregistrée avec le code d'identification {string}")
    public void institutionAvecCode(String bic) throws Exception {
        enregistrer(charge(), "Première " + bic, "BANQUE", bic);
        actions.andExpect(status().isCreated());
    }

    @Quand("il enregistre une autre institution avec le code d'identification {string}")
    public void autreInstitutionAvecCode(String bic) throws Exception {
        enregistrer(agent, "Seconde " + bic, "BANQUE", bic);
    }

    @Alors("la réponse a le statut {int} et le code {string}")
    public void statutEtCode(int statut, String code) throws Exception {
        actions.andExpect(status().is(statut)).andExpect(jsonPath("$.code").value(code));
    }

    @Alors("la réponse a le statut {int}")
    public void statut(int statut) throws Exception {
        actions.andExpect(status().is(statut));
    }

    // ------------------------------------------------------------------ adhésion (D-57)

    @Étantdonné("un candidat {string} enregistré par la DDRM")
    public void candidatEnregistre(String nom) throws Exception {
        enregistrer(charge(), nom, "BANQUE", null);
        actions.andExpect(status().isCreated());
        institutionId = JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Étantdonné("toutes ses pièces obligatoires déposées et validées")
    public void piecesValidees() throws Exception {
        List<Map<String, Object>> pieces = JsonPath.read(
            mvc.perform(get("/api/v1/institutions/{id}", institutionId).with(charge())).andReturn().getResponse().getContentAsString(),
            "$.pieces[?(@.obligatoire == true)]"
        );
        for (Map<String, Object> p : pieces) {
            String code = (String) p.get("code");
            String depot = "{\"documentId\":\"" + UUID.randomUUID() + "\",\"documentSha256\":\"" + "e".repeat(64) + "\"}";
            mvc
                .perform(
                    post("/api/v1/institutions/{id}/pieces/{code}/depot", institutionId, code)
                        .with(charge())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depot)
                )
                .andExpect(status().isOk());
            mvc.perform(post("/api/v1/institutions/{id}/pieces/{code}/validation", institutionId, code).with(chef())).andExpect(status().isOk());
        }
    }

    @Et("ses conditions d'adhésion directes arrêtées avec le code participant {string}")
    public void conditionsDirectes(String code) throws Exception {
        String codeUnique = code + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        mvc
            .perform(
                put("/api/v1/institutions/{id}/conditions-adhesion", institutionId)
                    .with(charge())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"codeParticipant\":\"" + codeUnique + "\",\"mode\":\"DIRECT\",\"compteReglement\":\"BEAC-" + codeUnique + "\"}")
            )
            .andExpect(status().isOk());
    }

    @Quand("le chargé de la DDRM propose l'adhésion avec le motif {string}")
    public void proposeAdhesion(String motif) throws Exception {
        proposer("ADHERENT", motif);
    }

    @Quand("le chargé de la DDRM propose la clôture sans suite avec le motif {string}")
    public void proposeCloture(String motif) throws Exception {
        proposer("CLOS_SANS_SUITE", motif);
        actions.andExpect(status().isCreated());
    }

    @Et("le chef de la DDRM approuve la proposition après réauthentification forte")
    public void chefApprouve() throws Exception {
        actions = mvc.perform(
            post("/api/v1/institutions/{id}/transitions/{t}/approbation", institutionId, transitionId).with(chef()).header("X-Reauth-Assertion", "assertion")
        );
        actions.andExpect(status().isOk());
    }

    @Et("le chargé de la DDRM tente d'approuver sa propre proposition")
    public void chargeApprouve() throws Exception {
        actions = mvc.perform(
            post("/api/v1/institutions/{id}/transitions/{t}/approbation", institutionId, transitionId)
                .with(AbstractParticipantIT.utilisateur(chargeId, "PAR_TRANSITION_APPROVE"))
                .header("X-Reauth-Assertion", "assertion")
        );
    }

    @Et("le chef de la DDRM rejette la proposition avec le motif {string}")
    public void chefRejette(String motif) throws Exception {
        mvc
            .perform(
                post("/api/v1/institutions/{id}/transitions/{t}/rejet", institutionId, transitionId)
                    .with(chef())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motif\":\"" + motif + "\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("REJETEE"));
    }

    @Alors("l'institution est à l'état {string}")
    public void etatInstitution(String etat) throws Exception {
        mvc.perform(get("/api/v1/institutions/{id}", institutionId).with(chef())).andExpect(jsonPath("$.institution.statut").value(etat));
    }

    @Et("la réponse restitue le prérequis manquant {string}")
    public void prerequisManquant(String code) throws Exception {
        actions.andExpect(jsonPath("$.details.manquants[*].code").value(org.hamcrest.Matchers.hasItem(code)));
    }

    // ------------------------------------------------------------------ outils

    private void enregistrer(RequestPostProcessor qui, String nom, String categorie, String bic) throws Exception {
        String corps =
            "{\"nom\":\"" +
            nom +
            "\",\"pays\":\"CM\",\"categorie\":\"" +
            categorie +
            "\",\"codeIdentification\":" +
            (bic == null ? "null" : "\"" + bic + "\"") +
            ",\"codeBanqueBeac\":\"" +
            unique("B") +
            "\"}";
        actions = mvc.perform(post("/api/v1/institutions").with(qui).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private void proposer(String vers, String motif) throws Exception {
        actions = mvc.perform(
            post("/api/v1/institutions/{id}/transitions", institutionId)
                .with(charge())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vers\":\"" + vers + "\",\"motif\":\"" + motif + "\"}")
        );
        String reponse = actions.andReturn().getResponse().getContentAsString();
        if (actions.andReturn().getResponse().getStatus() == 201) {
            transitionId = JsonPath.read(reponse, "$.id");
        }
    }
}
