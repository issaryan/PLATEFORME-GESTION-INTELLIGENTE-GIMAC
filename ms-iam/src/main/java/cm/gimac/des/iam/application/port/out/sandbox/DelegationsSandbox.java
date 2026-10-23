package cm.gimac.des.iam.application.port.out.sandbox;

import cm.gimac.des.iam.domain.sandbox.Delegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Délégations temporaires de la sandbox (D-119, US-EXI-04, US-EXI-24), table delegation_sandbox. */
public interface DelegationsSandbox {
    void enregistrer(Delegation d);

    /** Remplace l'état, l'approbateur, le motif, la fin et la date de clôture. */
    void mettreAJour(Delegation d);

    Optional<Delegation> lire(String id);

    /** Délégations accordées d'un compte, non révoquées, dont la fin suit l'instant (actives ou programmées). */
    List<Delegation> enCours(String compteId, TypeDelegation type, Instant t);

    /** Comptes titulaires d'un mandat accordé sur cette institution, non échu à l'instant. */
    List<String> titulaires(String institutionId, Instant t);

    /** Délégations d'un type (toutes si nul), les plus récentes d'abord, page à partir de 0. */
    List<Delegation> lister(TypeDelegation type, int page, int taille);

    long compter(TypeDelegation type);
}
