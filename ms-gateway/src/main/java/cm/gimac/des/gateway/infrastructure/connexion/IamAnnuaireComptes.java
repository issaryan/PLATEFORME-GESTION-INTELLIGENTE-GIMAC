package cm.gimac.des.gateway.infrastructure.connexion;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.domain.connexion.EtatCompte;
import cm.gimac.des.gateway.domain.connexion.port.AnnuaireComptes;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Routes internes de MS-IAM (contrat D-65 §3.1), appelées avec le jeton de service gateway-service.
 * Aucune n'est relayée depuis une couche de présentation : seule la passerelle les appelle.
 */
public class IamAnnuaireComptes implements AnnuaireComptes {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};

    private final WebClient iam;
    private final Supplier<Mono<String>> jetonService;
    private final Duration delai;

    public IamAnnuaireComptes(WebClient iam, Supplier<Mono<String>> jetonService, Duration delai) {
        this.iam = iam;
        this.jetonService = jetonService;
        this.delai = delai;
    }

    @Override
    public Mono<EtatCompte> etat(String sujet) {
        return jeton()
            .flatMap(j ->
                iam
                    .get()
                    .uri("/api/v1/interne/comptes/{id}/etat-connexion", sujet)
                    .headers(h -> h.setBearerAuth(j))
                    .accept(MediaType.APPLICATION_JSON)
                    .exchangeToMono(r -> {
                        if (r.statusCode().is2xxSuccessful()) {
                            return r.bodyToMono(JSON).map(IamAnnuaireComptes::etatCompte);
                        }
                        if (r.statusCode().value() == HttpStatus.NOT_FOUND.value()) {
                            return r.releaseBody().then(Mono.error(new EchecConnexion(CauseEchec.COMPTE_INACTIF, "compte inconnu de MS-IAM")));
                        }
                        return r.releaseBody().then(Mono.error(new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "MS-IAM " + r.statusCode().value())));
                    })
            )
            .timeout(delai)
            .onErrorMap(e -> !(e instanceof EchecConnexion), e -> new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "MS-IAM injoignable : " + e.getMessage()));
    }

    @SuppressWarnings("unchecked")
    static EtatCompte etatCompte(Map<String, Object> corps) {
        Object actions = corps.get("actionsRequises");
        return new EtatCompte(
            Boolean.TRUE.equals(corps.get("actif")),
            Boolean.TRUE.equals(corps.get("secondFacteur")),
            actions instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of()
        );
    }

    @Override
    public Mono<Void> motDePasseOublie(String identifiant, String couche) {
        Map<String, Object> corps = new HashMap<>();
        corps.put("identifiant", identifiant);
        corps.put("couche", couche);
        return jeton()
            .flatMap(j ->
                iam
                    .post()
                    .uri("/api/v1/interne/connexion/mot-de-passe-oublie")
                    .headers(h -> h.setBearerAuth(j))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(corps)
                    .retrieve()
                    .toBodilessEntity()
            )
            .timeout(delai)
            .then();
    }

    @Override
    public Mono<ReponseRelayee> nouveauMotDePasse(String jetonLien, String motDePasse) {
        return jeton()
            .flatMap(j ->
                iam
                    .post()
                    .uri("/api/v1/interne/connexion/nouveau-mot-de-passe")
                    .headers(h -> h.setBearerAuth(j))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("jeton", jetonLien, "motDePasse", motDePasse))
                    .exchangeToMono(r -> {
                        int statut = r.statusCode().value();
                        if (r.statusCode().is2xxSuccessful()) {
                            return r.releaseBody().thenReturn(new ReponseRelayee(HttpStatus.NO_CONTENT.value(), null, ""));
                        }
                        if (statut == HttpStatus.BAD_REQUEST.value()) {
                            String type = r.headers().contentType().map(MediaType::toString).orElse(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            return r.bodyToMono(String.class).defaultIfEmpty("").map(c -> new ReponseRelayee(statut, type, c));
                        }
                        return r.releaseBody().then(Mono.error(new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "MS-IAM " + statut)));
                    })
            )
            .timeout(delai)
            .onErrorMap(e -> !(e instanceof EchecConnexion), e -> new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "MS-IAM injoignable : " + e.getMessage()));
    }

    private Mono<String> jeton() {
        return jetonService
            .get()
            .switchIfEmpty(Mono.error(() -> new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "jeton de service gateway-service indisponible")));
    }
}
