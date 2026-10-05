package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.ReferenceOperation;
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

/**
 * Opération du référentiel instanciée pour une liaison : état, qualités (RACI + E), échéance, preuve. Le modèle
 * de l'opération (libellé, geste, jalon, déclencheur, particularité, livrables, durée, responsable de référence)
 * est recopié de la version publiée de la liaison (D-77) : le moteur ne lit plus aucun rang codé en dur.
 */
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

    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "libelle", length = 300)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "geste", length = 40)
    private Geste geste;

    @Enumerated(EnumType.STRING)
    @Column(name = "jalon", length = 40)
    private Jalon jalon;

    @Column(name = "declencheur", length = 120)
    private String declencheur;

    @Enumerated(EnumType.STRING)
    @Column(name = "particularite", length = 40)
    private Particularite particularite;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "livrables", columnDefinition = "jsonb")
    private String livrables;

    @Column(name = "delai_jours")
    private Integer delaiJours;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsable", length = 40)
    private Fonction responsable;

    protected OperationParcours() {}

    /** Opération de la version publiée instanciée pour une liaison, verrouillée jusqu'à son éligibilité. */
    public OperationParcours(UUID liaisonId, ReferenceOperation ref, int ordre) {
        this.id = UUID.randomUUID();
        this.liaisonId = liaisonId;
        this.rang = ref.getCode();
        this.ordre = ordre;
        this.etat = EtatOperationParcours.VERROUILLEE;
        appliquerModele(ref);
    }

    /** Recopie le modèle de l'opération de référence (instanciation, migration vers une nouvelle version). */
    public final void appliquerModele(ReferenceOperation ref) {
        this.versionId = ref.getVersion() == null ? null : ref.getVersion().getId();
        this.libelle = ref.getLibelle();
        this.etape = ref.etapeEffective();
        this.segment = ref.segmentEffectif();
        this.plateforme = ref.plateformeEffective();
        this.grain = ref.grainEffectif();
        this.cle = ref.cleEffective();
        this.secret = ref.secretEffectif();
        this.valeurReference = ref.valeurReferenceAttendue();
        this.oubliSansSymptome = ref.isOubliSansSymptome();
        this.pivot = ref.pivotEffectif();
        this.predecesseurs = String.join("|", ref.predecesseursListe());
        this.geste = ref.gesteEffectif();
        this.jalon = ref.getJalon();
        this.declencheur = ref.getDeclencheur();
        this.particularite = ref.getParticularite();
        this.livrables = JsonTexte.ecrire(ref.getLivrables());
        this.delaiJours = ref.delaiEffectif();
        this.responsable = ref.responsableEffectif();
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public UUID getVersionId() {
        return versionId;
    }

    public void setVersionId(UUID versionId) {
        this.versionId = versionId;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Geste d'exécution ; une opération instanciée avant D-77 sans geste repris dépose une preuve. */
    public Geste getGeste() {
        return geste == null ? Geste.DEPOSER_PREUVE : geste;
    }

    public Jalon getJalon() {
        return jalon;
    }

    public String getDeclencheur() {
        return declencheur;
    }

    public Particularite getParticularite() {
        return particularite;
    }

    public List<Livrable> getLivrables() {
        return JsonTexte.liste(livrables, Livrable.class);
    }

    public int getDelaiJours() {
        return delaiJours == null ? 3 : delaiJours;
    }

    /** Fonction responsable (R) de référence, tenue par le référentiel quelle que soit la personne désignée. */
    public Fonction getResponsable() {
        return responsable != null ? responsable : fonctionR();
    }

    /** Attributs du modèle, comparés lors d'une migration (opération modifiée ou conservée). */
    public List<Object> signature() {
        return java.util.Arrays.asList(
            libelle,
            etape,
            segment,
            plateforme,
            grain,
            cle,
            secret,
            valeurReference,
            oubliSansSymptome,
            pivot,
            getPredecesseurs(),
            getGeste(),
            jalon,
            declencheur,
            particularite,
            getLivrables(),
            getDelaiJours(),
            responsable
        );
    }

    public boolean porte(Geste g) {
        return getGeste() == g;
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
