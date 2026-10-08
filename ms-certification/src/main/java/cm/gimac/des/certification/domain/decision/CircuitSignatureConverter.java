package cm.gimac.des.certification.domain.decision;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Circuit de signature figé sur l'objet signé, conservé en JSON (D-94). */
@Converter
public class CircuitSignatureConverter implements AttributeConverter<List<EtapeSignature>, String> {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<List<EtapeSignature>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<EtapeSignature> circuit) {
        if (circuit == null || circuit.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(circuit);
        } catch (JacksonException e) {
            throw new IllegalStateException("Circuit de signature non sérialisable", e);
        }
    }

    @Override
    public List<EtapeSignature> convertToEntityAttribute(String colonne) {
        if (colonne == null || colonne.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(MAPPER.readValue(colonne, TYPE));
        } catch (JacksonException e) {
            throw new IllegalStateException("Circuit de signature illisible", e);
        }
    }
}
