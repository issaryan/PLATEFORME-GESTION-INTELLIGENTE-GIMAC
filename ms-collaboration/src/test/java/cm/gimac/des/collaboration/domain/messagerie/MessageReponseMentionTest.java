package cm.gimac.des.collaboration.domain.messagerie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Réponse à un message et mentions (D-82) portées par le message, entité immuable. */
class MessageReponseMentionTest {

    static final Instant T = Instant.parse("2026-10-06T08:00:00Z");
    static final Collaborateur PAUL = new Collaborateur("paul", "Paul Mbarga", "Chef du DES", Set.of(), null, true);
    static final Collaborateur AMINATA = new Collaborateur("aminata", "Aminata Ngono", "Conductrice", Set.of(), null, true);
    static final Collaborateur SERGE = new Collaborateur("serge", "Serge Ndzana", "Ingénieur", Set.of(), null, true);

    final Conversation groupe = groupe();

    static Conversation groupe() {
        Conversation c = new Conversation(TypeConversation.GROUPE, "Atelier", null, null, null, "paul", T);
        c.ajouterMembre(PAUL, T);
        c.ajouterMembre(AMINATA, T);
        c.ajouterMembre(SERGE, T);
        return c;
    }

    Message message(Conversation c, Collaborateur auteur) {
        return new Message(UUID.randomUUID(), c, auteur, new byte[] { 1 }, T);
    }

    @Test
    @DisplayName("Réponse : message cité de la même conversation, y compris de l'auteur lui-même")
    void reponse() {
        Message cite = message(groupe, AMINATA);
        Message reponse = message(groupe, PAUL);
        reponse.repondreA(cite);
        assertThat(reponse.getReponseAId()).isEqualTo(cite.getId());

        Message miens = message(groupe, PAUL);
        Message suite = message(groupe, PAUL);
        suite.repondreA(miens);
        assertThat(suite.getReponseAId()).isEqualTo(miens.getId());
    }

    @Test
    @DisplayName("Réponse refusée : autre conversation, événement de groupe, soi-même ; un événement ne répond pas")
    void reponseRefusee() {
        Message reponse = message(groupe, PAUL);
        Message ailleurs = message(groupe(), AMINATA);
        assertThatThrownBy(() -> reponse.repondreA(ailleurs)).isInstanceOf(IllegalArgumentException.class);
        Message evenement = Message.evenement(UUID.randomUUID(), groupe, PAUL, new byte[] { 1 }, T, EvenementGroupe.GROUPE_CREE, List.of("Atelier"));
        assertThatThrownBy(() -> reponse.repondreA(evenement)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reponse.repondreA(reponse)).isInstanceOf(IllegalArgumentException.class);
        Message cite = message(groupe, AMINATA);
        assertThatThrownBy(() -> evenement.repondreA(cite)).isInstanceOf(IllegalStateException.class);
        assertThat(reponse.getReponseAId()).isNull();
    }

    @Test
    @DisplayName("Mentions : membres de la conversation, dans l'ordre, sans doublon, nom figé à l'envoi ; ni l'auteur ni un étranger")
    void mentions() {
        Message m = message(groupe, PAUL);
        m.mentionner(groupe.membre("serge").orElseThrow());
        m.mentionner(groupe.membre("aminata").orElseThrow());
        m.mentionner(groupe.membre("serge").orElseThrow());
        assertThat(m.getMentionnes()).containsExactly("serge", "aminata");
        assertThat(m.getMentions()).extracting(MessageMention::getNom).containsExactly("Serge Ndzana", "Aminata Ngono");
        assertThat(m.getMentions()).extracting(MessageMention::getRang).containsExactly(0, 1);

        assertThatThrownBy(() -> m.mentionner(groupe.membre("paul").orElseThrow())).isInstanceOf(IllegalArgumentException.class);
        ConversationMembre etranger = groupe().membre("aminata").orElseThrow();
        assertThatThrownBy(() -> m.mentionner(etranger)).isInstanceOf(IllegalArgumentException.class);
        Message evenement = Message.evenement(UUID.randomUUID(), groupe, PAUL, new byte[] { 1 }, T, EvenementGroupe.GROUPE_RENOMME, List.of("X"));
        ConversationMembre serge = groupe.membre("serge").orElseThrow();
        assertThatThrownBy(() -> evenement.mentionner(serge)).isInstanceOf(IllegalStateException.class);
    }
}
