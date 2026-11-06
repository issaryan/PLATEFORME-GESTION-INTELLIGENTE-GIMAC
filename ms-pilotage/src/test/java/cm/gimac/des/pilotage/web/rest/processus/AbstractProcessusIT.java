package cm.gimac.des.pilotage.web.rest.processus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.AbstractPilotageIT;
import cm.gimac.des.pilotage.infrastructure.client.ReauthentificationStubAdapter;
import cm.gimac.des.pilotage.infrastructure.demo.DemonstrationProcessus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base des tests d'intégration des processus d'unité (D-87) : pôle réseau de démonstration de l'organisation
 * simulée (chef de pôle, ingénieure et technicien portant ROLE_POLE_RESEAU), réauthentification simulée, dépôts
 * simulés de MS-DOCUMENT ; tables des processus vidées après chaque scénario.
 */
abstract class AbstractProcessusIT extends AbstractPilotageIT {

    static final String BASE = "/api/v1/processus";
    static final String INSTANCES = BASE + "/instances";

    @Autowired
    protected ReauthentificationStubAdapter reauthentification;

    @BeforeEach
    void poleReseau() {
        organisation.chargerDemonstrationReseau();
        reauthentification.effacer();
    }

    @AfterEach
    void viderProcessus() {
        jdbc.execute((ConnectionCallback<Void>) c -> {
            try (var st = c.createStatement()) {
                st.execute("truncate table processus_historique, processus_etape_instance, processus_instance, processus, processed_event cascade");
            }
            if (!c.getAutoCommit()) {
                c.commit();
            }
            return null;
        });
    }

    /** Responsable (matrice par défaut, PROCESSUS_DEFINIR et PROCESSUS_LANCER à UNITES). */
    static RequestPostProcessor chef(String id) {
        return responsable(id);
    }

    /** Administration des référentiels métier, tous domaines (D-92) : toutes les unités. */
    static RequestPostProcessor administration(String id) {
        return utilisateur(id, "PIL_ACTIVITY_OWN", "REF_METIER_ADMIN");
    }

    /** Administration des comptes et des profils seule (RSSI) : aucune unité de plus (D-94). */
    static RequestPostProcessor administrationDesComptes(String id) {
        return utilisateur(id, "PIL_ACTIVITY_OWN", "IAM_PROFILE_EDIT", "IAM_ACCOUNT_READ");
    }

    MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, Object corps) throws Exception {
        return r.contentType(MediaType.APPLICATION_JSON).content(corps(traduire(r.buildRequest(new MockServletContext()).getRequestURI(), corps)));
    }

    /*
     * D-136 : le service génère les codes d'étape d'un modèle saisi ({CODE}-E{N}) ; les scénarios gardent leurs clés
     * lisibles (ANALYSE, MISE_EN_OEUVRE…), traduites ici en codes générés dans les URL d'étape, les étapes et les
     * prédécesseurs d'une modification, et les désignations d'un lancement. Clé inconnue : rendue telle quelle (modèles
     * provisionnés par le système, codes fixes).
     */

    /** Code généré de chaque étape, par code de famille de processus puis clé provisoire envoyée. */
    private final Map<String, Map<String, String>> etapesGenerees = new HashMap<>();

    private static final Pattern MODIFICATION = Pattern.compile("^" + BASE + "/([0-9a-f-]{36})$");
    private static final Pattern LANCEMENT = Pattern.compile("^" + BASE + "/([0-9a-f-]{36})/instances$");

    void relever(JsonNode envoye, JsonNode recu) {
        if (envoye == null || recu == null || !envoye.has("etapes") || !recu.has("etapes") || !recu.hasNonNull("code")) {
            return;
        }
        Map<String, String> m = etapesGenerees.computeIfAbsent(recu.get("code").asText(), k -> new HashMap<>());
        for (int i = 0; i < Math.min(envoye.get("etapes").size(), recu.get("etapes").size()); i++) {
            m.put(envoye.get("etapes").get(i).get("code").asText(), recu.get("etapes").get(i).get("code").asText());
        }
    }

    /** Code de famille du processus ou de l'instance {@code id}. */
    String famille(String id) {
        List<String> l = jdbc.queryForList(
            "select code from processus where cast(id as varchar) = ? union all select p.code from processus_instance i join processus p on p.id = i.processus_id where cast(i.id as varchar) = ?",
            String.class, id, id
        );
        return l.isEmpty() ? id : l.get(0);
    }

    /** Code généré de l'étape {@code cle} du processus ou de l'instance {@code id}. */
    String etapeDe(String id, String cle) {
        return etapesGenerees.getOrDefault(famille(id), Map.of()).getOrDefault(cle, cle);
    }

    String urlEtape(String instance, String cle, String acte) {
        return INSTANCES + "/" + instance + "/etapes/" + etapeDe(instance, cle) + "/" + acte;
    }

    private Object traduire(String uri, Object corps) {
        Matcher mod = MODIFICATION.matcher(uri);
        Matcher lan = LANCEMENT.matcher(uri);
        if (corps == null || !(mod.matches() || lan.matches())) {
            return corps;
        }
        String famille = famille(mod.matches() ? mod.group(1) : lan.group(1));
        Map<String, String> m = etapesGenerees.getOrDefault(famille, Map.of());
        JsonNode copie = json.valueToTree(corps);
        if (!(copie instanceof ObjectNode o)) {
            return corps;
        }
        // Modification : le code provisoire d'origine devient le code généré de la famille (un changement est refusé).
        if (mod.matches() && o.hasNonNull("code")) {
            o.put("code", famille);
        }
        if (mod.matches() && o.get("etapes") instanceof com.fasterxml.jackson.databind.node.ArrayNode etapes) {
            for (JsonNode e : etapes) {
                ObjectNode etape = (ObjectNode) e;
                etape.put("code", m.getOrDefault(etape.path("code").asText(), etape.path("code").asText()));
                if (etape.get("predecesseurs") instanceof com.fasterxml.jackson.databind.node.ArrayNode preds) {
                    for (int i = 0; i < preds.size(); i++) {
                        preds.set(i, m.getOrDefault(preds.get(i).asText(), preds.get(i).asText()));
                    }
                }
            }
        }
        if (lan.matches() && o.get("designations") instanceof ObjectNode d) {
            ObjectNode traduites = json.createObjectNode();
            d.properties().forEach(x -> {
                String[] parties = x.getKey().split("\\.", 2);
                traduites.set(m.getOrDefault(parties[0], parties[0]) + (parties.length > 1 ? "." + parties[1] : ""), x.getValue());
            });
            o.set("designations", traduites);
        }
        return o;
    }

    /** Définition « Ouverture d'un flux réseau » du jeu de démonstration, au format de la saisie (lanceur intervenant admis). */
    ObjectNode definition(String code) {
        ObjectNode d = json.createObjectNode();
        d.put("code", code);
        d.put("unite", DemonstrationProcessus.UNITE);
        d.put("libelle", "Ouverture d'un flux réseau");
        d.put("description", "Demande, analyse, mise en œuvre et recette signée.");
        d.put("categorie", "Réseau");
        d.put("delaiCibleJours", 10);
        d.set("etapes", json.valueToTree(DemonstrationProcessus.etapes()));
        // D-99 : ces scénarios du moteur font intervenir le lanceur (technicien porteur du profil réseau) sur les étapes de
        // sa demande ; la définition l'autorise. La séparation des fonctions par défaut : ProcessusSeparationFonctionsIT.
        d.putObject("initiateurs").put("initiateurIntervenant", true);
        return d;
    }

    /** Code généré par le système pour chaque définition créée, rangé sous le code provisoire envoyé (ignoré à la création). */
    private final java.util.Map<String, String> codesGeneres = new java.util.HashMap<>();

    String code(String provisoire) {
        return java.util.Objects.requireNonNull(codesGeneres.get(provisoire), "aucune définition créée sous " + provisoire);
    }

    String creer(RequestPostProcessor qui, Object corps) throws Exception {
        JsonNode cree = lire(mvc.perform(json(post(BASE).with(qui), corps)).andExpect(status().isCreated()).andReturn());
        if (corps instanceof JsonNode d && d.hasNonNull("code")) {
            codesGeneres.put(d.get("code").asText(), cree.get("code").asText());
        }
        relever(json.valueToTree(corps), cree);
        return cree.get("id").asText();
    }

    void publier(RequestPostProcessor qui, String id) throws Exception {
        mvc.perform(json(post(BASE + "/" + id + "/publication").with(qui), Map.of("motif", "Mise en service"))).andExpect(status().isOk());
    }

    /** Processus de démonstration créé et publié par le chef du pôle ; rend son identifiant. */
    String publie(String code, String chef) throws Exception {
        String id = creer(chef(chef), definition(code));
        publier(chef(chef), id);
        return id;
    }

    /** Dépôt simulé d'un livrable dans MS-DOCUMENT (finalité PROCESSUS_UNITE, référence : l'instance). */
    String deposer(String instance, String deposant, String nom, String type) {
        return documents.deposer(nom, type, (nom + " " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8), "PROCESSUS_UNITE", instance, deposant).toString();
    }

    JsonNode etape(JsonNode instance, String code) {
        java.util.Set<String> candidats = new java.util.HashSet<>(List.of(code));
        etapesGenerees.values().forEach(m -> {
            if (m.containsKey(code)) {
                candidats.add(m.get(code));
            }
        });
        for (JsonNode e : instance.get("etapes")) {
            if (candidats.contains(e.get("code").asText())) {
                return e;
            }
        }
        throw new AssertionError("Étape absente : " + code);
    }

    JsonNode charge(String type, String agregat) throws Exception {
        return json.readTree(jdbc.queryForObject("select payload::text from outbox_event where event_type = ? and aggregate_id = ? order by created_at desc limit 1", String.class, type, agregat));
    }

    List<JsonNode> charges(String type, String agregat) throws Exception {
        List<JsonNode> l = new ArrayList<>();
        for (String s : jdbc.queryForList("select payload::text from outbox_event where event_type = ? and aggregate_id = ? order by created_at", String.class, type, agregat)) {
            l.add(json.readTree(s));
        }
        return l;
    }

    static List<String> textes(JsonNode tableau) {
        List<String> l = new ArrayList<>();
        tableau.forEach(n -> l.add(n.asText()));
        return l;
    }
}
