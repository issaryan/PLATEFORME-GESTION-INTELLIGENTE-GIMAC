package cm.gimac.des.iam.adapter.outbound.port;

/**
 * Fournisseur d'identité (OAuth2/OIDC/SAML) configuré au niveau du realm Keycloak.
 * Utilisé pour la fédération d'identité par utilisateur (liaison compte ↔ IdP externe).
 *
 * @param alias       identifiant du provider dans le realm (ex. {@code google})
 * @param displayName libellé lisible (ex. « Google »)
 * @param actif       provider activé
 */
public record IdentityProviderInfo(String alias, String displayName, boolean actif) {}
