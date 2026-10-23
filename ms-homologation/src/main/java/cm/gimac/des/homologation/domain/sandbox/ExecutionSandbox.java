package cm.gimac.des.homologation.domain.sandbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Exécution de la sandbox reprise (US-EXI-14) : pour chaque exécution, l'enregistrement de plus haute position lu
 * (création PENDING, puis validation, puis rectification), avec ses treize éléments, sa nature, la version de la table et
 * son origine ; {@code operationCode} est l'opération du référentiel de la liaison qui porte son scénario, nul pour un
 * scénario non rapproché. Une ligne par (campagne, exécution) : la relève est idempotente.
 */
@Entity
@Table(name = "sbx_execution")
public class ExecutionSandbox {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "campagne_id", length = 80, nullable = false)
    private String campagneId;

    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    @Column(name = "identifiant_execution", length = 80, nullable = false)
    private String identifiantExecution;

    @Column(name = "identifiant_cas", length = 80)
    private String identifiantCas;

    @Column(name = "identifiant_scenario", length = 80)
    private String identifiantScenario;

    @Column(name = "version_scenario", length = 40)
    private String versionScenario;

    @Column(name = "date_execution")
    private Instant dateExecution;

    @Column(name = "verdict_natif", length = 10)
    private String verdictNatif;

    @Column(name = "verdict_normalise", length = 20)
    private String verdictNormalise;

    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "mode_execution", length = 20)
    private String modeExecution;

    @Column(name = "reference_trace", length = 120)
    private String referenceTrace;

    @Column(name = "position", length = 300)
    private String position;

    @Column(name = "nature", length = 15)
    private String nature;

    @Column(name = "version_table", length = 20)
    private String versionTable;

    @Column(name = "origine", length = 12)
    private String origine;

    @Column(name = "operation_code", length = 40)
    private String operationCode;

    @Column(name = "releve_le", nullable = false)
    private Instant releveLe;

    protected ExecutionSandbox() {}

    public ExecutionSandbox(String campagneId, UUID liaisonId, String identifiantExecution) {
        this.id = UUID.randomUUID();
        this.campagneId = Objects.requireNonNull(campagneId, "campagneId");
        this.liaisonId = Objects.requireNonNull(liaisonId, "liaisonId");
        this.identifiantExecution = Objects.requireNonNull(identifiantExecution, "identifiantExecution");
    }

    /** Enregistrement plus récent (lu après le précédent, donc de position plus haute) : il remplace le précédent. */
    @SuppressWarnings("java:S107")
    public void retenir(
        String cas,
        String scenario,
        String version,
        Instant date,
        String natif,
        String normalise,
        String motifVerdict,
        String mode,
        String trace,
        String pos,
        String natureEnregistrement,
        String table,
        String origineExecution,
        String operation,
        Instant t
    ) {
        this.identifiantCas = cas;
        this.identifiantScenario = scenario;
        this.versionScenario = version;
        this.dateExecution = date;
        this.verdictNatif = natif;
        this.verdictNormalise = normalise;
        this.motif = motifVerdict;
        this.modeExecution = mode;
        this.referenceTrace = trace;
        this.position = pos;
        this.nature = natureEnregistrement;
        this.versionTable = table;
        this.origine = origineExecution;
        this.operationCode = operation;
        this.releveLe = t;
    }

    public UUID getId() {
        return id;
    }

    public String getCampagneId() {
        return campagneId;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getIdentifiantExecution() {
        return identifiantExecution;
    }

    public String getIdentifiantCas() {
        return identifiantCas;
    }

    public String getIdentifiantScenario() {
        return identifiantScenario;
    }

    public String getVersionScenario() {
        return versionScenario;
    }

    public Instant getDateExecution() {
        return dateExecution;
    }

    public String getVerdictNatif() {
        return verdictNatif;
    }

    public String getVerdictNormalise() {
        return verdictNormalise;
    }

    public String getMotif() {
        return motif;
    }

    public String getModeExecution() {
        return modeExecution;
    }

    public String getReferenceTrace() {
        return referenceTrace;
    }

    public String getPosition() {
        return position;
    }

    public String getNature() {
        return nature;
    }

    public String getVersionTable() {
        return versionTable;
    }

    public String getOrigine() {
        return origine;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public Instant getReleveLe() {
        return releveLe;
    }
}
