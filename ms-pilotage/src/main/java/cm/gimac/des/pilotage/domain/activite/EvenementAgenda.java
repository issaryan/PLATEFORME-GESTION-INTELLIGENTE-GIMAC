package cm.gimac.des.pilotage.domain.activite;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Événement de l'agenda d'un compte interne (§2.1), avec ses participants invités. */
@Entity
@Table(name = "evenement_agenda")
public class EvenementAgenda extends EntitePilotage {

    @Column(name = "proprietaire_id", nullable = false, length = 80)
    private String proprietaireId;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "lieu", length = 200)
    private String lieu;

    @Column(name = "debut", nullable = false)
    private Instant debut;

    @Column(name = "fin", nullable = false)
    private Instant fin;

    @Column(name = "journee_entiere", nullable = false)
    private boolean journeeEntiere;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 16)
    private CategorieEvenement categorie;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "evenement_participant", joinColumns = @JoinColumn(name = "evenement_id"))
    @OrderColumn(name = "rang")
    private List<ParticipantEvenement> participants = new ArrayList<>();

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EvenementAgenda() {}

    public EvenementAgenda(String proprietaireId, Instant le) {
        this.proprietaireId = proprietaireId;
        this.creeLe = le;
        this.modifieLe = le;
    }

    /** Contenu déjà validé par les règles de l'agenda ; les réponses des participants maintenus sont conservées. */
    public void decrire(
        String titre,
        String description,
        String lieu,
        Instant debut,
        Instant fin,
        boolean journeeEntiere,
        CategorieEvenement categorie,
        List<String> invites,
        Instant le
    ) {
        this.titre = titre;
        this.description = description;
        this.lieu = lieu;
        this.debut = debut;
        this.fin = fin;
        this.journeeEntiere = journeeEntiere;
        this.categorie = categorie;
        Set<String> gardes = Set.copyOf(invites);
        participants.removeIf(p -> !gardes.contains(p.getParticipantId()));
        for (String id : invites) {
            if (participant(id).isEmpty()) {
                participants.add(new ParticipantEvenement(id));
            }
        }
        this.modifieLe = le;
    }

    public Optional<ParticipantEvenement> participant(String id) {
        return participants.stream().filter(p -> p.getParticipantId().equals(id)).findFirst();
    }

    public boolean repondre(String participantId, ReponseParticipant reponse, Instant le) {
        Optional<ParticipantEvenement> p = participant(participantId);
        p.ifPresent(x -> x.repondre(reponse, le));
        return p.isPresent();
    }

    public String getProprietaireId() {
        return proprietaireId;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public String getLieu() {
        return lieu;
    }

    public Instant getDebut() {
        return debut;
    }

    public Instant getFin() {
        return fin;
    }

    public boolean isJourneeEntiere() {
        return journeeEntiere;
    }

    public CategorieEvenement getCategorie() {
        return categorie;
    }

    public List<ParticipantEvenement> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public Instant getCreeLe() {
        return creeLe;
    }

    public Instant getModifieLe() {
        return modifieLe;
    }
}
