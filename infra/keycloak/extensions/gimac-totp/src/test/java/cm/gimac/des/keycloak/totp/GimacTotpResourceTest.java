package cm.gimac.des.keycloak.totp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

/**
 * Extension gimac-totp (D-65) : authentification du compte de service (401, 403), compte cible (404),
 * vérification du code avec la politique OTP du domaine, création de l'identifiant OTP encodé en base 32,
 * remplacement et suppression.
 */
class GimacTotpResourceTest {

    static final byte[] OCTETS = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    static final String SECRET = Base32.encode(OCTETS);
    static final String ID = "u-1";

    KeycloakSession session = mock(KeycloakSession.class);
    RealmModel realm = mock(RealmModel.class);
    UserProvider users = mock(UserProvider.class);
    UserModel appelant = mock(UserModel.class);
    UserModel cible = mock(UserModel.class);
    SubjectCredentialManager identifiants = mock(SubjectCredentialManager.class);
    ClientModel gestion = mock(ClientModel.class);
    RoleModel manageUsers = mock(RoleModel.class);
    OTPPolicy politique = new OTPPolicy("totp", "HmacSHA1", 0, 6, 1, 30);
    AuthResult auth = new AuthResult(appelant, null, new AccessToken(), null);

    @BeforeEach
    void init() {
        KeycloakContext contexte = mock(KeycloakContext.class);
        when(session.getContext()).thenReturn(contexte);
        when(contexte.getRealm()).thenReturn(realm);
        when(session.users()).thenReturn(users);
        when(realm.getOTPPolicy()).thenReturn(politique);
        when(realm.getClientByClientId("realm-management")).thenReturn(gestion);
        when(gestion.getRole("manage-users")).thenReturn(manageUsers);
        when(appelant.getServiceAccountClientLink()).thenReturn("client-iam-service");
        when(appelant.hasRole(manageUsers)).thenReturn(true);
        when(users.getUserById(realm, ID)).thenReturn(cible);
        when(cible.credentialManager()).thenReturn(identifiants);
        when(identifiants.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE)).thenReturn(Stream.empty());
    }

    GimacTotpResource ressource(AuthResult resultat) {
        return new GimacTotpResource(session, s -> resultat);
    }

    static GimacTotpResource.Enregistrement corps(String secret, String code, String libelle) {
        GimacTotpResource.Enregistrement e = new GimacTotpResource.Enregistrement();
        e.secret = secret;
        e.code = code;
        e.libelle = libelle;
        return e;
    }

    static String codeCourant() {
        return new TimeBasedOTP("HmacSHA1", 6, 30, 1).generateTOTP(OCTETS);
    }

    @SuppressWarnings("unchecked")
    static String code(Response r) {
        return ((Map<String, Object>) r.getEntity()).get("code").toString();
    }

    @Test
    @DisplayName("401 sans jeton valide, y compris quand l'authentification échoue")
    void nonAuthentifie() {
        assertThat(ressource(null).etat(ID).getStatus()).isEqualTo(401);
        assertThat(code(ressource(null).supprimer(ID))).isEqualTo("GIMAC_TOTP_NON_AUTHENTIFIE");
        GimacTotpResource enPanne = new GimacTotpResource(session, s -> {
            throw new IllegalStateException("jeton illisible");
        });
        assertThat(enPanne.etat(ID).getStatus()).isEqualTo(401);
        assertThat(ressource(new AuthResult(null, null, new AccessToken(), null)).etat(ID).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("403 pour un utilisateur, un compte de service sans manage-users ou un domaine sans realm-management")
    void interdit() {
        when(appelant.getServiceAccountClientLink()).thenReturn(null);
        assertThat(ressource(auth).etat(ID).getStatus()).isEqualTo(403);
        when(appelant.getServiceAccountClientLink()).thenReturn("client-autre");
        when(appelant.hasRole(manageUsers)).thenReturn(false);
        Response r = ressource(auth).enregistrer(ID, corps(SECRET, codeCourant(), null));
        assertThat(r.getStatus()).isEqualTo(403);
        assertThat(code(r)).isEqualTo("GIMAC_TOTP_INTERDIT");
        when(gestion.getRole("manage-users")).thenReturn(null);
        assertThat(ressource(auth).etat(ID).getStatus()).isEqualTo(403);
        when(realm.getClientByClientId("realm-management")).thenReturn(null);
        assertThat(ressource(auth).etat(ID).getStatus()).isEqualTo(403);
        verify(identifiants, never()).createStoredCredential(any());
    }

    @Test
    @DisplayName("404 pour un compte inconnu ou un compte de service")
    void introuvable() {
        assertThat(ressource(auth).etat("inconnu").getStatus()).isEqualTo(404);
        assertThat(ressource(auth).etat(null).getStatus()).isEqualTo(404);
        when(cible.getServiceAccountClientLink()).thenReturn("client-x");
        assertThat(code(ressource(auth).etat(ID))).isEqualTo("GIMAC_TOTP_COMPTE_INTROUVABLE");
    }

    @Test
    @DisplayName("GET : configure selon la présence d'un identifiant OTP")
    @SuppressWarnings("unchecked")
    void etat() {
        assertThat((Map<String, Object>) ressource(auth).etat(ID).getEntity()).containsEntry("configure", false);
        when(identifiants.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE)).thenReturn(Stream.of(new CredentialModel()));
        Response r = ressource(auth).etat(ID);
        assertThat(r.getStatus()).isEqualTo(200);
        assertThat((Map<String, Object>) r.getEntity()).containsEntry("configure", true);
    }

    @Test
    @DisplayName("POST : code juste → identifiant OTP en base 32 selon la politique, ancien code remplacé, action Keycloak retirée")
    void enregistrement() {
        CredentialModel ancien = new CredentialModel();
        ancien.setId("ancien");
        when(identifiants.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE)).thenReturn(Stream.of(ancien));
        Response r = ressource(auth).enregistrer(ID, corps(SECRET.toLowerCase(), " " + codeCourant() + " ", "  Téléphone de bureau  "));
        assertThat(r.getStatus()).isEqualTo(204);
        verify(identifiants).removeStoredCredentialById("ancien");
        ArgumentCaptor<CredentialModel> cree = ArgumentCaptor.forClass(CredentialModel.class);
        verify(identifiants).createStoredCredential(cree.capture());
        OTPCredentialModel otp = (OTPCredentialModel) cree.getValue();
        assertThat(otp.getType()).isEqualTo(OTPCredentialModel.TYPE);
        assertThat(otp.getUserLabel()).isEqualTo("Téléphone de bureau");
        assertThat(otp.getDecodedSecret()).isEqualTo(OCTETS);
        assertThat(otp.getOTPCredentialData().getDigits()).isEqualTo(6);
        assertThat(otp.getOTPCredentialData().getPeriod()).isEqualTo(30);
        assertThat(otp.getOTPCredentialData().getAlgorithm()).isEqualTo("HmacSHA1");
        verify(cible).removeRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
    }

    @Test
    @DisplayName("POST : corps absent ou mal formé, code faux ou de mauvaise longueur → 400, rien n'est créé")
    void refus() {
        GimacTotpResource r = ressource(auth);
        assertThat(code(r.enregistrer(ID, null))).isEqualTo("GIMAC_TOTP_REQUETE_INVALIDE");
        assertThat(code(r.enregistrer(ID, corps(null, "123456", null)))).isEqualTo("GIMAC_TOTP_REQUETE_INVALIDE");
        assertThat(code(r.enregistrer(ID, corps(SECRET, null, null)))).isEqualTo("GIMAC_TOTP_REQUETE_INVALIDE");
        assertThat(code(r.enregistrer(ID, corps("pas-du-base32!", "123456", null)))).isEqualTo("GIMAC_TOTP_REQUETE_INVALIDE");
        assertThat(code(r.enregistrer(ID, corps(SECRET, "12ab56", null)))).isEqualTo("GIMAC_TOTP_REQUETE_INVALIDE");
        String faux = codeCourant().equals("000000") ? "111111" : "000000";
        Response f = r.enregistrer(ID, corps(SECRET, faux, null));
        assertThat(f.getStatus()).isEqualTo(400);
        assertThat(code(f)).isEqualTo("GIMAC_TOTP_CODE_INVALIDE");
        assertThat(code(r.enregistrer(ID, corps(SECRET, codeCourant() + "12", null)))).isEqualTo("GIMAC_TOTP_CODE_INVALIDE");
        verify(identifiants, never()).createStoredCredential(any());
        verify(identifiants, never()).removeStoredCredentialById(anyString());
    }

    @Test
    @DisplayName("DELETE : tous les identifiants OTP supprimés")
    void suppression() {
        CredentialModel a = new CredentialModel();
        a.setId("a");
        CredentialModel b = new CredentialModel();
        b.setId("b");
        when(identifiants.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE)).thenReturn(Stream.of(a, b));
        assertThat(ressource(auth).supprimer(ID).getStatus()).isEqualTo(204);
        verify(identifiants).removeStoredCredentialById("a");
        verify(identifiants).removeStoredCredentialById("b");
    }

    @Test
    @DisplayName("Outils : libellé par défaut et borné, secret indécodable refusé")
    void outils() {
        assertThat(GimacTotpResource.libelle(null)).isEqualTo("Application d'authentification");
        assertThat(GimacTotpResource.libelle("  ")).isEqualTo("Application d'authentification");
        assertThat(GimacTotpResource.libelle("x".repeat(150))).hasSize(GimacTotpResource.LIBELLE_MAX);
        assertThat(GimacTotpResource.verifier(politique, "1!", "123456")).isFalse();
        assertThat(GimacTotpResource.verifier(politique, SECRET, codeCourant())).isTrue();
    }

    @Test
    @DisplayName("Fabrique et fournisseur : identifiant gimac-totp, ressource servie")
    void fabrique() {
        GimacTotpResourceProviderFactory f = new GimacTotpResourceProviderFactory();
        f.init(null);
        f.postInit(null);
        assertThat(f.getId()).isEqualTo("gimac-totp");
        var fournisseur = f.create(session);
        assertThat(fournisseur.getResource()).isInstanceOf(GimacTotpResource.class);
        fournisseur.close();
        f.close();
    }
}
