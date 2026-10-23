package cm.gimac.des.homologation.domain.sandbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Campagne de la sandbox reprise en lecture seule (US-EXI-14) : en-tête restitué par SBX-LECTURE, taux tels que
 * restitués (JSON, jamais recalculés), version du bilan et de la table, signalement d'une collision, et position de
 * lecture retenue (jeton opaque) d'où la relève suivante repart.
 */
@Entity
@Table(name = "sbx_campagne")
public class CampagneSandbox {

    @Id
    @Column(name = "campagne_id", length = 80)
    private String campagneId;

    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    @Column(name = "code", length = 80)
    private String code;

    @Column(name = "etat", length = 20, nullable = false)
    private String etat;

    @Column(name = "interface_test", length = 40)
    private String interfaceTest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "services", columnDefinition = "jsonb")
    private String services;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "taux", columnDefinition = "jsonb")
    private String taux;

    @Column(name = "version_bilan")
    private Integer versionBilan;

    @Column(name = "version_table", length = 20)
    private String versionTable;

    @Column(name = "signalement", length = 300)
    private String signalement;

    @Column(name = "position", length = 300)
    private String position;

    @Column(name = "releve_le", nullable = false)
    private Instant releveLe;

    protected CampagneSandbox() {}

    public CampagneSandbox(String campagneId, UUID liaisonId) {
        this.campagneId = Objects.requireNonNull(campagneId, "campagneId");
        this.liaisonId = Objects.requireNonNull(liaisonId, "liaisonId");
    }

    /** En-tête restitué ; {@code services} et {@code taux} sont les listes JSON telles que relues. */
    public void reprendre(String code, String etat, String interfaceTest, String services, String taux, Integer versionBilan, String versionTable, Instant t) {
        this.code = code;
        this.etat = etat;
        this.interfaceTest = interfaceTest;
        this.services = services;
        this.taux = taux;
        this.versionBilan = versionBilan;
        this.versionTable = versionTable;
        this.signalement = null;
        this.releveLe = t;
    }

    /** Campagne remplacée par un signalement (collision de code participant au registre). */
    public void signaler(String code, String motif, Instant t) {
        this.code = code;
        this.etat = "SIGNALEE";
        this.signalement = motif;
        this.releveLe = t;
    }

    public void avancer(String nouvellePosition, Instant t) {
        this.position = nouvellePosition;
        this.releveLe = t;
    }

    public String getCampagneId() {
        return campagneId;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getCode() {
        return code;
    }

    public String getEtat() {
        return etat;
    }

    public String getInterfaceTest() {
        return interfaceTest;
    }

    public String getServices() {
        return services;
    }

    public String getTaux() {
        return taux;
    }

    public Integer getVersionBilan() {
        return versionBilan;
    }

    public String getVersionTable() {
        return versionTable;
    }

    public String getSignalement() {
        return signalement;
    }

    public String getPosition() {
        return position;
    }

    public Instant getReleveLe() {
        return releveLe;
    }
}
