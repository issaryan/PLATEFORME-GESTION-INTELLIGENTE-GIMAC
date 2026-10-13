package cm.gimac.des.participant.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Identifiant attribuable (D-107) : UUID attribué conservé, sinon généré ; à l'insertion seulement. */
class IdentifiantAttribuableGenerateurTest {

    @Test
    @DisplayName("UUID attribué conservé, sinon UUID aléatoire ; identifiants attribués admis")
    void generation() {
        IdentifiantAttribuableGenerateur g = new IdentifiantAttribuableGenerateur();
        UUID attribue = UUID.randomUUID();
        assertThat(g.generate(null, null, attribue, EventType.INSERT)).isEqualTo(attribue);
        assertThat(g.generate(null, null, null, EventType.INSERT)).isInstanceOf(UUID.class).isNotEqualTo(attribue);
        assertThat(g.allowAssignedIdentifiers()).isTrue();
        assertThat(g.getEventTypes()).isEqualTo(EventTypeSets.INSERT_ONLY);
    }
}
