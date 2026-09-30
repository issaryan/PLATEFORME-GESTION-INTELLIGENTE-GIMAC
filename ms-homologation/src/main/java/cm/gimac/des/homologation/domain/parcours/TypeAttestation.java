package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.ArrayList;
import java.util.List;

/** Attestations exigées du participant selon son activité (rangs 21 et 22, D-56). */
public enum TypeAttestation {
    ISO_27001,
    PCI_DSS,
    GSMA,
    COBAC;

    /** PCI DSS pour la carte et le délégataire ; ISO 27001 et GSMA pour le mobile ; COBAC si fintech. */
    public static List<TypeAttestation> exigees(TypeProjet type, boolean fintech) {
        List<TypeAttestation> l = new ArrayList<>(type == TypeProjet.PAIEMENT_MOBILE ? List.of(ISO_27001, GSMA) : List.of(PCI_DSS));
        if (fintech) {
            l.add(COBAC);
        }
        return List.copyOf(l);
    }
}
