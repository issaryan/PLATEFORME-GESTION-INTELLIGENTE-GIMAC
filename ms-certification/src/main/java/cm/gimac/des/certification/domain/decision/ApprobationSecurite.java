package cm.gimac.des.certification.domain.decision;

import cm.gimac.des.certification.domain.enumeration.EtatApprobation;
import cm.gimac.des.certification.domain.enumeration.EtatFicheChangement;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import cm.gimac.des.certification.domain.enumeration.TypeAttestation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Verrou 2 (D-56) : approbation du changement par la sécurité des SI, ouverte quand le procès-verbal est
 * signé des deux parties. Elle réunit le dossier de changement (rang 20), la fiche de changement référencée
 * et lue sur le dispositif existant, jamais refaite (rang 26), les attestations exigées selon l'activité
 * (rangs 21 à 23) et les visas optionnels. Son état est déduit de ces faits (ApprobationPolicy).
 */
@Entity
@Table(name = "approbation_securite")
public class ApprobationSecurite {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "liaison_id", nullable = false, unique = true, updatable = false)
    private UUID liaisonId;

    @Column(name = "institution_id", length = 80)
    private String institutionId;

    @Column(name = "type_projet", length = 40)
    private String typeProjet;

    @Column(name = "fintech", nullable = false)
    private boolean fintech;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", length = 30, nullable = false)
    private EtatApprobation etat = EtatApprobation.EN_ATTENTE_DOSSIER;

    @Column(name = "dossier_document_ref", length = 120)
    private String dossierDocumentRef;

    @Column(name = "dossier_nom", length = 255)
    private String dossierNom;

    @Column(name = "dossier_empreinte", length = 64)
    private String dossierEmpreinte;

    @Column(name = "dossier_depose_le")
    private Instant dossierDeposeLe;

    @Column(name = "dossier_depose_par_id", length = 80)
    private String dossierDeposeParId;

    @Column(name = "dossier_depose_par_nom", length = 200)
    private String dossierDeposeParNom;

    @Column(name = "fiche_reference", length = 40)
    private String ficheReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "fiche_etat", length = 20)
    private EtatFicheChangement ficheEtat;

    @Column(name = "fiche_lue_le")
    private Instant ficheLueLe;

    @Column(name = "archivee_le")
    private Instant archiveeLe;

    @Column(name = "approuvee_le")
    private Instant approuveeLe;

    @OneToMany(mappedBy = "approbation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("type")
    private List<AttestationSecurite> attestations = new ArrayList<>();

    @OneToMany(mappedBy = "approbation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("nature")
    private List<VisaSecurite> visas = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    protected ApprobationSecurite() {}

    /** Ouverture : attestations exigées selon l'activité, visas présents avec leur activation. */
    public ApprobationSecurite(UUID liaisonId, String institutionId, String typeProjet, boolean fintech, Collection<TypeAttestation> exigees, Set<NatureVisa> visasActifs) {
        this.id = UUID.randomUUID();
        this.liaisonId = liaisonId;
        this.institutionId = institutionId;
        this.typeProjet = typeProjet;
        this.fintech = fintech;
        exigees.forEach(t -> attestations.add(new AttestationSecurite(this, t)));
        for (NatureVisa n : NatureVisa.values()) {
            visas.add(new VisaSecurite(this, n, visasActifs.contains(n)));
        }
    }

    public Optional<AttestationSecurite> attestation(TypeAttestation type) {
        return attestations.stream().filter(a -> a.getType() == type).findFirst();
    }

    public Optional<VisaSecurite> visa(NatureVisa nature) {
        return visas.stream().filter(v -> v.getNature() == nature).findFirst();
    }

    public boolean aDossier() {
        return dossierDocumentRef != null;
    }

    public boolean estArchivee() {
        return archiveeLe != null;
    }

    public boolean estApprouvee() {
        return approuveeLe != null;
    }

    public void constituerDossier(String documentRef, String nom, String empreinte, Instant le, String parId, String parNom) {
        this.dossierDocumentRef = documentRef;
        this.dossierNom = nom;
        this.dossierEmpreinte = empreinte;
        this.dossierDeposeLe = le;
        this.dossierDeposeParId = parId;
        this.dossierDeposeParNom = parNom;
    }

    public void referencerFiche(String reference, EtatFicheChangement etat, Instant le) {
        this.ficheReference = reference;
        this.ficheEtat = etat;
        this.ficheLueLe = le;
    }

    public void lireFiche(EtatFicheChangement etat, Instant le) {
        this.ficheEtat = etat;
        this.ficheLueLe = le;
    }

    public void archiver(Instant le) {
        this.archiveeLe = le;
        attestations.forEach(a -> a.archiver(le));
    }

    public void approuver(Instant le) {
        this.approuveeLe = le;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public String getTypeProjet() {
        return typeProjet;
    }

    public boolean isFintech() {
        return fintech;
    }

    public EtatApprobation getEtat() {
        return etat;
    }

    public void setEtat(EtatApprobation etat) {
        this.etat = etat;
    }

    public String getDossierDocumentRef() {
        return dossierDocumentRef;
    }

    public String getDossierNom() {
        return dossierNom;
    }

    public String getDossierEmpreinte() {
        return dossierEmpreinte;
    }

    public Instant getDossierDeposeLe() {
        return dossierDeposeLe;
    }

    public String getDossierDeposeParId() {
        return dossierDeposeParId;
    }

    public String getDossierDeposeParNom() {
        return dossierDeposeParNom;
    }

    public String getFicheReference() {
        return ficheReference;
    }

    public EtatFicheChangement getFicheEtat() {
        return ficheEtat;
    }

    public Instant getFicheLueLe() {
        return ficheLueLe;
    }

    public Instant getArchiveeLe() {
        return archiveeLe;
    }

    public Instant getApprouveeLe() {
        return approuveeLe;
    }

    public List<AttestationSecurite> getAttestations() {
        return List.copyOf(attestations);
    }

    public List<VisaSecurite> getVisas() {
        return List.copyOf(visas);
    }
}
