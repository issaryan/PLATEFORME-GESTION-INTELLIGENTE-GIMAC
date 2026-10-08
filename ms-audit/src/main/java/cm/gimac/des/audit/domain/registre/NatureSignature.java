package cm.gimac.des.audit.domain.registre;

import java.util.Arrays;
import java.util.Optional;

/**
 * Signatures scellées au registre d'audit (D-93 §6 et §11) : type d'événement CloudEvents, domaine producteur et type
 * d'action inscrits avec l'entrée.
 */
public enum NatureSignature {
    /** Signature d'une étape d'un processus d'unité (MS-PILOTAGE, D-87, sujet pilotage.process). */
    PROCESSUS("pilotage.process-step-signed.v1", "pilotage", "SIGNATURE_PROCESSUS"),
    /** Signature d'une opération d'une liaison d'homologation (MS-HOMOLOGATION, D-91, sujet homologation.operation). */
    OPERATION("homologation.operation-signed.v1", "homologation", "SIGNATURE_OPERATION");

    private final String type;
    private final String domaine;
    private final String action;

    NatureSignature(String type, String domaine, String action) {
        this.type = type;
        this.domaine = domaine;
        this.action = action;
    }

    public String type() {
        return type;
    }

    public String domaine() {
        return domaine;
    }

    public String action() {
        return action;
    }

    /** Nature d'un type d'événement ; tout autre type : vide (non scellé par ce consommateur). */
    public static Optional<NatureSignature> deType(String type) {
        return Arrays.stream(values()).filter(n -> n.type.equals(type)).findFirst();
    }
}
