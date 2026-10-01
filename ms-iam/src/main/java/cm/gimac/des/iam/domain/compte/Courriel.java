package cm.gimac.des.iam.domain.compte;

/** Courriel de la plateforme (D-65) : destinataire, objet, corps en texte et en HTML. */
public record Courriel(String destinataire, String objet, String texte, String html) {
    /** Jamais le corps (il porte le lien à jeton) dans une trace. */
    @Override
    public String toString() {
        return "Courriel[objet=" + objet + "]";
    }
}
