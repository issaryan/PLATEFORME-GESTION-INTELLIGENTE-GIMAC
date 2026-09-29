package cm.gimac.des.certification.infrastructure.messaging;

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
}
