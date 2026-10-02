package cm.gimac.des.collaboration.application.port.in;

import java.util.UUID;

/**
 * Indicateur « en train d'écrire » de la messagerie (D-72) : signal éphémère, ni enregistré ni rejoué,
 * remis aux autres membres de la conversation.
 */
public interface SignalerSaisieUseCase {
    /** Signale que le membre écrit dans la conversation ; 404 s'il n'en est pas membre. */
    void signalerSaisie(String membreId, UUID conversationId);
}
