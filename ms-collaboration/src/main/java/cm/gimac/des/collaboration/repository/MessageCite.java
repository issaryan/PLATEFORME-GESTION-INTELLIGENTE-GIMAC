package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.messagerie.EvenementGroupe;
import java.time.Instant;
import java.util.UUID;

/**
 * Message cité par une réponse (D-82), lu en une requête pour toute une page du fil : auteur, contenu
 * chiffré (déchiffré pour l'extrait), date, nombre de pièces jointes, événement de groupe éventuel.
 */
public record MessageCite(
    UUID id,
    UUID conversationId,
    String auteurId,
    String auteurNom,
    byte[] contenuChiffre,
    Instant envoyeLe,
    EvenementGroupe evenement,
    long piecesJointes
) {}
