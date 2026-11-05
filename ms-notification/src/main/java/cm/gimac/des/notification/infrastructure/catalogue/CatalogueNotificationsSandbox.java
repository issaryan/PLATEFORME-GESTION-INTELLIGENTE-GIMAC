package cm.gimac.des.notification.infrastructure.catalogue;

import cm.gimac.des.notification.domain.service.ReglesSandbox;
import cm.gimac.des.notification.domain.service.ReglesSandbox.Modele;
import cm.gimac.des.notification.domain.service.ReglesSandbox.Portee;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Catalogue unique des modèles de notification de la Sandbox GIMAC (config/notifications-sandbox.json, contrat §11) :
 * modèles (code, titre, sous-titre, portée) et rôles admis. Le même fichier est copié dans le dépôt de la sandbox, dont
 * chaque service émetteur vérifie par un test que ses modèles et ses rôles y figurent.
 */
public final class CatalogueNotificationsSandbox {

    public static final String RESSOURCE = "/config/notifications-sandbox.json";

    private CatalogueNotificationsSandbox() {}

    /** Règles construites sur le catalogue livré avec le service. */
    public static ReglesSandbox regles() {
        return new ReglesSandbox(modeles(lire()));
    }

    static JsonNode lire() {
        try (InputStream in = CatalogueNotificationsSandbox.class.getResourceAsStream(RESSOURCE)) {
            if (in == null) {
                throw new IllegalStateException("catalogue des notifications de la sandbox absent : " + RESSOURCE);
            }
            return new ObjectMapper().readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<Modele> modeles(JsonNode catalogue) {
        List<Modele> l = new ArrayList<>();
        for (JsonNode m : catalogue.path("modeles")) {
            String code = m.path("code").asText();
            l.add(new Modele(code, m.path("titre").asText(), m.path("sousTitre").asText(), Portee.valueOf(m.path("portee").asText()), ReglesSandbox.intervenants(code)));
        }
        return l;
    }

    /** Rôles du catalogue et leur résolution (profil d'institution ou permission du GIMAC). */
    public static Map<String, ReglesSandbox.Role> roles() {
        Map<String, ReglesSandbox.Role> r = new TreeMap<>();
        lire().path("roles").fields().forEachRemaining(e -> r.put(e.getKey(), new ReglesSandbox.Role(texte(e.getValue(), "profilInstitution"), texte(e.getValue(), "permissionGimac"))));
        return r;
    }

    private static String texte(JsonNode n, String champ) {
        return n.hasNonNull(champ) ? n.get(champ).asText() : null;
    }
}
