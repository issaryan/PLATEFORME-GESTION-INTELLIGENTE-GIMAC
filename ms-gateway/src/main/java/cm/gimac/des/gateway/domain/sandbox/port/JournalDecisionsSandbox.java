package cm.gimac.des.gateway.domain.sandbox.port;

import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox;
import cm.gimac.des.gateway.domain.sandbox.RouteSandbox;

/** Journal des décisions de la passerelle sur les routes de la sandbox, vers MS-AUDIT (GW-10). */
public interface JournalDecisionsSandbox {
    /** Événement de sécurité publié pour chaque décision. */
    String EVENEMENT = "gateway.sandbox-decision.v1";

    void consigner(Entree entree);

    /** Décision journalisée : jamais de jeton ni de secret. */
    record Entree(
        String compte,
        RouteSandbox route,
        String methode,
        String chemin,
        String institutionJeton,
        String demandee,
        DecisionSandbox decision,
        int statut,
        String ip
    ) {}
}
