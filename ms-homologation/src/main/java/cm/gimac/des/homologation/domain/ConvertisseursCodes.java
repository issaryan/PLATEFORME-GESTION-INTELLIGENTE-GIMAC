package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Colonnes texte des codes devenus des données (D-91) : type de projet et fonction gardent en base les mêmes
 * valeurs que les anciennes énumérations.
 */
public final class ConvertisseursCodes {

    private ConvertisseursCodes() {}

    /** Code du type de projet (colonnes {@code type_projet}). */
    @Converter
    public static class TypeProjetEnTexte implements AttributeConverter<TypeProjet, String> {

        @Override
        public String convertToDatabaseColumn(TypeProjet t) {
            return t == null ? null : t.name();
        }

        @Override
        public TypeProjet convertToEntityAttribute(String code) {
            return code == null ? null : TypeProjet.valueOf(code);
        }
    }

    /** Code d'une fonction du catalogue (responsable, validateur). */
    @Converter
    public static class FonctionEnTexte implements AttributeConverter<Fonction, String> {

        @Override
        public String convertToDatabaseColumn(Fonction f) {
            return f == null ? null : f.name();
        }

        @Override
        public Fonction convertToEntityAttribute(String code) {
            return code == null ? null : Fonction.valueOf(code);
        }
    }
}
