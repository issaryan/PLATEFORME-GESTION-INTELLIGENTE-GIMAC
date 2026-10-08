package cm.gimac.des.homologation.domain.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Code d'un type de projet (D-91, docs/lot1/types-projet.md §1). Les types sont des données de MS-HOMOLOGATION
 * (table {@code type_projet}, entité {@code DefinitionTypeProjet}) ; cette valeur n'en porte que le code, sérialisé
 * tel quel (mêmes valeurs JSON et en base que l'ancienne énumération). Les trois types repris de l'énumération
 * restent nommés par des constantes ; {@link #values()} ne rend qu'eux (jeux d'essai, reprise de l'annexe A).
 */
public final class TypeProjet implements Comparable<TypeProjet>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Code d'un type : majuscule, puis 2 à 39 majuscules, chiffres ou soulignés. */
    public static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{2,39}$");

    public static final TypeProjet CARTE_INTERBANCAIRE = new TypeProjet("CARTE_INTERBANCAIRE");
    public static final TypeProjet PAIEMENT_MOBILE = new TypeProjet("PAIEMENT_MOBILE");
    public static final TypeProjet DELEGATAIRE = new TypeProjet("DELEGATAIRE");

    private static final List<TypeProjet> REPRIS = List.of(CARTE_INTERBANCAIRE, PAIEMENT_MOBILE, DELEGATAIRE);

    private final String code;

    private TypeProjet(String code) {
        this.code = code;
    }

    /** Type de ce code ; un code mal formé est refusé ({@link IllegalArgumentException}, 400 à la lecture d'une requête). */
    @JsonCreator
    public static TypeProjet valueOf(String code) {
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Code de type de projet invalide : " + code);
        }
        return REPRIS.stream().filter(t -> t.code.equals(code)).findFirst().orElseGet(() -> new TypeProjet(code));
    }

    /** Les trois types repris de l'ancienne énumération, dans leur ordre. */
    public static TypeProjet[] values() {
        return REPRIS.toArray(new TypeProjet[0]);
    }

    /** Type repris de l'ancienne énumération (carte interbancaire, paiement mobile, délégataire). */
    public boolean repris() {
        return REPRIS.contains(this);
    }

    @JsonValue
    public String name() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof TypeProjet t && t.code.equals(code));
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    /** Les types repris d'abord, dans leur ordre historique, puis les autres par code. */
    @Override
    public int compareTo(TypeProjet o) {
        int a = REPRIS.indexOf(this);
        int b = REPRIS.indexOf(o);
        if (a >= 0 || b >= 0) {
            return Integer.compare(a < 0 ? Integer.MAX_VALUE : a, b < 0 ? Integer.MAX_VALUE : b);
        }
        return code.compareTo(o.code);
    }
}
