package cm.gimac.des.gateway.domain.supervision.port;

import cm.gimac.des.gateway.domain.supervision.EnvironnementService;
import cm.gimac.des.gateway.domain.supervision.InfoService;
import cm.gimac.des.gateway.domain.supervision.Metriques;
import cm.gimac.des.gateway.domain.supervision.NiveauxJournalisation;
import cm.gimac.des.gateway.domain.supervision.Sante;
import cm.gimac.des.gateway.domain.supervision.ServiceCible;
import reactor.core.publisher.Mono;

/**
 * Port sortant vers les points de gestion (/management/**) d'un service de la plateforme.
 * Une erreur de transport se propage en erreur du Mono : le service d'application la traduit
 * en « injoignable » sans bloquer les autres services.
 */
public interface SondeService {
    /** /management/health : l'état est lu dans le corps, y compris sur une réponse 503. */
    Mono<Sante> sante(ServiceCible service);

    /** Sonde de santé d'un composant externe (Keycloak : /health/ready), adresse complète. */
    Mono<Sante> santeExterne(String url);

    /** /management/info. */
    Mono<InfoService> info(ServiceCible service);

    /** /management/prometheus, analysé. */
    Mono<Metriques> metriques(ServiceCible service);

    /** /management/env : profils actifs et valeur retenue de chaque propriété. */
    Mono<EnvironnementService> environnement(ServiceCible service);

    /** /management/loggers. */
    Mono<NiveauxJournalisation> journalisation(ServiceCible service);

    /** POST /management/loggers/{logger} : niveau configuré à chaud. */
    Mono<Void> modifierNiveau(ServiceCible service, String logger, String niveau);
}
