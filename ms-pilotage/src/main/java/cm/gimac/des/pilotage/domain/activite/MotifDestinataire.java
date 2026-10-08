package cm.gimac.des.pilotage.domain.activite;

/**
 * Façon dont le destinataire d'un rapport a été trouvé (§2.4, « Destinataire du rapport ») : n+1 actif du rattachement,
 * chef de l'unité de l'auteur, chef d'une unité parente (en remontant l'organigramme), ou choisi par l'auteur dans sa
 * chaîne hiérarchique.
 */
public enum MotifDestinataire {
    N_PLUS_UN,
    CHEF_UNITE,
    CHEF_UNITE_PARENTE,
    CHOISI,
}
