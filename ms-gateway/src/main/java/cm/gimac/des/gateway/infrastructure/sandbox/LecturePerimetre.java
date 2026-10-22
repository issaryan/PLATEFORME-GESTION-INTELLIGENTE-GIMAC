package cm.gimac.des.gateway.infrastructure.sandbox;

import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecture d'un instantané sbx-espace.scope-changed.v1 (CloudEvents 1.0, données de SBX-ESPACE : idCompte,
 * institutionPropre, mandantes {code: fin ISO 8601}, version). Toute enveloppe non conforme est écartée.
 */
public class LecturePerimetre {

    public static final String TYPE = "sbx-espace.scope-changed.v1";

    private final ObjectMapper json;

    public LecturePerimetre() {
        this.json = JsonMapper.builder().build();
    }

    public Optional<PerimetreMandats> lire(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode ce = json.readTree(valeur);
            JsonNode data = ce.path("data");
            if (!TYPE.equals(ce.path("type").asString(null)) || !data.isObject()) {
                return Optional.empty();
            }
            String compte = data.path("idCompte").asString(null);
            JsonNode version = data.path("version");
            if (compte == null || compte.isBlank() || !version.isIntegralNumber()) {
                return Optional.empty();
            }
            Map<String, Instant> mandantes = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> e : data.path("mandantes").properties()) {
                mandantes.put(e.getKey(), Instant.parse(e.getValue().asString()));
            }
            return Optional.of(new PerimetreMandats(compte, data.path("institutionPropre").asString(""), mandantes, version.asLong()));
        } catch (JacksonException | DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
