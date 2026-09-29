package cm.gimac.des.gateway.domain.supervision;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Masquage des secrets, sélection des propriétés et synthèse de la plateforme (D-59, D-61). */
class MasqueSecretsTest {

    @Test
    @DisplayName("Noms de propriété sensibles : mot de passe, secret, jeton, clé, magasin de clés")
    void clesSensibles() {
        assertThat(MasqueSecrets.cleSensible("spring.datasource.password")).isTrue();
        assertThat(MasqueSecrets.cleSensible("spring.security.oauth2.client.registration.oidc.client-secret")).isTrue();
        assertThat(MasqueSecrets.cleSensible("gimac.securite.jeton-fantome.secret")).isTrue();
        assertThat(MasqueSecrets.cleSensible("GIMAC_SECRET_TESTER_CLI")).isTrue();
        assertThat(MasqueSecrets.cleSensible("application.signing-key.key")).isTrue();
        assertThat(MasqueSecrets.cleSensible("gimac.s3.cle")).isTrue();
        assertThat(MasqueSecrets.cleSensible("server.ssl.key-store-password")).isTrue();
        assertThat(MasqueSecrets.cleSensible("gimac.azure.sas")).isTrue();
        assertThat(MasqueSecrets.cleSensible("jhipster.security.authentication.jwt.base64-secret")).isTrue();
        assertThat(MasqueSecrets.cleSensible("gimac.supervision.keycloak-sante-url")).isFalse();
        assertThat(MasqueSecrets.cleSensible("spring.kafka.properties.sasl.mechanism")).isFalse();
        assertThat(MasqueSecrets.cleSensible("gimac.services.iam")).isFalse();
        assertThat(MasqueSecrets.cleSensible(null)).isFalse();
    }

    @Test
    @DisplayName("Valeur masquée entièrement si la clé est sensible et non vide")
    void masquerParCle() {
        assertThat(MasqueSecrets.masquer("spring.datasource.password", "motdepasse")).isEqualTo(MasqueSecrets.MASQUE);
        assertThat(MasqueSecrets.masquer("spring.datasource.password", "")).isEmpty();
        assertThat(MasqueSecrets.masquer("gimac.services.iam", "http://iam:8081")).isEqualTo("http://iam:8081");
        assertThat(MasqueSecrets.masquer("x", null)).isNull();
    }

    @Test
    @DisplayName("Valeurs à l'allure de secret : JWT, clé PEM, mot de passe dans l'adresse ou en paramètre")
    void masquerParValeur() {
        assertThat(MasqueSecrets.masquerValeur("Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1In0.c2lnbmF0dXJl")).isEqualTo(MasqueSecrets.MASQUE);
        assertThat(MasqueSecrets.masquerValeur("-----BEGIN PRIVATE KEY-----\nMIIE")).isEqualTo(MasqueSecrets.MASQUE);
        assertThat(MasqueSecrets.masquerValeur("postgresql://gimac:s3cr3t@db:5432/participant")).isEqualTo(
            "postgresql://gimac:******@db:5432/participant"
        );
        assertThat(MasqueSecrets.masquerValeur("jdbc:postgresql://db:5432/p?user=gimac&password=s3cr3t&ssl=true")).isEqualTo(
            "jdbc:postgresql://db:5432/p?user=gimac&password=******&ssl=true"
        );
        assertThat(MasqueSecrets.masquerValeur("Server=db;Pwd=abc;Database=x")).isEqualTo("Server=db;Pwd=******;Database=x");
        assertThat(MasqueSecrets.masquerValeur("http://iam:8081")).isEqualTo("http://iam:8081");
        assertThat(MasqueSecrets.masquerValeur(null)).isNull();
    }

    @Test
    @DisplayName("Sélection : gimac.* et propriétés d'exploitation, triées et masquées ; le reste est écarté")
    void selection() {
        Map<String, String> proprietes = new HashMap<>();
        proprietes.put("gimac.services.iam", "http://iam:8081");
        proprietes.put("gimac.securite.jeton-fantome.secret", "abc");
        proprietes.put("spring.r2dbc.url", "r2dbc:postgresql://u:p@db/gateway");
        proprietes.put("spring.cloud.stream.kafka.binder.brokers", "kafka:9092");
        proprietes.put("spring.datasource.password", "abc");
        proprietes.put("java.home", "/opt/java");
        Map<String, String> retenues = ProprietesExploitation.selectionner(proprietes);
        assertThat(retenues.keySet()).containsExactly(
            "gimac.securite.jeton-fantome.secret",
            "gimac.services.iam",
            "spring.cloud.stream.kafka.binder.brokers",
            "spring.r2dbc.url"
        );
        assertThat(retenues).containsEntry("gimac.securite.jeton-fantome.secret", "******").containsEntry(
            "spring.r2dbc.url",
            "r2dbc:postgresql://u:******@db/gateway"
        );
        assertThat(ProprietesExploitation.retenue(null)).isFalse();
    }

    @Test
    @DisplayName("Statut Spring Boot lu avec tolérance")
    void etatSante() {
        assertThat(EtatSante.depuis("up")).isEqualTo(EtatSante.UP);
        assertThat(EtatSante.depuis("DOWN")).isEqualTo(EtatSante.DOWN);
        assertThat(EtatSante.depuis("OUT_OF_SERVICE")).isEqualTo(EtatSante.OUT_OF_SERVICE);
        assertThat(EtatSante.depuis("UNKNOWN")).isEqualTo(EtatSante.INCONNU);
        assertThat(EtatSante.depuis(null)).isEqualTo(EtatSante.INCONNU);
    }

    static EtatService etat(String nom, EtatSante e) {
        return new EtatService(nom, "http://x", e, 1L, null, InfoService.VIDE, Metriques.VIDES, null, null);
    }

    @Test
    @DisplayName("Synthèse : opérationnelle, dégradée, hors service ; injoignables comptés")
    void etatGlobal() {
        assertThat(EtatGlobal.de(List.of(etat("a", EtatSante.UP), etat("b", EtatSante.UP))).etat()).isEqualTo(EtatGlobal.OPERATIONNELLE);
        EtatGlobal g = EtatGlobal.de(List.of(etat("a", EtatSante.UP), etat("b", EtatSante.DOWN), etat("c", EtatSante.INJOIGNABLE)));
        assertThat(g).isEqualTo(new EtatGlobal(EtatGlobal.DEGRADEE, 3, 1, 2, 1));
        assertThat(EtatGlobal.de(List.of(etat("a", EtatSante.DOWN))).etat()).isEqualTo(EtatGlobal.HORS_SERVICE);
        assertThat(EtatGlobal.de(List.of()).etat()).isEqualTo(EtatGlobal.HORS_SERVICE);
        assertThat(new ServiceInconnuException("x")).hasMessageContaining("x");
        assertThat(new Sante(EtatSante.UP, null).composants()).isEmpty();
    }
}
