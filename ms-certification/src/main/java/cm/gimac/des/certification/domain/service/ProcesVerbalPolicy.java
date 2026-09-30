package cm.gimac.des.certification.domain.service;

import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.enumeration.EtatProcesVerbal;
import cm.gimac.des.certification.domain.enumeration.RoleSignature;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Règles du verrou 1 (D-56, rangs 17 à 19), reprises du serveur de substitution de l'interface
 * (services/simule/parcours.ts, objet decision) : établissement à 100 % de couverture après le constat,
 * signatures GIMAC et participant dans un ordre quelconque, chacune une seule fois.
 */
public final class ProcesVerbalPolicy {

    public static final int COUVERTURE_EXIGEE = 100;

    private ProcesVerbalPolicy() {}

    /** Établissement : 100 % exigés (CER_COVERAGE_INCOMPLETE), puis constat fait et procès-verbal non établi (CER_PV_NOT_READY). */
    public static void verifierEtablissement(Integer taux, boolean constatee, EtatProcesVerbal etat) {
        if (taux == null || taux != COUVERTURE_EXIGEE) {
            throw CertificationException.conflit(
                "CER_COVERAGE_INCOMPLETE",
                "La couverture de certification est de " + (taux == null ? 0 : taux) + " % : le procès-verbal exige 100 %."
            );
        }
        if (!constatee || etat != EtatProcesVerbal.NON_ETABLI) {
            throw CertificationException.conflit("CER_PV_NOT_READY", "Le constat de la couverture doit précéder le procès-verbal.");
        }
    }

    /** État après la signature de cette partie ; CER_PV_NOT_SIGNABLE si le procès-verbal n'attend pas sa signature. */
    public static EtatProcesVerbal apresSignature(EtatProcesVerbal etat, RoleSignature role) {
        if (role == RoleSignature.GIMAC) {
            return switch (etat) {
                case ETABLI -> EtatProcesVerbal.SIGNE_GIMAC;
                case SIGNE_PARTICIPANT -> EtatProcesVerbal.SIGNE;
                default -> throw nonSignable("Le procès-verbal n'attend pas la signature du GIMAC.");
            };
        }
        if (role == RoleSignature.PARTICIPANT) {
            return switch (etat) {
                case ETABLI -> EtatProcesVerbal.SIGNE_PARTICIPANT;
                case SIGNE_GIMAC -> EtatProcesVerbal.SIGNE;
                default -> throw nonSignable("Le procès-verbal n'attend pas votre signature.");
            };
        }
        throw nonSignable("Cette qualité ne signe pas le procès-verbal.");
    }

    public static boolean attendSignature(EtatProcesVerbal etat, RoleSignature role) {
        return role == RoleSignature.GIMAC
            ? etat == EtatProcesVerbal.ETABLI || etat == EtatProcesVerbal.SIGNE_PARTICIPANT
            : role == RoleSignature.PARTICIPANT && (etat == EtatProcesVerbal.ETABLI || etat == EtatProcesVerbal.SIGNE_GIMAC);
    }

    /** Actions du DES : ETABLIR (ou ETABLIR_BLOQUE sous 100 %) après le constat ; SIGNER_GIMAC ; filtrées par permission. */
    public static List<String> actionsDes(EtatProcesVerbal etat, boolean constatee, Integer taux, Set<String> permissions) {
        List<String> actions = new ArrayList<>();
        if (etat == EtatProcesVerbal.NON_ETABLI && constatee && permissions.contains(PermissionsDecision.ETABLIR_PV)) {
            actions.add(taux != null && taux == COUVERTURE_EXIGEE ? "ETABLIR" : "ETABLIR_BLOQUE");
        }
        if (attendSignature(etat, RoleSignature.GIMAC) && permissions.contains(PermissionsDecision.SIGNER_PV_GIMAC)) {
            actions.add("SIGNER_GIMAC");
        }
        return actions;
    }

    /** Action du portail : SIGNER_PARTICIPANT tant que le procès-verbal attend la signature du participant. */
    public static List<String> actionsPortail(EtatProcesVerbal etat, Set<String> permissions) {
        return attendSignature(etat, RoleSignature.PARTICIPANT) && permissions.contains(PermissionsDecision.PORTAIL_SIGNER)
            ? List.of("SIGNER_PARTICIPANT")
            : List.of();
    }

    private static CertificationException nonSignable(String message) {
        return CertificationException.conflit("CER_PV_NOT_SIGNABLE", message);
    }
}
