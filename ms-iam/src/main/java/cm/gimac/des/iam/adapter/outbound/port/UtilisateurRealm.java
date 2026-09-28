package cm.gimac.des.iam.adapter.outbound.port;

/**
 * Projection d'un utilisateur du realm Keycloak (source d'autorité des identités).
 *
 * Utilisée par la gestion des rôles globaux (DASH-10) : le référentiel affiché est
 * le realm lui-même — tous les utilisateurs internes existent dans Keycloak, alors
 * que la base métier locale ne contient que ceux provisionnés via le service. On
 * s'adapte donc à la base d'autorité (Keycloak) pour ne rien masquer.
 *
 * @param id       identifiant Keycloak ({@code sub}) — sert de clé pour la gestion des rôles
 * @param username nom d'utilisateur Keycloak (matricule ou login)
 * @param email    email
 * @param prenom   prénom ({@code firstName})
 * @param nom      nom ({@code lastName})
 * @param actif        compte activé ({@code enabled})
 * @param dateCreation horodatage de création Keycloak ({@code createdTimestamp}, epoch ms) — ancienneté
 * @param agence       attribut realm {@code agence} — rattachement de l'utilisateur à une agence
 * @param tenantRef    attribut realm {@code tenant_id} — référence du tenant/agence de rattachement
 * @param matricule    attribut realm {@code matricule} — matricule employé ({référence agence}-{référence globale})
 */
public record UtilisateurRealm(
    String id,
    String username,
    String email,
    String prenom,
    String nom,
    boolean actif,
    Long dateCreation,
    String agence,
    String tenantRef,
    String matricule
) {}
