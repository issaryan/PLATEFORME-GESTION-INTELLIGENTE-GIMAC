package cm.gimac.des.gateway.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Gestionnaire de Rate Limiting interagissant avec Redis via un script Lua
 * atomique.
 * Implémente la stratégie Fail-Open : si Redis est indisponible, la requête est
 * autorisée
 * pour garantir la continuité de service des flux critiques.
 */
@ConditionalOnProperty(name = "gimac.passerelle.limitation-debit.actif", havingValue = "true")
@Component
public class TokenBucketRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

    @NonNull
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @NonNull
    private final RedisScript<List<Long>> luaScript;

    @NonNull
    private final MeterRegistry meterRegistry;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TokenBucketRateLimiter(
            @NonNull ReactiveRedisTemplate<String, String> redisTemplate,
            @NonNull MeterRegistry meterRegistry) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "Le ReactiveRedisTemplate ne peut pas être nul");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "Le MeterRegistry ne peut pas être nul");

        RedisScript<List<Long>> parsedScript = RedisScript.of(new ClassPathResource("scripts/tenant_rate_limiter.lua"),
                (Class) List.class);
        this.luaScript = Objects.requireNonNull(parsedScript, "Le script Lua généré ne peut pas être nul");
    }

    /**
     * Évalue si la requête est autorisée pour le tenant_id fourni.
     * 
     * @param tenantId      Identifiant de l'agence
     * @param burstCapacity Capacité maximale instantanée du seau
     * @param replenishRate Taux de rechargement par minute
     * @return Mono contenant une liste de Long (index 0 : 1 si autorisé, 0 sinon ;
     *         index 1 : tokens restants)
     */
    @NonNull
    public Mono<List<Long>> isAllowed(@NonNull String tenantId, @NonNull Integer burstCapacity,
            @NonNull Integer replenishRate) {
        Objects.requireNonNull(tenantId, "Le tenantId ne peut pas être nul");
        Objects.requireNonNull(burstCapacity, "La burstCapacity ne peut pas être nulle");
        Objects.requireNonNull(replenishRate, "Le replenishRate ne peut pas être nul");

        String key = "rate_limit:" + tenantId;
        String nowStr = String.valueOf(Instant.now().getEpochSecond());

        // CORRECTION ICI : Forçage du typage @NonNull pour satisfaire le contrat de
        // redisTemplate.execute
        List<String> keys = Objects.requireNonNull(
                Collections.singletonList(key),
                "La liste des clés ne peut pas être nulle");

        List<String> args = Objects.requireNonNull(
                Arrays.asList(String.valueOf(burstCapacity), String.valueOf(replenishRate), nowStr),
                "La liste des arguments ne peut pas être nulle");

        return Objects.requireNonNull(
                redisTemplate.execute(this.luaScript, keys, args)
                        .next()
                        .map(result -> {
                            List<Long> finalRes = Objects.requireNonNull(result,
                                    "Le résultat du script Lua ne peut pas être nul");

                            if (!finalRes.isEmpty() && finalRes.get(0) == 0L) {
                                meterRegistry.counter("gateway_rate_limit_exceeded_total", "tenant", tenantId)
                                        .increment();
                            }
                            return finalRes;
                        })
                        .onErrorResume(throwable -> {
                            log.error("Redis indisponible pour le Rate Limiting (Tenant: {}). Fail-Open activé.",
                                    tenantId, throwable);
                            meterRegistry.counter("gateway_redis_errors_total").increment();
                            return Mono.just(Arrays.asList(1L, -1L));
                        }),
                "Le Mono résultant de l'exécution Redis ne peut pas être nul");
    }
}