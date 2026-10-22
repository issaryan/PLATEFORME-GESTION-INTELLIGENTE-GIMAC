package cm.gimac.des.homologation.contrat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrôle d'un document JSON contre un schéma publié dans {@code src/main/resources/contrats} (sous-ensemble de JSON
 * Schema 2020-12 employé par ces contrats : required, properties, type simple ou liste, enum, minimum, items, formats
 * uuid et date-time). Rend la liste des écarts, vide si le document est conforme.
 */
public final class SchemaContrat {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final JsonNode schema;

    private SchemaContrat(JsonNode schema) {
        this.schema = schema;
    }

    public static SchemaContrat charger(String chemin) {
        try (InputStream in = SchemaContrat.class.getResourceAsStream("/contrats/" + chemin)) {
            if (in == null) {
                throw new IllegalArgumentException("Schéma absent : " + chemin);
            }
            return new SchemaContrat(JSON.readTree(in));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public JsonNode schema() {
        return schema;
    }

    public List<String> ecarts(JsonNode document) {
        List<String> ecarts = new ArrayList<>();
        controler("$", schema, document, ecarts);
        return ecarts;
    }

    private static void controler(String chemin, JsonNode s, JsonNode d, List<String> ecarts) {
        if (!typeAdmis(s.path("type"), d)) {
            ecarts.add(chemin + " : type " + d.getNodeType() + " hors de " + s.path("type"));
            return;
        }
        if (s.has("enum")) {
            boolean trouve = false;
            for (JsonNode v : s.get("enum")) {
                trouve |= v.equals(d);
            }
            if (!trouve) {
                ecarts.add(chemin + " : valeur " + d + " hors de " + s.get("enum"));
            }
        }
        if (s.has("minimum") && d.isNumber() && d.asLong() < s.get("minimum").asLong()) {
            ecarts.add(chemin + " : " + d + " inférieur au minimum " + s.get("minimum"));
        }
        if (d.isString() && s.has("format") && !formatAdmis(s.get("format").asString(), d.asString())) {
            ecarts.add(chemin + " : " + d + " ne suit pas le format " + s.get("format").asString());
        }
        if (d.isObject()) {
            for (JsonNode r : s.path("required")) {
                if (!d.has(r.asString())) {
                    ecarts.add(chemin + "." + r.asString() + " : absent");
                }
            }
            for (var e : s.path("properties").properties()) {
                if (d.has(e.getKey())) {
                    controler(chemin + "." + e.getKey(), e.getValue(), d.get(e.getKey()), ecarts);
                }
            }
        }
        if (d.isArray() && s.has("items")) {
            int i = 0;
            for (JsonNode x : d) {
                controler(chemin + "[" + i++ + "]", s.get("items"), x, ecarts);
            }
        }
    }

    private static boolean typeAdmis(JsonNode type, JsonNode d) {
        if (type.isMissingNode()) {
            return true;
        }
        if (type.isArray()) {
            for (JsonNode t : type) {
                if (typeAdmis(t, d)) {
                    return true;
                }
            }
            return false;
        }
        return switch (type.asString()) {
            case "string" -> d.isString();
            case "integer" -> d.isIntegralNumber();
            case "number" -> d.isNumber();
            case "boolean" -> d.isBoolean();
            case "array" -> d.isArray();
            case "object" -> d.isObject();
            case "null" -> d.isNull();
            default -> false;
        };
    }

    private static boolean formatAdmis(String format, String valeur) {
        try {
            switch (format) {
                case "uuid" -> UUID.fromString(valeur);
                case "date-time" -> OffsetDateTime.parse(valeur);
                default -> {
                    return true;
                }
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
