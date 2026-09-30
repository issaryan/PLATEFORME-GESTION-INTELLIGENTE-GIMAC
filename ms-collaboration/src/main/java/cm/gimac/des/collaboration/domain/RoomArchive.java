package cm.gimac.des.collaboration.domain;

import cm.gimac.des.collaboration.domain.enumeration.ArchiveState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Archive scellée d'un salon (algorithme 6.63) : PDF/A-3 horodaté RFC 3161,
 * déposé en WORM 10 ans dans MS-DOCUMENT (RG-53, D-11). Écart CC corrigé :
 * CrisisArchive devient RoomArchive, D-28 étendant l'archivage aux salons
 * liés à un ticket.
 */
@Entity
@Table(name = "room_archive")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomArchive implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Cellule de crise (MS-INCIDENT), par exemple CRI-2026-009 ; vide pour un salon d'incident.
     */
    @Size(max = 50)
    @Column(name = "crisis_cell_reference", length = 50)
    private String crisisCellReference;

    /**
     * Ticket lié (MS-INCIDENT).
     */
    @Size(max = 50)
    @Column(name = "ticket_reference", length = 50)
    private String ticketReference;

    @NotNull
    @Min(value = 0)
    @Column(name = "message_count", nullable = false)
    private Integer messageCount;

    /**
     * Messages que le terminal d'archivage n'a pu déchiffrer, consignés comme tels.
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "undecryptable_count", nullable = false)
    private Integer undecryptableCount;

    /**
     * Suites de chiffres masquées en 6+4 (RG-52).
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "masked_sequences", nullable = false)
    private Integer maskedSequences;

    /**
     * Tête de chaîne hM des entrées, portée au manifeste (formule 6.78).
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "chain_head", length = 64)
    private String chainHead;

    /**
     * H = SHA256(document).
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "content_digest", length = 64)
    private String contentDigest;

    /**
     * Jeton RFC 3161 encodé en DER.
     */
    @Lob
    @Column(name = "timestamp_token")
    private byte[] timestampToken;

    @Column(name = "timestamp_token_content_type")
    private String timestampTokenContentType;

    /**
     * Archive déposée : identifiant du document dans MS-DOCUMENT.
     */
    @Column(name = "document_ref")
    private UUID documentRef;

    /**
     * Rétention de l'archive, en années ; 10 pour une archive de crise.
     */
    @NotNull
    @Min(value = 1)
    @Max(value = 99)
    @Column(name = "retention_years", nullable = false)
    private Integer retentionYears;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private ArchiveState state;

    @NotNull
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "sealed_at")
    private Instant sealedAt;

    /**
     * Une archive au plus par salon (idempotence de l'algorithme 6.63).
     */
    @JsonIgnoreProperties(value = { "roomArchive" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private Room room;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public RoomArchive id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCrisisCellReference() {
        return this.crisisCellReference;
    }

    public RoomArchive crisisCellReference(String crisisCellReference) {
        this.setCrisisCellReference(crisisCellReference);
        return this;
    }

    public void setCrisisCellReference(String crisisCellReference) {
        this.crisisCellReference = crisisCellReference;
    }

    public String getTicketReference() {
        return this.ticketReference;
    }

    public RoomArchive ticketReference(String ticketReference) {
        this.setTicketReference(ticketReference);
        return this;
    }

    public void setTicketReference(String ticketReference) {
        this.ticketReference = ticketReference;
    }

    public Integer getMessageCount() {
        return this.messageCount;
    }

    public RoomArchive messageCount(Integer messageCount) {
        this.setMessageCount(messageCount);
        return this;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public Integer getUndecryptableCount() {
        return this.undecryptableCount;
    }

    public RoomArchive undecryptableCount(Integer undecryptableCount) {
        this.setUndecryptableCount(undecryptableCount);
        return this;
    }

    public void setUndecryptableCount(Integer undecryptableCount) {
        this.undecryptableCount = undecryptableCount;
    }

    public Integer getMaskedSequences() {
        return this.maskedSequences;
    }

    public RoomArchive maskedSequences(Integer maskedSequences) {
        this.setMaskedSequences(maskedSequences);
        return this;
    }

    public void setMaskedSequences(Integer maskedSequences) {
        this.maskedSequences = maskedSequences;
    }

    public String getChainHead() {
        return this.chainHead;
    }

    public RoomArchive chainHead(String chainHead) {
        this.setChainHead(chainHead);
        return this;
    }

    public void setChainHead(String chainHead) {
        this.chainHead = chainHead;
    }

    public String getContentDigest() {
        return this.contentDigest;
    }

    public RoomArchive contentDigest(String contentDigest) {
        this.setContentDigest(contentDigest);
        return this;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public byte[] getTimestampToken() {
        return this.timestampToken;
    }

    public RoomArchive timestampToken(byte[] timestampToken) {
        this.setTimestampToken(timestampToken);
        return this;
    }

    public void setTimestampToken(byte[] timestampToken) {
        this.timestampToken = timestampToken;
    }

    public String getTimestampTokenContentType() {
        return this.timestampTokenContentType;
    }

    public RoomArchive timestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
        return this;
    }

    public void setTimestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
    }

    public UUID getDocumentRef() {
        return this.documentRef;
    }

    public RoomArchive documentRef(UUID documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public Integer getRetentionYears() {
        return this.retentionYears;
    }

    public RoomArchive retentionYears(Integer retentionYears) {
        this.setRetentionYears(retentionYears);
        return this;
    }

    public void setRetentionYears(Integer retentionYears) {
        this.retentionYears = retentionYears;
    }

    public ArchiveState getState() {
        return this.state;
    }

    public RoomArchive state(ArchiveState state) {
        this.setState(state);
        return this;
    }

    public void setState(ArchiveState state) {
        this.state = state;
    }

    public Instant getStartedAt() {
        return this.startedAt;
    }

    public RoomArchive startedAt(Instant startedAt) {
        this.setStartedAt(startedAt);
        return this;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getSealedAt() {
        return this.sealedAt;
    }

    public RoomArchive sealedAt(Instant sealedAt) {
        this.setSealedAt(sealedAt);
        return this;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public Room getRoom() {
        return this.room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public RoomArchive room(Room room) {
        this.setRoom(room);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomArchive)) {
            return false;
        }
        return getId() != null && getId().equals(((RoomArchive) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomArchive{" +
            "id=" + getId() +
            ", crisisCellReference='" + getCrisisCellReference() + "'" +
            ", ticketReference='" + getTicketReference() + "'" +
            ", messageCount=" + getMessageCount() +
            ", undecryptableCount=" + getUndecryptableCount() +
            ", maskedSequences=" + getMaskedSequences() +
            ", chainHead='" + getChainHead() + "'" +
            ", contentDigest='" + getContentDigest() + "'" +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", timestampTokenContentType='" + getTimestampTokenContentType() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", retentionYears=" + getRetentionYears() +
            ", state='" + getState() + "'" +
            ", startedAt='" + getStartedAt() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            "}";
    }
}
