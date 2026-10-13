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

    /** Échéance maximale (D-106) : au-delà, l'opération est en dépassement. */
    @Column(name = "echeance")
    private LocalDate echeance;

    /** Échéance cible (D-106) : objectif ; l'atteindre annonce l'échéance proche. */
    @Column(name = "echeance_cible")
    private LocalDate echeanceCible;

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

    /** Délai maximal recopié du référentiel (D-106) ; null = égal au délai cible. */
    @Column(name = "delai_max_jours")
    private Integer delaiMaxJours;

    @jakarta.persistence.Convert(converter = cm.gimac.des.homologation.domain.ConvertisseursCodes.FonctionEnTexte.class)
    @Column(name = "responsable", length = 40)
    private Fonction responsable;

    /** Signatures exigées, recopiées de la version (D-91). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "signatures", columnDefinition = "jsonb")
    private String signatures;

    /** Signatures recueillies (D-91). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "signatures_recueillies", columnDefinition = "jsonb")
    private String signaturesRecueillies;

    /** Code de l'étape de la version (D-89), recopié : l'interface affiche les vraies étapes (D-93 §5). */
    @Column(name = "etape_code", length = 40)
    private String etapeCode;

    /** Écran spécialisé de l'opération (D-93 §5), recopié de la version. */
    @Enumerated(EnumType.STRING)
    @Column(name = "vue", length = 30)
    private VueOperation vue;

    /** Les consultés internes désignés reçoivent une tâche (D-93 §1), recopié de la version. */
    @Column(name = "consultes_recoivent_tache", nullable = false)
    private boolean consultesRecoiventTache;

    /** Dépôts des livrables attendus, par code de livrable (D-93 §3). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "depots", columnDefinition = "jsonb")
    private String depots;

    /** Renvois par le validateur, du plus ancien au plus récent (D-93 §2). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "renvois", columnDefinition = "jsonb")
    private String renvois;

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
        this.delaiMaxJours = ref.delaiMaxEffectif();
        this.responsable = ref.responsableEffectif();
        List<SignatureOperation> sig = ref.getSignatures();
        this.signatures = sig.isEmpty() ? null : JsonTexte.ecrire(sig);
        this.etapeCode = ref.getEtape() != null && !ref.getEtape().isBlank() ? ref.getEtape() : this.etape.name();
        this.vue = ref.vueEffective();
        this.consultesRecoiventTache = ref.isConsultesRecoiventTache();
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

    /** Délai cible (objectif) de l'opération. */
    public int getDelaiJours() {
        return delaiJours == null ? 3 : delaiJours;
    }

    /** Délai maximal (D-106) : le délai cible pour une opération antérieure, jamais en deçà de la cible. */
    public int getDelaiMaxJours() {
        return delaiMaxJours == null ? getDelaiJours() : Math.max(delaiMaxJours, getDelaiJours());
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
            getDelaiMaxJours(),
            responsable,
            getSignatures(),
            getEtapeCode(),
            getVue(),
            consultesRecoiventTache
        );
    }

    // ------------------------------------------------------------------ D-93 : étape de la version, vue, livrables, renvois

    /** Code de l'étape de la version ; une opération antérieure à D-93 sans code repris : son étape canonique. */
    public String getEtapeCode() {
        return etapeCode != null ? etapeCode : etape == null ? null : etape.name();
    }

    /** Écran spécialisé ; une opération antérieure à D-93 sans vue reprise : déduite de son modèle. */
    public VueOperation getVue() {
        return vue != null ? vue : VueOperation.deduite(getGeste(), particularite, declencheur);
    }

    public boolean isConsultesRecoiventTache() {
        return consultesRecoiventTache;
    }

    public List<DepotLivrable> getDepots() {
        return depots == null ? List.of() : JsonTexte.liste(depots, DepotLivrable.class);
    }

    /** Dépôt d'un livrable attendu, le plus récent. */
    public java.util.Optional<DepotLivrable> depot(String livrable) {
        List<DepotLivrable> l = getDepots();
        for (int i = l.size() - 1; i >= 0; i--) {
            if (livrable.equals(l.get(i).livrable())) {
                return java.util.Optional.of(l.get(i));
            }
        }
        return java.util.Optional.empty();
    }

    /** Ajoute un dépôt ; un nouveau dépôt du même livrable remplace le précédent. */
    void deposer(DepotLivrable d) {
        List<DepotLivrable> l = new java.util.ArrayList<>(getDepots().stream().filter(x -> !x.livrable().equals(d.livrable())).toList());
        l.add(d);
        this.depots = JsonTexte.ecrire(l);
    }

    /** Livrables obligatoires du référentiel pas encore déposés. */
    public List<Livrable> livrablesManquants() {
        return getLivrables().stream().filter(Livrable::obligatoire).filter(l -> depot(l.code()).isEmpty()).toList();
    }

    public List<Renvoi> getRenvois() {
        return renvois == null ? List.of() : JsonTexte.liste(renvois, Renvoi.class);
    }

    void ajouterRenvoi(Renvoi r) {
        List<Renvoi> l = new java.util.ArrayList<>(getRenvois());
        l.add(r);
        this.renvois = JsonTexte.ecrire(l);
    }

    // ------------------------------------------------------------------ signatures (D-91)

    /** Signatures exigées à la clôture, dans leur ordre (ordre, puis code). */
    public List<SignatureOperation> getSignatures() {
        if (signatures == null) {
            return List.of();
        }
        return JsonTexte.liste(signatures, SignatureOperation.class)
            .stream()
            .sorted(java.util.Comparator.comparingInt(SignatureOperation::ordreEffectif).thenComparing(SignatureOperation::code))
            .toList();
    }

    public List<SignatureRecueillie> getSignaturesRecueillies() {
        return signaturesRecueillies == null ? List.of() : JsonTexte.liste(signaturesRecueillies, SignatureRecueillie.class);
    }

    public java.util.Optional<SignatureRecueillie> recueillie(String code) {
        return getSignaturesRecueillies().stream().filter(r -> r.code().equals(code)).findFirst();
    }

    /** Signatures exigées et pas encore recueillies, dans leur ordre. */
    public List<SignatureOperation> signaturesAttendues() {
        return getSignatures().stream().filter(s -> recueillie(s.code()).isEmpty()).toList();
    }

    /** Signatures attendues du plus petit ordre restant : celles qui peuvent être données maintenant. */
    public List<SignatureOperation> signaturesSuivantes() {
        List<SignatureOperation> attendues = signaturesAttendues();
        if (attendues.isEmpty()) {
            return List.of();
        }
        int ordre = attendues.get(0).ordreEffectif();
        return attendues.stream().filter(s -> s.ordreEffectif() == ordre).toList();
    }

    void ajouterSignature(SignatureRecueillie r) {
        List<SignatureRecueillie> l = new java.util.ArrayList<>(getSignaturesRecueillies());
        l.add(r);
        this.signaturesRecueillies = JsonTexte.ecrire(l);
    }

    /** Signatures remises en attente (opération rouverte). */
    void effacerSignatures() {
        this.signaturesRecueillies = null;
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

    /** Échéance cible (D-106) ; une opération planifiée avant D-106 : son échéance (cible = maximum). */
    public LocalDate getEcheanceCible() {
        return echeanceCible != null ? echeanceCible : echeance;
    }

    public void setEcheanceCible(LocalDate echeanceCible) {
        this.echeanceCible = echeanceCible;
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
