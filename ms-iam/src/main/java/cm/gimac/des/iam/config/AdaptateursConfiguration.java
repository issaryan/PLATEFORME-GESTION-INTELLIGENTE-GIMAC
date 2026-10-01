package cm.gimac.des.iam.config;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.ServiceCourriel;
import cm.gimac.des.iam.infrastructure.courriel.CourrielSmtp;
import cm.gimac.des.iam.infrastructure.courriel.CourrielsEnMemoire;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import cm.gimac.des.iam.infrastructure.idp.KeycloakAnnuaire;
import cm.gimac.des.iam.infrastructure.persistence.ParametreChiffreRepository;
import cm.gimac.des.iam.infrastructure.securite.ChiffrementSecrets;
import com.webauthn4j.WebAuthnManager;
import java.time.Clock;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sélection des adaptateurs de MS-IAM (PI §0.8) : Keycloak en production (gimac.adapters.annuaire=real),
 * annuaire en mémoire en développement et en test ; messagerie SMTP (gimac.adapters.courriel=smtp) ou
 * en mémoire (stub). Horloge, vérificateur WebAuthn, chiffrement des secrets gardés par MS-IAM.
 */
@Configuration
@EnableConfigurationProperties(IamProprietes.class)
public class AdaptateursConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public WebAuthnManager webAuthnManager() {
        // Attestation « none » acceptée : la plateforme vérifie la possession de la clé, pas son modèle.
        return WebAuthnManager.createNonStrictWebAuthnManager();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "gimac.adapters.annuaire", havingValue = "real")
    public org.keycloak.admin.client.Keycloak keycloakAdministration(IamProprietes p) {
        return KeycloakBuilder.builder()
            .serverUrl(p.keycloak().url())
            .realm(p.keycloak().realm())
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId(p.keycloak().clientId())
            .clientSecret(p.keycloak().clientSecret())
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "gimac.adapters.annuaire", havingValue = "real")
    public Annuaire annuaireKeycloak(org.keycloak.admin.client.Keycloak keycloak, IamProprietes p) {
        return new KeycloakAnnuaire(keycloak, p.keycloak().url(), p.keycloak().realm(), p.profils().privilegies(), p.connexion().clientId(), p.connexion().clientSecret());
    }

    @Bean
    public ChiffrementSecrets chiffrementSecrets(IamProprietes p) {
        return new ChiffrementSecrets(p.chiffrement().cle());
    }

    @Bean
    @ConditionalOnProperty(name = "gimac.adapters.courriel", havingValue = "smtp")
    public ServiceCourriel courrielSmtp(Annuaire annuaire, ParametreChiffreRepository parametres, ChiffrementSecrets chiffrement) {
        return new CourrielSmtp(annuaire, parametres, chiffrement);
    }

    @Bean
    @ConditionalOnProperty(name = "gimac.adapters.courriel", havingValue = "stub", matchIfMissing = true)
    public CourrielsEnMemoire courrielsEnMemoire() {
        return new CourrielsEnMemoire();
    }

    @Bean
    @ConditionalOnProperty(name = "gimac.adapters.annuaire", havingValue = "stub")
    public Annuaire annuaireEnMemoire(IamProprietes p) {
        return new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(p.profils().attribuables()), p.profils().privilegies());
    }
}
