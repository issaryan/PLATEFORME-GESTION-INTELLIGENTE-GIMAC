package cm.gimac.des.gateway.infrastructure.appareils;

import cm.gimac.des.gateway.domain.appareils.Appareil;
import cm.gimac.des.gateway.domain.appareils.ResultatTentative;
import cm.gimac.des.gateway.domain.appareils.TentativeConnexion;
import cm.gimac.des.gateway.domain.appareils.port.HistoriqueConnexions;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Historique des connexions en mémoire de l'instance : contextes sans base de données (tests de la
 * chaîne web). En production, l'historique est en base (HistoriqueConnexionsR2dbc).
 */
public class HistoriqueConnexionsMemoire implements HistoriqueConnexions {

    private final List<TentativeConnexion> tentatives = new CopyOnWriteArrayList<>();

    @Override
    public Mono<Void> enregistrer(TentativeConnexion tentative) {
        return Mono.fromRunnable(() -> tentatives.add(tentative));
    }

    @Override
    public Mono<Page> lire(String sujet, Collection<String> empreintes, int page, int taille) {
        return Mono.fromSupplier(() -> {
            List<TentativeConnexion> du = tentatives
                .stream()
                .filter(t -> (sujet != null && sujet.equals(t.sujet())) || (empreintes != null && empreintes.contains(t.empreinteIdentifiant())))
                .sorted(Comparator.comparing(TentativeConnexion::le).reversed())
                .toList();
            int debut = (int) Math.min((long) page * taille, du.size());
            return new Page(du.subList(debut, Math.min(debut + taille, du.size())), du.size());
        });
    }

    @Override
    public Flux<Appareil> appareilsConnus(String sujet) {
        return Flux.defer(() ->
            Flux.fromStream(tentatives.stream().filter(t -> Objects.equals(sujet, t.sujet()) && t.resultat() == ResultatTentative.REUSSIE).map(TentativeConnexion::appareil).distinct())
        );
    }

    @Override
    public Mono<Long> purger(Instant avant) {
        return Mono.fromSupplier(() -> {
            List<TentativeConnexion> anciennes = tentatives.stream().filter(t -> t.le().isBefore(avant)).toList();
            tentatives.removeAll(anciennes);
            return (long) anciennes.size();
        });
    }
}
