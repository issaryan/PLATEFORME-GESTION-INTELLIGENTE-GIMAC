package cm.gimac.des.gateway.infrastructure.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.IntegrationTest;
import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import cm.gimac.des.gateway.domain.sandbox.port.ProjectionPerimetres;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Projection d'événements conservée pour les autres usages ; elle ne décide plus de l'accès à la sandbox. */
@IntegrationTest
class ProjectionPerimetresR2dbcIT {

    @Autowired
    ProjectionPerimetres projection;

    @Test
    @DisplayName("Adaptateur R2DBC choisi ; version supérieure appliquée, redélivrance et instantané périmé sans effet")
    void projection() {
        assertThat(projection).isInstanceOf(ProjectionPerimetresR2dbc.class);
        String compte = "u-" + UUID.randomUUID();
        assertThat(projection.lire(compte).blockOptional()).isEmpty();
        assertThat(projection.appliquer(new PerimetreMandats(compte, "99010", Map.of("99002", Instant.parse("2026-11-10T00:00:00Z")), 8)).block()).isTrue();
        assertThat(projection.appliquer(new PerimetreMandats(compte, "99010", Map.of(), 8)).block()).isFalse();
        assertThat(projection.appliquer(new PerimetreMandats(compte, "99010", Map.of(), 7)).block()).isFalse();
        PerimetreMandats lu = projection.lire(compte).block();
        assertThat(lu.version()).isEqualTo(8);
        assertThat(lu.institutionPropre()).isEqualTo("99010");
        assertThat(lu.finMandat("99002")).isEqualTo(Instant.parse("2026-11-10T00:00:00Z"));
        assertThat(projection.appliquer(new PerimetreMandats(compte, "99010", Map.of(), 9)).block()).isTrue();
        assertThat(projection.lire(compte).block().mandantes()).isEmpty();
    }
}
