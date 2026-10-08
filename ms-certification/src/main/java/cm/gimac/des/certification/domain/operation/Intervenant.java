package cm.gimac.des.certification.domain.operation;

/**
 * Intervenant d'une opération, lu dans les événements de MS-HOMOLOGATION (Destinataire, D-93 §8.2) : qualité (R, V,
 * C, I), fonction du catalogue, compte interne désigné, institution qui tient une fonction externe, fonction à affecter.
 */
public record Intervenant(String qualite, String fonction, String compteId, String institutionId, boolean aAffecter) {
    public boolean tenuPar(String compte) {
        return compteId != null && compte != null && compteId.equalsIgnoreCase(compte);
    }

    public boolean tenuParInstitution(String institution) {
        return institutionId != null && institution != null && institutionId.equalsIgnoreCase(institution);
    }
}
