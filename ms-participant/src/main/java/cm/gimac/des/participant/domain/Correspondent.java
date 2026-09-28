package cm.gimac.des.participant.domain;

import cm.gimac.des.participant.domain.enumeration.CorrespondentFunction;
import cm.gimac.des.participant.domain.enumeration.Reachability;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Correspondant d'une institution, par fonction (UC-REF-04). Les mises à jour
 * proposées depuis le portail restent en attente jusqu'à validation par le DES (PAR-04, PAR-11).
 */
@Entity
@Table(name = "correspondent")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Correspondent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 255)
    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    /**
     * Fonction (function du CC, mot réservé).
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "duty", nullable = false)
    private CorrespondentFunction duty;

    /**
     * Canaux déclarés, séparés par des virgules, par exemple messagerie,téléphone.
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "channels", length = 255, nullable = false)
    private String channels;

    @Size(max = 255)
    @Column(name = "email", length = 255)
    private String email;

    @Size(max = 30)
    @Column(name = "phone", length = 30)
    private String phone;

    /**
     * Référence de la clé publique déposée, pour vérifier les pièces signées.
     */
    @Size(max = 255)
    @Column(name = "public_key_ref", length = 255)
    private String publicKeyRef;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reachability", nullable = false)
    private Reachability reachability;

    @Column(name = "last_reachability_check_at")
    private Instant lastReachabilityCheckAt;

    /**
     * Contact d'astreinte 24/7 (RG-70 : au moins deux par participant).
     */
    @NotNull
    @Column(name = "on_call_247", nullable = false)
    private Boolean onCall247;

    /**
     * Compte du portail (identifiant dans MS-IAM), s'il existe.
     */
    @Column(name = "user_id")
    private UUID userId;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "proposed_by")
    private UUID proposedBy;

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution institution;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Correspondent id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return this.fullName;
    }

    public Correspondent fullName(String fullName) {
        this.setFullName(fullName);
        return this;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public CorrespondentFunction getDuty() {
        return this.duty;
    }

    public Correspondent duty(CorrespondentFunction duty) {
        this.setDuty(duty);
        return this;
    }

    public void setDuty(CorrespondentFunction duty) {
        this.duty = duty;
    }

    public String getChannels() {
        return this.channels;
    }

    public Correspondent channels(String channels) {
        this.setChannels(channels);
        return this;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public String getEmail() {
        return this.email;
    }

    public Correspondent email(String email) {
        this.setEmail(email);
        return this;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return this.phone;
    }

    public Correspondent phone(String phone) {
        this.setPhone(phone);
        return this;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPublicKeyRef() {
        return this.publicKeyRef;
    }

    public Correspondent publicKeyRef(String publicKeyRef) {
        this.setPublicKeyRef(publicKeyRef);
        return this;
    }

    public void setPublicKeyRef(String publicKeyRef) {
        this.publicKeyRef = publicKeyRef;
    }

    public Reachability getReachability() {
        return this.reachability;
    }

    public Correspondent reachability(Reachability reachability) {
        this.setReachability(reachability);
        return this;
    }

    public void setReachability(Reachability reachability) {
        this.reachability = reachability;
    }

    public Instant getLastReachabilityCheckAt() {
        return this.lastReachabilityCheckAt;
    }

    public Correspondent lastReachabilityCheckAt(Instant lastReachabilityCheckAt) {
        this.setLastReachabilityCheckAt(lastReachabilityCheckAt);
        return this;
    }

    public void setLastReachabilityCheckAt(Instant lastReachabilityCheckAt) {
        this.lastReachabilityCheckAt = lastReachabilityCheckAt;
    }

    public Boolean getOnCall247() {
        return this.onCall247;
    }

    public Correspondent onCall247(Boolean onCall247) {
        this.setOnCall247(onCall247);
        return this;
    }

    public void setOnCall247(Boolean onCall247) {
        this.onCall247 = onCall247;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public Correspondent userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Correspondent active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public UUID getProposedBy() {
        return this.proposedBy;
    }

    public Correspondent proposedBy(UUID proposedBy) {
        this.setProposedBy(proposedBy);
        return this;
    }

    public void setProposedBy(UUID proposedBy) {
        this.proposedBy = proposedBy;
    }

    public UUID getValidatedBy() {
        return this.validatedBy;
    }

    public Correspondent validatedBy(UUID validatedBy) {
        this.setValidatedBy(validatedBy);
        return this;
    }

    public void setValidatedBy(UUID validatedBy) {
        this.validatedBy = validatedBy;
    }

    public Instant getValidatedAt() {
        return this.validatedAt;
    }

    public Correspondent validatedAt(Instant validatedAt) {
        this.setValidatedAt(validatedAt);
        return this;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Institution getInstitution() {
        return this.institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Correspondent institution(Institution institution) {
        this.setInstitution(institution);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Correspondent)) {
            return false;
        }
        return getId() != null && getId().equals(((Correspondent) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Correspondent{" +
            "id=" + getId() +
            ", fullName='" + getFullName() + "'" +
            ", duty='" + getDuty() + "'" +
            ", channels='" + getChannels() + "'" +
            ", email='" + getEmail() + "'" +
            ", phone='" + getPhone() + "'" +
            ", publicKeyRef='" + getPublicKeyRef() + "'" +
            ", reachability='" + getReachability() + "'" +
            ", lastReachabilityCheckAt='" + getLastReachabilityCheckAt() + "'" +
            ", onCall247='" + getOnCall247() + "'" +
            ", userId='" + getUserId() + "'" +
            ", active='" + getActive() + "'" +
            ", proposedBy='" + getProposedBy() + "'" +
            ", validatedBy='" + getValidatedBy() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            "}";
    }
}
