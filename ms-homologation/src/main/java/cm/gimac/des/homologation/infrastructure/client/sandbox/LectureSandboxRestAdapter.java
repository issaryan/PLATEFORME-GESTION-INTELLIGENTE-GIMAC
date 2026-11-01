package cm.gimac.des.homologation.infrastructure.client.sandbox;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort;
import cm.gimac.des.homologation.infrastructure.client.ServiceTokenInterceptor;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Client du contrat de lecture v1 de SBX-LECTURE (US-EXI-14), par la passerelle interne de service : jeton du compte de
 * service de la plateforme du DES (lié au certificat client, RFC 8705, configuré par l'infrastructure). Seules les
 * méthodes GET et HEAD sont émises. 503 et 429 (Retry-After), comme un service injoignable, donnent
 * {@link LectureSandboxPort.Indisponible} sans aucune donnée ; toute autre réponse en erreur, ou hors contrat, donne
 * {@link LectureSandboxPort.HorsContrat}.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.sbx-lecture", havingValue = "real")
public class LectureSandboxRestAdapter implements LectureSandboxPort {

    static final Duration DELAI_PAR_DEFAUT = Duration.ofMinutes(5);
    private final RestClient client;

    @Autowired
    public LectureSandboxRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.sbx-lecture.url}") String url) {
        this(builder.baseUrl(url).requestInterceptor(jeton).build());
    }

    LectureSandboxRestAdapter(RestClient client) {
        this.client = client;
    }

    @Override
    public List<CampagneListee> campagnes(UUID liaisonId) {
        return TraducteurLectureSandbox.campagnes(get("/v1/liaisons/{id}/campagnes", liaisonId));
    }

    @Override
    public Campagne campagne(String campagneId) {
        return TraducteurLectureSandbox.campagne(get("/v1/campagnes/{c}", campagneId));
    }

    @Override
    public PageExecutions executions(String campagneId, String depuis, int taille) {
        return TraducteurLectureSandbox.page(
            appel(() ->
                client
                    .get()
                    .uri(u -> {
                        u.path("/v1/campagnes/{c}/executions").queryParam("taille", taille);
                        if (depuis != null) {
                            u.queryParam("depuis", depuis);
                        }
                        return u.build(campagneId);
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(byte[].class)
            )
        );
    }

    @Override
    public List<Piece> pieces(String campagneId) {
        return TraducteurLectureSandbox.pieces(get("/v1/campagnes/{c}/pieces", campagneId));
    }

    @Override
    public String empreinte(String campagneId, String nature, int version) {
        HttpHeaders h = appel(() -> client.head().uri("/v1/campagnes/{c}/pieces/{n}/{v}", campagneId, nature, version).retrieve().toBodilessEntity().getHeaders());
        return TraducteurLectureSandbox.empreinteRepr(h.getFirst("Repr-Digest"));
    }

    @Override
    public byte[] telecharger(String campagneId, String nature, int version) {
        return appel(() -> client.get().uri("/v1/campagnes/{c}/pieces/{n}/{v}", campagneId, nature, version).retrieve().body(byte[].class));
    }

    private byte[] get(String chemin, Object... variables) {
        return appel(() -> client.get().uri(chemin, variables).accept(MediaType.APPLICATION_JSON).retrieve().body(byte[].class));
    }

    private static <T> T appel(Supplier<T> f) {
        T r;
        try {
            r = f.get();
        } catch (HttpStatusCodeException e) {
            HttpStatusCode s = e.getStatusCode();
            if (s.value() == 503 || s.value() == 429) {
                throw new Indisponible("SBX-LECTURE répond " + s.value(), delai(e.getResponseHeaders()));
            }
            throw new HorsContrat("SBX-LECTURE répond " + s.value());
        } catch (ResourceAccessException e) {
            throw new Indisponible("SBX-LECTURE injoignable", DELAI_PAR_DEFAUT);
        }
        if (r == null) {
            throw new HorsContrat("réponse vide");
        }
        return r;
    }

    /** Retry-After en secondes (RFC 9110) ; absent ou illisible : délai par défaut de la relève. */
    static Duration delai(HttpHeaders h) {
        String v = h == null ? null : h.getFirst(HttpHeaders.RETRY_AFTER);
        if (v != null && v.matches("\\d{1,6}")) {
            return Duration.ofSeconds(Long.parseLong(v));
        }
        return DELAI_PAR_DEFAUT;
    }
}
