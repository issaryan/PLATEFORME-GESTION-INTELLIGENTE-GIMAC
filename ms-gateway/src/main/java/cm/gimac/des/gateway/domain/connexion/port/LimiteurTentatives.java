package cm.gimac.des.gateway.domain.connexion.port;

import reactor.core.publisher.Mono;

/** Limitation des tentatives par clé (adresse IP, identifiant) : vrai si la tentative est admise. */
public interface LimiteurTentatives {
    Mono<Boolean> consommer(String cle);
}
