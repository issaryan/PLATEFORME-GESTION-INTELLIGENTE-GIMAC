package cm.gimac.des.pilotage.domain.activite;

/** État d'un rapport (§2.4) : un rapport SOUMIS n'est plus modifiable, VISE est final. */
public enum EtatRapport {
    BROUILLON,
    SOUMIS,
    VISE,
}
