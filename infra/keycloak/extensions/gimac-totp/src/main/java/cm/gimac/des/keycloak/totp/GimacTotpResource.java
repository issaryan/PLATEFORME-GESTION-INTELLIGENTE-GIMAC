package cm.gimac.des.keycloak.totp;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

/**
 * Code TOTP configuré sur la plateforme (D-65, docs/lot1/connexion.md §4) : MS-IAM affiche le code QR,
 * vérifie un premier code, puis enregistre ici le secret ; Keycloak vérifie à nouveau le code avec la
 * politique OTP du domaine avant de créer l'identifiant OTP. Aucune page de Keycloak n'est montrée.
 *
 * <p>Réservé à un compte de service portant le rôle {@code manage-users} du client
 * {@code realm-management} (le compte de service {@code iam-service}) : 401 sans jeton valide, 403
 * sinon. Ni le secret ni le code ne sont journalisés.
 */
@Produces(MediaType.APPLICATION_JSON)
public class GimacTotpResource {

    static final String CLIENT_GESTION = "realm-management";
    static final String ROLE_GESTION = "manage-users";
    static final int LIBELLE_MAX = 100;
    private static final Pattern BASE32 = Pattern.compile("^[A-Z2-7]{16,128}=*$");
    private static final Pattern CODE = Pattern.compile("^\\d{6,8}$");

    /** Authentifie la requête courante par son jeton porteur ; null si le jeton est absent ou invalide. */
    @FunctionalInterface
    public interface Authentificateur {
        AuthResult authentifier(KeycloakSession session);
    }

    /** Corps de l'enregistrement : secret en base 32, premier code, libellé facultatif. */
    public static class Enregistrement {

        public String secret;
        public String code;
        public String libelle;
    }

    private final KeycloakSession session;
    private final Authentificateur authentificateur;

    public GimacTotpResource(KeycloakSession session, Authentificateur authentificateur) {
        this.session = session;
        this.authentificateur = authentificateur;
    }

    @GET
    @Path("utilisateurs/{id}/totp")
    public Response etat(@PathParam("id") String id) {
        return executer(id, utilisateur -> Response.ok(Map.of("configure", !otp(utilisateur).isEmpty())).build());
    }

    @POST
    @Path("utilisateurs/{id}/totp")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enregistrer(@PathParam("id") String id, Enregistrement corps) {
        return executer(id, utilisateur -> {
            if (corps == null || corps.secret == null || corps.code == null) {
                return erreur(Response.Status.BAD_REQUEST, "GIMAC_TOTP_REQUETE_INVALIDE", "Secret et code obligatoires");
            }
            String secret = corps.secret.trim().toUpperCase(java.util.Locale.ROOT);
            String code = corps.code.trim();
            if (!BASE32.matcher(secret).matches() || !CODE.matcher(code).matches()) {
                return erreur(Response.Status.BAD_REQUEST, "GIMAC_TOTP_REQUETE_INVALIDE", "Secret (base 32) ou code mal formé");
            }
            RealmModel realm = session.getContext().getRealm();
            OTPPolicy politique = realm.getOTPPolicy();
            if (code.length() != politique.getDigits() || !verifier(politique, secret, code)) {
                return erreur(Response.Status.BAD_REQUEST, "GIMAC_TOTP_CODE_INVALIDE", "Code de vérification incorrect");
            }
            // Un seul code par compte : l'éventuel code précédent est remplacé.
            supprimer(utilisateur);
            OTPCredentialModel identifiant = OTPCredentialModel.createTOTP(
                secret,
                politique.getDigits(),
                politique.getPeriod(),
                politique.getAlgorithm(),
                "BASE32"
            );
            identifiant.setUserLabel(libelle(corps.libelle));
            utilisateur.credentialManager().createStoredCredential(identifiant);
            utilisateur.removeRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
            return Response.noContent().build();
        });
    }

    @DELETE
    @Path("utilisateurs/{id}/totp")
    public Response supprimer(@PathParam("id") String id) {
        return executer(id, utilisateur -> {
            supprimer(utilisateur);
            return Response.noContent().build();
        });
    }

    // ------------------------------------------------------------------ outils

    private Response executer(String id, java.util.function.Function<UserModel, Response> action) {
        AuthResult auth;
        try {
            auth = authentificateur.authentifier(session);
        } catch (RuntimeException e) {
            auth = null;
        }
        if (auth == null || auth.getUser() == null) {
            return erreur(Response.Status.UNAUTHORIZED, "GIMAC_TOTP_NON_AUTHENTIFIE", "Jeton porteur valide exigé");
        }
        RealmModel realm = session.getContext().getRealm();
        if (!autorise(realm, auth.getUser())) {
            return erreur(Response.Status.FORBIDDEN, "GIMAC_TOTP_INTERDIT", "Réservé au compte de service d'administration des comptes");
        }
        UserModel utilisateur = id == null ? null : session.users().getUserById(realm, id);
        if (utilisateur == null || utilisateur.getServiceAccountClientLink() != null) {
            return erreur(Response.Status.NOT_FOUND, "GIMAC_TOTP_COMPTE_INTROUVABLE", "Compte introuvable");
        }
        return action.apply(utilisateur);
    }

    static boolean autorise(RealmModel realm, UserModel appelant) {
        if (appelant.getServiceAccountClientLink() == null) {
            return false;
        }
        ClientModel gestion = realm.getClientByClientId(CLIENT_GESTION);
        RoleModel role = gestion == null ? null : gestion.getRole(ROLE_GESTION);
        return role != null && appelant.hasRole(role);
    }

    static boolean verifier(OTPPolicy politique, String secret, String code) {
        byte[] octets;
        try {
            octets = Base32.decode(secret);
        } catch (RuntimeException e) {
            return false;
        }
        return new TimeBasedOTP(politique.getAlgorithm(), politique.getDigits(), politique.getPeriod(), politique.getLookAheadWindow()).validateTOTP(code, octets);
    }

    static String libelle(String libelle) {
        if (libelle == null || libelle.isBlank()) {
            return "Application d'authentification";
        }
        String l = libelle.strip();
        return l.length() > LIBELLE_MAX ? l.substring(0, LIBELLE_MAX) : l;
    }

    private static List<CredentialModel> otp(UserModel utilisateur) {
        return utilisateur.credentialManager().getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE).toList();
    }

    private static void supprimer(UserModel utilisateur) {
        otp(utilisateur).forEach(c -> utilisateur.credentialManager().removeStoredCredentialById(c.getId()));
    }

    private static Response erreur(Response.Status statut, String code, String message) {
        return Response.status(statut).type(MediaType.APPLICATION_JSON).entity(Map.of("code", code, "message", message)).build();
    }
}
