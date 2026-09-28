package cm.gimac.des.iam.adapter.outbound.port;

/**
 * Identité brute d'un compte Keycloak (toujours présente, contrairement à la fiche
 * métier en base qui peut ne pas exister). Alimente l'onglet « Identité » de Mon profil.
 *
 * <p>Keycloak FAIT AUTORITÉ sur le rattachement : l'agence vient de son groupe realm et
 * le matricule de son attribut. Les lire ici (plutôt que dans la seule fiche métier)
 * évite d'afficher « — » à un utilisateur pourtant affecté.</p>
 */
public record ProfilKeycloak(
    String prenom,
    String nom,
    String email,
    String username,
    boolean emailVerifie,
    /** Matricule dynamique ({@code AG-YDE-01-01007}) — attribut Keycloak, autorité. */
    String matricule,
    /** Clé de l'agence de rattachement ({@code AG-YDE-01}) — groupe Keycloak, autorité. */
    String agence
) {}
