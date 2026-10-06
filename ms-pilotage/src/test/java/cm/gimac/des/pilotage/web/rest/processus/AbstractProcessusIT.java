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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
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

    static RequestPostProcessor administration(String id) {
        return utilisateur(id, "PIL_ACTIVITY_OWN", "IAM_PROFILE_EDIT");
    }

    MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, Object corps) throws Exception {
        return r.contentType(MediaType.APPLICATION_JSON).content(corps(corps));
    }

    /** Définition « Ouverture d'un flux réseau » du jeu de démonstration, au format de la saisie. */
    ObjectNode definition(String code) {
        ObjectNode d = json.createObjectNode();
        d.put("code", code);
        d.put("unite", DemonstrationProcessus.UNITE);
        d.put("libelle", "Ouverture d'un flux réseau");
        d.put("description", "Demande, analyse, mise en œuvre et recette signée.");
        d.put("categorie", "Réseau");
        d.put("delaiCibleJours", 10);
        d.set("etapes", json.valueToTree(DemonstrationProcessus.etapes()));
        return d;
    }

    String creer(RequestPostProcessor qui, Object corps) throws Exception {
        return lire(mvc.perform(json(post(BASE).with(qui), corps)).andExpect(status().isCreated()).andReturn()).get("id").asText();
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
        for (JsonNode e : instance.get("etapes")) {
            if (code.equals(e.get("code").asText())) {
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
