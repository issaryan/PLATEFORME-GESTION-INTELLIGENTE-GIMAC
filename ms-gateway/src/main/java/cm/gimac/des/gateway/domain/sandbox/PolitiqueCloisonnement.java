package cm.gimac.des.gateway.domain.sandbox;

import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox.Motif;
import java.time.Instant;

/**
 * Cloisonnement par la revendication institution_id étendu aux mandats (US-EXI-07, CC-SBX §6.1.1) : un compte
 * d'institution agit au nom de son institution, ou d'une institution mandante tant que le mandat est actif
 * (fin strictement postérieure à l'instant de la requête). Service de domaine pur.
 */
public final class PolitiqueCloisonnement {

    private PolitiqueCloisonnement() {}

    /**
     * @param institutionJeton institution du jeton (vide pour un compte du GIMAC)
     * @param demandee institution mandante choisie par le partenaire (en-tête), vide sinon
     * @param perimetre projection du périmètre du compte, nulle si aucun instantané n'a été reçu
     */
    public static DecisionSandbox decider(String institutionJeton, String demandee, PerimetreMandats perimetre, Instant maintenant) {
        if (vide(institutionJeton)) {
            return DecisionSandbox.de(Motif.COMPTE_GIMAC, "");
        }
        if (vide(demandee) || demandee.equals(institutionJeton)) {
            return DecisionSandbox.de(Motif.INSTITUTION_PROPRE, institutionJeton);
        }
        Instant fin = perimetre == null ? null : perimetre.finMandat(demandee);
        if (fin == null) {
            return DecisionSandbox.de(Motif.MANDAT_ABSENT, institutionJeton);
        }
        if (!maintenant.isBefore(fin)) {
            return DecisionSandbox.de(Motif.MANDAT_ECHU, institutionJeton);
        }
        return DecisionSandbox.de(Motif.MANDAT_ACTIF, demandee);
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
