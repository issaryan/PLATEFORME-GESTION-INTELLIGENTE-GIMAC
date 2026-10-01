package cm.gimac.des.gateway.test.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * Keycloak simulé (domaine gimac) pour les tests de la connexion sur la plateforme (D-65) :
 * découverte OIDC, JWKS (clé RSA propre au test), point de jetons (octroi direct avec code TOTP en
 * paramètre otp, rafraîchissement, client_credentials), révocation. Les clients confidentiels
 * posent leur niveau d'assurance (acr) ; web_app exige le code.
 */
public final class KeycloakSimule implements AutoCloseable {

    public static final String DOMAINE = "/realms/gimac";

    /** Client confidentiel : secret, acr posé, code TOTP exigé. */
    public record Client(String secret, String acr, boolean codeExige) {}

    /** Compte : sujet (identifiant MS-IAM), mot de passe, code TOTP attendu, actif, claims ajoutés. */
    public record Compte(String sujet, String motDePasse, String code, boolean actif, Map<String, Object> claims) {}

    /** Octroi reçu (jamais le mot de passe). */
    public record Octroi(String client, String type, String identifiant, boolean avecCode) {}

    private final HttpServer serveur;
    private final RSAKey cle;
    private RSAKey cleEtrangere;
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Map<String, Compte> comptes = new ConcurrentHashMap<>();
    private final Map<String, String[]> rafraichissements = new ConcurrentHashMap<>();
    private final List<Octroi> octrois = new CopyOnWriteArrayList<>();
    private final List<String> revoques = new CopyOnWriteArrayList<>();
    private volatile long dureeAcces = 300;
    private volatile int panne;

    public KeycloakSimule() {
        try {
            cle = new RSAKeyGenerator(2048).keyID("cle-test").generate();
            serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException | JOSEException e) {
            throw new IllegalStateException(e);
        }
        serveur.setExecutor(Executors.newCachedThreadPool());
        serveur.createContext("/", this::traiter);
        serveur.start();
        clients.put("gimac-connexion", new Client("secret-connexion", "1", false));
        clients.put("web_app", new Client("secret-web", "2", true));
        clients.put("gateway-service", new Client("secret-service", null, false));
    }

    public String url() {
        return "http://127.0.0.1:" + serveur.getAddress().getPort();
    }

    public String emetteur() {
        return url() + DOMAINE;
    }

    public String pointJetons() {
        return emetteur() + "/protocol/openid-connect/token";
    }

    public String jwks() {
        return emetteur() + "/protocol/openid-connect/certs";
    }

    public KeycloakSimule compte(String identifiant, Compte compte) {
        comptes.put(identifiant, compte);
        return this;
    }

    public KeycloakSimule dureeAcces(long secondes) {
        this.dureeAcces = secondes;
        return this;
    }

    /** Statut renvoyé par le point de jetons (0 : fonctionnement normal). */
    public KeycloakSimule panne(int statut) {
        this.panne = statut;
        return this;
    }

    public List<Octroi> octrois() {
        return octrois;
    }

    public List<String> revoques() {
        return revoques;
    }

    public void reinitialiser() {
        comptes.clear();
        octrois.clear();
        revoques.clear();
        panne = 0;
        dureeAcces = 300;
    }

    /** Jeton signé par la clé du domaine (ou par une autre clé si « etrangere »). */
    public String signer(Map<String, Object> claims, boolean etrangere) {
        try {
            RSAKey k = etrangere ? etrangere() : cle;
            JWTClaimsSet.Builder b = new JWTClaimsSet.Builder();
            claims.forEach((n, v) -> b.claim(n, v instanceof Instant i ? Date.from(i) : v));
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(k.getKeyID()).build(), b.build());
            jwt.sign(new RSASSASigner(k));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Autre clé portant le même identifiant : générée une fois (la génération RSA est lente). */
    private synchronized RSAKey etrangere() throws JOSEException {
        if (cleEtrangere == null) {
            cleEtrangere = new RSAKeyGenerator(2048).keyID("cle-test").generate();
        }
        return cleEtrangere;
    }

    private void traiter(HttpExchange e) throws IOException {
        String chemin = e.getRequestURI().getPath();
        String corps = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            switch (chemin) {
                case DOMAINE + "/.well-known/openid-configuration" -> repondre(e, 200, decouverte());
                case DOMAINE + "/protocol/openid-connect/certs" -> repondre(e, 200, new JWKSet(cle.toPublicJWK()).toString());
                case DOMAINE + "/protocol/openid-connect/token" -> jetons(e, formulaire(corps));
                case DOMAINE + "/protocol/openid-connect/revoke" -> {
                    revoques.add(formulaire(corps).get("token"));
                    repondre(e, 200, "");
                }
                default -> repondre(e, 404, "{}");
            }
        } catch (RuntimeException ex) {
            repondre(e, 500, "{\"error\":\"server_error\"}");
        }
    }

    private String decouverte() {
        String p = emetteur() + "/protocol/openid-connect";
        return (
            "{\"issuer\":\"" + emetteur() + "\",\"authorization_endpoint\":\"" + p + "/auth\",\"token_endpoint\":\"" + p +
            "/token\",\"jwks_uri\":\"" + p + "/certs\",\"userinfo_endpoint\":\"" + p + "/userinfo\",\"end_session_endpoint\":\"" + p +
            "/logout\",\"revocation_endpoint\":\"" + p + "/revoke\",\"response_types_supported\":[\"code\"]," +
            "\"subject_types_supported\":[\"public\"],\"id_token_signing_alg_values_supported\":[\"RS256\"]}"
        );
    }

    private void jetons(HttpExchange e, Map<String, String> f) throws IOException {
        if (panne != 0) {
            repondre(e, panne, "{\"error\":\"temporarily_unavailable\"}");
            return;
        }
        String[] auth = basique(e.getRequestHeaders().getFirst("Authorization"));
        String idClient = auth != null ? auth[0] : f.get("client_id");
        String secret = auth != null ? auth[1] : f.get("client_secret");
        Client client = clients.get(idClient);
        if (client == null || !client.secret().equals(secret)) {
            repondre(e, 401, "{\"error\":\"invalid_client\",\"error_description\":\"Invalid client or Invalid client credentials\"}");
            return;
        }
        String type = f.get("grant_type");
        octrois.add(new Octroi(idClient, type, f.get("username"), f.containsKey("otp")));
        switch (String.valueOf(type)) {
            case "password" -> motDePasse(e, idClient, client, f);
            case "refresh_token" -> {
                String[] lien = rafraichissements.get(String.valueOf(f.get("refresh_token")));
                if (lien == null || !lien[0].equals(idClient) || revoques.contains(f.get("refresh_token"))) {
                    repondre(e, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"Token is not active\"}");
                    return;
                }
                repondre(e, 200, reponseJetons(idClient, client, comptes.get(lien[1]), lien[1]));
            }
            case "client_credentials" -> repondre(
                e,
                200,
                "{\"access_token\":\"" + signer(Map.of("iss", emetteur(), "sub", "service-" + idClient, "exp", Instant.now().plusSeconds(300), "iat", Instant.now()), false) +
                "\",\"token_type\":\"Bearer\",\"expires_in\":300}"
            );
            default -> repondre(e, 400, "{\"error\":\"unsupported_grant_type\"}");
        }
    }

    private void motDePasse(HttpExchange e, String idClient, Client client, Map<String, String> f) throws IOException {
        String identifiant = f.get("username");
        Compte compte = comptes.get(identifiant);
        if (compte == null || !compte.motDePasse().equals(f.get("password"))) {
            repondre(e, 401, "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}");
            return;
        }
        if (!compte.actif()) {
            repondre(e, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"Account disabled\"}");
            return;
        }
        if (client.codeExige() && (compte.code() == null || !compte.code().equals(f.get("otp")))) {
            repondre(e, 401, "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}");
            return;
        }
        repondre(e, 200, reponseJetons(idClient, client, compte, identifiant));
    }

    private String reponseJetons(String idClient, Client client, Compte compte, String identifiant) {
        Instant maintenant = Instant.now();
        Map<String, Object> commun = new HashMap<>();
        commun.put("iss", emetteur());
        commun.put("sub", compte.sujet());
        commun.put("iat", maintenant);
        commun.put("exp", maintenant.plusSeconds(dureeAcces));
        commun.put("preferred_username", identifiant);
        commun.put("sid", UUID.randomUUID().toString());
        if (client.acr() != null) {
            commun.put("acr", client.acr());
        }
        commun.putAll(compte.claims());
        Map<String, Object> acces = new HashMap<>(commun);
        acces.put("aud", List.of("account"));
        acces.put("azp", idClient);
        Map<String, Object> identite = new HashMap<>(commun);
        identite.put("aud", idClient);
        identite.put("azp", idClient);
        String rafraichissement = "rt-" + UUID.randomUUID();
        rafraichissements.put(rafraichissement, new String[] { idClient, identifiant });
        return (
            "{\"access_token\":\"" + signer(acces, false) + "\",\"id_token\":\"" + signer(identite, false) + "\",\"refresh_token\":\"" +
            rafraichissement + "\",\"token_type\":\"Bearer\",\"expires_in\":" + dureeAcces + ",\"scope\":\"openid profile\"}"
        );
    }

    private static String[] basique(String entete) {
        if (entete == null || !entete.startsWith("Basic ")) {
            return null;
        }
        String[] p = new String(Base64.getDecoder().decode(entete.substring(6)), StandardCharsets.UTF_8).split(":", 2);
        return new String[] { URLDecoder.decode(p[0], StandardCharsets.UTF_8), URLDecoder.decode(p[1], StandardCharsets.UTF_8) };
    }

    static Map<String, String> formulaire(String corps) {
        Map<String, String> f = new LinkedHashMap<>();
        for (String paire : corps.split("&")) {
            if (paire.isEmpty()) continue;
            String[] kv = paire.split("=", 2);
            f.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return f;
    }

    private static void repondre(HttpExchange e, int statut, String corps) throws IOException {
        byte[] b = corps.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().add("Content-Type", "application/json");
        e.sendResponseHeaders(statut, b.length == 0 ? -1 : b.length);
        if (b.length > 0) {
            e.getResponseBody().write(b);
        }
        e.close();
    }

    /** Comptes de démonstration usuels. */
    public static Compte interne(String sujet, String... roles) {
        return new Compte(sujet, "Mot-de-passe-1", "123456", true, Map.of("roles", List.of(roles)));
    }

    public static Compte correspondant(String sujet, String institution, String... roles) {
        return new Compte(sujet, "Mot-de-passe-1", "123456", true, Map.of("roles", List.of(roles), "institution_id", institution));
    }

    @Override
    public void close() {
        serveur.stop(0);
    }
}
