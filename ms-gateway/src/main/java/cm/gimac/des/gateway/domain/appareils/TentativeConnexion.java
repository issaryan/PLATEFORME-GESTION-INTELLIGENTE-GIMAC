package cm.gimac.des.gateway.domain.appareils;

import java.time.Instant;

/**
 * Entrée de l'historique des connexions : date, compte (sujet, nul si le mot de passe a été refusé),
 * empreinte de l'identifiant saisi (jamais l'identifiant en clair), résultat, appareil, adresse IP et
 * couche.
 */
public record TentativeConnexion(
    Instant le,
    String sujet,
    String empreinteIdentifiant,
    ResultatTentative resultat,
    Appareil appareil,
    String adresseIp,
    String couche
) {}
