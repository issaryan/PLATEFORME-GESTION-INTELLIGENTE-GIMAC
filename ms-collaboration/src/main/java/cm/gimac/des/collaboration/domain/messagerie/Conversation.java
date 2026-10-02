package cm.gimac.des.collaboration.domain.messagerie;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

/**
 * Conversation de la messagerie de travail (D-64). Une conversation directe réunit deux personnes
 * sans titre ni liaison ; sa clé (identifiants triés) garantit qu'elle est unique et réutilisée.
 */
@Entity
@Table(name = "conversation")
public class Conversation extends EntiteMessagerie {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private TypeConversation type;

    @Column(name = "titre", length = 200)
    private String titre;

    @Column(name = "liaison_id")
    private UUID liaisonId;

    @Column(name = "liaison_reference", length = 30)
    private String liaisonReference;

    /** Identifiants des deux membres d'une conversation directe, triés ; vide pour un groupe. */
    @Column(name = "cle_directe", length = 200, unique = true)
    private String cleDirecte;

    @Column(name = "cree_par", nullable = false, length = 64)
    private String creePar;

    @Column(name = "creee_le", nullable = false)
    private Instant creeeLe;

    @Column(name = "mis_a_jour_le", nullable = false)
    private Instant misAJourLe;

    @Column(name = "dernier_message_id")
    private UUID dernierMessageId;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ajouteLe ASC, nom ASC")
    @BatchSize(size = 50)
    private List<ConversationMembre> membres = new ArrayList<>();

    protected Conversation() {}

    public Conversation(TypeConversation type, String titre, UUID liaisonId, String liaisonReference, String cleDirecte, String creePar, Instant creeeLe) {
        this.type = type;
        this.titre = titre;
        this.liaisonId = liaisonId;
        this.liaisonReference = liaisonReference;
        this.cleDirecte = cleDirecte;
        this.creePar = creePar;
        this.creeeLe = creeeLe;
        this.misAJourLe = creeeLe;
    }

    public boolean estGroupe() {
        return type == TypeConversation.GROUPE;
    }

    /** Nouveau nom d'un groupe (D-68). */
    public void renommer(String nouveauTitre, Instant le) {
        this.titre = nouveauTitre;
        this.misAJourLe = le;
    }

    /**
     * Retire un membre d'un groupe (D-68). S'il ne reste aucun administrateur, le membre présent depuis
     * le plus longtemps le devient : un groupe garde toujours quelqu'un pour le gérer.
     */
    public void retirerMembre(ConversationMembre m) {
        membres.remove(m);
        if (!membres.isEmpty() && membres.stream().noneMatch(ConversationMembre::isAdministrateur)) {
            membres.stream().min(java.util.Comparator.comparing(ConversationMembre::getAjouteLe)).ifPresent(ConversationMembre::nommerAdministrateur);
        }
    }

    public ConversationMembre ajouterMembre(Collaborateur c, Instant le) {
        ConversationMembre m = new ConversationMembre(this, c.id(), c.nom(), c.fonction(), le);
        membres.add(m);
        return m;
    }

    public Optional<ConversationMembre> membre(String membreId) {
        return membres.stream().filter(m -> m.getMembreId().equals(membreId)).findFirst();
    }

    /** Un nouveau message remonte la conversation en tête de liste. */
    public void messageAjoute(UUID messageId, Instant envoyeLe) {
        this.dernierMessageId = messageId;
        this.misAJourLe = envoyeLe;
    }

    public TypeConversation getType() {
        return type;
    }

    public String getTitre() {
        return titre;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getLiaisonReference() {
        return liaisonReference;
    }

    public String getCleDirecte() {
        return cleDirecte;
    }

    public String getCreePar() {
        return creePar;
    }

    public Instant getCreeeLe() {
        return creeeLe;
    }

    public Instant getMisAJourLe() {
        return misAJourLe;
    }

    public UUID getDernierMessageId() {
        return dernierMessageId;
    }

    public List<ConversationMembre> getMembres() {
        return List.copyOf(membres);
    }
}
