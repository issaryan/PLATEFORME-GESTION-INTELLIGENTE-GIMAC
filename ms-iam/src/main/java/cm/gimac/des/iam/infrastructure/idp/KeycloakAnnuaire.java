package cm.gimac.des.iam.infrastructure.idp;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.domain.service.PolitiqueMotDePasse;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;

/**
 * Adaptateur réel de l'annuaire : API d'administration de Keycloak, avec le compte de service de
 * MS-IAM (rôles manage-users, view-users, manage-realm, view-realm). Toute panne de Keycloak donne
 * une erreur 503 explicite, jamais une valeur par défaut.
 */
public class KeycloakAnnuaire implements Annuaire {

    static final String TELEPHONE = "phone_number";
    static final String FONCTION = "fonction";
    static final String LANGUE = "locale";
    /** Langue lue par les applications (claim langKey du client scope jhipster), tenue égale à locale. */
    static final String LANGUE_APPLICATION = "langKey";
    static final String INSTITUTION = "institution_id";
    static final String PROFIL_COMPLET = "profil_complet";
    private static final Pattern PERMISSION = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)+$");
    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%*-_";

    private final Keycloak keycloak;
    private final String realm;
    private final Set<String> privilegies;
    private final SecureRandom aleatoire = new SecureRandom();

    public KeycloakAnnuaire(Keycloak keycloak, String realm, Set<String> privilegies) {
        this.keycloak = keycloak;
        this.realm = realm;
        this.privilegies = Set.copyOf(privilegies);
    }

    private RealmResource r() {
        return keycloak.realm(realm);
    }

    private <T> T appel(Supplier<T> action) {
        try {
            return action.get();
        } catch (NotFoundException e) {
            throw IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", "compte");
        } catch (WebApplicationException e) {
            int statut = e.getResponse() == null ? 500 : e.getResponse().getStatus();
            if (statut == 409) {
                throw IamException.conflit("IAM_ACCOUNT_CONFLICT", "Identifiant ou courriel déjà utilisé", Map.of());
            }
            if (statut == 400) {
                throw IamException.invalide("IAM_IDP_REJECTED", "Données refusées par le fournisseur d'identité", Map.of());
            }
            throw IamException.indisponible("IAM_IDP_UNAVAILABLE", "Fournisseur d'identité indisponible");
        } catch (ProcessingException e) {
            throw IamException.indisponible("IAM_IDP_UNAVAILABLE", "Fournisseur d'identité indisponible");
        }
    }

    private void faire(Runnable action) {
        appel(() -> {
            action.run();
            return null;
        });
    }

    // ------------------------------------------------------------------ comptes

    @Override
    public List<Compte> lister() {
        return appel(() -> {
            Map<String, Set<String>> profilsParCompte = profilsParCompte();
            List<Compte> comptes = new ArrayList<>();
            int debut = 0;
            List<UserRepresentation> page;
            do {
                page = r().users().list(debut, 200);
                for (UserRepresentation u : page) {
                    if (u.getServiceAccountClientId() == null && !u.getUsername().startsWith("service-account-")) {
                        comptes.add(vers(u, profilsParCompte.getOrDefault(u.getId(), Set.of()), secondFacteur(u.getId())));
                    }
                }
                debut += page.size();
            } while (page.size() == 200);
            return comptes;
        });
    }

    @Override
    public Optional<Compte> lire(String id) {
        try {
            return Optional.of(appel(() -> {
                UserResource u = r().users().get(id);
                UserRepresentation rep = u.toRepresentation();
                return vers(rep, profilsDirects(u), secondFacteur(id));
            }));
        } catch (IamException e) {
            if (e.status() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public Optional<Compte> parIdentifiant(String identifiant) {
        return appel(() -> r().users().search(identifiant, true).stream().findFirst()).flatMap(u -> lire(u.getId()));
    }

    @Override
    public boolean emailPris(String email, String saufId) {
        return appel(() -> r().users().search(null, null, null, email, 0, 20).stream().anyMatch(u -> email.equalsIgnoreCase(u.getEmail()) && !u.getId().equals(saufId)));
    }

    @Override
    public String creer(NouveauCompte c, List<ActionRequise> actions) {
        UserRepresentation u = new UserRepresentation();
        u.setUsername(c.identifiant());
        u.setFirstName(c.prenom());
        u.setLastName(c.nom());
        u.setEmail(c.email());
        u.setEnabled(true);
        u.setEmailVerified(false);
        u.setRequiredActions(actions.stream().map(Enum::name).toList());
        Map<String, List<String>> attributs = new HashMap<>();
        mettre(attributs, TELEPHONE, c.telephone());
        mettre(attributs, FONCTION, c.fonction());
        mettre(attributs, INSTITUTION, c.institutionId());
        attributs.put(PROFIL_COMPLET, List.of("false"));
        attributs.put(LANGUE, List.of("fr"));
        attributs.put(LANGUE_APPLICATION, List.of("fr"));
        u.setAttributes(attributs);
        String id = appel(() -> {
            try (Response rep = r().users().create(u)) {
                if (rep.getStatus() == 409) {
                    throw IamException.conflit("IAM_ACCOUNT_CONFLICT", "Identifiant ou courriel déjà utilisé", Map.of());
                }
                if (rep.getStatus() != 201) {
                    throw IamException.indisponible("IAM_IDP_UNAVAILABLE", "Création refusée par le fournisseur d'identité (HTTP " + rep.getStatus() + ")");
                }
                String chemin = rep.getLocation().getPath();
                return chemin.substring(chemin.lastIndexOf('/') + 1);
            }
        });
        definirProfils(id, c.profils());
        return id;
    }

    @Override
    public void modifierFiche(String id, FicheCompte f) {
        faire(() -> {
            UserResource u = r().users().get(id);
            UserRepresentation rep = u.toRepresentation();
            if (f.prenom() != null) rep.setFirstName(f.prenom());
            if (f.nom() != null) rep.setLastName(f.nom());
            if (f.email() != null && !f.email().equalsIgnoreCase(rep.getEmail())) {
                rep.setEmail(f.email());
                rep.setEmailVerified(false);
            }
            Map<String, List<String>> a = rep.getAttributes() == null ? new HashMap<>() : new HashMap<>(rep.getAttributes());
            if (f.telephone() != null) mettre(a, TELEPHONE, f.telephone());
            if (f.fonction() != null) mettre(a, FONCTION, f.fonction());
            if (f.langue() != null) {
                mettre(a, LANGUE, f.langue());
                mettre(a, LANGUE_APPLICATION, f.langue());
            }
            if (f.institutionId() != null) mettre(a, INSTITUTION, f.institutionId());
            rep.setAttributes(a);
            u.update(rep);
        });
    }

    @Override
    public void marquerProfilComplet(String id, boolean complet) {
        faire(() -> {
            UserResource u = r().users().get(id);
            UserRepresentation rep = u.toRepresentation();
            Map<String, List<String>> a = rep.getAttributes() == null ? new HashMap<>() : new HashMap<>(rep.getAttributes());
            a.put(PROFIL_COMPLET, List.of(String.valueOf(complet)));
            rep.setAttributes(a);
            u.update(rep);
        });
    }

    @Override
    public void definirProfils(String id, Set<String> profils) {
        faire(() -> {
            UserResource u = r().users().get(id);
            Set<String> actuels = profilsDirects(u);
            List<RoleRepresentation> ajouts = profils.stream().filter(p -> !actuels.contains(p)).map(p -> r().roles().get(p).toRepresentation()).toList();
            List<RoleRepresentation> retraits = actuels.stream().filter(p -> !profils.contains(p)).map(p -> r().roles().get(p).toRepresentation()).toList();
            if (!ajouts.isEmpty()) u.roles().realmLevel().add(ajouts);
            if (!retraits.isEmpty()) u.roles().realmLevel().remove(retraits);
        });
    }

    @Override
    public void definirActif(String id, boolean actif) {
        faire(() -> {
            UserResource u = r().users().get(id);
            UserRepresentation rep = u.toRepresentation();
            rep.setEnabled(actif);
            u.update(rep);
            if (!actif) {
                u.logout();
            }
        });
    }

    @Override
    public void supprimer(String id) {
        faire(() -> {
            try (Response rep = r().users().delete(id)) {
                if (rep.getStatus() == 404) {
                    throw new NotFoundException();
                }
            }
        });
    }

    @Override
    public void exigerActions(String id, Set<ActionRequise> actions) {
        faire(() -> {
            UserResource u = r().users().get(id);
            UserRepresentation rep = u.toRepresentation();
            Set<String> toutes = new LinkedHashSet<>(rep.getRequiredActions() == null ? List.of() : rep.getRequiredActions());
            actions.forEach(a -> toutes.add(a.name()));
            rep.setRequiredActions(List.copyOf(toutes));
            u.update(rep);
        });
    }

    @Override
    public void envoyerInvitation(String id, List<ActionRequise> actions, Duration validite) {
        faire(() -> r().users().get(id).executeActionsEmail(actions.stream().map(Enum::name).toList(), (int) validite.toSeconds()));
    }

    @Override
    public String definirMotDePasseProvisoire(String id) {
        StringBuilder mdp = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            mdp.append(CARACTERES.charAt(aleatoire.nextInt(CARACTERES.length())));
        }
        CredentialRepresentation c = new CredentialRepresentation();
        c.setType(CredentialRepresentation.PASSWORD);
        c.setValue(mdp.toString());
        c.setTemporary(true);
        faire(() -> r().users().get(id).resetPassword(c));
        return mdp.toString();
    }

    @Override
    public void reinitialiserSecondFacteur(String id) {
        faire(() -> {
            UserResource u = r().users().get(id);
            u.credentials().stream().filter(c -> "otp".equals(c.getType())).forEach(c -> u.removeCredential(c.getId()));
        });
        exigerActions(id, Set.of(ActionRequise.CONFIGURE_TOTP));
    }

    @Override
    public List<SessionCompte> sessions(String id) {
        return appel(() -> r().users().get(id).getUserSessions().stream().map(KeycloakAnnuaire::session).toList());
    }

    @Override
    public void fermerSessions(String id) {
        faire(() -> r().users().get(id).logout());
    }

    @Override
    public void fermerSession(String id, String sessionId) {
        faire(() -> {
            boolean sienne = r().users().get(id).getUserSessions().stream().anyMatch(s -> s.getId().equals(sessionId));
            if (!sienne) {
                throw new NotFoundException();
            }
            r().deleteSession(sessionId, false);
        });
    }

    // ------------------------------------------------------------------ configuration du realm

    @Override
    public PolitiqueSecurite politique() {
        return appel(() -> {
            RealmRepresentation rr = r().toRepresentation();
            Map<String, Integer> p = PolitiqueMotDePasse.lire(rr.getPasswordPolicy());
            return new PolitiqueSecurite(
                p.getOrDefault("length", 8),
                p.getOrDefault("upperCase", 0),
                p.getOrDefault("lowerCase", 0),
                p.getOrDefault("digits", 0),
                p.getOrDefault("specialChars", 0),
                p.getOrDefault("passwordHistory", 0),
                p.getOrDefault("forceExpiredPasswordChange", 0),
                p.containsKey("notUsername"),
                valeur(rr.getOtpPolicyDigits(), 6),
                valeur(rr.getOtpPolicyPeriod(), 30),
                rr.getOtpPolicyAlgorithm() == null ? "HmacSHA1" : rr.getOtpPolicyAlgorithm(),
                valeur(rr.getSsoSessionIdleTimeout(), 1800) / 60,
                valeur(rr.getSsoSessionMaxLifespan(), 36000) / 3600,
                Boolean.TRUE.equals(rr.isBruteForceProtected()),
                valeur(rr.getFailureFactor(), 30),
                valeur(rr.getWaitIncrementSeconds(), 60) / 60
            );
        });
    }

    @Override
    public void appliquerPolitique(PolitiqueSecurite p) {
        faire(() -> {
            RealmRepresentation rr = r().toRepresentation();
            rr.setPasswordPolicy(PolitiqueMotDePasse.versChaine(p));
            rr.setOtpPolicyType("totp");
            rr.setOtpPolicyDigits(p.otpChiffres());
            rr.setOtpPolicyPeriod(p.otpPeriodeSecondes());
            rr.setOtpPolicyAlgorithm(p.otpAlgorithme());
            rr.setSsoSessionIdleTimeout(p.sessionInactiviteMinutes() * 60);
            rr.setSsoSessionMaxLifespan(p.sessionDureeMaxHeures() * 3600);
            rr.setBruteForceProtected(p.forceBruteActive());
            rr.setFailureFactor(p.forceBruteEchecsMax());
            rr.setWaitIncrementSeconds(p.forceBruteAttenteMinutes() * 60);
            rr.setMaxFailureWaitSeconds(Math.max(p.forceBruteAttenteMinutes() * 60, 900));
            r().update(rr);
        });
    }

    @Override
    public ConfigurationMessagerie messagerie() {
        return appel(() -> {
            Map<String, String> s = r().toRepresentation().getSmtpServer();
            Map<String, String> m = s == null ? Map.of() : s;
            return new ConfigurationMessagerie(
                m.get("host"),
                m.get("port") == null ? 0 : Integer.parseInt(m.get("port")),
                m.get("from"),
                m.get("fromDisplayName"),
                "true".equals(m.get("starttls")),
                "true".equals(m.get("ssl")),
                "true".equals(m.get("auth")),
                m.get("user"),
                null,
                m.get("password") != null && !m.get("password").isEmpty()
            );
        });
    }

    @Override
    public void appliquerMessagerie(ConfigurationMessagerie c) {
        faire(() -> {
            RealmRepresentation rr = r().toRepresentation();
            Map<String, String> ancien = rr.getSmtpServer() == null ? Map.of() : rr.getSmtpServer();
            rr.setSmtpServer(smtp(c, ancien));
            r().update(rr);
        });
    }

    @Override
    public void testerMessagerie(String destinataire) {
        faire(() -> {
            Map<String, String> s = new HashMap<>(r().toRepresentation().getSmtpServer() == null ? Map.of() : r().toRepresentation().getSmtpServer());
            s.put("to", destinataire);
            try (Response rep = r().testSMTPConnection(s)) {
                if (rep.getStatus() >= 300) {
                    throw IamException.indisponible("IAM_SMTP_FAILED", "Le serveur de messagerie a refusé l'envoi d'essai");
                }
            }
        });
    }

    @Override
    public List<Profil> profils() {
        return appel(() ->
            r().roles().list().stream()
                .filter(role -> role.getName().startsWith("ROLE_") && Boolean.TRUE.equals(role.isComposite()))
                .map(role -> {
                    Set<String> permissions = new java.util.TreeSet<>();
                    r().roles().get(role.getName()).getRoleComposites().stream().map(RoleRepresentation::getName).filter(n -> PERMISSION.matcher(n).matches() && !n.startsWith("ROLE_")).forEach(permissions::add);
                    long titulaires = r().roles().get(role.getName()).getUserMembers(0, 1000).size();
                    return new Profil(role.getName(), role.getDescription(), permissions, privilegies.contains(role.getName()), titulaires);
                })
                .toList()
        );
    }

    @Override
    public List<String> permissions() {
        return appel(() ->
            r().roles().list().stream().map(RoleRepresentation::getName).filter(n -> !n.startsWith("ROLE_") && PERMISSION.matcher(n).matches()).sorted().toList()
        );
    }

    @Override
    public void definirPermissionsProfil(String profil, Set<String> permissions) {
        faire(() -> {
            var role = r().roles().get(profil);
            Set<String> actuelles = new HashSet<>();
            role.getRoleComposites().stream().map(RoleRepresentation::getName).filter(n -> !n.startsWith("ROLE_") && PERMISSION.matcher(n).matches()).forEach(actuelles::add);
            List<RoleRepresentation> ajouts = permissions.stream().filter(p -> !actuelles.contains(p)).map(p -> r().roles().get(p).toRepresentation()).toList();
            List<RoleRepresentation> retraits = actuelles.stream().filter(p -> !permissions.contains(p)).map(p -> r().roles().get(p).toRepresentation()).toList();
            if (!ajouts.isEmpty()) role.addComposites(ajouts);
            if (!retraits.isEmpty()) role.deleteComposites(retraits);
        });
    }

    // ------------------------------------------------------------------ outils

    static Map<String, String> smtp(ConfigurationMessagerie c, Map<String, String> ancien) {
        Map<String, String> s = new HashMap<>();
        s.put("host", c.hote());
        s.put("port", String.valueOf(c.port()));
        s.put("from", c.expediteur());
        s.put("fromDisplayName", c.nomExpediteur() == null ? "" : c.nomExpediteur());
        s.put("starttls", String.valueOf(c.starttls()));
        s.put("ssl", String.valueOf(c.ssl()));
        s.put("auth", String.valueOf(c.authentification()));
        if (c.authentification()) {
            s.put("user", c.utilisateur());
            // Mot de passe en écriture seule : conservé s'il n'est pas ressaisi.
            String mdp = c.motDePasse() != null && !c.motDePasse().isEmpty() ? c.motDePasse() : ancien.get("password");
            if (mdp != null) s.put("password", mdp);
        }
        return s;
    }

    private Map<String, Set<String>> profilsParCompte() {
        Map<String, Set<String>> m = new HashMap<>();
        for (RoleRepresentation role : r().roles().list()) {
            if (role.getName().startsWith("ROLE_") && Boolean.TRUE.equals(role.isComposite())) {
                r().roles().get(role.getName()).getUserMembers(0, 5000).forEach(u -> m.computeIfAbsent(u.getId(), k -> new java.util.TreeSet<>()).add(role.getName()));
            }
        }
        return m;
    }

    private Set<String> profilsDirects(UserResource u) {
        Set<String> s = new java.util.TreeSet<>();
        u.roles().realmLevel().listAll().stream().map(RoleRepresentation::getName).filter(n -> n.startsWith("ROLE_") && !"ROLE_USER".equals(n)).forEach(s::add);
        return s;
    }

    private boolean secondFacteur(String id) {
        return r().users().get(id).credentials().stream().anyMatch(c -> "otp".equals(c.getType()) || "webauthn".equals(c.getType()));
    }

    static Compte vers(UserRepresentation u, Set<String> profils, boolean secondFacteur) {
        Map<String, List<String>> a = u.getAttributes() == null ? Map.of() : u.getAttributes();
        List<ActionRequise> actions = new ArrayList<>();
        if (u.getRequiredActions() != null) {
            for (String x : u.getRequiredActions()) {
                try {
                    actions.add(ActionRequise.valueOf(x));
                } catch (IllegalArgumentException e) {
                    // Action propre au fournisseur, sans objet pour la plateforme.
                }
            }
        }
        return new Compte(
            u.getId(),
            u.getUsername(),
            u.getFirstName(),
            u.getLastName(),
            u.getEmail(),
            Boolean.TRUE.equals(u.isEmailVerified()),
            Boolean.TRUE.equals(u.isEnabled()),
            u.getCreatedTimestamp() == null ? null : Instant.ofEpochMilli(u.getCreatedTimestamp()),
            premier(a, TELEPHONE),
            premier(a, FONCTION),
            premier(a, LANGUE),
            premier(a, INSTITUTION),
            "true".equals(premier(a, PROFIL_COMPLET)),
            profils,
            actions,
            secondFacteur
        );
    }

    static SessionCompte session(UserSessionRepresentation s) {
        return new SessionCompte(
            s.getId(),
            s.getIpAddress(),
            Instant.ofEpochMilli(s.getStart()),
            Instant.ofEpochMilli(s.getLastAccess()),
            s.getClients() == null ? List.of() : List.copyOf(s.getClients().values())
        );
    }

    private static String premier(Map<String, List<String>> a, String cle) {
        List<String> v = a.get(cle);
        return v == null || v.isEmpty() ? null : v.get(0);
    }

    private static void mettre(Map<String, List<String>> a, String cle, String valeur) {
        if (valeur == null || valeur.isBlank()) {
            a.remove(cle);
        } else {
            a.put(cle, List.of(valeur.trim()));
        }
    }

    private static int valeur(Integer v, int defaut) {
        return v == null ? defaut : v;
    }
}
