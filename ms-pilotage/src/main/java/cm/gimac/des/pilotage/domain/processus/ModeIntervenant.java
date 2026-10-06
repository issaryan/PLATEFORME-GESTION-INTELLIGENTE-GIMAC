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

    /** Une personne nommée ou désignée peut quitter le GIMAC : un profil de remplacement est exigé. */
    public boolean exigeRemplacement() {
        return this == PERSONNE || this == AU_LANCEMENT;
    }
}
