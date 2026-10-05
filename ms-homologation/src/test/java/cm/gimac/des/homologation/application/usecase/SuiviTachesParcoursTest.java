package cm.gimac.des.homologation.application.usecase;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Preuve;
import cm.gimac.des.homologation.domain.parcours.Qualite;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Tâches nées du parcours (D-77, §5), calculées avant la validation de la transaction. */
class SuiviTachesParcoursTest {

    static final LocalDate J = LocalDate.parse("2026-10-06");
    static final Preuve PIECE = Preuve.piece("DOC", "p.pdf", "a".repeat(64), J, "Test");
    static final String DDRM = UUID.randomUUID().toString();
    static final String SNA = ParcoursSamples.SNA.toString();

    DemandesTaches demandes;
    SuiviTachesParcours suivi;

    @BeforeEach
    void init() {
        demandes = mock(DemandesTaches.class);
        suivi = new SuiviTachesParcours(demandes);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void fin() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    static void valider(boolean readOnly) {
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.beforeCommit(readOnly);
        }
    }

    List<PilotageTachesPort.Tache> creees() {
        ArgumentCaptor<PilotageTachesPort.Tache> c = ArgumentCaptor.forClass(PilotageTachesPort.Tache.class);
        verify(demandes, org.mockito.Mockito.atLeast(0)).creer(c.capture());
        return c.getAllValues();
    }

    List<PilotageTachesPort.Designation> closes() {
        ArgumentCaptor<PilotageTachesPort.Designation> c = ArgumentCaptor.forClass(PilotageTachesPort.Designation.class);
        verify(demandes, org.mockito.Mockito.atLeast(0)).cloturer(c.capture());
        return c.getAllValues();
    }

    @Test
    @DisplayName("Liaison ouverte : tâche du conducteur (affecter les intervenants) ; R désigné de l'opération éligible ; R non désigné : aucune")
    void ouverture() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        // Le rang 3 est tenu par un titulaire sans compte (« U-DDRM ») : aucune tâche ; le conducteur en reçoit une.
        suivi.observerNouveau(p);
        suivi.observerNouveau(p);
        valider(false);
        List<PilotageTachesPort.Tache> t = creees();
        assertThat(t).singleElement().satisfies(x -> {
            assertThat(x.titre()).isEqualTo("Affecter les intervenants de la liaison LIA-2026-001");
            assertThat(x.responsableId()).isEqualTo(ParcoursSamples.CONDUCTEUR.toString());
            assertThat(x.origine().type()).isEqualTo("OPERATION");
            assertThat(x.origine().reference()).isEqualTo("LIA-2026-001/AFFECTATION");
            assertThat(x.origine().lien()).isEqualTo("/des/parcours/" + p.id());
            assertThat(x.echeance()).isEqualTo(p.operation("6").orElseThrow().getEcheance().toString());
        });
    }

    @Test
    @DisplayName("Éligible : tâche à R ; validation demandée : R terminée, tâche à V ; faite : V terminée ; transaction en lecture : rien")
    void cycleDUneOperation() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "9", J);
        OperationParcours o10 = p.operation("10").orElseThrow();
        assertThat(o10.getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
        // Une photographie en lecture seule n'écrit rien.
        suivi.observer(p);
        valider(true);
        assertThat(creees()).isEmpty();
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();

        // L'opération 10 est tenue par l'ingénieur SNA (compte du GIMAC) : sa tâche naît à la photographie suivante.
        p.attribuer(o10, Qualite.R, new Personne(UUID.randomUUID().toString(), "Autre", Fonction.SNA));
        suivi.observer(p);
        p.attribuer(o10, Qualite.R, new Personne(SNA, "Ingénieur SNA", Fonction.SNA));
        p.deposerPreuve(o10, PIECE, null, null, null);
        valider(false);
        // Avant : R « Autre » actif ; après : EN_VALIDATION, R n'est plus actif, V (conducteur) l'est.
        assertThat(closes()).singleElement().satisfies(d -> assertThat(d.origine().reference()).isEqualTo("LIA-2026-001/10/R"));
        assertThat(creees()).singleElement().satisfies(x -> {
            assertThat(x.titre()).startsWith("Valider : Atelier interfaces de programmation (LIA-2026-001)");
            assertThat(x.responsableId()).isEqualTo(ParcoursSamples.CONDUCTEUR.toString());
            assertThat(x.origine().reference()).isEqualTo("LIA-2026-001/10/V");
            assertThat(x.origine().lien()).contains("onglet=operations");
            assertThat(x.description()).contains("Validation demandée");
        });
    }

    @Test
    @DisplayName("Changement de R sur une opération éligible : tâche de l'ancien terminée, tâche du nouveau ; opération reprise : rouverte")
    void changementEtReprise() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        OperationParcours o3 = p.operation("3").orElseThrow();
        p.attribuer(o3, Qualite.R, new Personne(DDRM, "Chargée DDRM", Fonction.DDRM));
        suivi.observer(p);
        String autre = UUID.randomUUID().toString();
        p.attribuer(o3, Qualite.R, new Personne(autre, "Autre DDRM", Fonction.DDRM));
        valider(false);
        assertThat(closes()).extracting(PilotageTachesPort.Designation::responsableId).containsExactly(DDRM);
        assertThat(creees()).singleElement().satisfies(x -> {
            assertThat(x.responsableId()).isEqualTo(autre);
            assertThat(x.titre()).isEqualTo("Arrêt du périmètre des services (LIA-2026-001)");
            assertThat(x.description()).contains("étape Cadrage").contains("responsable (R)");
        });
        verify(demandes, never()).rouvrir(any());

        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();
        p.deposerPreuve(o3, PIECE, null, null, null);
        suivi.observer(p);
        // Reprise : l'opération en validation redevient à faire (cas d'une migration ou d'un refus) : la tâche de R est rouverte.
        o3.setEtat(EtatOperationParcours.A_FAIRE);
        valider(false);
        verify(demandes).rouvrir(new PilotageTachesPort.Designation(new PilotageTachesPort.Origine("OPERATION", "LIA-2026-001/3/R", "homologation", cm.gimac.des.homologation.domain.service.ParcoursProjection.lienOperation(p, o3)), autre));
    }

    @Test
    @DisplayName("Opération retirée par une migration : ses tâches actives (R, V) sont terminées ; incident d'écriture consigné sans échec")
    void retraitEtIncident() throws Exception {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        OperationParcours o3 = p.operation("3").orElseThrow();
        p.attribuer(o3, Qualite.R, new Personne(DDRM, "Chargée DDRM", Fonction.DDRM));
        SuiviTachesParcours.Photo photo = SuiviTachesParcours.photo(p);
        List<OperationParcours> restantes = new ArrayList<>(p.operations());
        restantes.remove(o3);
        Parcours apres = new Parcours(p.liaison(), p.etat(), restantes, p.services(), p.blocages());
        suivi.emettre(apres, photo);
        assertThat(closes()).extracting(d -> d.origine().reference()).contains("LIA-2026-001/3/R");

        // V en validation retiré.
        OperationParcours o4 = p.operation("4").orElseThrow();
        o4.setEtat(EtatOperationParcours.EN_VALIDATION);
        o4.setRaci(o4.getRaci().avecV(new Personne(DDRM, "V", Fonction.CONDUCTEUR)));
        SuiviTachesParcours.Photo photo2 = SuiviTachesParcours.photo(p);
        restantes.remove(o4);
        suivi.emettre(new Parcours(p.liaison(), p.etat(), restantes, p.services(), p.blocages()), photo2);
        assertThat(closes()).extracting(d -> d.origine().reference()).contains("LIA-2026-001/4/V");

        doThrow(new IllegalStateException("base")).when(demandes).creer(any());
        suivi.observerNouveau(parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE"));
        valider(false);
        assertThat(SuiviTachesParcours.interne(null)).isNull();
        assertThat(SuiviTachesParcours.interne(Personne.aAffecter(Fonction.SNA))).isNull();
        assertThat(SuiviTachesParcours.interne(Personne.correspondant("x", "y"))).isNull();
        assertThat(SuiviTachesParcours.interne(new Personne("U-SNA", "x", Fonction.SNA))).isNull();
    }

    @Test
    @DisplayName("Hors transaction : rien n'est photographié ; titre long abrégé à 200 caractères")
    void horsTransactionEtTitre() {
        TransactionSynchronizationManager.clearSynchronization();
        Parcours p = parcours(TypeProjet.DELEGATAIRE, "RETRAIT");
        suivi.observer(p);
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
        TransactionSynchronizationManager.initSynchronization();
        OperationParcours o = p.operation("D1").orElseThrow();
        var t = SuiviTachesParcours.tache(p, o, "R", "x", new PilotageTachesPort.Origine("OPERATION", "r", "homologation", null));
        assertThat(t.titre()).endsWith("(LIA-2026-001)").hasSizeLessThanOrEqualTo(200);
        assertThat(t.echeance()).isEqualTo(o.getEcheance().toString());
    }
}
