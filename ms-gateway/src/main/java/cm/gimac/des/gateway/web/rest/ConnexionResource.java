package cm.gimac.des.gateway.web.rest;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.DemandeConnexion;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.domain.connexion.MasqueIdentifiant;
import cm.gimac.des.gateway.domain.connexion.ResultatConnexion;
import cm.gimac.des.gateway.domain.live.Couche;
import cm.gimac.des.gateway.security.session.ReponseProbleme;
import cm.gimac.des.gateway.service.connexion.IssueConnexion;
import cm.gimac.des.gateway.service.connexion.ServiceConnexion;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Connexion sur la plateforme (D-65, docs/lot1/connexion.md §2) : l'utilisateur ne voit jamais
 * Keycloak. Routes publiques, protégées contre la CSRF (témoin XSRF-TOKEN) ; aucun jeton n'est
 * renvoyé au navigateur, seulement le témoin de session HttpOnly.
 */
@RestController
@RequestMapping("/api/connexion")
public class ConnexionResource {

    /** Corps de POST /api/connexion ; le mot de passe et le code ne sont jamais journalisés. */
    public record CorpsConnexion(String identifiant, String motDePasse, String code, String couche) {
        @Override
        public String toString() {
            return "CorpsConnexion[identifiant=" + MasqueIdentifiant.masquer(identifiant) + ", couche=" + couche + "]";
        }
    }

    public record CorpsMotDePasseOublie(String identifiant, String couche) {
        @Override
        public String toString() {
            return "CorpsMotDePasseOublie[identifiant=" + MasqueIdentifiant.masquer(identifiant) + "]";
        }
    }

    public record CorpsNouveauMotDePasse(String jeton, String motDePasse) {
        @Override
        public String toString() {
            return "CorpsNouveauMotDePasse[]";
        }
    }

    /** Réponse 200 : état, session (CONNECTE) ou étapes (PREMIER_ACCES). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReponseConnexion(String etat, SessionResource.SessionVue session, List<String> etapes) {}

    private final ServiceConnexion service;
    private final SessionResource sessions;

    public ConnexionResource(ServiceConnexion service, SessionResource sessions) {
        this.service = service;
        this.sessions = sessions;
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> connecter(@RequestBody(required = false) CorpsConnexion corps, ServerWebExchange exchange) {
        Optional<Couche> couche = Optional.ofNullable(corps).flatMap(c -> Couche.depuis(c.couche()));
        if (corps == null || vide(corps.identifiant()) || vide(corps.motDePasse()) || couche.isEmpty()) {
            return Mono.just(probleme(CauseEchec.REQUETE_INVALIDE, exchange));
        }
        DemandeConnexion demande = new DemandeConnexion(
            corps.identifiant().trim(),
            corps.motDePasse(),
            corps.code(),
            couche.orElseThrow(),
            adresseIp(exchange),
            exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT)
        );
        return service
            .connecter(demande, exchange)
            .flatMap(issue -> reponse(issue, exchange))
            .onErrorResume(EchecConnexion.class, e -> Mono.just(probleme(e.causeEchec(), exchange)));
    }

    private Mono<ResponseEntity<Object>> reponse(IssueConnexion issue, ServerWebExchange exchange) {
        return switch (issue.resultat()) {
            case ResultatConnexion.CodeRequis c -> Mono.just(ResponseEntity.ok(new ReponseConnexion("CODE_REQUIS", null, null)));
            case ResultatConnexion.PremierAcces p -> Mono.just(ResponseEntity.ok(new ReponseConnexion("PREMIER_ACCES", null, p.etapes())));
            case ResultatConnexion.Connecte c -> exchange
                .getSession()
                .flatMap(session -> sessions.session(issue.authentification(), session))
                .map(vue -> ResponseEntity.ok(new ReponseConnexion("CONNECTE", vue, null)));
        };
    }

    /** 202 toujours : on ne révèle pas l'existence d'un compte. */
    @PostMapping("/mot-de-passe-oublie")
    public Mono<ResponseEntity<Void>> motDePasseOublie(@RequestBody(required = false) CorpsMotDePasseOublie corps, ServerWebExchange exchange) {
        Mono<Void> demande = corps == null ? Mono.empty() : service.motDePasseOublie(corps.identifiant(), corps.couche(), adresseIp(exchange));
        return demande.thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).<Void>build());
    }

    /** 204, ou 400 de MS-IAM relayé tel quel (CNX_JETON_INVALIDE, IAM_PASSWORD_POLICY et ses violations). */
    @PostMapping("/nouveau-mot-de-passe")
    public Mono<ResponseEntity<Object>> nouveauMotDePasse(@RequestBody(required = false) CorpsNouveauMotDePasse corps, ServerWebExchange exchange) {
        if (corps == null || vide(corps.jeton()) || vide(corps.motDePasse())) {
            return Mono.just(probleme(CauseEchec.REQUETE_INVALIDE, exchange));
        }
        return service
            .nouveauMotDePasse(corps.jeton(), corps.motDePasse())
            .map(r -> {
                if (r.statut() == HttpStatus.NO_CONTENT.value()) {
                    return ResponseEntity.noContent().build();
                }
                return ResponseEntity.status(r.statut()).header(HttpHeaders.CONTENT_TYPE, r.typeContenu()).body((Object) r.corps().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            })
            .onErrorResume(EchecConnexion.class, e -> Mono.just(probleme(e.causeEchec(), exchange)));
    }

    static ResponseEntity<Object> probleme(CauseEchec cause, ServerWebExchange exchange) {
        ResponseEntity.BodyBuilder b = ResponseEntity.status(cause.statut()).contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (cause == CauseEchec.TROP_DE_TENTATIVES) {
            b.header(HttpHeaders.RETRY_AFTER, "60");
        }
        return b.body(ReponseProbleme.corps(cause, exchange.getRequest().getPath().value()));
    }

    /** Adresse du client ; derrière le mandataire des couches, résolue par server.forward-headers-strategy. */
    static String adresseIp(ServerWebExchange exchange) {
        InetSocketAddress a = exchange.getRequest().getRemoteAddress();
        if (a == null) {
            return "inconnue";
        }
        return a.getAddress() != null ? a.getAddress().getHostAddress() : a.getHostString();
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
