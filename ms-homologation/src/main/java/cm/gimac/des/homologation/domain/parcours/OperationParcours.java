package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.JsonTexte;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Opération du référentiel instanciée pour une liaison : état, qualités (RACI + E), échéance, preuve. */
@Entity
@Table(name = "parcours_operation")
public class OperationParcours {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    @Column(name = "rang", nullable = false)
    private String rang;

    @Column(name = "ordre", nullable = false)
    private int ordre;

    @Enumerated(EnumType.STRING)
    @Column(name = "etape", nullable = false)
    private EtapeCode etape;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment", nullable = false)
    private SegmentParcours segment;

    @Enumerated(EnumType.STRING)
    @Column(name = "plateforme")
    private PlateformeExecution plateforme;

    @Enumerated(EnumType.STRING)
    @Column(name = "grain", nullable = false)
    private GrainPreuve grain;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false)
    private EtatOperationParcours etat;

    @Column(name = "cle", nullable = false)
    private boolean cle;

    @Column(name = "secret", nullable = false)
    private boolean secret;

    @Column(name = "valeur_reference", nullable = false)
    private boolean valeurReference;

    @Column(name = "oubli_sans_symptome", nullable = false)
    private boolean oubliSansSymptome;

    @Column(name = "pivot", nullable = false)
    private boolean pivot;

    @Column(name = "predecesseurs")
    private String predecesseurs;

    @Column(name = "echeance")
    private LocalDate echeance;

    @Column(name = "faite_le")
    private LocalDate faiteLe;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raci", columnDefinition = "jsonb", nullable = false)
    private String raci;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preuve", columnDefinition = "jsonb")
    private String preuve;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected OperationParcours() {}

    public OperationParcours(UUID liaisonId, ReferentielOperation ref, int ordre, List<String> predecesseurs) {
        this.id = UUID.randomUUID();
        this.liaisonId = liaisonId;
        this.rang = ref.getRang();
        this.ordre = ordre;
        this.etape = ref.getEtape();
        this.segment = ref.getSegment();
        this.plateforme = ref.getPlateforme();
        this.grain = ref.getGrain();
        this.cle = ref.isCle();
        this.secret = ref.isSecret();
        this.valeurReference = ref.isValeurReference();
        this.oubliSansSymptome = ref.isOubliSansSymptome();
        this.pivot = ref.isPivot();
        this.predecesseurs = String.join("|", predecesseurs);
        this.etat = EtatOperationParcours.VERROUILLEE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getRang() {
        return rang;
    }

    public int getOrdre() {
        return ordre;
    }

    public EtapeCode getEtape() {
        return etape;
    }

    public SegmentParcours getSegment() {
        return segment;
    }

    public PlateformeExecution getPlateforme() {
        return plateforme;
    }

    public void setPlateforme(PlateformeExecution plateforme) {
        this.plateforme = plateforme;
    }

    public GrainPreuve getGrain() {
        return grain;
    }

    public EtatOperationParcours getEtat() {
        return etat;
    }

    public void setEtat(EtatOperationParcours etat) {
        this.etat = etat;
    }

    public boolean isCle() {
        return cle;
    }

    public boolean isSecret() {
        return secret;
    }

    public boolean isValeurReference() {
        return valeurReference;
    }

    public boolean isOubliSansSymptome() {
        return oubliSansSymptome;
    }

    public boolean isPivot() {
        return pivot;
    }

    public List<String> getPredecesseurs() {
        return predecesseurs == null || predecesseurs.isBlank() ? List.of() : Arrays.asList(predecesseurs.split("\\|"));
    }

    public LocalDate getEcheance() {
        return echeance;
    }

    public void setEcheance(LocalDate echeance) {
        this.echeance = echeance;
    }

    public LocalDate getFaiteLe() {
        return faiteLe;
    }

    public void setFaiteLe(LocalDate faiteLe) {
        this.faiteLe = faiteLe;
    }

    public Raci getRaci() {
        return raci == null ? new Raci(null, List.of(), null, List.of(), List.of()) : JsonTexte.lire(raci, Raci.class);
    }

    public void setRaci(Raci r) {
        this.raci = JsonTexte.ecrire(r);
    }

    public Preuve getPreuve() {
        return preuve == null ? null : JsonTexte.lire(preuve, Preuve.class);
    }

    public void setPreuve(Preuve p) {
        this.preuve = p == null ? null : JsonTexte.ecrire(p);
    }

    public boolean faite() {
        return etat == EtatOperationParcours.FAITE;
    }

    /** Fonction du responsable principal (R), le cas échéant. */
    public Fonction fonctionR() {
        Personne r = getRaci().r();
        return r == null ? null : r.fonction();
    }
}
