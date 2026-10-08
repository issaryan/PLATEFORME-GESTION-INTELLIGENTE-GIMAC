package cm.gimac.des.iam.infrastructure.idp;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.PresentationProfil;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.domain.service.PolitiqueMotDePasse;
import cm.gimac.des.iam.domain.service.ReglesProfils;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
 *
 * <p>D-65 : actions exigées tenues dans l'attribut {@code gimac_actions_requises} (jamais les actions
 * requises de Keycloak, qui bloqueraient l'octroi direct) ; mot de passe actuel vérifié par l'octroi
 * direct du client {@code gimac-connexion} ; code TOTP enregistré par l'extension {@code gimac-totp}
 * avec le jeton du compte de service. Aucun mot de passe, secret ni code n'est journalisé.
 */
public class KeycloakAnnuaire implements Annuaire {

    static final String TELEPHONE = "phone_number";
    static final String FONCTION = "fonction";
    /** Rattachement organisationnel (D-76) : code de l'unité et identifiant du responsable (n+1). */
    static final String UNITE = "unite";
    static final String RESPONSABLE = "responsable_id";
    /** Borne d'une recherche des comptes d'un responsable (l'équipe entière est bornée à 500). */
    private static final int EQUIPE_MAX = 500;
    static final String LANGUE = "locale";
    /** Langue lue par les applications (claim langKey du client scope jhipster), tenue égale à locale. */
    static final String LANGUE_APPLICATION = "langKey";
    static final String INSTITUTION = "institution_id";
    static final String PROFIL_COMPLET = "profil_complet";
    /** Actions exigées par la plateforme (D-65), attribut multivalué déclaré par le profil utilisateur. */
    static final String ACTIONS = "gimac_actions_requises";
    /** Attribut d'un rôle composite créé par l'administration (D-81). */
    static final String PROFIL_CREE = "gimac_profil_cree";
    /** Libellé d'un profil (D-86, docs/lot1/profils-organisation.md §2), attribut du rôle composite. */
    static final String LIBELLE = "gimac_libelle";
    /** Profil externe (D-94, docs/lot1/ecrans-par-configuration.md) : compte d'institution, portail. */
    static final String EXTERNE = "gimac_externe";
    private static final Pattern ERREUR = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CODE_EXTENSION = Pattern.compile("\"code\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JETON_RAFRAICHISSEMENT = Pattern.compile("\"refresh_token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Map<String, String> VIOLATIONS = Map.of(
        "invalidPasswordMinLengthMessage", "LONGUEUR",
        "invalidPasswordMinUpperCaseCharsMessage", "MAJUSCULE",
        "invalidPasswordMinLowerCaseCharsMessage", "MINUSCULE",
        "invalidPasswordMinDigitsMessage", "CHIFFRE",
        "invalidPasswordMinSpecialCharsMessage", "SPECIAL",
        "invalidPasswordNotUsernameMessage", "IDENTIFIANT",
        "invalidPasswordNotEmailMessage", "COURRIEL",
        "invalidPasswordHistoryMessage", "HISTORIQUE"
    );
    private static final Pattern PERMISSION = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)+$");
    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%*-_";

    private final Keycloak keycloak;
    private final String url;
    private final String realm;
    private final String clientConnexion;
    private final String secretConnexion;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final SecureRandom aleatoire = new SecureRandom();

    /**
     * @param url adresse de Keycloak (sans /realms)
     * @param clientConnexion client à octroi direct, mot de passe seul (gimac-connexion)
     * @param secretConnexion son secret, fourni par l'environnement
     */
    public KeycloakAnnuaire(Keycloak keycloak, String url, String realm, String clientConnexion, String secretConnexion) {
        this.keycloak = keycloak;
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.realm = realm;
        this.clientConnexion = clientConnexion;
        this.secretConnexion = secretConnexion;
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

    /** Comptes actifs, lus par pages de 200 sans profils ni second facteur (une requête par page). */
    @Override
    public List<Compte> actifs() {
        return appel(() -> {
            List<Compte> comptes = new ArrayList<>();
            int debut = 0;
            List<UserRepresentation> page;
            do {
                page = r().users().list(debut, 200);
                for (UserRepresentation u : page) {
                    if (Boolean.TRUE.equals(u.isEnabled()) && u.getServiceAccountClientId() == null && !u.getUsername().startsWith("service-account-")) {
                        comptes.add(vers(u, Set.of(), false));
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
    public Optional<Compte> parEmail(String email) {
        String adresse = email.trim().toLowerCase(java.util.Locale.ROOT);
        List<UserRepresentation> trouves = appel(() -> r().users().searchByEmail(adresse, true)).stream().filter(u -> adresse.equalsIgnoreCase(u.getEmail())).toList();
        return trouves.size() == 1 ? lire(trouves.get(0).getId()) : Optional.empty();
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
        Map<String, List<String>> attributs = new HashMap<>();
        attributs.put(ACTIONS, noms(actions));
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
    public void definirRattachement(String id, String unite, String responsableId) {
        faire(() -> {
            UserResource u = r().users().get(id);
            UserRepresentation rep = u.toRepresentation();
            Map<String, List<String>> a = rep.getAttributes() == null ? new HashMap<>() : new HashMap<>(rep.getAttributes());
            mettre(a, UNITE, unite);
            mettre(a, RESPONSABLE, responsableId);
            rep.setAttributes(a);
            u.update(rep);
        });
    }

    /**
     * Recherche par attribut de l'API d'administration (q=responsable_id:X, correspondance exacte),
     * représentation complète pour lire les attributs. Comptes allégés : ni profils ni second facteur.
     */
    @Override
    public List<Compte> parResponsable(String responsableId) {
        // Arguments : first, max, enabled, exact, briefRepresentation, q.
        return appel(() ->
            r().users().searchByAttributes(0, EQUIPE_MAX, null, true, false, RESPONSABLE + ":" + responsableId).stream()
                .filter(u -> u.getServiceAccountClientId() == null && !u.getUsername().startsWith("service-account-"))
                .filter(u -> u.getAttributes() != null && responsableId.equals(premier(u.getAttributes(), RESPONSABLE)))
                .map(u -> vers(u, Set.of(), false))
                .toList()
        );
    }

    /** Recherche par attribut (q=unite:X, correspondance exacte), comme {@link #parResponsable(String)}. */
    @Override
    public List<Compte> parUnite(String unite) {
        return appel(() ->
            r().users().searchByAttributes(0, EQUIPE_MAX, null, true, false, UNITE + ":" + unite).stream()
                .filter(u -> u.getServiceAccountClientId() == null && !u.getUsername().startsWith("service-account-"))
                .filter(u -> u.getAttributes() != null && unite.equals(premier(u.getAttributes(), UNITE)))
                .map(u -> vers(u, Set.of(), false))
                .toList()
        );
    }

    /** Recherche par attribut (q=institution_id:X, correspondance exacte), comptes actifs seulement. */
    @Override
    public List<Compte> parInstitution(String institutionId) {
        return appel(() ->
            r().users().searchByAttributes(0, EQUIPE_MAX, true, true, false, INSTITUTION + ":" + institutionId).stream()
                .filter(u -> u.getServiceAccountClientId() == null && !u.getUsername().startsWith("service-account-"))
                .filter(u -> u.getAttributes() != null && institutionId.equals(premier(u.getAttributes(), INSTITUTION)))
                .map(u -> vers(u, Set.of(), false))
                .toList()
        );
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
        modifierActions(id, actions, true);
    }

    @Override
    public void retirerActions(String id, Set<ActionRequise> actions) {
        modifierActions(id, actions, false);
    }

    private void modifierActions(String id, Set<ActionRequise> actions, boolean ajout) {
        faire(() -> {
            UserResource u = r().users().get(id);
            UserRepresentation rep = u.toRepresentation();
            Map<String, List<String>> a = rep.getAttributes() == null ? new HashMap<>() : new HashMap<>(rep.getAttributes());
            Set<ActionRequise> toutes = new TreeSet<>(actions(a));
            if (ajout) {
                toutes.addAll(actions);
            } else {
                toutes.removeAll(actions);
            }
            a.put(ACTIONS, noms(toutes));
            rep.setAttributes(a);
            u.update(rep);
        });
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
        // Non temporaire : une action requise de Keycloak bloquerait l'octroi direct (D-65) ; le
        // changement est exigé par l'action MOT_DE_PASSE de la plateforme.
        c.setTemporary(false);
        faire(() -> r().users().get(id).resetPassword(c));
        return mdp.toString();
    }

    @Override
    public void definirMotDePasse(String id, String motDePasse) {
        CredentialRepresentation c = new CredentialRepresentation();
        c.setType(CredentialRepresentation.PASSWORD);
        c.setValue(motDePasse);
        c.setTemporary(false);
        try {
            r().users().get(id).resetPassword(c);
        } catch (NotFoundException e) {
            throw IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id);
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 400) {
                throw refusMotDePasse(corps(e.getResponse()));
            }
            throw IamException.indisponible("IAM_IDP_UNAVAILABLE", "Fournisseur d'identité indisponible");
        } catch (ProcessingException e) {
            throw IamException.indisponible("IAM_IDP_UNAVAILABLE", "Fournisseur d'identité indisponible");
        }
    }

    /** Refus de la politique par Keycloak, ramené aux violations de la plateforme. */
    static IamException refusMotDePasse(String corps) {
        Matcher m = ERREUR.matcher(corps == null ? "" : corps);
        String erreur = m.find() ? m.group(1) : "";
        if ("invalidPasswordHistoryMessage".equals(erreur)) {
            return IamException.invalide("IAM_PASSWORD_REUSED", "Le nouveau mot de passe doit différer des précédents", Map.of("violations", List.of("HISTORIQUE")));
        }
        if (erreur.startsWith("invalidPassword")) {
            return IamException.invalide("IAM_PASSWORD_POLICY", "Mot de passe non conforme à la politique de sécurité", Map.of("violations", List.of(VIOLATIONS.getOrDefault(erreur, erreur))));
        }
        return IamException.invalide("IAM_IDP_REJECTED", "Données refusées par le fournisseur d'identité", Map.of());
    }

    @Override
    public boolean verifierMotDePasse(String identifiant, String motDePasse) {
        HttpResponse<String> reponse = envoyer(
            HttpRequest.newBuilder(URI.create(url + "/realms/" + realm + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(formulaire(Map.of("grant_type", "password", "client_id", clientConnexion, "client_secret", secretConnexion, "username", identifiant, "password", motDePasse, "scope", "openid")))
                .build()
        );
        if (reponse.statusCode() == 400 || reponse.statusCode() == 401) {
            return false;
        }
        if (reponse.statusCode() != 200) {
            throw IamException.indisponible("IAM_IDP_UNAVAILABLE", "Fournisseur d'identité indisponible");
        }
        // Vérification seulement : la session ouverte par l'octroi est aussitôt fermée.
        Matcher m = JETON_RAFRAICHISSEMENT.matcher(reponse.body());
        if (m.find()) {
            envoyer(
                HttpRequest.newBuilder(URI.create(url + "/realms/" + realm + "/protocol/openid-connect/logout"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(formulaire(Map.of("client_id", clientConnexion, "client_secret", secretConnexion, "refresh_token", m.group(1))))
                    .build()
            );
        }
        return true;
    }

    @Override
    public void reinitialiserSecondFacteur(String id) {
        faire(() -> {
            UserResource u = r().users().get(id);
            u.credentials().stream().filter(c -> "otp".equals(c.getType())).forEach(c -> u.removeCredential(c.getId()));
        });
    }

    @Override
    public void enregistrerTotp(String id, String secretBase32, String code, String libelle) {
        String jeton = appel(() -> keycloak.tokenManager().getAccessTokenString());
        String corps = "{\"secret\":\"" + json(secretBase32) + "\",\"code\":\"" + json(code) + "\",\"libelle\":" + (libelle == null ? "null" : "\"" + json(libelle) + "\"") + "}";
        HttpResponse<String> reponse = envoyer(
            HttpRequest.newBuilder(URI.create(url + "/realms/" + realm + "/gimac-totp/utilisateurs/" + URLEncoder.encode(id, StandardCharsets.UTF_8) + "/totp"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jeton)
                .POST(HttpRequest.BodyPublishers.ofString(corps))
                .build()
        );
        int statut = reponse.statusCode();
        if (statut == 204 || statut == 200) {
            return;
        }
        Matcher m = CODE_EXTENSION.matcher(reponse.body() == null ? "" : reponse.body());
        String motif = m.find() ? m.group(1) : "";
        if (statut == 400 && "GIMAC_TOTP_CODE_INVALIDE".equals(motif)) {
            throw IamException.invalide("IAM_TOTP_CODE_INVALID", "Code de vérification incorrect", Map.of());
        }
        if (statut == 404 && "GIMAC_TOTP_COMPTE_INTROUVABLE".equals(motif)) {
            throw IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id);
        }
        throw IamException.indisponible("IAM_IDP_UNAVAILABLE", "Enregistrement du code refusé par le fournisseur d'identité (HTTP " + statut + (motif.isEmpty() ? "" : ", " + motif) + ")");
    }

    @Override
    public void marquerEmailVerifie(String id) {
        faire(() -> {
            UserResource u = r().users().get(id);
            UserRepresentation rep = u.toRepresentation();
            rep.setEmailVerified(true);
            u.update(rep);
        });
    }

    private HttpResponse<String> envoyer(HttpRequest requete) {
        try {
            return http.send(requete, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw IamException.indisponible("IAM_IDP_UNAVAILABLE", "Fournisseur d'identité indisponible");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw IamException.indisponible("IAM_IDP_UNAVAILABLE", "Fournisseur d'identité indisponible");
        }
    }

    private static HttpRequest.BodyPublisher formulaire(Map<String, String> champs) {
        return HttpRequest.BodyPublishers.ofString(
            champs.entrySet().stream().map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"))
        );
    }

    static String json(String texte) {
        StringBuilder b = new StringBuilder();
        for (char c : texte.toCharArray()) {
            if (c == '"' || c == '\\') {
                b.append('\\').append(c);
            } else if (c < 0x20) {
                b.append("\\u%04x".formatted((int) c));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String corps(Response r) {
        try {
            return r.readEntity(String.class);
        } catch (RuntimeException e) {
            return "";
        }
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

    /**
     * Profils avec leurs permissions et leur nombre de titulaires ; privilégié par ses permissions (IAM_* ou
     * PLATFORM_*), externe par son attribut gimac_externe ou à défaut une permission *_PORTAL_* (D-94).
     */
    @Override
    public List<Profil> profils() {
        return appel(() ->
            r().roles().list(false).stream()
                .filter(KeycloakAnnuaire::estProfil)
                .map(role -> {
                    Set<String> permissions = permissionsDuRole(role.getName());
                    long titulaires = r().roles().get(role.getName()).getUserMembers(0, 1000).size();
                    return new Profil(role.getName(), libelle(role), role.getDescription(), permissions, ReglesProfils.privilegie(permissions), titulaires, false, null, null, ReglesProfils.externe(attributExterne(role), permissions));
                })
                .toList()
        );
    }

    private Set<String> permissionsDuRole(String code) {
        Set<String> permissions = new java.util.TreeSet<>();
        r().roles().get(code).getRoleComposites().stream().map(RoleRepresentation::getName).filter(n -> PERMISSION.matcher(n).matches() && !n.startsWith("ROLE_")).forEach(permissions::add);
        return permissions;
    }

    /** Compositions des profils, sans compter les titulaires (D-94). */
    @Override
    public Map<String, Set<String>> compositions() {
        return appel(() -> {
            Map<String, Set<String>> m = new java.util.TreeMap<>();
            r().roles().list(false).stream().filter(KeycloakAnnuaire::estProfil).forEach(role -> m.put(role.getName(), permissionsDuRole(role.getName())));
            return m;
        });
    }

    /** Profils externes : attribut gimac_externe ; composition lue seulement pour les rôles sans l'attribut (repli). */
    @Override
    public Set<String> profilsExternes() {
        return appel(() -> {
            Set<String> s = new java.util.TreeSet<>();
            r().roles().list(false).stream()
                .filter(KeycloakAnnuaire::estProfil)
                .filter(role -> {
                    Boolean a = attributExterne(role);
                    return a != null ? a : ReglesProfils.externe(null, permissionsDuRole(role.getName()));
                })
                .forEach(role -> s.add(role.getName()));
            return s;
        });
    }

    @Override
    public Map<String, Boolean> attributsExternes() {
        return appel(() -> {
            Map<String, Boolean> m = new java.util.TreeMap<>();
            r().roles().list(false).stream().filter(KeycloakAnnuaire::estProfil).forEach(role -> {
                Boolean a = attributExterne(role);
                if (a != null) {
                    m.put(role.getName(), a);
                }
            });
            return m;
        });
    }

    /** Attribut gimac_externe du rôle (représentation complète relue), les autres attributs conservés. */
    @Override
    public void definirExterne(String code, boolean externe) {
        try {
            faire(() -> {
                var role = r().roles().get(code);
                RoleRepresentation rep = role.toRepresentation();
                Map<String, List<String>> attributs = new HashMap<>(rep.getAttributes() == null ? Map.of() : rep.getAttributes());
                attributs.put(EXTERNE, List.of(String.valueOf(externe)));
                rep.setAttributes(attributs);
                role.update(rep);
            });
        } catch (IamException e) {
            if ("IAM_ACCOUNT_NOT_FOUND".equals(e.code())) {
                throw IamException.introuvable("IAM_PROFILE_NOT_FOUND", code);
            }
            throw e;
        }
    }

    /** Attribut gimac_externe du rôle : vrai ou faux s'il est posé (casse ignorée), nul sinon. */
    static Boolean attributExterne(RoleRepresentation role) {
        Map<String, List<String>> a = role.getAttributes();
        String v = a == null ? null : premier(a, EXTERNE);
        if (v == null || v.isBlank()) {
            return null;
        }
        return "true".equalsIgnoreCase(v.strip());
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

    /**
     * Rôle composite du realm (D-81) : description, attribut gimac_profil_cree (repère lisible dans le
     * realm ; l'autorité reste la table profil_cree de MS-IAM), permissions et ROLE_USER s'il existe
     * (comme les profils livrés).
     */
    @Override
    public void creerProfil(String code, String libelle, String description, Set<String> permissions) {
        faire(() -> {
            boolean existe = r().roles().list().stream().anyMatch(role -> role.getName().equals(code));
            if (existe) {
                throw IamException.conflit("IAM_PROFIL_EXISTANT", "Un profil porte déjà ce code", Map.of("code", code));
            }
            RoleRepresentation rep = new RoleRepresentation(code, description, false);
            Map<String, List<String>> attributs = new HashMap<>();
            attributs.put(PROFIL_CREE, List.of("true"));
            if (libelle != null && !libelle.isBlank()) {
                attributs.put(LIBELLE, List.of(libelle));
            }
            rep.setAttributes(attributs);
            r().roles().create(rep);
            List<RoleRepresentation> composites = new ArrayList<>();
            for (String p : new TreeSet<>(permissions)) {
                composites.add(r().roles().get(p).toRepresentation());
            }
            r().roles().list().stream().filter(role -> "ROLE_USER".equals(role.getName())).findFirst().ifPresent(composites::add);
            if (!composites.isEmpty()) {
                r().roles().get(code).addComposites(composites);
            }
        });
    }

    @Override
    public void supprimerProfil(String code) {
        faire(() -> r().roles().deleteRole(code));
    }

    /**
     * Libellé et description du rôle (D-86) : représentation complète relue (attributs compris), puis
     * mise à jour ; les autres attributs (gimac_profil_cree) sont conservés.
     */
    @Override
    public void definirPresentationProfil(String code, String libelle, String description) {
        try {
            faire(() -> {
                var role = r().roles().get(code);
                RoleRepresentation rep = role.toRepresentation();
                Map<String, List<String>> attributs = new HashMap<>(rep.getAttributes() == null ? Map.of() : rep.getAttributes());
                if (libelle == null || libelle.isBlank()) {
                    attributs.remove(LIBELLE);
                } else {
                    attributs.put(LIBELLE, List.of(libelle));
                }
                rep.setAttributes(attributs);
                rep.setDescription(description);
                role.update(rep);
            });
        } catch (IamException e) {
            if ("IAM_ACCOUNT_NOT_FOUND".equals(e.code())) {
                throw IamException.introuvable("IAM_PROFILE_NOT_FOUND", code);
            }
            throw e;
        }
    }

    @Override
    public Map<String, PresentationProfil> presentationsProfils() {
        return appel(() -> {
            Map<String, PresentationProfil> m = new java.util.TreeMap<>();
            r().roles().list(false).stream().filter(KeycloakAnnuaire::estProfil).forEach(role -> m.put(role.getName(), PresentationProfil.lue(role.getName(), libelle(role), role.getDescription())));
            return m;
        });
    }

    @Override
    public Map<String, Set<String>> profilsParCompte() {
        return appel(() -> {
            Map<String, Set<String>> m = new HashMap<>();
            for (RoleRepresentation role : r().roles().list()) {
                if (estProfil(role) && !"ROLE_USER".equals(role.getName())) {
                    r().roles().get(role.getName()).getUserMembers(0, 5000).forEach(u -> m.computeIfAbsent(u.getId(), k -> new java.util.TreeSet<>()).add(role.getName()));
                }
            }
            return m;
        });
    }

    private static boolean estProfil(RoleRepresentation role) {
        return role.getName().startsWith("ROLE_") && Boolean.TRUE.equals(role.isComposite());
    }

    /** Attribut gimac_libelle du rôle ; nul s'il est absent ou vide. */
    static String libelle(RoleRepresentation role) {
        Map<String, List<String>> a = role.getAttributes();
        String v = a == null ? null : premier(a, LIBELLE);
        return v == null || v.isBlank() ? null : v;
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
        List<ActionRequise> actions = List.copyOf(actions(a));
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
            secondFacteur,
            premier(a, UNITE),
            premier(a, RESPONSABLE)
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

    /** Actions exigées lues dans l'attribut de la plateforme ; une valeur inconnue est ignorée. */
    static Set<ActionRequise> actions(Map<String, List<String>> a) {
        Set<ActionRequise> s = new TreeSet<>();
        a.getOrDefault(ACTIONS, List.of()).forEach(v -> ActionRequise.lire(v).ifPresent(s::add));
        return s;
    }

    private static List<String> noms(java.util.Collection<ActionRequise> actions) {
        return actions.stream().distinct().sorted().map(Enum::name).toList();
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
