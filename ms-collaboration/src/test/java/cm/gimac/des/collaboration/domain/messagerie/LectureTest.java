package cm.gimac.des.collaboration.domain.messagerie;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.domain.messagerie.Lecture.EtatLecture;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** État de lecture d'un message pour son auteur (D-88). */
class LectureTest {

    static final Instant T = Instant.parse("2026-10-06T08:00:00Z");

    static Collaborateur personne(String id) {
        return new Collaborateur(id, "Nom " + id, null, Set.of(), null, true);
    }

    @Test
    @DisplayName("Directe : ENVOYE puis LU quand la position atteint l'envoi (égalité comprise)")
    void directe() {
        Conversation c = new Conversation(TypeConversation.DIRECTE, null, null, null, "a|b", "a", T);
        c.ajouterMembre(personne("a"), T);
        ConversationMembre b = c.ajouterMembre(personne("b"), T);
        assertThat(Lecture.de("a", T, c.getMembres())).isEqualTo(new Lecture(EtatLecture.ENVOYE, 0, 1));
        b.marquerLu(UUID.randomUUID(), T.minusSeconds(1));
        assertThat(Lecture.de("a", T, c.getMembres()).etat()).isEqualTo(EtatLecture.ENVOYE);
        b.marquerLu(UUID.randomUUID(), T);
        assertThat(Lecture.de("a", T, c.getMembres())).isEqualTo(new Lecture(EtatLecture.LU, 1, 1));
    }

    @Test
    @DisplayName("Groupe : LU_PAR_CERTAINS puis LU ; membre ajouté après l'envoi ignoré ; membre parti ne compte plus")
    void groupe() {
        Conversation c = new Conversation(TypeConversation.GROUPE, "G", null, null, null, "a", T);
        c.ajouterMembre(personne("a"), T);
        ConversationMembre b = c.ajouterMembre(personne("b"), T);
        ConversationMembre d = c.ajouterMembre(personne("d"), T);
        Instant envoi = T.plusSeconds(10);
        ConversationMembre tard = c.ajouterMembre(personne("tard"), T.plusSeconds(20));
        tard.marquerLu(UUID.randomUUID(), T.plusSeconds(30));
        assertThat(Lecture.de("a", envoi, c.getMembres())).isEqualTo(new Lecture(EtatLecture.ENVOYE, 0, 2));
        b.marquerLu(UUID.randomUUID(), T.plusSeconds(15));
        assertThat(Lecture.de("a", envoi, c.getMembres())).isEqualTo(new Lecture(EtatLecture.LU_PAR_CERTAINS, 1, 2));
        c.retirerMembre(d);
        assertThat(Lecture.de("a", envoi, c.getMembres())).isEqualTo(new Lecture(EtatLecture.LU, 1, 1));
    }

    @Test
    @DisplayName("Événement de groupe : pas d'état ; plus aucun destinataire : ENVOYE")
    void evenementEtSeul() {
        Conversation c = new Conversation(TypeConversation.GROUPE, "G", null, null, null, "a", T);
        c.ajouterMembre(personne("a"), T);
        Message e = Message.evenement(UUID.randomUUID(), c, personne("a"), new byte[] { 0 }, T, EvenementGroupe.GROUPE_CREE, java.util.List.of("G"));
        assertThat(Lecture.de(e, c.getMembres())).isNull();
        assertThat(Lecture.de("a", T, c.getMembres())).isEqualTo(new Lecture(EtatLecture.ENVOYE, 0, 0));
    }

    @Test
    @DisplayName("Position : avance seulement vers un instant postérieur")
    void avance() {
        Conversation c = new Conversation(TypeConversation.DIRECTE, null, null, null, "a|b", "a", T);
        ConversationMembre b = c.ajouterMembre(personne("b"), T);
        assertThat(b.marquerLu(UUID.randomUUID(), T)).isTrue();
        assertThat(b.marquerLu(UUID.randomUUID(), T)).isFalse();
        assertThat(b.marquerLu(UUID.randomUUID(), T.minusSeconds(1))).isFalse();
        assertThat(b.marquerLu(UUID.randomUUID(), T.plusNanos(1000))).isTrue();
    }
}
