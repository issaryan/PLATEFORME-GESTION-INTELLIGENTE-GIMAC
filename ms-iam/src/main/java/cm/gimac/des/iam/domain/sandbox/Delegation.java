package cm.gimac.des.iam.domain.sandbox;

import java.time.Instant;
import java.util.Set;

/**
 * Délégation temporaire de MS-IAM pour la sandbox (D-119) : mandat de partenaire (US-EXI-04), accordé par la saga MANDAT
 * de SBX-ESPACE à un intervenant désigné, ou accès de prestataire de maintenance (US-EXI-24), approuvé par le RSSI. Les
 * bornes sont tenues ici : l'accès cesse à la date de fin, quel que soit l'état enregistré.
 *
 * @param id identifiant (UUID)
 * @param type MANDAT ou PRESTATAIRE
 * @param compteId compte de l'intervenant ou du prestataire
 * @param institutionId institution mandante (mandat) ; nulle pour un accès de prestataire
 * @param permissions droits délégués (mandat) ; vide pour un prestataire
 * @param composants composants techniques ouverts (prestataire) ; vide pour un mandat
 * @param debut début de validité
 * @param fin fin de validité (exclue)
 * @param etat état enregistré (DEMANDEE, ACCORDEE, REFUSEE, REVOQUEE) ; l'état effectif dépend aussi de l'instant
 * @param demandeurId auteur de la demande ou de l'octroi
 * @param approbateurId approbateur (prestataire), nul sinon
 * @param motif motif de la demande, du refus ou de la révocation
 * @param creeLe création
 * @param closeLe révocation ou refus, nul sinon
 */
public record Delegation(
    String id,
    TypeDelegation type,
    String compteId,
    String institutionId,
    Set<String> permissions,
    Set<String> composants,
    Instant debut,
    Instant fin,
    EtatDelegation etat,
    String demandeurId,
    String approbateurId,
    String motif,
    Instant creeLe,
    Instant closeLe
) {
    public Delegation {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        composants = composants == null ? Set.of() : Set.copyOf(composants);
    }

    /** État effectif à l'instant donné. */
    public EtatEffectif etatA(Instant t) {
        return ReglesDelegation.etatEffectif(etat, debut, fin, t);
    }

    /** Même délégation avec un autre état enregistré. */
    public Delegation avecEtat(EtatDelegation nouveau, String acteur, String nouveauMotif, Instant le) {
        String approbateur = nouveau == EtatDelegation.ACCORDEE && type == TypeDelegation.PRESTATAIRE ? acteur : approbateurId;
        Instant close = nouveau == EtatDelegation.REVOQUEE || nouveau == EtatDelegation.REFUSEE ? le : closeLe;
        return new Delegation(id, type, compteId, institutionId, permissions, composants, debut, fin, nouveau, demandeurId, approbateur, nouveauMotif == null ? motif : nouveauMotif, creeLe, close);
    }

    /** Même délégation avec une autre fin (prolongation). */
    public Delegation avecFin(Instant nouvelleFin) {
        return new Delegation(id, type, compteId, institutionId, permissions, composants, debut, nouvelleFin, etat, demandeurId, approbateurId, motif, creeLe, closeLe);
    }

    public enum TypeDelegation {
        MANDAT,
        PRESTATAIRE,
    }

    public enum EtatDelegation {
        DEMANDEE,
        ACCORDEE,
        REFUSEE,
        REVOQUEE,
    }

    /** État vu à un instant : demandée, programmée, active, échue, refusée ou révoquée. */
    public enum EtatEffectif {
        DEMANDEE,
        PROGRAMMEE,
        ACTIVE,
        ECHUE,
        REFUSEE,
        REVOQUEE,
    }
}
