package cm.gimac.des.iam.infrastructure.persistence;

import cm.gimac.des.iam.domain.demande.EtatDemande;
import cm.gimac.des.iam.domain.demande.TypeDemande;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Demande d'un chef à l'administration (D-81, docs/lot1/pouvoirs-hierarchiques.md §4). Le contenu est
 * gardé en JSON ; cle_cible porte l'unicité des demandes en attente (une par cible et par type, index
 * unique partiel). Version optimiste : deux décisions concurrentes ne passent pas toutes les deux.
 */
@Entity
@Table(name = "demande_administration")
public class DemandeAdministration {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TypeDemande type;

    @Column(name = "demandeur_id", nullable = false, length = 36)
    private String demandeurId;

    @Column(name = "demandeur_nom", length = 255)
    private String demandeurNom;

    @Column(name = "cible_id", length = 36)
    private String cibleId;

    @Column(name = "cible_nom", length = 255)
    private String cibleNom;

    @Column(name = "cle_cible", nullable = false, length = 120)
    private String cleCible;

    @Column(name = "contenu", nullable = false, length = 4000)
    private String contenu;

    @Column(name = "motif", nullable = false, length = 500)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 12)
    private EtatDemande etat;

    @Column(name = "decision_par_id", length = 36)
    private String decisionParId;

    @Column(name = "decision_par_nom", length = 255)
    private String decisionParNom;

    @Column(name = "decision_motif", length = 500)
    private String decisionMotif;

    @Column(name = "decision_le")
    private Instant decisionLe;

    @Column(name = "creee_le", nullable = false)
    private Instant creeeLe;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected DemandeAdministration() {}

    @SuppressWarnings("java:S107")
    public DemandeAdministration(TypeDemande type, String demandeurId, String demandeurNom, String cibleId, String cibleNom, String cleCible, String contenu, String motif, Instant creeeLe) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.demandeurId = demandeurId;
        this.demandeurNom = demandeurNom;
        this.cibleId = cibleId;
        this.cibleNom = cibleNom;
        this.cleCible = cleCible;
        this.contenu = contenu;
        this.motif = motif;
        this.etat = EtatDemande.EN_ATTENTE;
        this.creeeLe = creeeLe;
    }

    /** Décision (acceptation, refus) ou annulation : nouvel état, auteur, motif éventuel, instant. */
    public void decider(EtatDemande nouvelEtat, String parId, String parNom, String motifDecision, Instant le) {
        this.etat = nouvelEtat;
        this.decisionParId = parId;
        this.decisionParNom = parNom;
        this.decisionMotif = motifDecision;
        this.decisionLe = le;
    }

    /** Compte créé par l'acceptation d'une demande de création. */
    public void definirCible(String id, String nom) {
        this.cibleId = id;
        this.cibleNom = nom;
    }

    public UUID getId() { return id; }
    public TypeDemande getType() { return type; }
    public String getDemandeurId() { return demandeurId; }
    public String getDemandeurNom() { return demandeurNom; }
    public String getCibleId() { return cibleId; }
    public String getCibleNom() { return cibleNom; }
    public String getCleCible() { return cleCible; }
    public String getContenu() { return contenu; }
    public String getMotif() { return motif; }
    public EtatDemande getEtat() { return etat; }
    public String getDecisionParId() { return decisionParId; }
    public String getDecisionParNom() { return decisionParNom; }
    public String getDecisionMotif() { return decisionMotif; }
    public Instant getDecisionLe() { return decisionLe; }
    public Instant getCreeeLe() { return creeeLe; }
}
