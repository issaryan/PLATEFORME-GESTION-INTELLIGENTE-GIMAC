package cm.gimac.des.homologation.domain.enumeration;

/**
 * Phase d'une opération de référence. Écart CC corrigé : chaîne libre dans le
 * CC, typée ici car les algorithmes 6.33 et 6.34 traitent à part la phase
 * « mise en service ».
 */
public enum OperationPhase {
    PARAMETRAGE,
    ECHANGE_CERTIFICATS,
    ESSAIS,
    RECETTE,
    MISE_EN_SERVICE,
}
