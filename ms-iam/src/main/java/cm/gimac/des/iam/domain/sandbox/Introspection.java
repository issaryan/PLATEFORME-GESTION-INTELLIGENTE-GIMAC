package cm.gimac.des.iam.domain.sandbox;

import java.time.Instant;
import java.util.Set;

/**
 * Décision d'introspection de MS-IAM pour la sandbox (D-119, US-EXI-04, US-EXI-06, US-EXI-24) : portée effective d'un
 * jeton à l'instant de la requête, projetée dans le jeton interne réduit. Un refus ne rend que {@code active = false}
 * à l'appelant (RFC 7662) ; son motif reste interne (journal).
 *
 * @param active jeton admis
 * @param sujet compte porteur du jeton
 * @param institutionId institution projetée (celle du compte, ou celle du mandant pour une délégation de mandat ; nulle
 *     pour un accès de prestataire)
 * @param permissions droits projetés (ceux du jeton, ou ceux de la délégation)
 * @param type COMPTE, MANDAT ou PRESTATAIRE
 * @param delegationId délégation appliquée, nulle pour un compte
 * @param expiration fin de validité : la plus proche de la fin du jeton et de la fin de la délégation
 * @param composants composants techniques ouverts (accès de prestataire), vide sinon
 * @param motifRefus motif interne d'un refus, nul si le jeton est admis
 * @param institutionPropre code participant de l'institution du compte (D-134), nul pour un compte du GIMAC
 */
public record Introspection(
    boolean active,
    String sujet,
    String institutionId,
    Set<String> permissions,
    String type,
    String delegationId,
    Instant expiration,
    Set<String> composants,
    String motifRefus,
    String institutionPropre
) {
    public static final String COMPTE = "COMPTE";

    public Introspection(
        boolean active,
        String sujet,
        String institutionId,
        Set<String> permissions,
        String type,
        String delegationId,
        Instant expiration,
        Set<String> composants,
        String motifRefus
    ) {
        this(active, sujet, institutionId, permissions, type, delegationId, expiration, composants, motifRefus, null);
    }

    public Introspection {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        composants = composants == null ? Set.of() : Set.copyOf(composants);
    }

    /** Jeton refusé, avec son motif interne. */
    public static Introspection refusee(String sujet, String motif) {
        return new Introspection(false, sujet, null, Set.of(), null, null, null, Set.of(), motif);
    }
}
