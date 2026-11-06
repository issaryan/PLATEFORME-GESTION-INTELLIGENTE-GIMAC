package cm.gimac.des.pilotage.domain.processus;

/**
 * Désignation d'un intervenant (§1) : par profil (un membre de l'unité portant le profil, tâche à prendre),
 * par personne nommée, par désignation au lancement, chef de l'unité, lanceur, ou n+1 du lanceur.
 */
public enum ModeIntervenant {
    PROFIL,
    PERSONNE,
    AU_LANCEMENT,
    CHEF_UNITE,
    LANCEUR,
    N_PLUS_1_LANCEUR;

    /**
     * Une personne nommée peut quitter le GIMAC : un profil de remplacement est exigé. Pour une personne désignée au
     * lancement (AU_LANCEMENT), il est facultatif : sans lui, la désignation est ouverte à toute personne interne active
     * du GIMAC (D-142) et, au départ de la personne désignée, l'étape revient à l'unité.
     */
    public boolean exigeRemplacement() {
        return this == PERSONNE;
    }
}
