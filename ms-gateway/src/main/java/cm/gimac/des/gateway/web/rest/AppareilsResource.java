package cm.gimac.des.gateway.web.rest;

import cm.gimac.des.gateway.domain.appareils.MasqueAdresseIp;
import cm.gimac.des.gateway.domain.appareils.TentativeConnexion;
import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.service.appareils.ServiceAppareils;
import cm.gimac.des.gateway.service.appareils.ServiceAppareils.SessionOuverte;
import cm.gimac.des.gateway.service.appareils.SuiviConnexions;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * Appareils connectés et historique des connexions (docs/lot1/mon-compte.md §3), servis par la
 * passerelle, porteuse des sessions. Session à deux facteurs exigée (chaîne de sécurité) ; chaque
 * utilisateur ne voit et ne ferme que les sessions de son propre compte.
 */
@RestController
@RequestMapping("/api/session")
public class AppareilsResource {

    public record AppareilVue(String navigateur, String systeme, String type) {}

    public record SessionVue(
        String id,
        boolean courante,
        AppareilVue appareil,
        String adresseIp,
        String couche,
        Instant ouverteLe,
        Instant derniereActivite,
        Instant expireA
    ) {
        static SessionVue de(SessionOuverte s) {
            return new SessionVue(
                s.id(),
                s.courante(),
                new AppareilVue(s.navigateur(), s.systeme(), s.type()),
                s.adresseIp(),
                s.couche(),
                s.ouverteLe(),
                s.derniereActivite(),
                s.expireA()
            );
        }
    }

    /** Tentative de l'historique ; l'adresse est masquée au dernier octet. */
    public record ConnexionVue(Instant le, String resultat, AppareilVue appareil, String adresseIp, String couche) {
        static ConnexionVue de(TentativeConnexion t) {
            return new ConnexionVue(
                t.le(),
                t.resultat().name(),
                new AppareilVue(t.appareil().navigateur(), t.appareil().systeme(), t.appareil().type().name()),
                MasqueAdresseIp.masquer(t.adresseIp()),
                t.couche()
            );
        }
    }

    public record HistoriqueVue(List<ConnexionVue> contenu, long total) {}

    private final ServiceAppareils service;

    public AppareilsResource(ServiceAppareils service) {
        this.service = service;
    }

    @GetMapping("/appareils")
    public Mono<List<SessionVue>> appareils(Authentication auth, WebSession session) {
        return Mono.fromSupplier(() -> service.sessions(sujet(auth), session).stream().map(SessionVue::de).toList());
    }

    /** Ferme une autre session ; 409 CNX_SESSION_COURANTE pour la session courante (déconnexion). */
    @DeleteMapping("/appareils/{id}")
    public Mono<ResponseEntity<Object>> fermer(@PathVariable String id, Authentication auth, WebSession session, ServerWebExchange exchange) {
        return service
            .fermer(sujet(auth), id, session)
            .then(Mono.just(ResponseEntity.noContent().build()))
            .onErrorResume(EchecConnexion.class, e -> Mono.just(ConnexionResource.probleme(e.causeEchec(), exchange)));
    }

    /** Ferme toutes les autres sessions du compte. */
    @DeleteMapping("/appareils")
    public Mono<ResponseEntity<Void>> fermerAutres(Authentication auth, WebSession session) {
        return service.fermerAutres(sujet(auth), session).thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/historique")
    public Mono<ResponseEntity<Object>> historique(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille,
        Authentication auth,
        ServerWebExchange exchange
    ) {
        Map<String, Object> claims = SessionResource.claims(auth);
        var empreintes = SuiviConnexions.empreintes(texte(claims.get("preferred_username")), texte(claims.get("email")));
        return service
            .historique(sujet(auth), empreintes, page, taille)
            .map(p -> ResponseEntity.ok((Object) new HistoriqueVue(p.contenu().stream().map(ConnexionVue::de).toList(), p.total())))
            .onErrorResume(EchecConnexion.class, e -> Mono.just(ConnexionResource.probleme(e.causeEchec(), exchange)));
    }

    static String sujet(Authentication auth) {
        String sub = texte(SessionResource.claims(auth).get("sub"));
        if (sub == null) {
            throw new EchecConnexion(CauseEchec.APPAREIL_INCONNU, "session sans sujet");
        }
        return sub;
    }

    private static String texte(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString();
    }
}
