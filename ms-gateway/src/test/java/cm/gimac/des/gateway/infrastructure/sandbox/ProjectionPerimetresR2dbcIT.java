package cm.gimac.des.gateway.infrastructure.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.IntegrationTest;
import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox.Motif;
import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import cm.gimac.des.gateway.domain.sandbox.RouteSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.JournalDecisionsSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.ProjectionPerimetres;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Projection des périmètres dans la base PostgreSQL de la passerelle (Testcontainers, table Liquibase sbx_perimetre,
 * R2DBC) et refus d'un mandat échu lu dans cette projection (US-EXI-07, test d'intégration du carnet).
 */
@IntegrationTest
class ProjectionPerimetresR2dbcIT {

    static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);

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

    @Test
    @DisplayName("Intégration : refus d'un mandat échu lu dans la projection, 404 et tentative journalisée")
    void refusMandatEchu() {
        String compte = "u-" + UUID.randomUUID();
        projection.appliquer(new PerimetreMandats(compte, "99010", Map.of("99002", Instant.parse("2026-10-01T00:00:00Z"), "99001", Instant.parse("2026-11-10T00:00:00Z")), 1)).block();
        List<JournalDecisionsSandbox.Entree> journal = new ArrayList<>();
        CloisonnementSandbox service = new CloisonnementSandbox(projection, journal::add, Map.of(), j -> Optional.of("jeton"), HORLOGE);
        RouteSandbox route = new RouteSandbox("portail", "campagne");
        CloisonnementSandbox.Resultat echu = service
            .evaluer(new CloisonnementSandbox.Demande(compte, "99010", "99002", route, "GET", "/bff/portail/sbx-campagne/campagnes", List.of(), "2", ""))
            .block();
        assertThat(echu.statut()).isEqualTo(404);
        assertThat(echu.decision().motif()).isEqualTo(Motif.MANDAT_ECHU);
        CloisonnementSandbox.Resultat actif = service
            .evaluer(new CloisonnementSandbox.Demande(compte, "99010", "99001", route, "GET", "/bff/portail/sbx-campagne/campagnes", List.of(), "2", ""))
            .block();
        assertThat(actif.statut()).isEqualTo(200);
        assertThat(actif.decision().institutionEffective()).isEqualTo("99001");
        assertThat(journal).extracting(e -> e.statut()).containsExactly(404, 200);
    }
}
