package cm.gimac.des.gateway.infrastructure.sandbox;

import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import cm.gimac.des.gateway.domain.sandbox.port.ProjectionPerimetres;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Projection des périmètres dans la base PostgreSQL de la passerelle (table sbx_perimetre, Liquibase) : une ligne par
 * compte, remplacée seulement par une version supérieure (insert … on conflict … where version &lt; excluded.version),
 * partagée par toutes les instances de la passerelle.
 */
public class ProjectionPerimetresR2dbc implements ProjectionPerimetres {

    private final DatabaseClient base;
    private final Clock horloge;
    private final ObjectMapper json = JsonMapper.builder().build();

    public ProjectionPerimetresR2dbc(DatabaseClient base, Clock horloge) {
        this.base = base;
        this.horloge = horloge;
    }

    @Override
    public Mono<PerimetreMandats> lire(String compte) {
        return base
            .sql("select compte, institution_propre, mandantes, version from sbx_perimetre where compte = :compte")
            .bind("compte", compte)
            .map(r -> perimetre(r.get("compte", String.class), r.get("institution_propre", String.class), r.get("mandantes", String.class), r.get("version", Long.class)))
            .one();
    }

    @Override
    public Mono<Boolean> appliquer(PerimetreMandats p) {
        Map<String, String> mandantes = new LinkedHashMap<>();
        p.mandantes().forEach((code, fin) -> mandantes.put(code, fin.toString()));
        return base
            .sql(
                "insert into sbx_perimetre (compte, institution_propre, mandantes, version, recu_le) values (:compte, :propre, :mandantes, :version, :le) " +
                "on conflict (compte) do update set institution_propre = excluded.institution_propre, mandantes = excluded.mandantes, " +
                "version = excluded.version, recu_le = excluded.recu_le where sbx_perimetre.version < excluded.version"
            )
            .bind("compte", p.compte())
            .bind("propre", p.institutionPropre())
            .bind("mandantes", json.writeValueAsString(mandantes))
            .bind("version", p.version())
            .bind("le", OffsetDateTime.ofInstant(horloge.instant(), ZoneOffset.UTC))
            .fetch()
            .rowsUpdated()
            .map(n -> n > 0);
    }

    PerimetreMandats perimetre(String compte, String propre, String mandantes, long version) {
        Map<String, Instant> fins = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> e : json.readTree(mandantes).properties()) {
            fins.put(e.getKey(), Instant.parse(e.getValue().asString()));
        }
        return new PerimetreMandats(compte, propre, fins, version);
    }
}
