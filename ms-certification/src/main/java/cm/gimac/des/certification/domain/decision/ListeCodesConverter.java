package cm.gimac.des.certification.domain.decision;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;

/** Liste de codes de service conservée en une colonne, séparés par des virgules (les codes n'en contiennent pas). */
@Converter
public class ListeCodesConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> codes) {
        return codes == null || codes.isEmpty() ? null : String.join(",", codes);
    }

    @Override
    public List<String> convertToEntityAttribute(String colonne) {
        return colonne == null || colonne.isBlank() ? List.of() : Arrays.stream(colonne.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
