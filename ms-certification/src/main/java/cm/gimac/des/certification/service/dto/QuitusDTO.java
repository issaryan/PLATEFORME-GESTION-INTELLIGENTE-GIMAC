package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.EtatQuitus;
import cm.gimac.des.certification.domain.enumeration.NatureQuitus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.Quitus} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class QuitusDTO implements Serializable {

    private UUID id;

    @NotNull
    private NatureQuitus nature;

    @NotNull
    private EtatQuitus etat;

    private Instant dateAttendu;

    @Size(max = 500)
    private String motifCaducite;

    @NotNull
    private CertificationCircuitDTO circuit;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public NatureQuitus getNature() {
        return nature;
    }

    public void setNature(NatureQuitus nature) {
        this.nature = nature;
    }

    public EtatQuitus getEtat() {
        return etat;
    }

    public void setEtat(EtatQuitus etat) {
        this.etat = etat;
    }

    public Instant getDateAttendu() {
        return dateAttendu;
    }

    public void setDateAttendu(Instant dateAttendu) {
        this.dateAttendu = dateAttendu;
    }

    public String getMotifCaducite() {
        return motifCaducite;
    }

    public void setMotifCaducite(String motifCaducite) {
        this.motifCaducite = motifCaducite;
    }

    public CertificationCircuitDTO getCircuit() {
        return circuit;
    }

    public void setCircuit(CertificationCircuitDTO circuit) {
        this.circuit = circuit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QuitusDTO)) {
            return false;
        }

        QuitusDTO quitusDTO = (QuitusDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, quitusDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "QuitusDTO{" +
            "id='" + getId() + "'" +
            ", nature='" + getNature() + "'" +
            ", etat='" + getEtat() + "'" +
            ", dateAttendu='" + getDateAttendu() + "'" +
            ", motifCaducite='" + getMotifCaducite() + "'" +
            ", circuit=" + getCircuit() +
            "}";
    }
}
