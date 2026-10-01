package cm.gimac.des.gateway.domain.appareils.port;

import cm.gimac.des.gateway.domain.appareils.Appareil;
import java.time.Instant;
import reactor.core.publisher.Mono;

/**
 * Alerte « nouvel appareil » (docs/lot1/mon-compte.md §3) : MS-IAM envoie le courriel au titulaire
 * (POST /api/v1/interne/comptes/{id}/alerte-connexion, jeton de service gateway-service).
 */
public interface AlerteNouvelAppareil {
    Mono<Void> alerter(String sujet, Appareil appareil, String adresseIp, String couche, Instant le);
}
