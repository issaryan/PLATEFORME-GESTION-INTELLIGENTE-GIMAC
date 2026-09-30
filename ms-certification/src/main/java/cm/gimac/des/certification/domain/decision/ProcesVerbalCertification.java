package cm.gimac.des.certification.domain.decision;

import cm.gimac.des.certification.domain.enumeration.EtatProcesVerbal;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Verrou 1 (D-56) : procès-verbal de certification d'une liaison. Créé à la réception du constat de
 * couverture (homologation.coverage-established.v1) ou à l'établissement ; la couverture et l'identité de
 * la liaison sont figées à l'établissement (rang 18). Les signatures sont des lignes immuables de
 * signature_verrou (rang 19).
 */
@Entity
@Table(name = "proces_verbal_certification")
public class ProcesVerbalCertification {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "liaison_id", nullable = false, unique = true, updatable = false)
    private UUID liaisonId;

    @Column(name = "reference", length = 60)
    private String reference;

    @Column(name = "institution_id", length = 80)
    private String institutionId;

    @Column(name = "institution_nom", length = 200)
    private String institutionNom;

    @Column(name = "type_projet", length = 30)
    private String typeProjet;

    @Column(name = "fintech", nullable = false)
    private boolean fintech;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", length = 20, nullable = false)
    private EtatProcesVerbal etat = EtatProcesVerbal.NON_ETABLI;

    @Column(name = "couverture_taux")
    private Integer couvertureTaux;

    @Column(name = "couverture_numerateur", nullable = false)
    private int couvertureNumerateur;

    @Column(name = "couverture_denominateur", nullable = false)
    private int couvertureDenominateur;

    @Column(name = "couverture_constatee", nullable = false)
    private boolean couvertureConstatee;

    @Convert(converter = ListeCodesConverter.class)
    @Column(name = "services_couverts", length = 2000)
    private List<String> servicesCouverts = new ArrayList<>();

    @Column(name = "etabli_par_id", length = 80)
    private String etabliParId;

    @Column(name = "etabli_par_nom", length = 200)
    private String etabliParNom;

    @Column(name = "etabli_le")
    private Instant etabliLe;

    @Column(name = "empreinte", length = 64)
    private String empreinte;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    protected ProcesVerbalCertification() {}

    public ProcesVerbalCertification(UUID liaisonId) {
        this.id = UUID.randomUUID();
        this.liaisonId = liaisonId;
    }

    /** Constat de la couverture reçu de MS-HOMOLOGATION (rang 17) : le procès-verbal devient établissable. */
    public void constaterCouverture(Integer taux, List<String> services) {
        this.couvertureConstatee = true;
        this.couvertureTaux = taux;
        this.servicesCouverts = new ArrayList<>(services);
    }

    /** Établissement (rang 18) : identité de la liaison et couverture figées, empreinte du contenu. */
    public void etablir(Identite identite, Couverture couverture, List<String> services, String parId, String parNom, Instant le, String empreinte) {
        this.reference = identite.reference();
        this.institutionId = identite.institutionId();
        this.institutionNom = identite.institutionNom();
        this.typeProjet = identite.typeProjet();
        this.fintech = identite.fintech();
        this.couvertureTaux = couverture.taux();
        this.couvertureNumerateur = couverture.numerateur();
        this.couvertureDenominateur = couverture.denominateur();
        this.couvertureConstatee = true;
        this.servicesCouverts = new ArrayList<>(services);
        this.etabliParId = parId;
        this.etabliParNom = parNom;
        this.etabliLe = le;
        this.empreinte = empreinte;
        this.etat = EtatProcesVerbal.ETABLI;
    }

    /** Identité de la liaison figée sur le procès-verbal. */
    public record Identite(String reference, String institutionId, String institutionNom, String typeProjet, boolean fintech) {}

    /** Couverture de certification du périmètre, calculée par MS-HOMOLOGATION (HOM-17). */
    public record Couverture(Integer taux, int numerateur, int denominateur) {}

    public boolean estEtabli() {
        return etat != EtatProcesVerbal.NON_ETABLI;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getReference() {
        return reference;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public String getInstitutionNom() {
        return institutionNom;
    }

    public String getTypeProjet() {
        return typeProjet;
    }

    public boolean isFintech() {
        return fintech;
    }

    public EtatProcesVerbal getEtat() {
        return etat;
    }

    public void setEtat(EtatProcesVerbal etat) {
        this.etat = etat;
    }

    public Integer getCouvertureTaux() {
        return couvertureTaux;
    }

    public int getCouvertureNumerateur() {
        return couvertureNumerateur;
    }

    public int getCouvertureDenominateur() {
        return couvertureDenominateur;
    }

    public boolean isCouvertureConstatee() {
        return couvertureConstatee;
    }

    public List<String> getServicesCouverts() {
        return List.copyOf(servicesCouverts);
    }

    public String getEtabliParId() {
        return etabliParId;
    }

    public String getEtabliParNom() {
        return etabliParNom;
    }

    public Instant getEtabliLe() {
        return etabliLe;
    }

    public String getEmpreinte() {
        return empreinte;
    }
}
