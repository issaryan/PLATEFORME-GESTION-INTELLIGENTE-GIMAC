package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.List;

/** Attestations exigées du participant selon son activité (rangs 21 et 22, D-56). */
public enum TypeAttestation {
    ISO_27001,
    PCI_DSS,
    GSMA,
    COBAC;

    /** Attestations exigées d'un type repris, par son modèle (D-91) : voir {@link ModeleTypeProjet#attestationsExigees}. */
    public static List<TypeAttestation> exigees(TypeProjet type, boolean fintech) {
        return ModeleTypeProjet.parDefaut(type).attestationsExigees(fintech);
    }
}
