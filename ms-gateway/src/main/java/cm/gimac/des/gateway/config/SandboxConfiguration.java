package cm.gimac.des.gateway.config;

import cm.gimac.des.gateway.audit.GatewayAuditEventPublisher;
import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import cm.gimac.des.gateway.domain.sandbox.port.JournalDecisionsSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.ProjectionPerimetres;
import cm.gimac.des.gateway.infrastructure.connexion.LimiteurTentativesMemoire;
import cm.gimac.des.gateway.infrastructure.connexion.LimiteurTentativesValkey;
import cm.gimac.des.gateway.infrastructure.sandbox.ConsommateurPerimetresKafka;
import cm.gimac.des.gateway.security.EmetteurJetonReduitJwt;
import cm.gimac.des.gateway.infrastructure.sandbox.JournalDecisionsSandboxAudit;
import cm.gimac.des.gateway.infrastructure.sandbox.ProjectionPerimetresMemoire;
import cm.gimac.des.gateway.infrastructure.sandbox.ProjectionPerimetresR2dbc;
import cm.gimac.des.gateway.ratelimit.TokenBucketRateLimiter;
import cm.gimac.des.gateway.security.EmetteurJwtInterne;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * Assemblage du cloisonnement des routes de la sandbox (D-120, US-EXI-07) : projection des périmètres en base (mémoire
 * sans base), journal des décisions, quotas par couche (Valkey partagé si la limitation de débit est active, mémoire
 * sinon), jeton interne réduit quand l'émetteur de la passerelle est actif, consommateur des instantanés de périmètre.
 */
@Configuration
@EnableConfigurationProperties(SandboxProperties.class)
public class SandboxConfiguration {

    @Bean
    public ProjectionPerimetres projectionPerimetres(ObjectProvider<DatabaseClient> base, ObjectProvider<Clock> horloge) {
        DatabaseClient client = base.getIfAvailable();
        return client == null ? new ProjectionPerimetresMemoire() : new ProjectionPerimetresR2dbc(client, ConnexionConfiguration.horloge(horloge));
    }

    @Bean
    public JournalDecisionsSandbox journalDecisionsSandbox(ObjectProvider<GatewayAuditEventPublisher> publication) {
        return new JournalDecisionsSandboxAudit(publication.getIfAvailable());
    }

    @Bean
    public CloisonnementSandbox cloisonnementSandbox(
        SandboxProperties p,
        ProjectionPerimetres projection,
        JournalDecisionsSandbox journal,
        ObjectProvider<TokenBucketRateLimiter> valkey,
        ObjectProvider<EmetteurJwtInterne> emetteur,
        ObjectProvider<Clock> horloge
    ) {
        Clock h = ConnexionConfiguration.horloge(horloge);
        EmetteurJwtInterne jwt = emetteur.getIfAvailable();
        return new CloisonnementSandbox(projection, journal, quotas(p.getQuotas(), valkey.getIfAvailable(), h), jwt == null ? null : new EmetteurJetonReduitJwt(jwt), h);
    }

    @Bean
    @ConditionalOnProperty(prefix = "gimac.sandbox.perimetres", name = "actif", havingValue = "true")
    public ConsommateurPerimetresKafka consommateurPerimetresKafka(SandboxProperties p, ProjectionPerimetres projection) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, p.getPerimetres().getBootstrapServers());
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10_000);
        config.putAll(p.getPerimetres().getKafka());
        return new ConsommateurPerimetresKafka(config, p.getPerimetres().getSujet(), projection);
    }

    /** Un limiteur par couche : seau Valkey partagé entre instances, la mémoire en secours. */
    static Map<String, LimiteurTentatives> quotas(Map<String, Integer> parCouche, TokenBucketRateLimiter valkey, Clock horloge) {
        Map<String, LimiteurTentatives> quotas = new LinkedHashMap<>();
        parCouche.forEach((couche, n) -> {
            LimiteurTentatives memoire = new LimiteurTentativesMemoire(n, Duration.ofMinutes(1), horloge);
            quotas.put(couche, valkey == null ? memoire : new LimiteurTentativesValkey(valkey, memoire, n, n));
        });
        return quotas;
    }
}
