package cm.gimac.des.homologation.cucumber.stepdefs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import cm.gimac.des.homologation.HomologationFixtures;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Scénarios Gherkin du cadrage (étape 3, D-57, D-58), exécutés par MockMvc sur l'application complète. */
public class CadrageStepDefs {

    /** Permissions des profils du realm (infra/keycloak/generer-realm-gimac.py). */
    private static final String[] CHEF_DE_DEPARTEMENT = { "HOM_LIAISON_LIST", "HOM_LIAISON_READ", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE" };
    private static final String[] INGENIEUR = { "HOM_LIAISON_LIST", "HOM_LIAISON_READ", "HOM_LIAISON_OPEN" };
    private static final Map<String, UUID> CONDUCTEURS = Map.of("Aminata Ngono", CollaboratorStubAdapter.CONDUCTEUR_1, "Paul Essomba", CollaboratorStubAdapter.CONDUCTEUR_2);

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final HomologationFixtures fixtures;
    private final ParticipantStubAdapter registre;

    private TypeProjet type;
    private RequestPostProcessor chef;
    private UUID institution;
    private UUID conducteur = CollaboratorStubAdapter.CONDUCTEUR_1;
    private String liaisonId;
    private String referenceOrigine;
    private ResultActions reponse;

    public CadrageStepDefs(MockMvc mvc, JdbcTemplate jdbc, HomologationFixtures fixtures, ParticipantStubAdapter registre) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.fixtures = fixtures;
        this.registre = registre;
    }

    private static RequestPostProcessor utilisateur(UUID id, String... permissions) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("acr", "2")).authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    private String corps() throws Exception {
        return reponse.andReturn().getResponse().getContentAsString();
    }

    private UUID institution(String nom, String statut, String... services) {
        UUID id = UUID.randomUUID();
        registre.enregistrer(new InstitutionView(id, nom, statut, "ETABLISSEMENT_PAIEMENT", Arrays.stream(services).map(s -> new InstitutionView.ServiceSouscrit(s, "2026-09-01")).toList()));
        return id;
    }

    private ResultActions ouvrir(RequestPostProcessor qui, UUID lui) throws Exception {
        return ouvrir(qui, lui, null);
    }

    private ResultActions ouvrir(RequestPostProcessor qui, UUID lui, String intitule) throws Exception {
        String json = "{\"institutionId\":\"%s\",\"typeProjet\":\"%s\",\"chefDeProjetId\":\"%s\"%s%s}".formatted(
            institution,
            type,
            lui,
            intitule == null ? "" : ",\"intituleProjet\":\"" + intitule + "\"",
            referenceOrigine == null ? "" : ",\"referenceOrigine\":\"" + referenceOrigine + "\""
        );
        return mvc.perform(post("/api/v1/liaisons").with(qui).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @io.cucumber.java.After
    public void effacer() {
        cm.gimac.des.homologation.AbstractHomologationIT.effacer(jdbc);
    }

    @Étantdonné("une version publiée du référentiel de cadrage {string}")
    public void referentiel(String t) {
        type = TypeProjet.valueOf(t);
        fixtures.referentielPublie(type);
    }

    @Et("un chef de département authentifié à deux facteurs")
    public void chefDeDepartement() {
        chef = utilisateur(UUID.randomUUID(), CHEF_DE_DEPARTEMENT);
    }

    @Étantdonné("l'institution {string} à l'état {string} qui a souscrit {string}")
    public void institutionAvecService(String nom, String statut, String service) {
        institution = institution(nom, statut, service);
    }

    @Quand("le chef de département ouvre une liaison {string} en désignant le conducteur {string}")
    public void ouvre(String t, String nom) throws Exception {
        reponse = ouvrir(chef, CONDUCTEURS.get(nom));
        liaisonId = JsonPath.read(corps(), "$.id");
    }

    @Et("le chef de département a ouvert une liaison {string} en désignant le conducteur {string}")
    public void aOuvert(String t, String nom) throws Exception {
        ouvre(t, nom);
    }

    @Alors("la liaison est créée à l'état {string} en phase {string}")
    public void creee(String etat, String phase) throws Exception {
        assertThat(reponse.andReturn().getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonPath.<String>read(corps(), "$.etat")).isEqualTo(etat);
        assertThat(JsonPath.<String>read(corps(), "$.phaseCourante")).isEqualTo(phase);
    }

    @Et("son périmètre contractualisé comprend {string} et {string}")
    public void perimetre(String a, String b) throws Exception {
        assertThat(JsonPath.<List<String>>read(corps(), "$.perimetre")).containsExactlyInAnyOrder(a, b);
    }

    @Et("l'événement {string} est prêt pour MS-PARTICIPANT")
    public void evenement(String type) {
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ?", String.class, liaisonId)).contains(type);
    }

    @Alors("la conductrice {string} voit la liaison parmi ses projets")
    public void voit(String nom) throws Exception {
        assertThat(mesProjets(nom)).contains(liaisonId);
    }

    @Et("le conducteur {string} ne la voit pas parmi ses projets")
    public void neVoitPas(String nom) throws Exception {
        assertThat(mesProjets(nom)).doesNotContain(liaisonId);
    }

    private List<String> mesProjets(String nom) throws Exception {
        String r = mvc
            .perform(get("/api/v1/liaisons").param("mesProjets", "true").with(utilisateur(CONDUCTEURS.get(nom), "HOM_LIAISON_LIST")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(r, "$.contenu[*].id");
    }

    @Quand("le chef de département désigne le conducteur {string} avec le motif {string}")
    public void designe(String nom, String motif) throws Exception {
        reponse = mvc.perform(
            put("/api/v1/liaisons/{id}/chef-de-projet", liaisonId)
                .with(chef)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"chefDeProjetId\":\"" + CONDUCTEURS.get(nom) + "\",\"motif\":\"" + motif + "\"}")
        );
    }

    @Alors("le conducteur de la liaison est {string}")
    public void conducteurEst(String nom) throws Exception {
        assertThat(JsonPath.<String>read(corps(), "$.chefDeProjetNom")).isEqualTo(nom);
    }

    @Étantdonné("une institution à l'état {string}")
    public void institutionEtat(String statut) {
        institution = institution("Institution de recette", statut);
    }

    @Quand("le chef de département ouvre une liaison {string} intitulée {string} en désignant le conducteur {string}")
    public void ouvreIntitulee(String t, String intitule, String nom) throws Exception {
        reponse = ouvrir(chef, CONDUCTEURS.get(nom), intitule);
        liaisonId = JsonPath.read(corps(), "$.id");
    }

    @Et("son intitulé est {string}")
    public void intitule(String intitule) throws Exception {
        assertThat(JsonPath.<String>read(corps(), "$.intituleProjet")).isEqualTo(intitule);
    }

    @Et("l'institution compte {int} liaisons actives")
    public void liaisonsActives(int n) throws Exception {
        String r = mvc
            .perform(get("/api/v1/liaisons").param("institutionId", institution.toString()).param("actives", "true").with(chef))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(JsonPath.<Integer>read(r, "$.total")).isEqualTo(n);
    }

    @Étantdonné("un adhérent dont une liaison a été reprise sous la référence d'origine {string}")
    public void dejaReprise(String reference) throws Exception {
        institution = institution("Adhérent repris", "ADHERENT");
        referenceOrigine = reference;
        assertThat(ouvrir(chef, CollaboratorStubAdapter.CONDUCTEUR_1).andReturn().getResponse().getStatus()).isEqualTo(201);
    }

    @Étantdonné("un adhérent et un conducteur sans le rôle CONDUCTEUR_PROJET")
    public void conducteurNonHabilite() {
        institution = institution("Adhérent", "ADHERENT");
        conducteur = UUID.randomUUID();
    }

    @Quand("l'ouverture de la liaison est demandée")
    public void ouvertureDemandee() throws Exception {
        reponse = ouvrir(chef, conducteur);
    }

    @Quand("un ingénieur d'homologation sans droit de désignation ouvre une liaison {string}")
    public void ingenieur(String t) throws Exception {
        reponse = ouvrir(utilisateur(UUID.randomUUID(), INGENIEUR), CollaboratorStubAdapter.CONDUCTEUR_1);
    }

    @Alors("l'ouverture est refusée avec le code {string}")
    public void refusee(String code) throws Exception {
        assertThat(reponse.andReturn().getResponse().getStatus()).isIn(403, 409);
        assertThat(JsonPath.<String>read(corps(), "$.code")).isEqualTo(code);
    }
}
