package cm.gimac.des.iam.application.port.out.sandbox;

/**
 * Verrou d'une tâche planifiée pour la transaction courante (D-119) : une seule réplique de MS-IAM l'exécute à la fois ;
 * le verrou est rendu à la fin de la transaction.
 */
public interface VerrouTaches {
    /** Vrai si le verrou est pris pour cette transaction, faux s'il est déjà tenu ailleurs. */
    boolean prendre(String tache);
}
