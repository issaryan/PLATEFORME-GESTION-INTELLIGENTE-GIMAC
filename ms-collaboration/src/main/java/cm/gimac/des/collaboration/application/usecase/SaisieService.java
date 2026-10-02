package cm.gimac.des.collaboration.application.usecase;

import cm.gimac.des.collaboration.application.port.in.SignalerSaisieUseCase;
import cm.gimac.des.collaboration.application.port.out.SignalEphemerePort;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.repository.ConversationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * « En train d'écrire » (D-72) : le membre est vérifié, puis le signal part aux autres membres. Un
 * signal au plus toutes les deux secondes par membre et par conversation, quel que soit le rythme
 * de l'interface.
 */
@Service
@Transactional(readOnly = true)
public class SaisieService implements SignalerSaisieUseCase {

    static final Duration INTERVALLE = Duration.ofSeconds(2);
    private static final int PURGE = 10_000;

    private final ConversationRepository conversations;
    private final SignalEphemerePort signaux;
    private final Clock clock;
    private final Map<String, Instant> derniers = new ConcurrentHashMap<>();

    public SaisieService(ConversationRepository conversations, SignalEphemerePort signaux, Clock clock) {
        this.conversations = conversations;
        this.signaux = signaux;
        this.clock = clock;
    }

    @Override
    public void signalerSaisie(String membreId, UUID conversationId) {
        var conversation = conversations
            .pourMembre(conversationId, membreId)
            .orElseThrow(() -> CollaborationException.introuvable("COL_CONVERSATION_NOT_FOUND", conversationId));
        Instant maintenant = clock.instant();
        if (!admis(conversationId + "/" + membreId, maintenant)) {
            return;
        }
        List<String> autres = conversation.getMembres().stream().map(ConversationMembre::getMembreId).filter(m -> !m.equals(membreId)).toList();
        if (!autres.isEmpty()) {
            signaux.saisie(conversationId, membreId, autres);
        }
    }

    /** Vrai si le dernier signal de cette clé date d'au moins {@link #INTERVALLE}. */
    private boolean admis(String cle, Instant maintenant) {
        if (derniers.size() > PURGE) {
            derniers.values().removeIf(t -> t.isBefore(maintenant.minus(INTERVALLE)));
        }
        boolean[] admis = { false };
        derniers.compute(cle, (k, dernier) -> {
            if (dernier == null || !maintenant.isBefore(dernier.plus(INTERVALLE))) {
                admis[0] = true;
                return maintenant;
            }
            return dernier;
        });
        return admis[0];
    }
}
