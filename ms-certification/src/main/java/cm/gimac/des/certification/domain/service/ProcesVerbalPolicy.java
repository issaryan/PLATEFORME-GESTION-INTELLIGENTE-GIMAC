package cm.gimac.des.certification.domain.service;

import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.enumeration.EtatProcesVerbal;
import cm.gimac.des.certification.domain.decision.EtapeSignature;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Règles du verrou 1 (D-56, rangs 17 à 19), reprises du serveur de substitution de l'interface
 * (services/simule/parcours.ts, objet decision) : établissement à 100 % de couverture après le constat, puis
 * signatures du circuit configuré (D-94), dans l'ordre croissant, chacune une seule fois.
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

    /**
     * État du procès-verbal établi selon les signatures recueillies du circuit (D-94) : SIGNE quand toutes sont
     * données ; SIGNE_GIMAC quand toutes celles du DES le sont et qu'il en reste au portail ; SIGNE_PARTICIPANT dans le
     * cas inverse ; sinon ETABLI.
     */
    public static EtatProcesVerbal etat(List<EtapeSignature> circuit, Set<String> signees) {
        boolean des = circuit.stream().filter(e -> !e.portail()).allMatch(e -> signees.contains(e.code()));
        boolean portail = circuit.stream().filter(EtapeSignature::portail).allMatch(e -> signees.contains(e.code()));
        if (des && portail) {
            return EtatProcesVerbal.SIGNE;
        }
        boolean aucuneDes = circuit.stream().noneMatch(e -> !e.portail());
        boolean aucunePortail = circuit.stream().noneMatch(EtapeSignature::portail);
        if (des && !aucuneDes) {
            return EtatProcesVerbal.SIGNE_GIMAC;
        }
        if (portail && !aucunePortail) {
            return EtatProcesVerbal.SIGNE_PARTICIPANT;
        }
        return EtatProcesVerbal.ETABLI;
    }

    /**
     * Signatures que ce côté (DES ou portail) peut donner maintenant : non données, d'ordre égal au plus petit ordre
     * restant du circuit (ordre croissant ; même ordre : indifférent).
     */
    public static List<EtapeSignature> signables(List<EtapeSignature> circuit, Set<String> signees, boolean portail) {
        int prochain = circuit.stream().filter(e -> !signees.contains(e.code())).mapToInt(EtapeSignature::ordre).min().orElse(Integer.MAX_VALUE);
        return circuit.stream().filter(e -> e.portail() == portail && !signees.contains(e.code()) && e.ordre() == prochain).toList();
    }

    /**
     * Signature de ce côté refusée faute d'étape signable : 409 CER_SIGNATURE_ORDRE si ce côté a encore des signatures
     * attendues après d'autres ({@code details.attendues}), sinon 409 CER_PV_NOT_SIGNABLE.
     */
    public static CertificationException nonSignable(List<EtapeSignature> circuit, Set<String> signees, boolean portail) {
        boolean resteDeCeCote = circuit.stream().anyMatch(e -> e.portail() == portail && !signees.contains(e.code()));
        if (resteDeCeCote) {
            int prochain = circuit.stream().filter(e -> !signees.contains(e.code())).mapToInt(EtapeSignature::ordre).min().orElse(0);
            List<String> attendues = circuit.stream().filter(e -> !signees.contains(e.code()) && e.ordre() == prochain).map(EtapeSignature::code).toList();
            return CertificationException.conflit(
                "CER_SIGNATURE_ORDRE",
                "D'autres signatures du procès-verbal doivent précéder la vôtre.",
                java.util.Map.of("attendues", attendues)
            );
        }
        return nonSignable(portail ? "Le procès-verbal n'attend pas votre signature." : "Le procès-verbal n'attend pas la signature du GIMAC.");
    }

    /**
     * Actions du DES : ETABLIR (ou ETABLIR_BLOQUE sous 100 %) après le constat ; SIGNER_GIMAC quand une signature du
     * DES est signable par l'acteur ; filtrées par permission.
     */
    public static List<String> actionsDes(EtatProcesVerbal etat, boolean constatee, Integer taux, Set<String> permissions, boolean signableDes) {
        List<String> actions = new ArrayList<>();
        if (etat == EtatProcesVerbal.NON_ETABLI && constatee && permissions.contains(PermissionsDecision.ETABLIR_PV)) {
            actions.add(taux != null && taux == COUVERTURE_EXIGEE ? "ETABLIR" : "ETABLIR_BLOQUE");
        }
        if (signableDes && permissions.contains(PermissionsDecision.SIGNER_PV_GIMAC)) {
            actions.add("SIGNER_GIMAC");
        }
        return actions;
    }

    /** Action du portail : SIGNER_PARTICIPANT tant qu'une signature du portail est signable. */
    public static List<String> actionsPortail(boolean signablePortail, Set<String> permissions) {
        return signablePortail && permissions.contains(PermissionsDecision.PORTAIL_SIGNER) ? List.of("SIGNER_PARTICIPANT") : List.of();
    }

    private static CertificationException nonSignable(String message) {
        return CertificationException.conflit("CER_PV_NOT_SIGNABLE", message);
    }
}
