package cm.gimac.des.notification.domain.boite;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Notification remise à un destinataire (docs/lot1/notifications.md §2) : titre, sous-titre, lien dans la
 * couche, objet désigné, acteur, nombre d'événements regroupés et lecture. Aucun contenu de message n'est
 * conservé. Consolidation (entité ConsolidationWindow du JDL) : une seule notification de message non lue
 * par destinataire et conversation ; un nouveau message l'incrémente et la remonte en tête. Les mentions
 * et les réponses (D-82) ne sont jamais regroupées ; elles portent, comme les messages, la conversation
 * (colonne conversation_id) pour être marquées lues à l'ouverture de celle-ci.
 */
@Entity
@Table(name = "notification_utilisateur")
public class NotificationUtilisateur {

    /** Bornes des colonnes texte (le titre est tronqué, jamais refusé). */
    public static final int TITRE_MAX = 200;
    public static final int LIEN_MAX = 500;

    @Id
    private UUID id;

    @Column(name = "destinataire_id", nullable = false, length = 80)
    private String destinataireId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private TypeNotification type;

    @Column(name = "titre", nullable = false, length = TITRE_MAX)
    private String titre;

    @Column(name = "sous_titre", length = TITRE_MAX)
    private String sousTitre;

    @Column(name = "lien", nullable = false, length = LIEN_MAX)
    private String lien;

    @Embedded
    private Reference reference;

    /** Conversation de la messagerie (MESSAGE, MENTION, REPONSE) ; null pour les autres types. */
    @Column(name = "conversation_id", length = 80)
    private String conversationId;

    @Column(name = "acteur_id", length = 80)
    private String acteurId;

    @Column(name = "acteur_nom", length = TITRE_MAX)
    private String acteurNom;

    @Column(name = "nombre", nullable = false)
    private int nombre;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "mis_a_jour_le", nullable = false)
    private Instant misAJourLe;

    @Column(name = "lue_le")
    private Instant lueLe;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected NotificationUtilisateur() {}

    public NotificationUtilisateur(
        String destinataireId,
        TypeNotification type,
        String titre,
        String sousTitre,
        String lien,
        Reference reference,
        String acteurId,
        String acteurNom,
        Instant le
    ) {
        this.id = UUID.randomUUID();
        this.destinataireId = destinataireId;
        this.type = type;
        this.titre = borne(titre);
        this.sousTitre = borne(sousTitre);
        this.lien = lien;
        this.reference = reference;
        this.acteurId = acteurId;
        this.acteurNom = borne(acteurNom);
        this.nombre = 1;
        this.creeLe = le;
        this.misAJourLe = le;
    }

    /** Notification de la messagerie : rattachée à sa conversation (marquage en lecture à l'ouverture). */
    public NotificationUtilisateur dansConversation(String conversationId) {
        this.conversationId = conversationId;
        return this;
    }

    /** Nouvel événement regroupé dans cette notification non lue : nombre incrémenté, remontée en tête. */
    public void regrouper(String titre, String sousTitre, String acteurId, String acteurNom, Instant le) {
        this.titre = borne(titre);
        this.sousTitre = borne(sousTitre);
        this.acteurId = acteurId;
        this.acteurNom = borne(acteurNom);
        this.nombre++;
        if (le.isAfter(misAJourLe)) {
            this.misAJourLe = le;
        }
    }

    /** Marque la notification lue ; sans effet si elle l'est déjà. */
    public boolean marquerLue(Instant le) {
        if (lueLe != null) {
            return false;
        }
        this.lueLe = le;
        return true;
    }

    public boolean estLue() {
        return lueLe != null;
    }

    private static String borne(String texte) {
        if (texte == null) {
            return null;
        }
        String t = texte.strip();
        return t.length() <= TITRE_MAX ? t : t.substring(0, TITRE_MAX - 1) + "…";
    }

    public UUID getId() {
        return id;
    }

    public String getDestinataireId() {
        return destinataireId;
    }

    public TypeNotification getType() {
        return type;
    }

    public String getTitre() {
        return titre;
    }

    public String getSousTitre() {
        return sousTitre;
    }

    public String getLien() {
        return lien;
    }

    public Reference getReference() {
        return reference;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getActeurId() {
        return acteurId;
    }

    public String getActeurNom() {
        return acteurNom;
    }

    public int getNombre() {
        return nombre;
    }

    public Instant getCreeLe() {
        return creeLe;
    }

    public Instant getMisAJourLe() {
        return misAJourLe;
    }

    public Instant getLueLe() {
        return lueLe;
    }
}
