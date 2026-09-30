package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.JsonTexte;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Blocage daté (CR annexe B, D-59) : motif, depuis le, prochaine action, responsable, échéance de levée. */
@Entity
@Table(name = "parcours_blocage")
public class BlocageParcours {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    @Column(name = "motif", nullable = false)
    private String motif;

    @Column(name = "depuis", nullable = false)
    private LocalDate depuis;

    @Column(name = "prochaine_action", nullable = false)
    private String prochaineAction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "responsable", columnDefinition = "jsonb", nullable = false)
    private String responsable;

    @Column(name = "echeance_levee", nullable = false)
    private LocalDate echeanceLevee;

    @Column(name = "leve_le")
    private LocalDate leveLe;

    @Column(name = "commentaire_levee")
    private String commentaireLevee;

    @Enumerated(EnumType.STRING)
    @Column(name = "etape", nullable = false)
    private EtapeCode etape;

    @Column(name = "declare_par")
    private UUID declarePar;

    protected BlocageParcours() {}

    public BlocageParcours(UUID liaisonId, String motif, LocalDate depuis, String prochaineAction, Personne responsable, LocalDate echeanceLevee, EtapeCode etape, UUID declarePar) {
        this.id = UUID.randomUUID();
        this.liaisonId = liaisonId;
        this.motif = motif;
        this.depuis = depuis;
        this.prochaineAction = prochaineAction;
        this.responsable = JsonTexte.ecrire(responsable);
        this.echeanceLevee = echeanceLevee;
        this.etape = etape;
        this.declarePar = declarePar;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getMotif() {
        return motif;
    }

    public LocalDate getDepuis() {
        return depuis;
    }

    public String getProchaineAction() {
        return prochaineAction;
    }

    public Personne getResponsable() {
        return JsonTexte.lire(responsable, Personne.class);
    }

    public LocalDate getEcheanceLevee() {
        return echeanceLevee;
    }

    public LocalDate getLeveLe() {
        return leveLe;
    }

    public String getCommentaireLevee() {
        return commentaireLevee;
    }

    public EtapeCode getEtape() {
        return etape;
    }

    public UUID getDeclarePar() {
        return declarePar;
    }

    public boolean actif() {
        return leveLe == null;
    }

    public void lever(LocalDate le, String commentaire) {
        this.leveLe = le;
        this.commentaireLevee = commentaire;
    }
}
