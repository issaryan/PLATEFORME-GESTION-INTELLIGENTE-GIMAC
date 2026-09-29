package cm.gimac.des.iam.domain.event;

/** iam.reauthentication-verified.v1 : résultat d'une vérification FIDO2 (IAM-07), réussie ou non. */
public record ReauthentificationPayload(String sujet, String finalite, boolean valide, String motifRefus) {
    public static final String TYPE = "iam.reauthentication-verified.v1";
}
