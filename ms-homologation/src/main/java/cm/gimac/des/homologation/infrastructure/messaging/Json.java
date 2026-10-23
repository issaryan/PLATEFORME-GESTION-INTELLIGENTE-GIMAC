package cm.gimac.des.homologation.infrastructure.messaging;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Lecture d'une chaîne JSON en arbre (Jackson 3), sans dépendre du contexte Spring. */
public final class Json {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private Json() {}

    public static JsonNode tree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("JSON invalide", e);
        }
    }

    /** Valeur textuelle d'un champ : nulle s'il est absent, nul, non textuel ou vide. */
    public static String texte(JsonNode n, String champ) {
        JsonNode v = n == null ? null : n.get(champ);
        if (v == null || !v.isValueNode() || v.isNull()) {
            return null;
        }
        String s = v.asString();
        return s.isBlank() ? null : s;
    }
}
