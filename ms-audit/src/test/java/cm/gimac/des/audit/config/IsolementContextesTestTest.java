package cm.gimac.des.audit.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringProperties;

/**
 * Garde de l'isolement des tests d'intégration (US-EXI-10) : sans spring.properties, Spring Framework 7 met en pause
 * les contextes de test inutilisés et leur redémarrage relance les consommateurs @KafkaListener à démarrage manuel, qui
 * liraient alors les sujets sbx-* du courtier localhost:9092 de la recette de la sandbox.
 */
class IsolementContextesTestTest {

    @Test
    void aucunePauseDesContextesDeTest() {
        assertThat(SpringProperties.getProperty("spring.test.context.cache.pause")).isEqualTo("never");
    }
}
