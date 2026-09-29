package cm.gimac.des.homologation.domain;

import java.util.List;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Conversion entre les colonnes jsonb des entités (chaînes JSON) et leurs valeurs typées.
 * Sans dépendance à Spring : utilisable par le domaine.
 */
public final class JsonTexte {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JsonTexte() {}

    public static String ecrire(Object valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(valeur);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Valeur non sérialisable en JSON", e);
        }
    }

    public static List<String> listeDeTextes(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> liste = MAPPER.readValue(json, new TypeReference<List<String>>() {});
            return liste == null ? List.of() : List.copyOf(liste);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Liste JSON invalide", e);
        }
    }

    public static Map<String, Object> objet(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> objet = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            return objet == null ? Map.of() : objet;
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Objet JSON invalide", e);
        }
    }
}
