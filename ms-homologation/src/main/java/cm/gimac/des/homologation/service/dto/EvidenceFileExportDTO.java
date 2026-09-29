package cm.gimac.des.homologation.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.EvidenceFileExport} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EvidenceFileExportDTO implements Serializable {

    private UUID id;

    @Lob
    private String manifeste;

    @Size(max = 64)
    private String empreinteManifeste;

    @Size(max = 120)
    private String documentRef;

    private Instant dateExport;

    private UUID demandeurId;

    @Size(max = 64)
    private String socleEmpreinte;

    @Size(max = 30)
    private String etat;

    @NotNull
    private LiaisonDTO liaison;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getManifeste() {
        return manifeste;
    }

    public void setManifeste(String manifeste) {
        this.manifeste = manifeste;
    }

    public String getEmpreinteManifeste() {
        return empreinteManifeste;
    }

    public void setEmpreinteManifeste(String empreinteManifeste) {
        this.empreinteManifeste = empreinteManifeste;
    }

    public String getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public Instant getDateExport() {
        return dateExport;
    }

    public void setDateExport(Instant dateExport) {
        this.dateExport = dateExport;
    }

    public UUID getDemandeurId() {
        return demandeurId;
    }

    public void setDemandeurId(UUID demandeurId) {
        this.demandeurId = demandeurId;
    }

    public String getSocleEmpreinte() {
        return socleEmpreinte;
    }

    public void setSocleEmpreinte(String socleEmpreinte) {
        this.socleEmpreinte = socleEmpreinte;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvidenceFileExportDTO)) {
            return false;
        }

        EvidenceFileExportDTO evidenceFileExportDTO = (EvidenceFileExportDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, evidenceFileExportDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EvidenceFileExportDTO{" +
            "id='" + getId() + "'" +
            ", manifeste='" + getManifeste() + "'" +
            ", empreinteManifeste='" + getEmpreinteManifeste() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", dateExport='" + getDateExport() + "'" +
            ", demandeurId='" + getDemandeurId() + "'" +
            ", socleEmpreinte='" + getSocleEmpreinte() + "'" +
            ", etat='" + getEtat() + "'" +
            ", liaison=" + getLiaison() +
            "}";
    }
}
