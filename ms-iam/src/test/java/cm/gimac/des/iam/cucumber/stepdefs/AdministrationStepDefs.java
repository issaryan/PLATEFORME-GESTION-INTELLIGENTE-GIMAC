package cm.gimac.des.iam.cucumber.stepdefs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.CleDeSecuriteEmulee;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Scénarios Gherkin de l'administration des comptes et du premier accès (D-62), par MockMvc sur l'application complète. */
public class AdministrationStepDefs {

    /** Acteurs des scénarios : sujet du jeton et permissions de leur profil dans le realm. */
    private static final Map<String, String> SUJETS = Map.of(
        "super administrateur",
        AbstractIamIT.SUPER_ADMIN,
        "RSSI",
        AbstractIamIT.RSSI,
        "chargé DDRM",
        "demo-charge-ddrm"
    );
    private static final Map<String, String[]> PERMISSIONS = Map.of(
        "super administrateur",
        AbstractIamIT.PERMISSIONS_SUPER_ADMIN,
        "RSSI",
        AbstractIamIT.PERMISSIONS_RSSI,
        "chargé DDRM",
        new String[] { "PAR_INSTITUTION_READ", "PAR_INSTITUTION_CREATE" }
    );

    private final MockMvc mvc;
    private String acteur;
    private RequestPostProcessor jeton;
    private String compteId;
    private ResultActions reponse;
    private CleDeSecuriteEmulee cleActeur;
    private CleDeSecuriteEmulee cleTitulaire;

    public AdministrationStepDefs(MockMvc mvc) {
        this.mvc = mvc;
    }

    private String corps() throws Exception {
        return reponse.andReturn().getResponse().getContentAsString();
    }

    private static String unique(String prefixe) {
        return prefixe + "." + UUID.randomUUID().toString().substring(0, 6);
    }

    private String creer(String identifiant, String profil) throws Exception {
        String json = """
            {"identifiant":"%s","prenom":"Agent","nom":"Recette","email":"%s@gimac.test","profils":["%s"],"envoyerInvitation":true}"""
            .formatted(identifiant, identifiant, profil);
        reponse = mvc.perform(
            post("/api/v1/comptes")
                .with(AbstractIamIT.utilisateur(AbstractIamIT.SUPER_ADMIN, AbstractIamIT.PERMISSIONS_SUPER_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        );
        return JsonPath.read(corps(), "$.compte.id");
    }

    private String defi(String sujet, String finalite) throws Exception {
        return mvc
            .perform(
                post("/api/v1/reauthentification/defi").with(AbstractIamIT.utilisateur(sujet)).contentType(MediaType.APPLICATION_JSON).content("{\"purpose\":\"" + finalite + "\"}")
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private CleDeSecuriteEmulee enroler(String sujet, String nom) throws Exception {
        CleDeSecuriteEmulee cle = new CleDeSecuriteEmulee();
        String options = mvc.perform(post("/api/v1/moi/cles-securite/defi").with(AbstractIamIT.utilisateur(sujet))).andReturn().getResponse().getContentAsString();
        reponse = mvc.perform(
            post("/api/v1/moi/cles-securite").with(AbstractIamIT.utilisateur(sujet)).contentType(MediaType.APPLICATION_JSON).content(cle.enrolement(options, nom))
        );
        return cle;
    }

    private String assertionActeur() throws Exception {
        if (cleActeur == null) {
            cleActeur = enroler(SUJETS.get(acteur), "Clé de l'administrateur");
        }
        return cleActeur.assertion(defi(SUJETS.get(acteur), "ADMIN_SENSITIVE"));
    }

    // ------------------------------------------------------------------ administration

    @Étantdonné("le {string} authentifié à deux facteurs")
    public void acteur(String nom) {
        acteur = nom;
        jeton = AbstractIamIT.utilisateur(SUJETS.get(nom), PERMISSIONS.get(nom));
    }

    @Quand("il crée le compte {string} avec le profil {string}")
    public void creeLeCompte(String identifiant, String profil) throws Exception {
        compteId = creer(unique(identifiant), profil);
    }

    @Et("un compte existant avec le profil {string}")
    public void compteExistant(String profil) throws Exception {
        compteId = creer(unique("agent"), profil);
    }

    @Quand("il exige la mise à jour du mot de passe et la configuration du second facteur")
    public void exigeActions() throws Exception {
        reponse = mvc.perform(
            post("/api/v1/comptes/{id}/actions-requises", compteId)
                .with(jeton)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actions\":[\"MOT_DE_PASSE\",\"SECOND_FACTEUR\"],\"parCourriel\":true,\"motif\":\"Revue des accès\"}")
        );
    }

    @Quand("il attribue le profil {string} avec une réauthentification forte")
    public void attribueProfil(String profil) throws Exception {
        String assertion = assertionActeur();
        reponse = mvc.perform(
            put("/api/v1/comptes/{id}/profils", compteId)
                .with(jeton)
                .header("X-Reauth-Assertion", assertion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"profils\":[\"" + profil + "\"],\"motif\":\"Mutation\"}")
        );
    }

    @Quand("il supprime le compte sans réauthentification")
    public void supprimeSans() throws Exception {
        reponse = mvc.perform(delete("/api/v1/comptes/{id}", compteId).param("motif", "Départ").with(jeton));
    }

    @Quand("il supprime le compte avec une réauthentification forte")
    public void supprimeAvec() throws Exception {
        String assertion = assertionActeur();
        reponse = mvc.perform(delete("/api/v1/comptes/{id}", compteId).param("motif", "Départ").header("X-Reauth-Assertion", assertion).with(jeton));
    }

    @Quand("il désactive son propre compte")
    public void desactiveSoiMeme() throws Exception {
        reponse = mvc.perform(
            post("/api/v1/comptes/{id}/desactivation", SUJETS.get(acteur)).with(jeton).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Essai\"}")
        );
    }

    @Quand("il consulte la liste des comptes")
    public void consulteLesComptes() throws Exception {
        reponse = mvc.perform(get("/api/v1/comptes").with(jeton));
    }

    @Alors("la réponse a le statut {int}")
    public void statut(int attendu) throws Exception {
        reponse.andExpect(status().is(attendu));
    }

    @Et("le code d'erreur est {string}")
    public void code(String attendu) throws Exception {
        assertThat(JsonPath.<String>read(corps(), "$.code")).isEqualTo(attendu);
    }

    @Et("le compte attend son premier accès")
    public void premierAccesEnAttente() throws Exception {
        assertThat(JsonPath.<Boolean>read(corps(), "$.compte.premierAccesEnAttente")).isTrue();
    }

    @Et("le journal d'administration du compte contient l'acte {string}")
    public void journal(String action) throws Exception {
        String j = mvc
            .perform(get("/api/v1/comptes/journal").param("compte", compteId).with(AbstractIamIT.utilisateur(AbstractIamIT.RSSI, "IAM_JOURNAL_READ")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(JsonPath.<List<String>>read(j, "$.contenu[*].action")).contains(action);
    }

    @Et("le compte doit accomplir les actions {string}")
    public void actions(String attendues) throws Exception {
        String c = mvc
            .perform(get("/api/v1/comptes/{id}", compteId).with(AbstractIamIT.utilisateur(AbstractIamIT.RSSI, "IAM_ACCOUNT_READ")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(JsonPath.<List<String>>read(c, "$.actionsRequises")).containsAll(Arrays.asList(attendues.split(",")));
    }

    // ------------------------------------------------------------------ premier accès

    @Étantdonné("un compte créé par le super administrateur avec le profil {string}")
    public void compteCree(String profil) throws Exception {
        compteId = creer(unique("titulaire"), profil);
    }

    @Quand("le titulaire consulte son compte")
    public void consulteSonCompte() throws Exception {
        reponse = mvc.perform(get("/api/v1/moi").with(AbstractIamIT.utilisateur(compteId)));
    }

    @Alors("les étapes restantes comprennent {string}")
    public void etapes(String etape) throws Exception {
        assertThat(JsonPath.<List<String>>read(corps(), "$.etapesRestantes")).contains(etape);
    }

    @Quand("il renseigne le téléphone {string}, la langue {string} et accepte la charte")
    public void premiereConfiguration(String telephone, String langue) throws Exception {
        configurer(telephone, langue, true);
    }

    @Quand("il renseigne le téléphone {string}, la langue {string} sans accepter la charte")
    public void premiereConfigurationSansCharte(String telephone, String langue) throws Exception {
        configurer(telephone, langue, false);
    }

    private void configurer(String telephone, String langue, boolean charte) throws Exception {
        reponse = mvc.perform(
            post("/api/v1/moi/premiere-configuration")
                .with(AbstractIamIT.utilisateur(compteId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telephone\":\"%s\",\"langue\":\"%s\",\"charteAcceptee\":%s}".formatted(telephone, langue, charte))
        );
    }

    @Et("sa fiche est complète")
    public void ficheComplete() throws Exception {
        assertThat(JsonPath.<Boolean>read(corps(), "$.compte.profilComplet")).isTrue();
        assertThat(JsonPath.<List<String>>read(corps(), "$.etapesRestantes")).doesNotContain("FICHE");
    }

    @Quand("le titulaire enrôle une clé de sécurité nommée {string}")
    public void enroleCle(String nom) throws Exception {
        cleTitulaire = enroler(compteId, nom);
    }

    @Et("son compte compte {int} clé de sécurité")
    public void nombreDeCles(int n) throws Exception {
        String m = mvc.perform(get("/api/v1/moi").with(AbstractIamIT.utilisateur(compteId))).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<Integer>read(m, "$.clesSecurite")).isEqualTo(n);
    }

    @Et("une assertion pour la finalité {string} est reconnue valide par la route interne")
    public void assertionValide(String finalite) throws Exception {
        String assertion = cleTitulaire.assertion(defi(compteId, finalite));
        String v = mvc
            .perform(
                post("/api/v1/interne/reauthentifications/verification")
                    .with(AbstractIamIT.jetonDeService("INTERNAL_IAM_READ"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"assertion\":\"%s\",\"sujet\":\"%s\",\"finalite\":\"%s\"}".formatted(assertion, compteId, finalite))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(JsonPath.<Boolean>read(v, "$.valide")).isTrue();
    }

    @Quand("le titulaire demande l'action {string}")
    public void demandeAction(String action) throws Exception {
        reponse = mvc.perform(
            post("/api/v1/moi/actions").with(AbstractIamIT.utilisateur(compteId)).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"" + action + "\"}")
        );
    }

    // ------------------------------------------------------------------ connexion sur la plateforme (D-65)

    @org.springframework.beans.factory.annotation.Autowired
    private cm.gimac.des.iam.infrastructure.courriel.CourrielsEnMemoire courriels;

    private String lienInvitation;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor passerelle() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
            .jwt(j -> j.subject("service-account-gateway-service").claim("azp", "gateway-service"))
            .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("INTERNAL_IAM_READ"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor restreinte() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(compteId).claim("acr", "1"));
    }

    private void nouveauMotDePasse(String jeton, String motDePasse) throws Exception {
        reponse = mvc.perform(
            post("/api/v1/interne/connexion/nouveau-mot-de-passe")
                .with(passerelle())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jeton\":\"%s\",\"motDePasse\":\"%s\"}".formatted(jeton, motDePasse))
        );
    }

    @Quand("le titulaire suit le lien de son invitation avec le mot de passe {string}")
    public void suitLeLien(String motDePasse) throws Exception {
        String email = JsonPath.read(
            mvc.perform(get("/api/v1/comptes/{id}", compteId).with(AbstractIamIT.utilisateur(AbstractIamIT.RSSI, "IAM_ACCOUNT_READ"))).andReturn().getResponse().getContentAsString(),
            "$.email"
        );
        String texte = courriels.dernierPour(email).orElseThrow().texte();
        int i = texte.indexOf("jeton=");
        lienInvitation = texte.substring(i + 6, i + 6 + 43);
        nouveauMotDePasse(lienInvitation, motDePasse);
    }

    @Quand("le titulaire suit à nouveau le même lien avec le mot de passe {string}")
    public void suitANouveau(String motDePasse) throws Exception {
        nouveauMotDePasse(lienInvitation, motDePasse);
    }

    private String etatConnexion() throws Exception {
        return mvc.perform(get("/api/v1/interne/comptes/{id}/etat-connexion", compteId).with(passerelle())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Et("la passerelle lit pour le compte les actions {string}")
    public void etatActions(String attendues) throws Exception {
        assertThat(JsonPath.<List<String>>read(etatConnexion(), "$.actionsRequises")).containsExactly(attendues.split(","));
    }

    @Et("la passerelle lit pour le compte un second facteur configuré")
    public void etatSecondFacteur() throws Exception {
        assertThat(JsonPath.<Boolean>read(etatConnexion(), "$.secondFacteur")).isTrue();
        assertThat(JsonPath.<List<String>>read(etatConnexion(), "$.actionsRequises")).doesNotContain("SECOND_FACTEUR");
    }

    @Quand("le titulaire, en session restreinte, configure son code TOTP avec un code juste")
    public void configureTotp() throws Exception {
        String e = mvc.perform(post("/api/v1/moi/second-facteur/totp/enrolement").with(restreinte())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String code = AbstractIamIT.codeTotp(JsonPath.read(e, "$.uri"));
        reponse = mvc.perform(
            post("/api/v1/moi/second-facteur/totp")
                .with(restreinte())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enrolementId\":\"%s\",\"code\":\"%s\"}".formatted(JsonPath.<String>read(e, "$.enrolementId"), code))
        );
    }

    @Quand("le titulaire, en session restreinte, consulte ses sessions")
    public void sessionsRestreintes() throws Exception {
        reponse = mvc.perform(get("/api/v1/moi/sessions").with(restreinte()));
    }
}
