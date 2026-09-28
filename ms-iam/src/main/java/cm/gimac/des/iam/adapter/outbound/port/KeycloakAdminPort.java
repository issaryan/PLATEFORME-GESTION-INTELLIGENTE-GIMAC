package cm.gimac.des.iam.adapter.outbound.port;

/**
 * UTI-01 — Port sortant de provisioning Keycloak.
 * ────────────────────────────────────────────────────────────────────────────
 * Keycloak reste l'autorité EXCLUSIVE de l'authentification et du matériel
 * cryptographique. Ce port permet au Service Utilisateurs (référentiel maître
 * métier) de PROVISIONNER Keycloak — sans jamais dupliquer sa logique
 * d'authentification (cf. principe d'identité fédérée).
 */
public interface KeycloakAdminPort {

    /**
     * Crée un compte Keycloak pour un nouvel utilisateur métier (compte désactivé
     * à la création — l'activation Keycloak suit l'activation métier, cf. UTI-05).
     *
     * @param matricule   identifiant métier — utilisé comme {@code username} Keycloak
     * @param email       email professionnel
     * @param nom         nom de famille (attribut Keycloak {@code lastName})
     * @param tenantId    identifiant de l'entité de rattachement — attribut realm {@code tenant_id}
     *                    (User Profile du realm le rend obligatoire)
     * @param codeAgence  code de l'entité de rattachement — attribut realm {@code agence}
     * @param telephone   téléphone professionnel — attribut realm {@code phone_number}
     * @return l'identifiant Keycloak ({@code sub}) du compte créé
     */
    String creerUtilisateur(String matricule, String email, String nom,
                             java.util.UUID tenantId, String codeAgence, String telephone);

    /**
     * Affecte un rôle realm à un utilisateur Keycloak existant.
     *
     * @param keycloakId identifiant Keycloak ({@code sub})
     * @param role       nom du rôle realm (sans préfixe {@code ROLE_} — résolu par l'adaptateur)
     */
    void affecterRoleRealm(String keycloakId, String role);

    /**
     * Retire un rôle realm d'un utilisateur Keycloak.
     */
    void retirerRoleRealm(String keycloakId, String role);

    /**
     * (Ré)affecte l'agence de rattachement d'un utilisateur Keycloak existant. Le
     * rattachement AUTORITAIRE est l'appartenance au GROUPE Keycloak de l'agence
     * (validation-safe, fonctionne même sur les comptes au profil incomplet) ; les
     * attributs realm {@code tenant_id} (= clé métier de l'agence) et {@code agence}
     * (= libellé) sont mis à jour en MIROIR, au mieux (une validation User-Profile qui
     * échoue sur un compte incomplet n'empêche pas le rattachement par groupe).
     * Règle métier : tout utilisateur (hors Super-Admin DSI) DOIT être rattaché.
     *
     * @param keycloakId        identifiant Keycloak ({@code sub})
     * @param cleAgence         clé métier de l'agence (attribut {@code tenant_id})
     * @param libelleAgence     libellé de l'agence (attribut {@code agence})
     * @param groupeKeycloakId  identifiant du groupe Keycloak de l'agence (rattachement autoritaire)
     */
    void affecterAgence(String keycloakId, String cleAgence, String libelleAgence, String groupeKeycloakId);

    /**
     * Liste les GROUPES realm qui représentent des agences (porteurs d'un attribut
     * {@code tenant_id}). Source d'autorité pour synchroniser le référentiel interne
     * {@code AgenceTenant} depuis Keycloak.
     */
    java.util.List<GroupeAgenceKc> listerGroupesAgences();

    /**
     * Écrit l'attribut realm {@code matricule} d'un compte (attribution dynamique).
     *
     * @return {@code true} si l'attribut a été écrit ; {@code false} si la validation
     *         User-Profile du realm a rejeté la mise à jour (compte au profil incomplet)
     */
    boolean definirMatricule(String keycloakId, String matricule);

    /**
     * Liste les rôles realm DIRECTEMENT affectés à un utilisateur Keycloak (sans préfixe
     * {@code ROLE_}). On utilise {@code listAll()} (affectations directes) et non
     * {@code listEffective()} : ce dernier inclut les rôles composites non retirables,
     * ce qui ferait mentir la bascule d'attribution.
     *
     * @param keycloakId identifiant Keycloak ({@code sub})
     * @return codes des rôles metier (préfixe {@code ROLE_} retiré) directement affectés
     */
    java.util.List<String> listerRolesRealm(String keycloakId);

    /**
     * Désactive un compte Keycloak (bascule {@code enabled=false}) — n'efface pas
     * le compte, conforme à la traçabilité exigée (cf. Service Audit).
     */
    void desactiver(String keycloakId);

    /**
     * Liste TOUS les utilisateurs du realm Keycloak (hors comptes de service),
     * référentiel d'autorité des identités (DASH-10). Les comptes de service
     * ({@code service-account-*}) sont exclus : ce ne sont pas des utilisateurs humains.
     *
     * @return projection {@link UtilisateurRealm} de chaque compte du realm
     */
    java.util.List<UtilisateurRealm> listerUtilisateursRealm();

    // ── Gestion complète d'un utilisateur interne (Keycloak-natif, par sub) ──────

    /** Met à jour le profil (prénom/nom/email) d'un compte Keycloak. */
    void mettreAJourProfil(String keycloakId, String prenom, String nom, String email);

    /** Lit l'identité Keycloak d'un compte (prénom, nom, email, username). */
    ProfilKeycloak recupererProfil(String keycloakId);

    /** Active ou désactive un compte ({@code enabled}) — désactivation douce, réversible. */
    void definirActivation(String keycloakId, boolean actif);

    /** Actions requises Keycloak en attente pour l'utilisateur (ex. {@code CONFIGURE_TOTP}). */
    java.util.List<String> listerRequiredActions(String keycloakId);

    /** Impose (ou lève) l'ensemble des actions requises à la prochaine connexion. */
    void definirRequiredActions(String keycloakId, java.util.List<String> actions);

    /** Envoie l'email invitant l'utilisateur à exécuter les actions requises indiquées. */
    void envoyerEmailActions(String keycloakId, java.util.List<String> actions);

    /** Vrai si une identité TOTP (2FA) est déjà configurée sur le compte. */
    boolean totpConfigure(String keycloakId);

    /**
     * Vrai si le compte identifie par ce LOGIN a un second facteur configure.
     *
     * <p>Interroge AVANT l'authentification : l'ecran de connexion ne peut pas deviner
     * s'il doit demander un code, Keycloak repondant « Invalid user credentials » aussi
     * bien pour un mot de passe faux que pour un code manquant.</p>
     *
     * <p>Un login inconnu rend {@code false}, comme un compte sans second facteur : la
     * sonde ne doit pas servir a enumerer les comptes.</p>
     */
    boolean totpConfigurePourLogin(String login);

    /** Supprime les identités TOTP configurées (réinitialisation 2FA). */
    void supprimerTotp(String keycloakId);

    /** Fournisseurs d'identité (OAuth2/OIDC/SAML) configurés dans le realm. */
    java.util.List<IdentityProviderInfo> listerIdentityProviders();

    /** Alias des IdP auxquels le compte est déjà fédéré. */
    java.util.List<String> listerFederationsUtilisateur(String keycloakId);

    /** Lie le compte à un fournisseur d'identité externe (fédération). */
    void lierIdentityProvider(String keycloakId, String alias, String userId, String userName);

    /** Retire la liaison à un fournisseur d'identité. */
    void delierIdentityProvider(String keycloakId, String alias);

    /** Sessions Keycloak actives de l'utilisateur (self-service). */
    java.util.List<SessionInfo> listerSessions(String keycloakId);

    /** Déconnecte TOUTES les sessions de l'utilisateur (révocation globale). */
    void deconnecterToutesSessions(String keycloakId);

    /** Révoque une session Keycloak ciblée. */
    void supprimerSession(String sessionId);

    /** Réinitialise le mot de passe (self-service : force la saisie d'un nouveau via action requise). */
    void demanderChangementMotDePasse(String keycloakId);

    /**
     * Ajoute UNE action requise sans écraser les autres (contrairement à
     * {@link #definirRequiredActions}). Sans dépendance SMTP : l'action est présentée
     * à l'utilisateur à sa prochaine connexion.
     */
    void ajouterRequiredAction(String keycloakId, String action);

    /**
     * Résout l'identifiant Keycloak d'un utilisateur à partir des claims du JWT.
     * Certains clients n'émettent pas de claim {@code sub} :
     * on retombe alors sur une recherche exacte par {@code preferred_username}.
     */
    String resoudreUserId(String sub, String username);
}
