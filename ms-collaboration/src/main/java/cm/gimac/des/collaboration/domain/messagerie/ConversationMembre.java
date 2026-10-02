package cm.gimac.des.collaboration.domain.messagerie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Membre d'une conversation : identifiant IAM, nom et fonction figés à l'entrée (instantané de
 * l'annuaire), et marque de lecture (dernier message lu) qui donne le nombre de non-lus.
 */
@Entity
@Table(name = "conversation_membre")
public class ConversationMembre extends EntiteMessagerie {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "membre_id", nullable = false, length = 64)
    private String membreId;

    @Column(name = "nom", nullable = false, length = 200)
    private String nom;

    @Column(name = "fonction", length = 200)
    private String fonction;

    @Column(name = "ajoute_le", nullable = false)
    private Instant ajouteLe;

    @Column(name = "dernier_lu_message_id")
    private UUID dernierLuMessageId;

    @Column(name = "dernier_lu_le")
    private Instant dernierLuLe;

    /** Administrateur d'un groupe (D-68) : le créateur, puis le plus ancien membre s'il part. */
    @Column(name = "administrateur", nullable = false)
    private boolean administrateur;

    protected ConversationMembre() {}

    ConversationMembre(Conversation conversation, String membreId, String nom, String fonction, Instant ajouteLe) {
        this.conversation = conversation;
        this.membreId = membreId;
        this.nom = nom;
        this.fonction = fonction;
        this.ajouteLe = ajouteLe;
    }

    public boolean isAdministrateur() {
        return administrateur;
    }

    public void nommerAdministrateur() {
        this.administrateur = true;
    }

    /** La marque ne recule jamais : lire un message plus ancien ne rend pas les suivants non lus. */
    public void marquerLu(UUID messageId, Instant envoyeLe) {
        if (dernierLuLe == null || !envoyeLe.isBefore(dernierLuLe)) {
            this.dernierLuMessageId = messageId;
            this.dernierLuLe = envoyeLe;
        }
    }

    public Conversation getConversation() {
        return conversation;
    }

    public String getMembreId() {
        return membreId;
    }

    public String getNom() {
        return nom;
    }

    public String getFonction() {
        return fonction;
    }

    public Instant getAjouteLe() {
        return ajouteLe;
    }

    public UUID getDernierLuMessageId() {
        return dernierLuMessageId;
    }

    public Instant getDernierLuLe() {
        return dernierLuLe;
    }
}
