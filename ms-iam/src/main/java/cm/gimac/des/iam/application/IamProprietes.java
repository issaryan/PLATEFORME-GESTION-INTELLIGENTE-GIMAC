package cm.gimac.des.iam.application;

import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Couche;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres de MS-IAM (bloc gimac.iam) : fournisseur d'identité, premier accès, profils, FIDO2, liens
 * des courriels de la plateforme, client de connexion (vérification du mot de passe actuel), clé de
 * chiffrement des secrets gardés par MS-IAM (D-62, D-65).
 */
@ConfigurationProperties(prefix = "gimac.iam")
public record IamProprietes(
    Keycloak keycloak,
    PremiereConnexion premiereConnexion,
    Profils profils,
    Webauthn webauthn,
    Liens liens,
    Connexion connexion,
    Chiffrement chiffrement
) {
    public IamProprietes {
        premiereConnexion = premiereConnexion == null ? new PremiereConnexion(null) : premiereConnexion;
        profils = profils == null ? new Profils(null, null) : profils;
        liens = liens == null ? new Liens(null, null, null, null, null, null, null) : liens;
        connexion = connexion == null ? new Connexion(null, null, null) : connexion;
        chiffrement = chiffrement == null ? new Chiffrement(null) : chiffrement;
    }

    public record Keycloak(String url, String realm, String clientId, String clientSecret) {}

    /** Actions exigées d'un compte créé par l'administration (premier accès, D-65). */
    public record PremiereConnexion(List<ActionRequise> actions) {
        public PremiereConnexion {
            actions = actions == null || actions.isEmpty()
                ? List.of(ActionRequise.MOT_DE_PASSE, ActionRequise.SECOND_FACTEUR, ActionRequise.FICHE, ActionRequise.CHARTE)
                : List.copyOf(actions);
        }
    }

    /**
     * Profils livrés attribuables (rôles composites du realm). Aucune qualité n'y est décrite par nom (D-94) :
     * un profil est privilégié par ses permissions (IAM_* ou PLATFORM_*) et externe par son attribut
     * gimac_externe. {@code externesInitiaux} : profils qui reçoivent gimac_externe=true au démarrage quand
     * l'attribut n'est pas encore posé (valeur de départ, modifiable ensuite depuis la console).
     */
    public record Profils(Set<String> attribuables, Set<String> externesInitiaux) {
        public Profils {
            attribuables = attribuables == null ? Set.of() : Set.copyOf(attribuables);
            externesInitiaux = externesInitiaux == null ? Set.of() : Set.copyOf(externesInitiaux);
        }
    }

    public record Webauthn(String rpId, String rpNom, List<String> origines, Duration validiteDefi) {
        public Webauthn {
            origines = origines == null ? List.of() : List.copyOf(origines);
            validiteDefi = validiteDefi == null ? Duration.ofMinutes(2) : validiteDefi;
        }
    }

    /**
     * Adresses publiques des couches, sur lesquelles se construisent les liens des courriels
     * ({@code /connexion/nouveau-mot-de-passe?jeton=…}), et durées de validité : invitation 24 h,
     * réinitialisation 30 min, enrôlement TOTP 10 min. {@code permissionsConsole} (D-94) : permissions dont
     * l'une suffit pour que les liens d'un compte mènent à la console (même liste que PERMISSIONS_CONSOLE de
     * l'interface, frontend/packages/socle/src/session/aiguillage.ts).
     */
    public record Liens(
        String des,
        String portail,
        String console,
        Duration validiteInvitation,
        Duration validiteReinitialisation,
        Duration validiteEnrolementTotp,
        Set<String> permissionsConsole
    ) {
        /** Permissions qui ouvrent la console, par défaut (aiguillage.ts, PERMISSIONS_CONSOLE). */
        public static final Set<String> PERMISSIONS_CONSOLE = Set.of(
            "IAM_ACCOUNT_READ",
            "PLATFORM_SUPERVISION_READ",
            "PLATFORM_CONFIG_READ",
            "PLATFORM_OPS_READ",
            "PLATFORM_SECURITY_POLICY_READ",
            "PLATFORM_MESSAGING_EDIT"
        );

        public Liens {
            permissionsConsole = permissionsConsole == null || permissionsConsole.isEmpty() ? PERMISSIONS_CONSOLE : Set.copyOf(permissionsConsole);
            des = des == null || des.isBlank() ? "http://localhost:4200" : des;
            portail = portail == null || portail.isBlank() ? "http://localhost:4201" : portail;
            console = console == null || console.isBlank() ? "http://localhost:4202" : console;
            validiteInvitation = validiteInvitation == null ? Duration.ofHours(24) : validiteInvitation;
            validiteReinitialisation = validiteReinitialisation == null ? Duration.ofMinutes(30) : validiteReinitialisation;
            validiteEnrolementTotp = validiteEnrolementTotp == null ? Duration.ofMinutes(10) : validiteEnrolementTotp;
        }

        /** Adresse publique d'une couche, sans barre oblique finale. */
        public String adresse(Couche couche) {
            String a = switch (couche) {
                case DES -> des;
                case PORTAIL -> portail;
                case CONSOLE -> console;
            };
            return a.endsWith("/") ? a.substring(0, a.length() - 1) : a;
        }
    }

    /**
     * Client {@code gimac-connexion} (octroi direct, mot de passe seul) avec lequel MS-IAM vérifie le mot de
     * passe actuel ; clients de service admis sur les routes internes de connexion (la passerelle).
     */
    public record Connexion(String clientId, String clientSecret, Set<String> clientsInternes) {
        public Connexion {
            clientId = clientId == null || clientId.isBlank() ? "gimac-connexion" : clientId;
            clientsInternes = clientsInternes == null ? Set.of("gateway-service") : Set.copyOf(clientsInternes);
        }
    }

    /** Clé AES-256 (base 64) des secrets gardés par MS-IAM : enrôlements TOTP, mot de passe SMTP. */
    public record Chiffrement(String cle) {}
}
