package cm.gimac.des.homologation.domain.sandbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Pièce d'une campagne reprise (US-EXI-14, INT-08) : nature, version, empreinte SHA-256 et taille annoncées, document
 * de MS-DOCUMENT qui la conserve. Une pièce dont l'empreinte annoncée par la liste, celle du HEAD et celle des octets
 * téléchargés ne concordent pas n'est jamais déposée ; une pièce déjà conservée (même empreinte) n'est pas redéposée.
 */
@Entity
@Table(name = "sbx_piece")
public class PieceSandbox {

    public enum Statut {
        DEPOSEE,
        DEJA_CONSERVEE,
        EMPREINTE_DIVERGENTE,
    }

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "campagne_id", length = 80, nullable = false)
    private String campagneId;

    @Column(name = "nature", length = 30, nullable = false)
    private String nature;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "empreinte", length = 64, nullable = false)
    private String empreinte;

    @Column(name = "taille", nullable = false)
    private long taille;

    @Column(name = "nom", length = 200)
    private String nom;

    @Column(name = "document_id")
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 25, nullable = false)
    private Statut statut;

    @Column(name = "releve_le", nullable = false)
    private Instant releveLe;

    protected PieceSandbox() {}

    @SuppressWarnings("java:S107")
    public PieceSandbox(String campagneId, String nature, int version, String empreinte, long taille, String nom, UUID documentId, Statut statut, Instant t) {
        this.id = UUID.randomUUID();
        this.campagneId = Objects.requireNonNull(campagneId, "campagneId");
        this.nature = Objects.requireNonNull(nature, "nature");
        this.version = version;
        this.empreinte = Objects.requireNonNull(empreinte, "empreinte");
        this.taille = taille;
        this.nom = nom;
        this.documentId = documentId;
        this.statut = Objects.requireNonNull(statut, "statut");
        this.releveLe = t;
    }

    public UUID getId() {
        return id;
    }

    public String getCampagneId() {
        return campagneId;
    }

    public String getNature() {
        return nature;
    }

    public int getVersion() {
        return version;
    }

    public String getEmpreinte() {
        return empreinte;
    }

    public long getTaille() {
        return taille;
    }

    public String getNom() {
        return nom;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public Statut getStatut() {
        return statut;
    }

    public Instant getReleveLe() {
        return releveLe;
    }
}
