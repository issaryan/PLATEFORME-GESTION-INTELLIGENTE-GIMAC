package cm.gimac.des.homologation.cucumber.stepdefs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import cm.gimac.des.homologation.HomologationFixtures;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import cm.gimac.des.homologation.infrastructure.messaging.consumer.EvenementsCertificationConsumer;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Et;
import io.cucumber.java.fr.Quand;
import io.cucumber.java.fr.Étantdonné;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Scénarios Gherkin du parcours (étapes 4 à 10), exécutés par MockMvc sur l'application complète. */
public class ParcoursStepDefs {

    private static final UUID CHEF_DES = UUID.fromString("00000000-0000-4000-8000-0000000e0001");
    private static final UUID SNA = UUID.fromString("00000000-0000-4000-8000-0000000e0002");
    private static final UUID DDRM = UUID.fromString("00000000-0000-4000-8000-0000000e0003");
    private static final UUID VALIDATEUR = UUID.fromString("00000000-0000-4000-8000-0000000e0004");
    private static final String SHA = "a".repeat(64);
    private static final String[] COMMUN = { "HOM_LIAISON_LIST", "HOM_LIAISON_READ", "HOM_DASHBOARD_READ" };

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final HomologationFixtures fixtures;
    private final ParticipantStubAdapter registre;
    private final CollaboratorStubAdapter annuaire;
    private final EvenementsCertificationConsumer consommateur;

    private UUID institution;
    private String liaison;
    private String blocage;
    private MvcResult reponse;

    public ParcoursStepDefs(
        MockMvc mvc,
        JdbcTemplate jdbc,
        HomologationFixtures fixtures,
        ParticipantStubAdapter registre,
        CollaboratorStubAdapter annuaire,
        EvenementsCertificationConsumer consommateur
    ) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.fixtures = fixtures;
        this.registre = registre;
        this.annuaire = annuaire;
        this.consommateur = consommateur;
    }

    @io.cucumber.java.After
    public void effacer() {
        cm.gimac.des.homologation.AbstractHomologationIT.effacer(jdbc);
    }

    // ------------------------------------------------------------------ jetons des acteurs (profil, permissions, second facteur)

    private static RequestPostProcessor utilisateur(UUID id, String... permissions) {
        String[] toutes = Arrays.copyOf(COMMUN, COMMUN.length + permissions.length);
        System.arraycopy(permissions, 0, toutes, COMMUN.length, permissions.length);
        return jwt().jwt(j -> j.subject(id.toString()).claim("acr", "2")).authorities(Arrays.stream(toutes).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    private static RequestPostProcessor chefDes() {
        return utilisateur(CHEF_DES, "ROLE_CHEF_DES", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE", "HOM_STEP_MANAGE", "HOM_OPERATION_VALIDATE");
    }

    private static RequestPostProcessor conducteur() {
        return utilisateur(CollaboratorStubAdapter.CONDUCTEUR_1, "ROLE_CONDUCTEUR_PROJET", "HOM_OPERATION_ASSIGN", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "HOM_STEP_MANAGE");
    }

    private static RequestPostProcessor sna() {
        return utilisateur(SNA, "ROLE_SNA", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "CER_DOSSIER_VERIFY");
    }

    private static RequestPostProcessor ddrm() {
        return utilisateur(DDRM, "ROLE_CHARGE_DDRM", "HOM_STEP_MANAGE", "HOM_EVIDENCE_SUBMIT");
    }

    private static RequestPostProcessor validateur() {
        return utilisateur(VALIDATEUR, "ROLE_CHEF_DES", "HOM_OPERATION_VALIDATE");
    }

    private static RequestPostProcessor correspondant(UUID inst) {
        return jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("acr", "2").claim("institution_id", inst.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_CORRESPONDANT"), new SimpleGrantedAuthority("HOM_PORTAL_READ"), new SimpleGrantedAuthority("HOM_PORTAL_SUBMIT"));
    }

    // ------------------------------------------------------------------ outils

    private String lire(String uri, RequestPostProcessor qui) throws Exception {
        return mvc.perform(get(uri).with(qui)).andReturn().getResponse().getContentAsString();
    }

    private List<Map<String, Object>> operations() throws Exception {
        return JsonPath.read(lire("/api/v1/liaisons/" + liaison + "/parcours/operations", conducteur()), "$");
    }

    /**
     * Validateur admis par le RACI imposé (D-93 §2) : le V désigné (conducteur ou ingénieur SNA), sinon un porteur de la
     * fonction V à affecter (chef du DES).
     */
    private RequestPostProcessor validateurPour(String opId) throws Exception {
        Object v = operations().stream().filter(o -> opId.equals(o.get("id"))).findFirst().map(o -> JsonPath.read(o, "$.raci.V.id")).orElse(null);
        if (CollaboratorStubAdapter.CONDUCTEUR_1.toString().equals(v)) {
            return conducteur();
        }
        return SNA.toString().equals(v) ? sna() : validateur();
    }

    private Map<String, Object> operation(String rang) throws Exception {
        return operations().stream().filter(o -> rang.equals(o.get("rang"))).findFirst().orElseThrow();
    }

    private String opId(String rang) throws Exception {
        return (String) operation(rang).get("id");
    }

    private static String piece(String complement) {
        return "{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"piece.pdf\"" + (complement.isEmpty() ? "" : "," + complement) + "}";
    }

    private int statut(MvcResult r) {
        return r.getResponse().getStatus();
    }

    private MvcResult livrable(String opId, RequestPostProcessor qui) throws Exception {
        return mvc.perform(post("/api/v1/parcours/operations/" + opId + "/livrable").with(qui).contentType(MediaType.APPLICATION_JSON).content(piece(""))).andReturn();
    }

    private MvcResult preuve(String opId, String complement) throws Exception {
        return mvc.perform(post("/api/v1/parcours/operations/" + opId + "/preuves").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content(piece(complement))).andReturn();
    }

    private MvcResult valider(String opId, RequestPostProcessor qui, boolean assertion) throws Exception {
        var r = post("/api/v1/parcours/operations/" + opId + "/validation").with(qui);
        if (assertion) {
            r.header("X-Reauth-Assertion", "YXNzZXJ0aW9u");
        }
        return mvc.perform(r).andReturn();
    }

    private void evenement(String type, String data) {
        consommateur.recevoir("{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"" + type + "\",\"data\":{\"liaisonId\":\"" + liaison + "\"," + data + "}}");
    }

    /** Fait progresser la liaison jusqu'à l'opération {@code avant} exclue, par le geste de chaque opération. */
    private void avancer(String avant) throws Exception {
        for (int garde = 0; garde < 120; garde++) {
            Map<String, Object> o = operations().stream().filter(x -> !"FAITE".equals(x.get("etat"))).findFirst().orElseThrow();
            String rang = (String) o.get("rang");
            if (avant.equals(rang)) {
                return;
            }
            String id = (String) o.get("id");
            switch (rang) {
                case "6" -> leChefDuDesAffecteLesRessources();
                case "12" -> leParticipantCommuniqueLesCoordonnees("essai.exemple", "8443");
                case "15" -> leParticipantDeposeLeDocumentDeParametrage();
                case "17" -> lIngenieurConstateLaCouverture();
                case "18" -> evenement("certification.pv-established.v1", "\"etabliPar\":\"Conducteur\",\"empreinte\":\"" + SHA + "\"");
                case "19" -> evenement("certification.pv-signed.v1", "\"signatures\":[{\"fonction\":\"CHEF_DES\",\"signataire\":\"Chef\"},{\"fonction\":\"PARTICIPANT\",\"signataire\":\"Représentant\"}],\"empreinte\":\"" + SHA + "\"");
                case "20" -> evenement("certification.change-record-constituted.v1", "\"documentRef\":\"CHG\",\"nom\":\"dossier.pdf\",\"empreinte\":\"" + SHA + "\"");
                case "21", "22" -> evenement("certification.attestations-archived.v1", "\"attestations\":[{\"type\":\"PCI_DSS\",\"documentRef\":\"ATT\"}]");
                case "23", "24", "25" -> evenement("certification.security-approved.v1", "\"approuveeLe\":\"2026-10-06\"");
                case "26" -> evenement("certification.change-record-referenced.v1", "\"reference\":\"CHG-2026-0412\"");
                default -> {
                    if (Boolean.TRUE.equals(o.get("valeurReference"))) {
                        preuve(id, "\"valeurReference\":\"REF-" + rang + "\"");
                        valider(id, validateurPour(id), true);
                    } else {
                        livrable(id, conducteur());
                    }
                }
            }
        }
        throw new AssertionError("Parcours bloqué avant " + avant);
    }

    // ------------------------------------------------------------------ étapes

    @Étantdonné("une liaison {string} ouverte au cadrage pour l'adhérent {string} qui a souscrit {string} et {string}")
    public void uneLiaisonOuverte(String type, String nom, String s1, String s2) throws Exception {
        annuaire.enregistrer(new CollaboratorView(SNA, "Ingénieur Normes et Agréments", Set.of("ROLE_SNA"), null, true));
        annuaire.enregistrer(new CollaboratorView(DDRM, "Chargé DDRM", Set.of("ROLE_CHARGE_DDRM"), null, true));
        institution = UUID.randomUUID();
        registre.enregistrer(new InstitutionView(institution, nom, "ADHERENT", "BANQUE", List.of(new InstitutionView.ServiceSouscrit(s1, "2026-09-01"), new InstitutionView.ServiceSouscrit(s2, "2026-09-01"))));
        fixtures.referentielPublie(TypeProjet.valueOf(type));
        String json = "{\"institutionId\":\"%s\",\"typeProjet\":\"%s\",\"chefDeProjetId\":\"%s\"}".formatted(institution, type, CollaboratorStubAdapter.CONDUCTEUR_1);
        MvcResult r = mvc.perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();
        assertThat(statut(r)).isEqualTo(201);
        liaison = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    @Alors("le parcours compte {int} opérations instanciées du référentiel de l'annexe A")
    public void leParcoursCompte(int n) throws Exception {
        assertThat(operations()).hasSize(n);
    }

    @Alors("la liaison est à l'étape {string} au statut {string}")
    public void laLiaisonEstA(String etape, String statut) throws Exception {
        String d = lire("/api/v1/liaisons/" + liaison + "/dossier", conducteur());
        assertThat(JsonPath.<String>read(d, "$.etapeCourante")).isEqualTo(etape);
        assertThat(JsonPath.<String>read(d, "$.statut")).isEqualTo(statut);
    }

    @Quand("la DDRM dépose le livrable du rang {string}")
    public void laDdrmDeposeLeLivrable(String rang) throws Exception {
        assertThat(statut(livrable(opId(rang), ddrm()))).isEqualTo(204);
    }

    @Quand("la DDRM dépose la preuve du rang {string}")
    public void laDdrmDeposeLaPreuve(String rang) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/parcours/operations/" + opId(rang) + "/preuves").with(ddrm()).contentType(MediaType.APPLICATION_JSON).content(piece(""))).andReturn();
        assertThat(statut(r)).isEqualTo(204);
    }

    @Et("le chef du DES affecte l'ingénieur Normes et Agréments aux ressources du projet")
    public void leChefDuDesAffecteLesRessources() throws Exception {
        MvcResult r = mvc
            .perform(put("/api/v1/liaisons/" + liaison + "/ressources").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content("{\"affectations\":[{\"fonction\":\"SNA\",\"personneId\":\"" + SNA + "\"}]}"))
            .andReturn();
        assertThat(statut(r)).isEqualTo(204);
    }

    @Alors("l'opération de rang {string} est {string}")
    public void lOperationEst(String rang, String etat) throws Exception {
        assertThat(operation(rang).get("etat")).isEqualTo(etat);
    }

    @Quand("les livrables sont déposés jusqu'au rang {string}")
    public void lesLivrablesSontDeposes(String rang) throws Exception {
        avancer(rang);
    }

    @Quand("le participant communique les coordonnées d'essai {string} et {string} sur le portail")
    public void leParticipantCommuniqueLesCoordonnees(String adresse, String port) throws Exception {
        MvcResult r = mvc
            .perform(put("/api/v1/portail/liaisons/" + liaison + "/coordonnees-techniques").with(correspondant(institution)).contentType(MediaType.APPLICATION_JSON).content("{\"adresse\":\"" + adresse + "\",\"port\":\"" + port + "\"}"))
            .andReturn();
        assertThat(statut(r)).isEqualTo(204);
    }

    @Et("les opérations sont prouvées et validées jusqu'au rang {string}")
    public void lesOperationsSontProuvees(String rang) throws Exception {
        for (Map<String, Object> o : operations()) {
            if (rang.equals(o.get("rang"))) {
                return;
            }
            if (!"FAITE".equals(o.get("etat"))) {
                String id = (String) o.get("id");
                assertThat(statut(preuve(id, ""))).isEqualTo(204);
                assertThat(statut(valider(id, validateurPour(id), true))).isEqualTo(204);
            }
        }
    }

    @Et("le participant dépose le document de paramétrage sur le portail")
    public void leParticipantDeposeLeDocumentDeParametrage() throws Exception {
        MvcResult r = mvc
            .perform(multipart("/api/v1/portail/operations/" + opId("15") + "/livrable").file(new MockMultipartFile("fichier", "document-parametrage.pdf", "application/pdf", "paramétrage".getBytes())).with(correspondant(institution)))
            .andReturn();
        assertThat(statut(r)).isEqualTo(204);
    }

    @Quand("les essais sont prouvés et validés")
    public void lesEssaisSontProuves() throws Exception {
        String o16 = opId("16");
        assertThat(statut(preuve(o16, ""))).isEqualTo(204);
        assertThat(statut(valider(o16, validateurPour(o16), true))).isEqualTo(204);
    }

    @Et("l'ingénieur Normes et Agréments constate la couverture")
    public void lIngenieurConstateLaCouverture() throws Exception {
        assertThat(statut(mvc.perform(post("/api/v1/liaisons/" + liaison + "/couverture/constat").with(sna())).andReturn())).isEqualTo(204);
    }

    @Alors("la couverture de certification constatée vaut {int} %")
    public void laCouvertureVaut(int taux) throws Exception {
        String c = mvc
            .perform(get("/api/v1/interne/liaisons/" + liaison + "/contexte-certification").with(jwt().jwt(j -> j.subject("service")).authorities(new SimpleGrantedAuthority("INTERNAL_HOMOLOGATION_READ"))))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(JsonPath.<Integer>read(c, "$.couvertureCertification.taux")).isEqualTo(taux);
        assertThat(JsonPath.<Boolean>read(c, "$.couvertureConstatee")).isTrue();
    }

    @Et("l'événement {string} est prêt pour MS-CERTIFICATION")
    public void lEvenementEstPret(String type) {
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ?", String.class, liaison)).contains(type);
    }

    @Et("le conducteur de projet voit le procès-verbal à établir sur son tableau de bord")
    public void leConducteurVoitLeProcesVerbal() throws Exception {
        String t = lire("/api/v1/tableaux-de-bord/acteur?fonctions=CONDUCTEUR", conducteur());
        assertThat(JsonPath.<List<Integer>>read(t, "$.compteurs[?(@.cle == 'pvAEtablir')].valeur")).allMatch(v -> v >= 1);
    }

    @Et("l'ingénieur Normes et Agréments est désigné responsable du rang {string}")
    public void lIngenieurEstResponsable(String rang) throws Exception {
        MvcResult r = mvc
            .perform(put("/api/v1/parcours/operations/" + opId(rang) + "/intervenants").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"qualite\":\"R\",\"personneId\":\"" + SNA + "\",\"motif\":\"relève\"}"))
            .andReturn();
        assertThat(statut(r)).isEqualTo(204);
    }

    @Alors("sa validation du rang {string} est refusée avec le statut {int} et le code {string}")
    public void saValidationEstRefusee(String rang, int statut, String code) throws Exception {
        verifierRefus(valider(opId(rang), sna(), true), statut, code);
    }

    @Et("la validation du rang {string} sans réauthentification est refusée avec le statut {int} et le code {string}")
    public void laValidationSansReauth(String rang, int statut, String code) throws Exception {
        verifierRefus(valider(opId(rang), validateur(), false), statut, code);
    }

    private void verifierRefus(MvcResult r, int statut, String code) throws Exception {
        assertThat(statut(r)).isEqualTo(statut);
        assertThat(JsonPath.<String>read(r.getResponse().getContentAsString(), "$.code")).isEqualTo(code);
    }

    @Quand("le conducteur de projet déclare un blocage {string} avec la prochaine action {string}")
    public void leConducteurDeclareUnBlocage(String motif, String action) throws Exception {
        String json = "{\"motif\":\"" + motif + "\",\"prochaineAction\":\"" + action + "\",\"responsableId\":\"" + DDRM + "\",\"echeanceLevee\":\"2026-12-31\"}";
        MvcResult r = mvc.perform(post("/api/v1/liaisons/" + liaison + "/blocages").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();
        assertThat(statut(r)).isEqualTo(201);
        blocage = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    @Et("la liaison figure parmi les projets bloqués du pilotage")
    public void laLiaisonFigureParmiLesBloques() throws Exception {
        String p = lire("/api/v1/tableaux-de-bord/pilotage", chefDes());
        assertThat(JsonPath.<List<String>>read(p, "$.questions.projetsBloques[*].liaisonId")).contains(liaison);
    }

    @Quand("le conducteur de projet lève le blocage")
    public void leConducteurLeveLeBlocage() throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/blocages/" + blocage + "/levee").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"commentaire\":\"Pièce reçue\"}")).andReturn();
        assertThat(statut(r)).isEqualTo(204);
    }

    @Étantdonné("une liaison dont la couverture a été constatée")
    public void uneLiaisonDontLaCouvertureAEteConstatee() throws Exception {
        avancer("18");
    }

    @Quand("MS-CERTIFICATION publie le procès-verbal établi puis signé des deux parties")
    public void msCertificationPublieLeProcesVerbal() throws Exception {
        avancer("20");
    }

    @Quand("MS-CERTIFICATION publie le dossier de changement, l'archivage des attestations et l'approbation de la sécurité")
    public void msCertificationPublieLApprobation() throws Exception {
        avancer("26");
    }

    @Étantdonné("une liaison approuvée par la sécurité")
    public void uneLiaisonApprouvee() throws Exception {
        avancer("C1");
    }

    @Quand("une preuve est déposée pour le rang {string} avec aucune cérémonie")
    public void unePreuveSansCeremonie(String rang) throws Exception {
        reponse = preuve(opId(rang), "");
    }

    @Quand("une preuve est déposée pour le rang {string} avec une valeur de clé en clair")
    public void unePreuveAvecValeurDeCle(String rang) throws Exception {
        reponse = preuve(opId(rang), "\"composanteCle\":\"0123456789ABCDEF0123456789ABCDEF\"");
    }

    @Alors("le dépôt est refusé avec le statut {int} et le code {string}")
    public void leDepotEstRefuse(int statut, String code) throws Exception {
        verifierRefus(reponse, statut, code);
    }

    @Alors("le correspondant de l'adhérent voit sa liaison et son dossier sans nom interne")
    public void leCorrespondantVoitSaLiaison() throws Exception {
        assertThat(JsonPath.<List<String>>read(lire("/api/v1/portail/liaisons", correspondant(institution)), "$[*].id")).containsExactly(liaison);
        String d = lire("/api/v1/portail/liaisons/" + liaison + "/dossier", correspondant(institution));
        assertThat(JsonPath.<String>read(d, "$.conducteur.nom")).isEqualTo("Conducteur de projet GIMAC");
        assertThat(d).doesNotContain("Aminata Ngono").doesNotContain("Chargé DDRM");
    }

    @Et("le correspondant d'une autre institution ne trouve pas la liaison")
    public void leCorrespondantDUneAutreInstitution() throws Exception {
        MvcResult r = mvc.perform(get("/api/v1/portail/liaisons/" + liaison + "/dossier").with(correspondant(UUID.randomUUID()))).andReturn();
        verifierRefus(r, 404, "HOM_LIAISON_NOT_FOUND");
    }
}
