package cm.gimac.des.collaboration.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Diffusion d'un signal éphémère de la messagerie (D-72) : publié directement sur le bus, hors outbox,
 * car sa perte est sans conséquence et sa valeur tient en quelques secondes.
 */
public interface SignalEphemerePort {
    /** « auteurId écrit dans conversationId », pour les destinataires désignés. */
    void saisie(UUID conversationId, String auteurId, List<String> destinataires);
}
