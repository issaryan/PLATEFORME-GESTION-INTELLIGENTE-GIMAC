package cm.gimac.des.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Propriétés de la connexion et des sessions (D-65) : valeurs par défaut du contrat et liaison. */
class ConnexionProprietesTest {

    @Test
    @DisplayName("gimac.connexion : MS-IAM, délai 5 s, 5 tentatives par minute")
    void connexion() {
        ConnexionProperties p = new ConnexionProperties();
        assertThat(p.getDelai()).isEqualTo(Duration.ofSeconds(5));
        assertThat(p.getTentativesParMinute()).isEqualTo(5);
        assertThat(p.getIamUrl()).isEqualTo("http://localhost:8081");
        p.setIamUrl("http://iam:8081");
        p.setDelai(Duration.ofSeconds(2));
        p.setTentativesParMinute(3);
        assertThat(p.getIamUrl()).isEqualTo("http://iam:8081");
        assertThat(p.getDelai()).isEqualTo(Duration.ofSeconds(2));
        assertThat(p.getTentativesParMinute()).isEqualTo(3);
    }

    @Test
    @DisplayName("gimac.session : 30 min d'inactivité, 10 h au plus, balayage chaque minute, témoin SESSION")
    void session() {
        SessionProperties p = new SessionProperties();
        assertThat(p.getInactiviteMax()).isEqualTo(Duration.ofMinutes(30));
        assertThat(p.getDureeMax()).isEqualTo(Duration.ofHours(10));
        assertThat(p.getBalayage()).isEqualTo(Duration.ofMinutes(1));
        assertThat(p.getTemoin()).isEqualTo("SESSION");
        p.setInactiviteMax(Duration.ofMinutes(15));
        p.setDureeMax(Duration.ofHours(8));
        p.setBalayage(Duration.ofSeconds(30));
        p.setTemoin("GIMAC");
        assertThat(p.getInactiviteMax()).isEqualTo(Duration.ofMinutes(15));
        assertThat(p.getDureeMax()).isEqualTo(Duration.ofHours(8));
        assertThat(p.getBalayage()).isEqualTo(Duration.ofSeconds(30));
        assertThat(p.getTemoin()).isEqualTo("GIMAC");
        var c = new ConnexionConfiguration();
        assertThat(c.parametresSession(p).inactiviteMax()).isEqualTo(Duration.ofMinutes(15));
        assertThat(c.registreSessions(p).sessions()).isEmpty();
    }
}
