package cm.gimac.des.certification.domain.operation;

/**
 * Gestes de MS-CERTIFICATION et opération du parcours qui les porte (D-93 §4) : l'opération correspondante est celle
 * dont le déclencheur (MS-HOMOLOGATION, {@code Declencheurs}) est l'événement que MS-CERTIFICATION émet au terme du
 * geste. Aucun rang : seul le contrat d'événement lie les deux services.
 */
public enum GesteCertification {
    ETABLIR_PV(Declencheur.PV_ETABLI, "Établir le procès-verbal de certification"),
    SIGNER_PV(Declencheur.PV_SIGNE, "Signer le procès-verbal de certification"),
    CONSTITUER_DOSSIER(Declencheur.DOSSIER_CHANGEMENT, "Constituer le dossier de changement"),
    DEPOSER_ATTESTATION(Declencheur.ATTESTATIONS_ARCHIVEES, "Déposer une attestation"),
    VERIFIER_ATTESTATION(Declencheur.ATTESTATIONS_ARCHIVEES, "Vérifier une attestation"),
    ARCHIVER_ATTESTATIONS(Declencheur.ATTESTATIONS_ARCHIVEES, "Archiver les attestations"),
    REFERENCER_FICHE(Declencheur.SECURITE_APPROUVEE, "Référencer la fiche de changement"),
    RELIRE_FICHE(Declencheur.SECURITE_APPROUVEE, "Relire la fiche de changement"),
    SIGNER_VISA(Declencheur.SECURITE_APPROUVEE, "Signer un visa");

    private final String declencheur;
    private final String libelle;

    GesteCertification(String declencheur, String libelle) {
        this.declencheur = declencheur;
        this.libelle = libelle;
    }

    /** Type d'événement qui clôt l'opération portant ce geste (déclencheur de l'opération). */
    public String declencheur() {
        return declencheur;
    }

    public String libelle() {
        return libelle;
    }

    /** Événements de MS-CERTIFICATION admis comme déclencheurs par MS-HOMOLOGATION. */
    public static final class Declencheur {

        public static final String PV_ETABLI = "certification.pv-established.v1";
        public static final String PV_SIGNE = "certification.pv-signed.v1";
        public static final String DOSSIER_CHANGEMENT = "certification.change-record-constituted.v1";
        public static final String ATTESTATIONS_ARCHIVEES = "certification.attestations-archived.v1";
        public static final String SECURITE_APPROUVEE = "certification.security-approved.v1";
        public static final String FICHE_CHANGEMENT = "certification.change-record-referenced.v1";

        private Declencheur() {}
    }
}
