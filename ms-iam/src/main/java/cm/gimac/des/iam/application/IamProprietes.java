package cm.gimac.des.iam.application;

import cm.gimac.des.iam.domain.compte.ActionRequise;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Paramètres de MS-IAM (bloc gimac.iam) : fournisseur d'identité, premier accès, profils, FIDO2. */
@ConfigurationProperties(prefix = "gimac.iam")
public record IamProprietes(Keycloak keycloak, PremiereConnexion premiereConnexion, Profils profils, Webauthn webauthn) {
    public record Keycloak(String url, String realm, String clientId, String clientSecret) {}

    public record PremiereConnexion(List<ActionRequise> actions, Duration validiteInvitation) {
        public PremiereConnexion {
            actions = actions == null || actions.isEmpty()
                ? List.of(ActionRequise.UPDATE_PASSWORD, ActionRequise.CONFIGURE_TOTP, ActionRequise.UPDATE_PROFILE)
                : List.copyOf(actions);
            validiteInvitation = validiteInvitation == null ? Duration.ofHours(72) : validiteInvitation;
        }
    }

    public record Profils(Set<String> attribuables, Set<String> privilegies) {
        public Profils {
            attribuables = attribuables == null ? Set.of() : Set.copyOf(attribuables);
            privilegies = privilegies == null ? Set.of() : Set.copyOf(privilegies);
        }
    }

    public record Webauthn(String rpId, String rpNom, List<String> origines, Duration validiteDefi) {
        public Webauthn {
            origines = origines == null ? List.of() : List.copyOf(origines);
            validiteDefi = validiteDefi == null ? Duration.ofMinutes(2) : validiteDefi;
        }
    }
}
