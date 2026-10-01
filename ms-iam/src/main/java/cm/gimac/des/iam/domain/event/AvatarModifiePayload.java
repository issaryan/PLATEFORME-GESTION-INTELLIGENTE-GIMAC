package cm.gimac.des.iam.domain.event;

/**
 * iam.avatar-changed.v1 : photo de profil remplacée ou retirée par son titulaire (docs/lot1/mon-compte.md
 * §1), inscrite au registre d'audit. Jamais l'image : sa version seulement (nulle après un retrait).
 *
 * @param action REMPLACEE ou RETIREE
 */
public record AvatarModifiePayload(String compteId, String identifiant, String action, String version, String acteurId) {
    public static final String TYPE = "iam.avatar-changed.v1";
    public static final String REMPLACEE = "REMPLACEE";
    public static final String RETIREE = "RETIREE";
}
