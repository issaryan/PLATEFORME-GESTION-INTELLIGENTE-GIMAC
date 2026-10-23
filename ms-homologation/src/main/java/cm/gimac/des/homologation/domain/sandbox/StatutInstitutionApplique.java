package cm.gimac.des.homologation.domain.sandbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Dernier statut d'une institution appliqué aux liaisons (HOM-06, D-122) : participant.status-changed.v1 de
 * MS-PARTICIPANT gèle les liaisons actives d'une institution suspendue ou retirée et les rétablit à son retour. Les
 * événements sont ordonnés par la version de statut du registre (D-120, US-EXI-20) ; à défaut (événement antérieur à
 * D-120), par leur date d'effet. Un événement qui n'est pas plus récent que le dernier appliqué est sans effet.
 */
@Entity
@Table(name = "institution_statut_applique")
public class StatutInstitutionApplique {

    /** Statuts du registre qui gèlent les liaisons de l'institution. */
    public static final Set<String> GELANTS = Set.of("SUSPENDU", "RETIRE");

    @Id
    @Column(name = "institution_id")
    private UUID institutionId;

    @Column(name = "statut", length = 30, nullable = false)
    private String statut;

    @Column(name = "version_statut")
    private Long versionStatut;

    @Column(name = "date_effet")
    private Instant dateEffet;

    @Column(name = "applique_le", nullable = false)
    private Instant appliqueLe;

    protected StatutInstitutionApplique() {}

    public StatutInstitutionApplique(UUID institutionId, String statut, Long versionStatut, Instant dateEffet, Instant appliqueLe) {
        this.institutionId = Objects.requireNonNull(institutionId, "institutionId");
        this.statut = Objects.requireNonNull(statut, "statut");
        this.versionStatut = versionStatut;
        this.dateEffet = dateEffet;
        this.appliqueLe = appliqueLe;
    }

    public static boolean gelant(String statut) {
        return GELANTS.contains(statut);
    }

    /**
     * Vrai si l'événement (version, date d'effet) est plus récent que le dernier appliqué : versions comparées quand
     * les deux en portent une, sinon dates d'effet ; sans l'une ni l'autre, l'événement le plus récemment reçu prévaut.
     */
    public boolean depasse(Long version, Instant date) {
        if (version != null && versionStatut != null) {
            return version > versionStatut;
        }
        if (date != null && dateEffet != null) {
            return date.isAfter(dateEffet);
        }
        return true;
    }

    /** Retient l'événement appliqué ; une version déjà connue n'est jamais effacée par un événement sans version. */
    public void retenir(String nouveau, Long version, Instant date, Instant t) {
        statut = Objects.requireNonNull(nouveau, "statut");
        if (version != null) {
            versionStatut = version;
        }
        if (date != null) {
            dateEffet = date;
        }
        appliqueLe = t;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public String getStatut() {
        return statut;
    }

    public Long getVersionStatut() {
        return versionStatut;
    }

    public Instant getDateEffet() {
        return dateEffet;
    }

    public Instant getAppliqueLe() {
        return appliqueLe;
    }
}
