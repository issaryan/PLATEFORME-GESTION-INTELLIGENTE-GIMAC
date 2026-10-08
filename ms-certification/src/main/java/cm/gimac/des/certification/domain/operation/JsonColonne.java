package cm.gimac.des.certification.domain.operation;

import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Listes de la projection conservées en JSON (texte). */
public final class JsonColonne {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JsonColonne() {}

    static String ecrire(List<?> valeurs) {
        if (valeurs == null || valeurs.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(valeurs);
        } catch (JacksonException e) {
            throw new IllegalStateException("Valeur non sérialisable", e);
        }
    }

    static <T> List<T> lire(String colonne, TypeReference<List<T>> type) {
        if (colonne == null || colonne.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(MAPPER.readValue(colonne, type));
        } catch (JacksonException e) {
            throw new IllegalStateException("Colonne JSON illisible", e);
        }
    }

    @jakarta.persistence.Converter
    public static class Intervenants implements jakarta.persistence.AttributeConverter<List<Intervenant>, String> {

        @Override
        public String convertToDatabaseColumn(List<Intervenant> v) {
            return ecrire(v);
        }

        @Override
        public List<Intervenant> convertToEntityAttribute(String c) {
            return lire(c, new TypeReference<>() {});
        }
    }

    @jakarta.persistence.Converter
    public static class Codes implements jakarta.persistence.AttributeConverter<List<String>, String> {

        @Override
        public String convertToDatabaseColumn(List<String> v) {
            return ecrire(v);
        }

        @Override
        public List<String> convertToEntityAttribute(String c) {
            return lire(c, new TypeReference<>() {});
        }
    }
}
