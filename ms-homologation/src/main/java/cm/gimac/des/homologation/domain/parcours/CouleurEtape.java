package cm.gimac.des.homologation.domain.parcours;

/**
 * Couleur d'une étape du référentiel (D-89) : jeton du design system, jamais une valeur libre. L'interface lit
 * la variable CSS {@link #jeton()} (avant-plan) et sa variante de fond quand elle existe.
 */
public enum CouleurEtape {
    NEUTRE("Neutre", "--gm-color-status-neutral-fg"),
    INFO("Information", "--gm-color-status-info-fg"),
    SUCCES("Succès", "--gm-color-status-ok-fg"),
    ALERTE("Alerte", "--gm-color-status-warn-fg"),
    CRITIQUE("Critique", "--gm-color-status-crit-fg"),
    ACTION("Action", "--gm-color-action"),
    ACCENT("Accent", "--gm-color-brand-accent");

    private final String libelle;
    private final String jeton;

    CouleurEtape(String libelle, String jeton) {
        this.libelle = libelle;
        this.jeton = jeton;
    }

    public String libelle() {
        return libelle;
    }

    /** Variable CSS du design system. */
    public String jeton() {
        return jeton;
    }
}
