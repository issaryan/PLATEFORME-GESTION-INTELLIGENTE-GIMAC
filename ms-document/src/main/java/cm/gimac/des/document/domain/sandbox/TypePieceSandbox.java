package cm.gimac.des.document.domain.sandbox;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Type de pièce de la Sandbox GIMAC au catalogue de MS-DOCUMENT (US-EXI-08, D-120, DOC-03) : code (aussi finalité du
 * dépôt), libellés fr, en et es, politique de rétention, caractère probant, services de la sandbox admis à déposer,
 * règle appliquée à une pièce portant un numéro de carte en clair, taille maximale, état et version.
 */
public record TypePieceSandbox(
    String code,
    Map<String, String> libelles,
    Period retention,
    boolean probante,
    Set<String> servicesAutorises,
    ReglePan reglePan,
    long tailleMax,
    boolean actif,
    int version
) {
    /** Conservation minimale des preuves de la plateforme du DES (dix ans). */
    public static final Period RETENTION_MIN_PROBANTE = Period.ofYears(10);

    /** Traitement d'une pièce portant un numéro de carte non masqué. */
    public enum ReglePan {
        /** Pièce mise en quarantaine : inscrite, jamais conservée dans le dépôt inaltérable ni restituée. */
        QUARANTAINE,
        /** Pièce refusée sans inscription. */
        REFUS,
    }

    public TypePieceSandbox {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(retention, "retention");
        Objects.requireNonNull(reglePan, "reglePan");
        libelles = Map.copyOf(libelles);
        servicesAutorises = Set.copyOf(servicesAutorises);
    }

    /** Vrai si ce service de la sandbox (client du jeton de service) peut déposer ce type. */
    public boolean autorise(String service) {
        return service != null && servicesAutorises.contains(service);
    }

    /** Fin de conservation d'une pièce de ce type déposée à cet instant (calendrier UTC). */
    public Instant conserveeJusquAu(Instant depot) {
        return depot.atOffset(ZoneOffset.UTC).plus(retention).toInstant();
    }

    /**
     * Rétention admise pour un type : période ISO 8601 positive, en années, mois ou jours ; au moins dix ans pour un
     * type probant. Rend le motif du refus, ou null si la rétention est admise.
     */
    public static String refusRetention(Period retention, boolean probante) {
        if (retention == null || retention.isNegative() || retention.isZero()) {
            return "DOC_SBX_RETENTION_INVALIDE";
        }
        // Comparaison sur un calendrier de référence fixe : P3653D vaut dix ans, P119M ne les vaut pas.
        java.time.LocalDate reference = java.time.LocalDate.of(2000, 1, 1);
        if (probante && reference.plus(retention).isBefore(reference.plus(RETENTION_MIN_PROBANTE))) {
            return "DOC_SBX_RETENTION_INSUFFISANTE";
        }
        return null;
    }

    public TypePieceSandbox avecRetention(Period nouvelle) {
        return new TypePieceSandbox(code, libelles, nouvelle, probante, servicesAutorises, reglePan, tailleMax, actif, version + 1);
    }
}
