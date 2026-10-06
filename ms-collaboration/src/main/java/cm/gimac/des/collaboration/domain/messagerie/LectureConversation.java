package cm.gimac.des.collaboration.domain.messagerie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/**
 * Avancée de la position de lecture d'un membre (D-88) : jusqu'où il a lu (instant d'envoi du dernier
 * message lu) et quand. Une ligne par avancée, jamais modifiée ; donne l'heure du « Lu par » d'un message :
 * la première avancée qui l'a couvert.
 */
@Entity
@Immutable
@Table(name = "conversation_lecture")
public class LectureConversation extends EntiteMessagerie {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "membre_id", nullable = false, length = 64)
    private String membreId;

    @Column(name = "lu_jusqu_a", nullable = false)
    private Instant luJusquA;

    @Column(name = "lu_le", nullable = false)
    private Instant luLe;

    protected LectureConversation() {}

    public LectureConversation(Conversation conversation, String membreId, Instant luJusquA, Instant luLe) {
        this.conversation = conversation;
        this.membreId = membreId;
        this.luJusquA = luJusquA;
        this.luLe = luLe;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public String getMembreId() {
        return membreId;
    }

    public Instant getLuJusquA() {
        return luJusquA;
    }

    public Instant getLuLe() {
        return luLe;
    }
}
