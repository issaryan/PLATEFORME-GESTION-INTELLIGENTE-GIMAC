package cm.gimac.des.iam.domain.event;

import java.util.List;

/**
 * iam.account-administered.v1 : acte d'administration d'un compte (création, fiche, profils,
 * activation, suppression, actions requises, second facteur, sessions), inscrit au registre d'audit.
 */
public record CompteAdministrePayload(String action, String compteId, String identifiant, String acteurId, String motif, List<String> details) {
    public static final String TYPE = "iam.account-administered.v1";
}
