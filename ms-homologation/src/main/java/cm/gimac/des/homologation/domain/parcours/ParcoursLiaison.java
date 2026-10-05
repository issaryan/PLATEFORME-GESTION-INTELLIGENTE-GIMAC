package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.JsonTexte;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * État du parcours d'une liaison (étapes 3 à 10) : référence lisible, statut et étape courante du CR
 * (dénormalisés pour les filtres), version publiée du référentiel suivie, ressources affectées (geste
 * AFFECTER_RESSOURCES), fiche de suivi, coordonnées
 * d'essai, constat de la couverture et projection de l'état des verrous tenus par MS-CERTIFICATION.
 */
@Entity
@Table(name = "parcours_liaison")
public class ParcoursLiaison {

    @Id
    @Column(name = "liaison_id")
    private UUID liaisonId;

    @Column(name = "reference", nullable = false)
    private String reference;

    /** Version du CR (« CR-1.4 ») des parcours instanciés avant D-77 ; vide ensuite. */
    @Column(name = "version_cr")
    private String versionCr;

    /** Version publiée du référentiel dont le parcours est instancié (D-77) et son numéro. */
    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "version_numero")
    private Integer versionNumero;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutParcours statut = StatutParcours.CADREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "etape_courante", nullable = false)
    private EtapeCode etapeCourante = EtapeCode.CADRAGE;

    @Column(name = "bloquee", nullable = false)
    private boolean bloquee;

    @Column(name = "date_demarrage", nullable = false)
    private LocalDate dateDemarrage;

    @Column(name = "liaison_origine_id")
    private UUID liaisonOrigineId;

    @Column(name = "fintech", nullable = false)
    private boolean fintech;

    @Column(name = "conducteur_a_designer", nullable = false)
    private boolean conducteurADesigner;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ressources", columnDefinition = "jsonb")
    private String ressources;

    @Enumerated(EnumType.STRING)
    @Column(name = "fiche_etat", nullable = false)
    private EtatFicheSuivi ficheEtat = EtatFicheSuivi.A_REMETTRE;

    @Column(name = "fiche_renseignee_le")
    private LocalDate ficheRenseigneeLe;

    @Column(name = "fiche_constatee_le")
    private LocalDate ficheConstateeLe;

    @Column(name = "points_terminaison")
    private String pointsTerminaison;

    @Column(name = "couverture_constatee", nullable = false)
    private boolean couvertureConstatee;

    @Column(name = "couverture_constatee_le")
    private LocalDate couvertureConstateeLe;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_pv", nullable = false)
    private EtatProcesVerbal etatPv = EtatProcesVerbal.NON_ETABLI;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_approbation", nullable = false)
    private EtatApprobation etatApprobation = EtatApprobation.EN_ATTENTE_DOSSIER;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attestations_deposees", columnDefinition = "jsonb")
    private String attestationsDeposees;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visas_attendus", columnDefinition = "jsonb")
    private String visasAttendus;

    @Column(name = "fiche_changement_reference")
    private String ficheChangementReference;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ParcoursLiaison() {}

    public ParcoursLiaison(UUID liaisonId, String reference, LocalDate dateDemarrage) {
        this.liaisonId = liaisonId;
        this.reference = reference;
        this.dateDemarrage = dateDemarrage;
    }

    public UUID getVersionId() {
        return versionId;
    }

    public Integer getVersionNumero() {
        return versionNumero;
    }

    /** Rattachement à une version publiée (instanciation, migration). */
    public void rattacher(UUID versionId, Integer numero) {
        this.versionId = versionId;
        this.versionNumero = numero;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getReference() {
        return reference;
    }

    public String getVersionCr() {
        return versionCr;
    }

    public StatutParcours getStatut() {
        return statut;
    }

    public void setStatut(StatutParcours statut) {
        this.statut = statut;
    }

    public EtapeCode getEtapeCourante() {
        return etapeCourante;
    }

    public void setEtapeCourante(EtapeCode etapeCourante) {
        this.etapeCourante = etapeCourante;
    }

    public boolean isBloquee() {
        return bloquee;
    }

    public void setBloquee(boolean bloquee) {
        this.bloquee = bloquee;
    }

    public LocalDate getDateDemarrage() {
        return dateDemarrage;
    }

    public UUID getLiaisonOrigineId() {
        return liaisonOrigineId;
    }

    public ParcoursLiaison liaisonOrigineId(UUID id) {
        this.liaisonOrigineId = id;
        return this;
    }

    public boolean isFintech() {
        return fintech;
    }

    public ParcoursLiaison fintech(boolean f) {
        this.fintech = f;
        return this;
    }

    public boolean isConducteurADesigner() {
        return conducteurADesigner;
    }

    public void setConducteurADesigner(boolean conducteurADesigner) {
        this.conducteurADesigner = conducteurADesigner;
    }

    /** Ressources affectées au rang 6, par fonction. */
    public Map<Fonction, Personne> getRessources() {
        Map<Fonction, Personne> m = new LinkedHashMap<>();
        JsonTexte.liste(ressources, Personne.class).forEach(p -> m.put(p.fonction(), p));
        return m;
    }

    public void setRessources(Map<Fonction, Personne> r) {
        this.ressources = JsonTexte.ecrire(new ArrayList<>(r.values()));
    }

    public EtatFicheSuivi getFicheEtat() {
        return ficheEtat;
    }

    public void setFicheEtat(EtatFicheSuivi ficheEtat) {
        this.ficheEtat = ficheEtat;
    }

    public LocalDate getFicheRenseigneeLe() {
        return ficheRenseigneeLe;
    }

    public void setFicheRenseigneeLe(LocalDate ficheRenseigneeLe) {
        this.ficheRenseigneeLe = ficheRenseigneeLe;
    }

    public LocalDate getFicheConstateeLe() {
        return ficheConstateeLe;
    }

    public void setFicheConstateeLe(LocalDate ficheConstateeLe) {
        this.ficheConstateeLe = ficheConstateeLe;
    }

    public String getPointsTerminaison() {
        return pointsTerminaison;
    }

    public void setPointsTerminaison(String pointsTerminaison) {
        this.pointsTerminaison = pointsTerminaison;
    }

    public boolean isCouvertureConstatee() {
        return couvertureConstatee;
    }

    public LocalDate getCouvertureConstateeLe() {
        return couvertureConstateeLe;
    }

    public void constaterCouverture(LocalDate le) {
        this.couvertureConstatee = true;
        this.couvertureConstateeLe = le;
    }

    public EtatProcesVerbal getEtatPv() {
        return etatPv;
    }

    public void setEtatPv(EtatProcesVerbal etatPv) {
        this.etatPv = etatPv;
    }

    public EtatApprobation getEtatApprobation() {
        return etatApprobation;
    }

    public void setEtatApprobation(EtatApprobation etatApprobation) {
        this.etatApprobation = etatApprobation;
    }

    /** Attestations déposées par le participant et non encore vérifiées (projection). */
    public List<TypeAttestation> getAttestationsDeposees() {
        return JsonTexte.liste(attestationsDeposees, TypeAttestation.class);
    }

    public void setAttestationsDeposees(List<TypeAttestation> l) {
        this.attestationsDeposees = JsonTexte.ecrire(l);
    }

    /** Visas optionnels activés et non encore signés (projection) : CONFORMITE, DIRECTION_GENERALE. */
    public List<String> getVisasAttendus() {
        return JsonTexte.liste(visasAttendus, String.class);
    }

    public void setVisasAttendus(List<String> l) {
        this.visasAttendus = JsonTexte.ecrire(l);
    }

    public String getFicheChangementReference() {
        return ficheChangementReference;
    }

    public void setFicheChangementReference(String ficheChangementReference) {
        this.ficheChangementReference = ficheChangementReference;
    }
}
