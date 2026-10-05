package cm.gimac.des.pilotage.domain.activite;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;

/** Participant invité à un événement d'agenda et sa réponse (§2.1). */
@Embeddable
public class ParticipantEvenement {

    @Column(name = "participant_id", nullable = false, length = 80)
    private String participantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reponse", nullable = false, length = 16)
    private ReponseParticipant reponse = ReponseParticipant.EN_ATTENTE;

    @Column(name = "repondu_le")
    private Instant reponduLe;

    protected ParticipantEvenement() {}

    public ParticipantEvenement(String participantId) {
        this.participantId = participantId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public ReponseParticipant getReponse() {
        return reponse;
    }

    public Instant getReponduLe() {
        return reponduLe;
    }

    void repondre(ReponseParticipant r, Instant le) {
        this.reponse = r;
        this.reponduLe = le;
    }
}
