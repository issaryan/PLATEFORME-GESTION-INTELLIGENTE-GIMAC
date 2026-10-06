package cm.gimac.des.pilotage.domain.processus;

import java.util.List;

/**
 * Signature d'une étape d'instance : signataire attendu (affectation résolue à l'activation), état, et une fois
 * signée : signataire, horodatage ISO 8601, empreinte signée (SHA-256 des empreintes des livrables portés) et
 * documents couverts.
 */
public record SignatureInstance(
    String code,
    String libelle,
    int ordre,
    List<String> porte,
    Affectation attendu,
    EtatSignature etat,
    String signePar,
    String signeLe,
    String empreinte,
    List<DocumentSigne> documents
) {
    public SignatureInstance {
        porte = porte == null ? List.of() : List.copyOf(porte);
        documents = documents == null ? List.of() : List.copyOf(documents);
        etat = etat == null ? EtatSignature.ATTENDUE : etat;
    }

    public static SignatureInstance attendue(SignatureRequise s, Affectation attendu) {
        return new SignatureInstance(s.code(), s.libelle(), s.ordre(), s.porte(), attendu, EtatSignature.ATTENDUE, null, null, null, List.of());
    }

    public SignatureInstance signee(String par, String le, String empreinteSignee, List<DocumentSigne> docs) {
        return new SignatureInstance(code, libelle, ordre, porte, attendu, EtatSignature.SIGNEE, par, le, empreinteSignee, docs);
    }

    public SignatureInstance remiseEnAttente() {
        return new SignatureInstance(code, libelle, ordre, porte, attendu, EtatSignature.ATTENDUE, null, null, null, List.of());
    }

    public SignatureInstance reaffectee(Affectation nouvelle) {
        return new SignatureInstance(code, libelle, ordre, porte, nouvelle, etat, signePar, signeLe, empreinte, documents);
    }
}
