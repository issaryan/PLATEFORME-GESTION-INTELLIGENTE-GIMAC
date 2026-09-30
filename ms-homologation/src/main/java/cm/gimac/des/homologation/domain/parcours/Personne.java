package cm.gimac.des.homologation.domain.parcours;

/**
 * Intervenant désigné par son identifiant, son nom et sa fonction. Tant qu'aucune personne n'est
 * affectée à une fonction (rang 6), la fonction elle-même tient la place ({@link #aAffecter}).
 */
public record Personne(String id, String nom, Fonction fonction) {
    public static final String PREFIXE_A_AFFECTER = "A_AFFECTER:";

    /** Place tenue par la fonction, en attendant l'affectation d'une personne. */
    public static Personne aAffecter(Fonction f) {
        return new Personne(PREFIXE_A_AFFECTER + f.name(), "Non affecté (" + f.name() + ")", f);
    }

    /** Correspondant du participant (qualité Externe) : désigné par son institution. */
    public static Personne correspondant(String institutionId, String institutionNom) {
        return new Personne("EXT-" + institutionId, "Correspondant " + (institutionNom == null ? "du participant" : institutionNom), Fonction.PARTICIPANT);
    }

    public boolean affectee() {
        return id != null && !id.startsWith(PREFIXE_A_AFFECTER);
    }

    /** Projection du portail : un intervenant du GIMAC n'apparaît que par sa fonction. */
    public Personne parFonction() {
        return fonction == Fonction.PARTICIPANT ? this : new Personne(fonction.name(), fonction.name(), fonction);
    }
}
