package cm.gimac.des.pilotage.domain.activite;

/**
 * État d'un rapport (§2.4) : un rapport SOUMIS n'est plus modifiable, VISE est final ; DEPOSE : rapport transmis sans
 * destinataire (auteur au sommet de l'organisation, aucun responsable nulle part), final, sans visa.
 */
public enum EtatRapport {
    BROUILLON,
    SOUMIS,
    VISE,
    DEPOSE,
}
