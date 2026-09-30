package cm.gimac.des.certification.domain.decision;

import cm.gimac.des.certification.domain.enumeration.TypeAttestation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Attestation de sécurité exigée du participant (rangs 21 et 22) : pièce déposée au portail (MS-DOCUMENT,
 * référence et empreinte), vérification motivée par la sécurité des SI, puis archivage (rang 23).
 */
@Entity
@Table(name = "attestation_securite")
public class AttestationSecurite {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approbation_id", nullable = false)
    private ApprobationSecurite approbation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private TypeAttestation type;

    @Column(name = "requise", nullable = false)
    private boolean requise = true;

    @Column(name = "document_ref", length = 120)
    private String documentRef;

    @Column(name = "nom", length = 255)
    private String nom;

    @Column(name = "empreinte", length = 64)
    private String empreinte;

    @Column(name = "depose_le")
    private Instant deposeLe;

    @Column(name = "depose_par", length = 200)
    private String deposePar;

    @Column(name = "verifiee_le")
    private Instant verifieeLe;

    @Column(name = "verifiee_par_id", length = 80)
    private String verifieeParId;

    @Column(name = "verifiee_par_nom", length = 200)
    private String verifieeParNom;

    @Column(name = "archivee_le")
    private Instant archiveeLe;

    @Column(name = "motif_rejet", length = 1000)
    private String motifRejet;

    protected AttestationSecurite() {}

    AttestationSecurite(ApprobationSecurite approbation, TypeAttestation type) {
        this.id = UUID.randomUUID();
        this.approbation = approbation;
        this.type = type;
    }

    public boolean estFournie() {
        return documentRef != null;
    }

    public boolean estVerifiee() {
        return verifieeLe != null;
    }

    /** Dépôt du participant : la pièce remplace un dépôt rejeté ; la vérification repart de zéro. */
    public void deposer(String documentRef, String nom, String empreinte, Instant le, String par) {
        this.documentRef = documentRef;
        this.nom = nom;
        this.empreinte = empreinte;
        this.deposeLe = le;
        this.deposePar = par;
        this.verifieeLe = null;
        this.verifieeParId = null;
        this.verifieeParNom = null;
        this.motifRejet = null;
    }

    /** Vérification conforme par la sécurité des SI. */
    public void verifierConforme(Instant le, String parId, String parNom) {
        this.verifieeLe = le;
        this.verifieeParId = parId;
        this.verifieeParNom = parNom;
        this.motifRejet = null;
    }

    /** Rejet motivé : la pièce est écartée, le participant doit en déposer une nouvelle. */
    public void rejeter(String motif) {
        this.documentRef = null;
        this.nom = null;
        this.empreinte = null;
        this.deposeLe = null;
        this.deposePar = null;
        this.verifieeLe = null;
        this.verifieeParId = null;
        this.verifieeParNom = null;
        this.motifRejet = motif;
    }

    void archiver(Instant le) {
        this.archiveeLe = le;
    }

    public UUID getId() {
        return id;
    }

    public TypeAttestation getType() {
        return type;
    }

    public boolean isRequise() {
        return requise;
    }

    public String getDocumentRef() {
        return documentRef;
    }

    public String getNom() {
        return nom;
    }

    public String getEmpreinte() {
        return empreinte;
    }

    public Instant getDeposeLe() {
        return deposeLe;
    }

    public String getDeposePar() {
        return deposePar;
    }

    public Instant getVerifieeLe() {
        return verifieeLe;
    }

    public String getVerifieeParId() {
        return verifieeParId;
    }

    public String getVerifieeParNom() {
        return verifieeParNom;
    }

    public Instant getArchiveeLe() {
        return archiveeLe;
    }

    public String getMotifRejet() {
        return motifRejet;
    }
}
