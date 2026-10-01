package cm.gimac.des.gateway.domain.appareils.port;

import cm.gimac.des.gateway.domain.appareils.Appareil;
import cm.gimac.des.gateway.domain.appareils.TentativeConnexion;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Historique persistant des connexions (base de la passerelle), rétention de 90 jours. */
public interface HistoriqueConnexions {
    /** Page de l'historique d'un compte, les plus récentes d'abord. */
    record Page(List<TentativeConnexion> contenu, long total) {
        public Page {
            contenu = List.copyOf(contenu);
        }
    }

    Mono<Void> enregistrer(TentativeConnexion tentative);

    /** Tentatives du compte : par son sujet, ou par l'empreinte d'un de ses identifiants (échecs du mot de passe). */
    Mono<Page> lire(String sujet, Collection<String> empreintesIdentifiant, int page, int taille);

    /** Appareils des connexions réussies du compte. */
    Flux<Appareil> appareilsConnus(String sujet);

    /** Supprime les tentatives antérieures à la date ; nombre supprimé. */
    Mono<Long> purger(Instant avant);
}
