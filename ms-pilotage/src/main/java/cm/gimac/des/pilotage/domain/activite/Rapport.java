package cm.gimac.des.pilotage.domain.activite;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Rapport quotidien ou hebdomadaire d'un compte interne à son responsable (§2.4) : un seul par
 * (auteur, type, période) ; BROUILLON modifiable, SOUMIS au destinataire (résolu à la soumission : n+1, chef de
 * l'unité, chef d'une unité parente, ou choisi par l'auteur ; {@code destinataireMotif}), VISE final ; DEPOSE (aucun
 * destinataire au sommet de l'organisation) final, sans visa. Le contenu n'est jamais publié dans un événement.
 */
@Entity
@Table(name = "rapport")
public class Rapport extends EntitePilotage {

    public static final int CONTENU_MAX = 20_000;
    public static final int COMMENTAIRE_MAX = 2000;

    @Column(name = "auteur_id", nullable = false, length = 80)
    private String auteurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private TypeRapport type;

    @Column(name = "periode", nullable = false, length = 10)
    private String periode;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 16)
    private ModeRapport mode;

    @Column(name = "contenu", columnDefinition = "text")
    private String contenu;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "document_nom", length = 255)
    private String documentNom;

    @Column(name = "document_type_mime", length = 255)
    private String documentTypeMime;

    @Column(name = "document_taille")
    private Long documentTaille;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 16)
    private EtatRapport etat = EtatRapport.BROUILLON;

    @Column(name = "destinataire_id", length = 80)
    private String destinataireId;

    @Enumerated(EnumType.STRING)
    @Column(name = "destinataire_motif", length = 24)
    private MotifDestinataire destinataireMotif;

    @Column(name = "soumis_le")
    private Instant soumisLe;

    @Column(name = "vise_le")
    private Instant viseLe;

    @Column(name = "visa_par_id", length = 80)
    private String visaParId;

    @Column(name = "visa_commentaire", length = COMMENTAIRE_MAX)
    private String visaCommentaire;

    /** Indicateurs figés à la soumission ; calculés à la lecture tant que le rapport est un brouillon. */
    @Column(name = "ind_activites_faites")
    private Integer indActivitesFaites;

    @Column(name = "ind_activites_prevues")
    private Integer indActivitesPrevues;

    @Column(name = "ind_minutes")
    private Integer indMinutes;

    @Column(name = "ind_taches_terminees")
    private Integer indTachesTerminees;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Rapport() {}

    public Rapport(UUID id, String auteurId, Periode periode, ModeRapport mode, Instant le) {
        super(id);
        this.auteurId = auteurId;
        this.type = periode.type();
        this.periode = periode.texte();
        this.mode = mode;
        this.creeLe = le;
        this.modifieLe = le;
    }

    /** Mode et texte d'un brouillon ; 409 PIL_RAPPORT_NON_MODIFIABLE après la soumission. */
    public void rediger(ModeRapport mode, String contenu, Instant le) {
        exigerBrouillon();
        this.mode = mode;
        this.contenu = contenu;
        this.modifieLe = le;
    }

    /** Document déjà vérifié auprès de MS-DOCUMENT (ticket d'un dépôt de finalité RAPPORT_ACTIVITE). */
    public void joindre(UUID documentId, String nom, String typeMime, long taille, Instant le) {
        exigerBrouillon();
        this.documentId = documentId;
        this.documentNom = nom;
        this.documentTypeMime = typeMime;
        this.documentTaille = taille;
        this.modifieLe = le;
    }

    public void retirerDocument(Instant le) {
        exigerBrouillon();
        this.documentId = null;
        this.documentNom = null;
        this.documentTypeMime = null;
        this.documentTaille = null;
        this.modifieLe = le;
    }

    /**
     * Soumission au destinataire : brouillon seulement (409), contenu ou document selon le mode (422
     * PIL_RAPPORT_INCOMPLET). Sans destinataire (sommet de l'organisation) : rapport DEPOSE, final, sans visa.
     */
    public void soumettre(String destinataireId, MotifDestinataire motif, Indicateurs indicateurs, Instant le) {
        exigerBrouillon();
        boolean texte = contenu != null && !contenu.isBlank();
        if (mode == ModeRapport.DOCUMENT ? documentId == null : !texte) {
            throw PilotageException.regle(
                "PIL_RAPPORT_INCOMPLET",
                mode == ModeRapport.DOCUMENT ? "Aucun document joint au rapport" : "Le rapport est vide",
                Map.of("mode", mode.name())
            );
        }
        boolean sansDestinataire = destinataireId == null || destinataireId.isBlank();
        this.destinataireId = sansDestinataire ? null : destinataireId;
        this.destinataireMotif = sansDestinataire ? null : motif;
        this.indActivitesFaites = indicateurs.activitesFaites();
        this.indActivitesPrevues = indicateurs.activitesPrevues();
        this.indMinutes = indicateurs.minutes();
        this.indTachesTerminees = indicateurs.tachesTerminees();
        this.etat = sansDestinataire ? EtatRapport.DEPOSE : EtatRapport.SOUMIS;
        this.soumisLe = le;
        this.modifieLe = le;
    }

    /** Visa d'un rapport soumis ; 409 PIL_TRANSITION_INTERDITE sinon. */
    public void viser(String parId, String commentaire, Instant le) {
        if (etat != EtatRapport.SOUMIS) {
            throw PilotageException.conflit(
                "PIL_TRANSITION_INTERDITE",
                "Seul un rapport soumis peut être visé",
                Map.of("de", etat.name(), "vers", EtatRapport.VISE.name())
            );
        }
        if (commentaire != null && commentaire.length() > COMMENTAIRE_MAX) {
            throw PilotageException.champ("commentaire", "Commentaire de " + COMMENTAIRE_MAX + " caractères au plus");
        }
        this.etat = EtatRapport.VISE;
        this.visaParId = parId;
        this.visaCommentaire = commentaire;
        this.viseLe = le;
        this.modifieLe = le;
    }

    public void exigerBrouillon() {
        if (etat != EtatRapport.BROUILLON) {
            throw PilotageException.conflit(
                "PIL_RAPPORT_NON_MODIFIABLE",
                "Un rapport soumis, visé ou déposé n'est plus modifiable",
                Map.of("etat", etat.name())
            );
        }
    }

    /** Indicateurs figés, ou null pour un brouillon. */
    public Indicateurs indicateursFiges() {
        if (indActivitesFaites == null) {
            return null;
        }
        return new Indicateurs(indActivitesFaites, indActivitesPrevues, indMinutes, indTachesTerminees);
    }

    public Periode periode() {
        return Periode.de(type, periode);
    }

    public String getAuteurId() {
        return auteurId;
    }

    public TypeRapport getType() {
        return type;
    }

    public String getPeriode() {
        return periode;
    }

    public ModeRapport getMode() {
        return mode;
    }

    public String getContenu() {
        return contenu;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getDocumentNom() {
        return documentNom;
    }

    public String getDocumentTypeMime() {
        return documentTypeMime;
    }

    public Long getDocumentTaille() {
        return documentTaille;
    }

    public EtatRapport getEtat() {
        return etat;
    }

    public String getDestinataireId() {
        return destinataireId;
    }

    public MotifDestinataire getDestinataireMotif() {
        return destinataireMotif;
    }

    public Instant getSoumisLe() {
        return soumisLe;
    }

    public Instant getViseLe() {
        return viseLe;
    }

    public String getVisaParId() {
        return visaParId;
    }

    public String getVisaCommentaire() {
        return visaCommentaire;
    }

    public Instant getCreeLe() {
        return creeLe;
    }

    public Instant getModifieLe() {
        return modifieLe;
    }
}
