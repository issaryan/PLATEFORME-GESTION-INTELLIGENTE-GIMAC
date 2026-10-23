package cm.gimac.des.iam.domain.compte;

import java.time.Instant;

/**
 * Verrouillage d'un compte par la protection contre la force brute du fournisseur d'identité (IAM-13, US-EXI-23) :
 * appliqué tel quel à tous les comptes, ceux de la sandbox compris, avec le nombre d'échecs paramétré dans la politique
 * de sécurité. Seul l'administrateur du GIMAC le lève depuis la console.
 *
 * @param verrouille compte verrouillé à l'instant de la lecture
 * @param echecs échecs de connexion comptés depuis le dernier succès ou déblocage
 * @param dernierEchec instant du dernier échec, nul si aucun
 */
public record Verrouillage(boolean verrouille, int echecs, Instant dernierEchec) {
    public static final Verrouillage AUCUN = new Verrouillage(false, 0, null);
}
