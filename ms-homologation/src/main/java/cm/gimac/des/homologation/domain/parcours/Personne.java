package cm.gimac.des.homologation.domain.parcours;

/**
 * Intervenant désigné par son identifiant, son nom et sa fonction. Tant qu'aucune personne n'est affectée à une
 * fonction, la fonction elle-même tient la place ({@link #aAffecter}) : jamais le premier titulaire de l'annuaire
 * (D-93 §1). Une fonction externe est tenue par une institution réelle ({@link #institution}), jamais par un
 * marqueur fictif.
 */
public record Personne(String id, String nom, Fonction fonction) {
    public static final String PREFIXE_A_AFFECTER = "A_AFFECTER:";
    public static final String PREFIXE_INSTITUTION = "EXT-";

    /** Place tenue par la fonction, en attendant l'affectation d'une personne. */
    public static Personne aAffecter(Fonction f) {
        return new Personne(PREFIXE_A_AFFECTER + f.name(), "Non affecté (" + f.name() + ")", f);
    }

    /** Correspondant du participant (qualité Externe) : désigné par son institution. */
    public static Personne correspondant(String institutionId, String institutionNom) {
        return new Personne(PREFIXE_INSTITUTION + institutionId, "Correspondant " + (institutionNom == null ? "du participant" : institutionNom), Fonction.PARTICIPANT);
    }

    /** Institution liée qui tient une fonction externe (banque partenaire, personnalisateur…), D-93 §1. */
    public static Personne institution(Fonction f, String institutionId, String institutionNom) {
        return new Personne(PREFIXE_INSTITUTION + institutionId, institutionNom == null || institutionNom.isBlank() ? f.libelle() : institutionNom, f);
    }

    public boolean affectee() {
        return id != null && !id.startsWith(PREFIXE_A_AFFECTER);
    }

    /** Tenue par une institution (participant ou institution liée), jamais par un compte du GIMAC. */
    public boolean institutionnelle() {
        return id != null && id.startsWith(PREFIXE_INSTITUTION);
    }

    /** Identifiant de l'institution qui tient la fonction, le cas échéant. */
    public String institutionId() {
        return institutionnelle() ? id.substring(PREFIXE_INSTITUTION.length()) : null;
    }

    /** Projection du portail : un intervenant du GIMAC n'apparaît que par sa fonction. */
    public Personne parFonction() {
        return fonction == Fonction.PARTICIPANT || institutionnelle() ? this : new Personne(fonction.name(), fonction.name(), fonction);
    }
}
