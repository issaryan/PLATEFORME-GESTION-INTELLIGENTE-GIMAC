package cm.gimac.des.iam.adapter.outbound.keycloak;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import cm.gimac.des.iam.adapter.outbound.port.IdentityProviderInfo;
import cm.gimac.des.iam.adapter.outbound.port.KeycloakAdminPort;
import cm.gimac.des.iam.adapter.outbound.port.ProfilKeycloak;
import cm.gimac.des.iam.adapter.outbound.port.SessionInfo;
import cm.gimac.des.iam.adapter.outbound.port.UtilisateurRealm;
import cm.gimac.des.iam.config.KeycloakProperties;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * UTI-01 — Adaptateur Admin REST Keycloak (client_credentials, cf. {@code KeycloakConfig}).
 * ────────────────────────────────────────────────────────────────────────────
 * Toute défaillance est propagée (exception non interceptée) : la création d'un
 * utilisateur métier est une opération Dual-Write anti split-brain — Keycloak DOIT
 * réussir AVANT toute persistance JPA locale (cf. MET-27/MET-28, principe d'identité
 * fédérée). Un échec Keycloak doit provoquer un rollback complet côté appelant.
 */
@ConditionalOnProperty(name = "gimac.iam.keycloak-admin.actif", havingValue = "true")
@Component
public class KeycloakAdminAdapter implements KeycloakAdminPort {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminAdapter.class);

    private final Keycloak keycloak;
    private final KeycloakProperties props;

    public KeycloakAdminAdapter(Keycloak keycloak, KeycloakProperties props) {
        this.keycloak = keycloak;
        this.props = props;
    }

    private RealmResource realm() {
        return keycloak.realm(props.getRealm());
    }

    @Override
    public String creerUtilisateur(String matricule, String email, String nom,
                                    UUID tenantId, String codeAgence, String telephone) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(matricule);
        user.setEmail(email);
        user.setLastName(nom);
        user.setEnabled(false); // désactivé à la création — cf. UTI-05/UTI-04 (non-objection COBAC)
        user.setEmailVerified(false);
        // Pas de mot de passe local : Keycloak matérialise l'identité, l'activation
        // effective (mot de passe/2FA) relève du parcours d'onboarding utilisateur.
        user.setCredentials(Collections.<CredentialRepresentation>emptyList());
        // Le User Profile du realm exige ces attributs (error-user-attribute-required
        // sinon) — tenant_id/agence pour le rattachement, phone_number requis par le realm.
        user.setAttributes(Map.of(
            "tenant_id", List.of(tenantId.toString()),
            "agence", List.of(codeAgence),
            "phone_number", List.of(telephone),
            "matricule", List.of(matricule)
        ));

        try (Response response = realm().users().create(user)) {
            if (response.getStatus() != 201) {
                throw new IllegalStateException(
                    "Échec de création Keycloak pour " + matricule + " — HTTP " + response.getStatus());
            }
            String keycloakId = extractIdFromLocation(response);
            log.info("Compte Keycloak provisionné : matricule={} keycloakId={}", matricule, keycloakId);
            return keycloakId;
        }
    }

    @Override
    public void affecterRoleRealm(String keycloakId, String role) {
        UserResource userResource = realm().users().get(keycloakId);
        RoleRepresentation roleRep = realm().roles().get(roleName(role)).toRepresentation();
        userResource.roles().realmLevel().add(List.of(roleRep));
        log.info("Rôle {} affecté à l'utilisateur Keycloak {}", roleName(role), keycloakId);
    }

    @Override
    public void retirerRoleRealm(String keycloakId, String role) {
        UserResource userResource = realm().users().get(keycloakId);
        RoleRepresentation roleRep = realm().roles().get(roleName(role)).toRepresentation();
        userResource.roles().realmLevel().remove(List.of(roleRep));
        log.info("Rôle {} retiré de l'utilisateur Keycloak {}", roleName(role), keycloakId);
    }

    @Override
    public List<String> listerRolesRealm(String keycloakId) {
        UserResource userResource = realm().users().get(keycloakId);
        // listAll() = affectations DIRECTES (pas listEffective, qui inclut les composites
        // non retirables). On ne garde que nos rôles d'autorisation (préfixe ROLE_), ce qui
        // écarte les rôles techniques Keycloak (offline_access, uma_authorization,
        // default-roles-*). On conserve le préfixe pour s'aligner sur le catalogue frontend
        // (data/roles.ts), dont les codes sont ROLE_*.
        return userResource.roles().realmLevel().listAll().stream()
            .map(RoleRepresentation::getName)
            .filter(name -> name.startsWith("ROLE_"))
            .toList();
    }

    @Override
    public List<UtilisateurRealm> listerUtilisateursRealm() {
        // Référentiel d'autorité des identités : on lit directement le realm (DASH-10).
        // list(0, max) : le realm interne reste de taille modeste ; on borne large.
        return realm().users().list(0, 1000).stream()
            .filter(u -> u.getUsername() == null || !u.getUsername().startsWith("service-account-"))
            .map(u -> new UtilisateurRealm(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.isEnabled() != null && u.isEnabled(),
                u.getCreatedTimestamp(),
                premierAttribut(u, "agence"),
                premierAttribut(u, "tenant_id"),
                premierAttribut(u, "matricule")))
            .toList();
    }

    /** Première valeur d'un attribut realm (les attributs Keycloak sont multivalués). */
    private static String premierAttribut(UserRepresentation u, String cle) {
        Map<String, List<String>> attrs = u.getAttributes();
        if (attrs == null) {
            return null;
        }
        List<String> vals = attrs.get(cle);
        return (vals == null || vals.isEmpty()) ? null : vals.get(0);
    }

    @Override
    public void desactiver(String keycloakId) {
        definirActivation(keycloakId, false);
    }

    @Override
    public void affecterAgence(String keycloakId, String cleAgence, String libelleAgence, String groupeKeycloakId) {
        UserResource userResource = realm().users().get(keycloakId);

        // 1) Rattachement AUTORITAIRE et EXCLUSIF : une seule agence par utilisateur.
        //    On quitte d'abord tout autre groupe-agence (les groupes techniques comme
        //    Admins/Users, sans tenant_id, ne sont pas touchés), puis on rejoint la cible.
        //    Non soumis à la validation User-Profile → fiable sur tout compte.
        java.util.Set<String> groupesAgences = listerGroupesAgences().stream()
            .map(cm.gimac.des.iam.adapter.outbound.port.GroupeAgenceKc::idGroupe)
            .collect(java.util.stream.Collectors.toSet());
        for (var g : userResource.groups()) {
            if (groupesAgences.contains(g.getId()) && !g.getId().equals(groupeKeycloakId)) {
                userResource.leaveGroup(g.getId());
            }
        }
        if (groupeKeycloakId != null && !groupeKeycloakId.isBlank()) {
            userResource.joinGroup(groupeKeycloakId);
        }

        // 2) Miroir dans les attributs (lus par l'annuaire / le pipeline). Au mieux :
        //    un compte au profil incomplet (email/nom manquants requis par le realm) fait
        //    échouer update() ; on ne casse pas le rattachement par groupe pour autant.
        try {
            UserRepresentation user = userResource.toRepresentation();
            Map<String, List<String>> attrs =
                user.getAttributes() == null ? new HashMap<>() : new HashMap<>(user.getAttributes());
            attrs.put("tenant_id", List.of(cleAgence));
            if (libelleAgence != null && !libelleAgence.isBlank()) attrs.put("agence", List.of(libelleAgence));
            user.setAttributes(attrs);
            userResource.update(user);
        } catch (RuntimeException ex) {
            log.warn("Attributs d'agence non mis à jour pour {} (profil incomplet ?) — rattachement par groupe conservé : {}",
                keycloakId, ex.getMessage());
        }
        log.info("Agence {} (groupe {}) affectée à l'utilisateur Keycloak {}", cleAgence, groupeKeycloakId, keycloakId);
    }

    @Override
    public java.util.List<cm.gimac.des.iam.adapter.outbound.port.GroupeAgenceKc> listerGroupesAgences() {
        // Les groupes en liste brève n'exposent pas les attributs → on récupère la
        // représentation complète de chaque groupe pour lire l'attribut tenant_id.
        return realm().groups().groups().stream()
            .map(brief -> realm().groups().group(brief.getId()).toRepresentation())
            .map(g -> {
                Map<String, List<String>> attrs = g.getAttributes();
                String tid = attrs != null && attrs.get("tenant_id") != null && !attrs.get("tenant_id").isEmpty()
                    ? attrs.get("tenant_id").get(0) : null;
                return tid == null ? null
                    : new cm.gimac.des.iam.adapter.outbound.port.GroupeAgenceKc(g.getId(), g.getName(), tid);
            })
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    @Override
    public boolean definirMatricule(String keycloakId, String matricule) {
        UserResource userResource = realm().users().get(keycloakId);
        try {
            UserRepresentation user = userResource.toRepresentation();
            Map<String, List<String>> attrs =
                user.getAttributes() == null ? new HashMap<>() : new HashMap<>(user.getAttributes());
            attrs.put("matricule", List.of(matricule));
            user.setAttributes(attrs);
            userResource.update(user);
            log.info("Matricule {} attribué à l'utilisateur Keycloak {}", matricule, keycloakId);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Matricule non attribué à {} (attribut requis manquant sur le compte — cf. User Profile du realm) : {}",
                keycloakId, ex.getMessage());
            return false;
        }
    }

    @Override
    public void mettreAJourProfil(String keycloakId, String prenom, String nom, String email) {
        UserResource userResource = realm().users().get(keycloakId);
        UserRepresentation user = userResource.toRepresentation();
        if (prenom != null) user.setFirstName(prenom);
        if (nom != null) user.setLastName(nom);
        if (email != null) user.setEmail(email);
        userResource.update(user);
        log.info("Profil Keycloak mis à jour : {}", keycloakId);
    }

    @Override
    public ProfilKeycloak recupererProfil(String keycloakId) {
        UserResource userResource = realm().users().get(keycloakId);
        UserRepresentation u = userResource.toRepresentation();

        // Agence : le GROUPE fait autorité (le rattachement y est exclusif et non soumis à
        // la validation User-Profile), l'attribut ne sert que de repli — un compte peut
        // avoir rejoint son groupe sans que le miroir d'attributs ait pu être écrit.
        String agence = null;
        java.util.Set<String> groupesAgences = listerGroupesAgences().stream()
            .map(cm.gimac.des.iam.adapter.outbound.port.GroupeAgenceKc::idGroupe)
            .collect(java.util.stream.Collectors.toSet());
        for (var g : userResource.groups()) {
            if (groupesAgences.contains(g.getId())) {
                agence = g.getName();
                break;
            }
        }
        if (agence == null) {
            agence = premierAttribut(u, "tenant_id");
        }
        if (agence == null) {
            agence = premierAttribut(u, "agence");
        }

        return new ProfilKeycloak(
            u.getFirstName(), u.getLastName(), u.getEmail(), u.getUsername(),
            u.isEmailVerified() != null && u.isEmailVerified(),
            premierAttribut(u, "matricule"), agence);
    }

    @Override
    public void definirActivation(String keycloakId, boolean actif) {
        UserResource userResource = realm().users().get(keycloakId);
        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(actif);
        userResource.update(user);
        log.info("Compte Keycloak {} : {}", actif ? "activé" : "désactivé", keycloakId);
    }

    @Override
    public List<String> listerRequiredActions(String keycloakId) {
        List<String> actions = realm().users().get(keycloakId).toRepresentation().getRequiredActions();
        return actions == null ? List.of() : actions;
    }

    @Override
    public void definirRequiredActions(String keycloakId, List<String> actions) {
        UserResource userResource = realm().users().get(keycloakId);
        UserRepresentation user = userResource.toRepresentation();
        user.setRequiredActions(actions == null ? List.of() : actions);
        userResource.update(user);
        log.info("Actions requises {} définies pour {}", actions, keycloakId);
    }

    @Override
    public void envoyerEmailActions(String keycloakId, List<String> actions) {
        realm().users().get(keycloakId).executeActionsEmail(actions);
        log.info("Email d'actions {} envoyé à {}", actions, keycloakId);
    }

    /**
     * Vrai si le compte identifie par ce login a un second facteur CONFIGURE.
     *
     * <p>Sert AVANT l'authentification, pour savoir s'il faut demander un code. Sans
     * cela, l'ecran de connexion ne peut pas trancher : Keycloak repond « Invalid user
     * credentials » aussi bien pour un mot de passe faux que pour un code manquant, et
     * les deux cas sont indiscernables cote client.</p>
     *
     * <p>Ce que cette methode divulgue est volontairement MINIMAL : un booleen, et rien
     * d'autre. Elle ne dit pas si le compte existe — un login inconnu rend {@code false},
     * exactement comme un compte sans second facteur — de sorte qu'elle ne serve pas a
     * enumerer les comptes.</p>
     */
    @Override
    public boolean totpConfigurePourLogin(String login) {
        if (login == null || login.isBlank()) {
            return false;
        }
        return realm().users().searchByUsername(login.trim(), true).stream()
            .findFirst()
            .or(() -> realm().users().searchByEmail(login.trim(), true).stream().findFirst())
            .map(u -> totpConfigure(u.getId()))
            .orElse(false);
    }

    @Override
    public boolean totpConfigure(String keycloakId) {
        return realm().users().get(keycloakId).credentials().stream()
            .anyMatch(c -> "otp".equalsIgnoreCase(c.getType()));
    }

    @Override
    public void supprimerTotp(String keycloakId) {
        UserResource userResource = realm().users().get(keycloakId);
        userResource.credentials().stream()
            .filter(c -> "otp".equalsIgnoreCase(c.getType()))
            .forEach(c -> userResource.removeCredential(c.getId()));
        log.info("Identités TOTP supprimées pour {}", keycloakId);
    }

    @Override
    public List<IdentityProviderInfo> listerIdentityProviders() {
        return realm().identityProviders().findAll().stream()
            .map(idp -> new IdentityProviderInfo(
                idp.getAlias(),
                idp.getDisplayName() != null ? idp.getDisplayName() : idp.getAlias(),
                idp.isEnabled()))
            .toList();
    }

    @Override
    public List<String> listerFederationsUtilisateur(String keycloakId) {
        return realm().users().get(keycloakId).getFederatedIdentity().stream()
            .map(FederatedIdentityRepresentation::getIdentityProvider)
            .toList();
    }

    @Override
    public List<SessionInfo> listerSessions(String keycloakId) {
        return realm().users().get(keycloakId).getUserSessions().stream()
            .map(s -> new SessionInfo(
                s.getId(),
                s.getIpAddress(),
                s.getStart(),
                s.getLastAccess(),
                s.getClients() != null ? s.getClients() : java.util.Map.of(),
                false))
            .toList();
    }

    @Override
    public void deconnecterToutesSessions(String keycloakId) {
        realm().users().get(keycloakId).logout();
        log.info("Toutes les sessions déconnectées pour {}", keycloakId);
    }

    @Override
    public void supprimerSession(String sessionId) {
        realm().deleteSession(sessionId, false);
        log.info("Session {} révoquée", sessionId);
    }

    @Override
    public void demanderChangementMotDePasse(String keycloakId) {
        // Sans SMTP : on pose l'action requise (présentée à la prochaine connexion)
        // plutôt que d'envoyer un e-mail (executeActionsEmail → 500 sans serveur mail).
        ajouterRequiredAction(keycloakId, "UPDATE_PASSWORD");
    }

    @Override
    public void ajouterRequiredAction(String keycloakId, String action) {
        UserResource userResource = realm().users().get(keycloakId);
        UserRepresentation user = userResource.toRepresentation();
        List<String> actions = new java.util.ArrayList<>(
            user.getRequiredActions() != null ? user.getRequiredActions() : List.of());
        if (!actions.contains(action)) {
            actions.add(action);
        }
        user.setRequiredActions(actions);
        userResource.update(user);
        log.info("Action requise {} ajoutée pour {}", action, keycloakId);
    }

    @Override
    public String resoudreUserId(String sub, String username) {
        // Chemin rapide : le sub est l'identifiant canonique Keycloak quand il est présent.
        if (sub != null && !sub.isBlank() && !"unknown".equalsIgnoreCase(sub)) {
            return sub;
        }
        // Repli : recherche exacte par nom d'utilisateur (clients sans claim sub).
        if (username == null || username.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Identité de l'appelant indéterminée (ni sub ni username)");
        }
        List<UserRepresentation> found = realm().users().search(username, true);
        if (found == null || found.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Utilisateur Keycloak introuvable : " + username);
        }
        return found.get(0).getId();
    }

    @Override
    public void lierIdentityProvider(String keycloakId, String alias, String userId, String userName) {
        FederatedIdentityRepresentation rep = new FederatedIdentityRepresentation();
        rep.setIdentityProvider(alias);
        rep.setUserId(userId);
        rep.setUserName(userName);
        try (Response response = realm().users().get(keycloakId).addFederatedIdentity(alias, rep)) {
            if (response.getStatus() >= 300) {
                throw new IllegalStateException("Échec liaison IdP " + alias + " — HTTP " + response.getStatus());
            }
        }
        log.info("Compte {} fédéré à l'IdP {}", keycloakId, alias);
    }

    @Override
    public void delierIdentityProvider(String keycloakId, String alias) {
        realm().users().get(keycloakId).removeFederatedIdentity(alias);
        log.info("Liaison IdP {} retirée de {}", alias, keycloakId);
    }

    /** Convention du realm : les rôles portent le préfixe {@code ROLE_}. */
    private static String roleName(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    private static String extractIdFromLocation(Response response) {
        String location = response.getLocation().getPath();
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
