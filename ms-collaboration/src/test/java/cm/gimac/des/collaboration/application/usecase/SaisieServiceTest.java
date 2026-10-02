package cm.gimac.des.collaboration.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.collaboration.application.port.out.SignalEphemerePort;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.TypeConversation;
import cm.gimac.des.collaboration.repository.ConversationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** « En train d'écrire » (D-72) : membre vérifié, autres membres seuls destinataires, un signal par 2 s. */
@ExtendWith(MockitoExtension.class)
class SaisieServiceTest {

    static final Instant T = Instant.parse("2026-10-02T08:00:00Z");

    @Mock
    ConversationRepository conversations;

    @Mock
    SignalEphemerePort signaux;

    final AtomicReference<Instant> maintenant = new AtomicReference<>(T);
    SaisieService service;
    Conversation groupe;

    @BeforeEach
    void init() {
        Clock horloge = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return maintenant.get();
            }
        };
        service = new SaisieService(conversations, signaux, horloge);
        groupe = new Conversation(TypeConversation.GROUPE, "Recette", null, null, null, "moi", T);
        for (String id : List.of("moi", "aminata", "serge")) {
            groupe.ajouterMembre(new Collaborateur(id, id, null, Set.of(), null, true), T);
        }
    }

    @Test
    @DisplayName("Signal aux autres membres seulement ; second signal dans les 2 s ignoré, admis ensuite")
    void signal() {
        when(conversations.pourMembre(groupe.getId(), "moi")).thenReturn(Optional.of(groupe));
        service.signalerSaisie("moi", groupe.getId());
        verify(signaux).saisie(groupe.getId(), "moi", List.of("aminata", "serge"));
        maintenant.set(T.plusMillis(1500));
        service.signalerSaisie("moi", groupe.getId());
        verify(signaux, times(1)).saisie(any(), anyString(), anyList());
        maintenant.set(T.plusSeconds(2));
        service.signalerSaisie("moi", groupe.getId());
        verify(signaux, times(2)).saisie(any(), anyString(), anyList());
    }

    @Test
    @DisplayName("Hors de la conversation : 404 et aucun signal")
    void horsConversation() {
        UUID id = UUID.randomUUID();
        when(conversations.pourMembre(id, "intrus")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.signalerSaisie("intrus", id)).isInstanceOf(CollaborationException.class);
        verify(signaux, never()).saisie(any(), anyString(), anyList());
    }
}
