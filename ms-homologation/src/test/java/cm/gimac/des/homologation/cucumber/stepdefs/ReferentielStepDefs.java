package cm.gimac.des.homologation.cucumber.stepdefs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.HomologationFixtures;
import cm.gimac.des.homologation.RelaisOutboxDeTest;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Et;
import io.cucumber.java.fr.Quand;
import io.cucumber.java.fr.Étantdonné;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Scénarios Gherkin de HOM-01 et HOM-02 (PI-GIMAC-01, sections D), exécutés par MockMvc. */
public class ReferentielStepDefs extends StepDefs {

    /**
     * Permissions des profils Keycloak utiles à ces scénarios, reprises de
     * infra/keycloak/generer-realm-gimac.py (le jeton simulé porte les permissions, pas le rôle).
     */
    private static final Map<String, List<String>> PROFILS = Map.of(
        "INGENIEUR_HOMOLOGATION",
        List.of("HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_EDIT", "HOM_REFERENTIEL_PUBLISH"),
        "CORRESPONDANT",
        List.of("HOM_PORTAL_READ", "HOM_PORTAL_SUBMIT", "CER_PORTAL_READ")
    );

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final HomologationFixtures fixtures;
    private final RelaisOutboxDeTest relais;

    private RequestPostProcessor utilisateur;
    private UUID versionId;

    public ReferentielStepDefs(MockMvc mvc, JdbcTemplate jdbc, HomologationFixtures fixtures, RelaisOutboxDeTest relais) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.fixtures = fixtures;
        this.relais = relais;
    }

    @Étantdonné("un utilisateur authentifié avec le rôle {string}")
    public void utilisateurAvecRole(String role) {
        List<String> permissions = PROFILS.get(role);
        assertThat(permissions).as("profil %s", role).isNotNull();
        utilisateur = jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()))
            .authorities(permissions.stream().map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    // ------------------------------------------------------------------ HOM-01

    @Étantdonné(
        "un brouillon de référentiel du type {string} dont chaque opération porte un code, un libellé, une phase, un segment, ses prédécesseurs, la partie à qui elle incombe, le service rattaché, la nature de couverture et la preuve attendue par catégorie de participant"
    )
    public void brouillonComplet(String type) {
        versionId = fixtures.brouillonComplet(TypeProjet.valueOf(type));
    }

    @Quand("l'ingénieur d'homologation demande la publication du brouillon")
    public void demandePublication() throws Exception {
        publier();
    }

    @Alors("une version numérotée et datée est créée à l'état {string} et devient immuable")
    public void versionPubliee(String etat) throws Exception {
        actions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value(etat))
            .andExpect(jsonPath("$.numero").isNumber())
            .andExpect(jsonPath("$.datePublication").isNotEmpty());
        modifierOperation("TRONC_COMMUN_AMONT_INEXISTANTE");
        actions.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_FROZEN"));
    }

    @Et("l'événement {string} est émis sur le sujet {string} à destination du registre d'audit")
    public void evenementEmis(String type, String sujet) throws Exception {
        assertThat(
            jdbc.queryForObject(
                "select count(*) from outbox_event where aggregate_id = ? and event_type = ?",
                Integer.class,
                versionId.toString(),
                type
            )
        ).isEqualTo(1);
        relais.relayerJusquaEnvoi(versionId.toString());
        assertThat(relais.lire(sujet, versionId.toString())).anySatisfy(r -> {
            assertThat(JsonPath.<String>read(r.value(), "$.type")).isEqualTo(type);
            assertThat(JsonPath.<String>read(r.value(), "$.specversion")).isEqualTo("1.0");
        });
    }

    @Étantdonné("une version publiée du référentiel {string}")
    public void versionPublieeDuType(String type) throws Exception {
        versionId = fixtures.brouillonComplet(TypeProjet.valueOf(type));
        publier();
        actions.andExpect(status().isOk());
    }

    @Quand("un ingénieur tente de modifier l'opération {string} de cette version")
    public void tenteModification(String code) throws Exception {
        modifierOperation(code);
    }

    @Alors("la réponse a le statut {int} et le code {string}")
    public void statutEtCode(int statut, String code) throws Exception {
        actions.andExpect(status().is(statut)).andExpect(jsonPath("$.code").value(code));
    }

    @Et("la réponse propose la création d'un brouillon dérivé de cette version")
    public void brouillonDerivePropose() throws Exception {
        actions.andExpect(jsonPath("$.details.brouillonDerivePropose.versionSourceId").value(versionId.toString()));
    }

    @Étantdonné("un brouillon dont l'opération {string} n'indique ni la partie à qui elle incombe ni la preuve attendue")
    public void brouillonIncomplet(String code) {
        versionId = fixtures.brouillonIncomplet(TypeProjet.PAIEMENT_MOBILE, code);
    }

    @Quand("la publication est demandée")
    public void publicationDemandee() throws Exception {
        publier();
    }

    @Et("la réponse restitue pour {string} les attributs manquants {string} et {string}")
    public void attributsManquants(String code, String premier, String second) throws Exception {
        actions
            .andExpect(jsonPath("$.details.operations[0].code").value(code))
            .andExpect(jsonPath("$.details.operations[0].attributs").value(contains(premier, second)));
    }

    // ------------------------------------------------------------------ HOM-02

    @Étantdonné("un brouillon dont les prédécesseurs forment un graphe orienté sans cycle")
    public void brouillonAcyclique() {
        versionId = fixtures.brouillonComplet(TypeProjet.CARTE_INTERBANCAIRE);
    }

    @Étantdonné(
        "un brouillon délégataire dont l'opération {string} du tronc commun amont déclare pour prédécesseur l'opération {string} de la branche délégataire"
    )
    public void brouillonIncoherent(String amont, String branche) {
        assertThat(List.of(amont, branche)).containsExactly("DL-AMONT-03", "DL-BR-02");
        versionId = fixtures.brouillonSegmentIncoherent();
    }

    @Étantdonné(
        "trois opérations {string}, {string} et {string} dont chacune désigne la suivante comme prédécesseur, la dernière désignant la première"
    )
    public void brouillonCyclique(String a, String b, String c) {
        versionId = fixtures.brouillonAvecCycle(TypeProjet.DELEGATAIRE, a, b, c);
    }

    @Quand("le contrôle de publication s'exécute")
    public void controlePublication() throws Exception {
        publier();
    }

    @Alors("un ordre topologique de référence est calculé et enregistré avec la version")
    public void ordreEnregistre() throws Exception {
        actions.andExpect(jsonPath("$.ordreTopologique").value(contains("CI-AMONT-01", "CI-BR-01", "CI-AVAL-01")));
        String enBase = jdbc.queryForObject(
            "select ordre_topologique::text from referential_version where id = ?",
            String.class,
            versionId
        );
        assertThat(JsonPath.<List<String>>read(enBase, "$")).containsExactly("CI-AMONT-01", "CI-BR-01", "CI-AVAL-01");
    }

    @Et("la publication se poursuit jusqu'à l'état {string}")
    public void publicationPoursuivie(String etat) throws Exception {
        actions.andExpect(status().isOk()).andExpect(jsonPath("$.etat").value(etat));
    }

    @Alors("la publication est refusée avec l'anomalie {string}")
    public void refuseeAvecAnomalie(String anomalie) throws Exception {
        refusee();
        actions.andExpect(jsonPath("$.details.anomalies[0].type").value(anomalie));
    }

    @Et("les codes {string} et {string} sont restitués")
    public void codesRestitues(String premier, String second) throws Exception {
        actions.andExpect(jsonPath("$.details.anomalies[0].codes").value(contains(premier, second)));
    }

    @Alors("la publication est refusée")
    public void refusee() throws Exception {
        actions.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_GRAPH_INVALID"));
    }

    @Et("le cycle est restitué sous la forme de la suite ordonnée {string}")
    public void cycleRestitue(String suite) throws Exception {
        String[] codes = Arrays.stream(suite.split(",")).map(String::trim).toArray(String[]::new);
        actions
            .andExpect(jsonPath("$.details.anomalies[?(@.type=='CYCLE')].codes[*]").value(contains(codes)));
    }

    // ------------------------------------------------------------------ outils

    private void publier() throws Exception {
        actions = mvc.perform(post("/api/v1/referentiels/{id}/publication", versionId).with(utilisateur));
    }

    private void modifierOperation(String code) throws Exception {
        actions = mvc.perform(
            put("/api/v1/referentiels/{id}/operations/{code}", versionId, code)
                .with(utilisateur)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fixtures.operationJson(code))
        );
    }
}
