package cm.gimac.des.certification.domain.enumeration;

/**
 * Qualité du signataire sur l'objet signé et fonction affichée (CR chapitre 3) : le GIMAC (chef du DES)
 * et le participant signent le procès-verbal ; la conformité et la Direction Générale portent les visas.
 */
public enum RoleSignature {
    GIMAC("CHEF_DES"),
    PARTICIPANT("PARTICIPANT"),
    CONFORMITE("CONFORMITE"),
    DIRECTION_GENERALE("DG");

    private final String fonction;

    RoleSignature(String fonction) {
        this.fonction = fonction;
    }

    public String fonction() {
        return fonction;
    }
}
