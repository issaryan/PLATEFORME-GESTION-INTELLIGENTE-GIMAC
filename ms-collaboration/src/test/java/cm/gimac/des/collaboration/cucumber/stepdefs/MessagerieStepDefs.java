package cm.gimac.des.collaboration.cucumber.stepdefs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import cm.gimac.des.collaboration.AbstractCollaborationIT;
import cm.gimac.des.collaboration.RelaisOutboxDeTest;
import cm.gimac.des.collaboration.infrastructure.client.CollaboratorStubAdapter;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Et;
import io.cucumber.java.fr.Quand;
import io.cucumber.java.fr.Étantdonné;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Scénarios Gherkin de la messagerie de travail (D-64), exécutés par MockMvc sur l'application complète. */
public class MessagerieStepDefs {

    private static final String BASE = "/api/v1/messagerie";
    private static final Map<String, String> ACTEURS = Map.of(
        "Paul Mbarga Atangana",
        CollaboratorStubAdapter.CHEF_DES,
        "Aminata Ngono",
        CollaboratorStubAdapter.CONDUCTEUR,
        "Serge Ndzana",
        CollaboratorStubAdapter.INGENIEUR
    );

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final RelaisOutboxDeTest relais;

    private String conversation;
    private String premiereConversation;
    private UUID liaison;
    private byte[] piece;
    private MvcResult reponse;

    public MessagerieStepDefs(MockMvc mvc, JdbcTemplate jdbc, RelaisOutboxDeTest relais) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.relais = relais;
    }

    @io.cucumber.java.After
    public void effacer() {
        AbstractCollaborationIT.effacer(jdbc);
    }

    private static RequestPostProcessor jeton(String nom) {
        return AbstractCollaborationIT.utilisateur(ACTEURS.get(nom), AbstractCollaborationIT.MESSAGERIE);
    }

    private static String corps(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private MvcResult creer(String de, String json, MockMultipartFile... fichiers) throws Exception {
        MockMultipartHttpServletRequestBuilder r = multipart(BASE + "/conversations").file(
            new MockMultipartFile("message", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8))
        );
        for (MockMultipartFile f : fichiers) {
            r.file(f);
        }
        MvcResult resultat = mvc.perform(r.with(jeton(de))).andReturn();
        assertThat(resultat.getResponse().getStatus()).as(corps(resultat)).isIn(200, 201);
        conversation = JsonPath.read(corps(resultat), "$.id");
        if (premiereConversation == null) {
            premiereConversation = conversation;
        }
        return resultat;
    }

    private String fil(String qui) throws Exception {
        MvcResult r = mvc.perform(get(BASE + "/conversations/" + conversation + "/messages").with(jeton(qui))).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return corps(r);
    }

    @Étantdonné("les acteurs internes de l'annuaire, authentifiés à deux facteurs avec les permissions de la messagerie")
    public void acteurs() {
        conversation = null;
        premiereConversation = null;
    }

    @Quand("{string} écrit {string} à {string}")
    public void ecrit(String de, String texte, String a) throws Exception {
        reponse = creer(de, "{\"destinataires\":[\"%s\"],\"contenu\":\"%s\"}".formatted(ACTEURS.get(a), texte));
    }

    @Quand("{string} écrit {string} à {string} en citant la liaison {string}")
    public void ecritEnCitant(String de, String texte, String a, String reference) throws Exception {
        liaison = UUID.randomUUID();
        String json =
            "{\"destinataires\":[\"%s\"],\"liaisonId\":\"%s\",\"contenu\":\"%s\",\"etiquettes\":[{\"liaisonId\":\"%s\",\"reference\":\"%s\"}]}".formatted(
                    ACTEURS.get(a),
                    liaison,
                    texte,
                    liaison,
                    reference
                );
        reponse = creer(de, json);
    }

    @Quand("{string} envoie à {string} la pièce {string}")
    public void envoiePiece(String de, String a, String nom) throws Exception {
        piece = ("%PDF-1.7 " + nom + " " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        reponse = creer(
            de,
            "{\"destinataires\":[\"%s\"],\"contenu\":\"Voici la pièce\"}".formatted(ACTEURS.get(a)),
            new MockMultipartFile("fichiers", nom, "application/pdf", piece)
        );
    }

    @Étantdonné("{string} a écrit {int} message(s) à {string}")
    public void aEcrit(String de, int nombre, String a) throws Exception {
        for (int i = 1; i <= nombre; i++) {
            ecrit(de, "Message " + i, a);
        }
    }

    @Alors("une conversation directe réunit {string} et {string}")
    public void directe(String un, String deux) throws Exception {
        assertThat(reponse.getResponse().getStatus()).isEqualTo(201);
        String c = corps(reponse);
        assertThat((String) JsonPath.read(c, "$.type")).isEqualTo("DIRECTE");
        assertThat((List<String>) JsonPath.read(c, "$.membres[*].nom")).containsExactlyInAnyOrder(un, deux);
    }

    @Alors("la même conversation directe est réutilisée")
    public void reutilisee() {
        assertThat(reponse.getResponse().getStatus()).isEqualTo(200);
        assertThat(conversation).isEqualTo(premiereConversation);
        assertThat(jdbc.queryForObject("select count(*) from conversation", Integer.class)).isEqualTo(1);
    }

    @Alors("{string} lit le message {string} de {string}")
    public void lit(String qui, String texte, String auteur) throws Exception {
        String f = fil(qui);
        assertThat((List<String>) JsonPath.read(f, "$.contenu[*].contenu")).contains(texte);
        assertThat((List<String>) JsonPath.read(f, "$.contenu[*].auteur.nom")).contains(auteur);
    }

    @Alors("l'événement {string} est remis pour {string} sans le contenu du message")
    public void evenement(String type, String destinataire) throws Exception {
        relais.relayerJusquaEnvoi(conversation);
        var recus = relais.lire("collaboration.message", conversation);
        assertThat(recus).isNotEmpty();
        String e = recus.get(0).value();
        assertThat((String) JsonPath.read(e, "$.type")).isEqualTo(type);
        assertThat((List<String>) JsonPath.read(e, "$.data.destinataires")).containsExactly(ACTEURS.get(destinataire));
        assertThat(e).doesNotContain("Bonjour");
    }

    @Alors("le message porte l'étiquette {string}")
    public void etiquette(String reference) throws Exception {
        String f = fil("Paul Mbarga Atangana");
        assertThat((List<String>) JsonPath.read(f, "$.contenu[0].etiquettes[*].reference")).containsExactly(reference);
    }

    @Et("{string} retrouve la conversation par la liaison citée")
    public void parLiaison(String qui) throws Exception {
        String c = corps(mvc.perform(get(BASE + "/conversations?liaisonId=" + liaison).with(jeton(qui))).andReturn());
        assertThat((List<String>) JsonPath.read(c, "$.contenu[*].id")).containsExactly(conversation);
    }

    @Alors("{string} télécharge la pièce {string} à l'identique, avec son empreinte SHA-256")
    public void telecharge(String qui, String nom) throws Exception {
        String f = fil(qui);
        assertThat((String) JsonPath.read(f, "$.contenu[0].piecesJointes[0].nom")).isEqualTo(nom);
        assertThat((String) JsonPath.read(f, "$.contenu[0].piecesJointes[0].empreinte")).isEqualTo(
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(piece))
        );
        String id = JsonPath.read(f, "$.contenu[0].piecesJointes[0].id");
        MvcResult r = mvc.perform(get(BASE + "/pieces/" + id).with(jeton(qui))).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(r.getResponse().getHeader("Content-Disposition")).startsWith("attachment;").contains(nom);
        assertThat(r.getResponse().getContentAsByteArray()).isEqualTo(piece);
    }

    @Alors("{string} a {int} message(s) non lu(s)")
    public void nonLus(String qui, int nombre) throws Exception {
        String c = corps(mvc.perform(get(BASE + "/non-lus").with(jeton(qui))).andReturn());
        assertThat(((Number) JsonPath.read(c, "$.total")).intValue()).isEqualTo(nombre);
    }

    @Quand("{string} marque la conversation comme lue")
    public void marqueLue(String qui) throws Exception {
        assertThat(mvc.perform(post(BASE + "/conversations/" + conversation + "/lu").with(jeton(qui))).andReturn().getResponse().getStatus()).isEqualTo(204);
    }

    @Quand("{string} tente de lire la conversation")
    public void tenteDeLire(String qui) throws Exception {
        reponse = mvc.perform(get(BASE + "/conversations/" + conversation).with(jeton(qui))).andReturn();
    }

    @Quand("{string} tente d'écrire {string} dans la conversation")
    public void tenteDEcrire(String qui, String texte) throws Exception {
        reponse = mvc
            .perform(
                multipart(BASE + "/conversations/" + conversation + "/messages")
                    .file(new MockMultipartFile("message", "", MediaType.APPLICATION_JSON_VALUE, ("{\"contenu\":\"" + texte + "\"}").getBytes(StandardCharsets.UTF_8)))
                    .with(jeton(qui))
            )
            .andReturn();
    }

    @Alors("la réponse est {int} avec le code {string}")
    public void refus(int statut, String code) throws Exception {
        assertThat(reponse.getResponse().getStatus()).isEqualTo(statut);
        assertThat((String) JsonPath.read(corps(reponse), "$.code")).isEqualTo(code);
    }

    @Et("aucun numéro de carte en clair n'est enregistré")
    public void aucunPanEnClair() {
        List<byte[]> chiffres = jdbc.queryForList("select contenu_chiffre from message", byte[].class);
        assertThat(chiffres).isNotEmpty();
        chiffres.forEach(c -> assertThat(new String(c, StandardCharsets.ISO_8859_1)).doesNotContain("4111", "Carte"));
    }
}
