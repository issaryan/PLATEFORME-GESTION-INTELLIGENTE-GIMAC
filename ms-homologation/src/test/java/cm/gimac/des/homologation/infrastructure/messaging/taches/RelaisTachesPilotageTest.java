package cm.gimac.des.homologation.infrastructure.messaging.taches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort.Designation;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort.Origine;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort.Resultat;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort.Tache;
import cm.gimac.des.homologation.domain.JsonTexte;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Relais des demandes de tâches (D-77, §5) : ordre par tâche, réessai croissant, refus définitif, abandon. */
@ExtendWith(MockitoExtension.class)
class RelaisTachesPilotageTest {

    static final Instant T = Instant.parse("2026-10-06T09:00:00Z");
    static final Origine O = new Origine("OPERATION", "LIA/3/R", "homologation", null);

    @Mock
    DemandeTacheRepository demandes;

    @Mock
    PilotageTachesPort pilotage;

    static DemandeTache creation(String responsable) {
        return new DemandeTache(DemandeTache.Nature.CREER, O.reference() + "|" + responsable, JsonTexte.ecrire(new Tache("t", null, null, "NORMALE", responsable, null, O)), T);
    }

    static DemandeTache cloture(String responsable) {
        return new DemandeTache(DemandeTache.Nature.CLOTURER, O.reference() + "|" + responsable, JsonTexte.ecrire(new Designation(O, responsable)), T);
    }

    @Test
    @DisplayName("Envoi dans l'ordre ; une création indisponible retient la clôture de la même tâche, pas celle d'une autre")
    void ordreEtReessai() {
        DemandeTache c1 = creation("a");
        DemandeTache k1 = cloture("a");
        DemandeTache c2 = creation("b");
        DemandeTache r2 = new DemandeTache(DemandeTache.Nature.ROUVRIR, O.reference() + "|b", JsonTexte.ecrire(new Designation(O, "b")), T);
        when(demandes.verrouillerEnAttente(RelaisTachesPilotage.LOT)).thenReturn(List.of(c1, k1, c2, r2));
        when(pilotage.creer(any())).thenAnswer(i -> ((Tache) i.getArgument(0)).responsableId().equals("a") ? Resultat.INDISPONIBLE : Resultat.FAIT);
        when(pilotage.rouvrir(any())).thenReturn(Resultat.REFUSE);
        new RelaisTachesPilotage(demandes, pilotage, Clock.fixed(T, ZoneOffset.UTC)).relayer();
        assertThat(c1.getStatut()).isEqualTo(DemandeTache.Statut.EN_ATTENTE);
        assertThat(c1.getTentatives()).isEqualTo(1);
        assertThat(c1.getProchainEssai()).isAfter(T);
        assertThat(c1.getDerniereErreur()).isEqualTo("MS-PILOTAGE indisponible");
        assertThat(k1.getStatut()).isEqualTo(DemandeTache.Statut.EN_ATTENTE);
        verify(pilotage, never()).cloturer(any());
        assertThat(c2.getStatut()).isEqualTo(DemandeTache.Statut.ENVOYEE);
        assertThat(c2.getTraiteeLe()).isEqualTo(T);
        assertThat(r2.getStatut()).isEqualTo(DemandeTache.Statut.REFUSEE);
        assertThat(c2.getNature()).isEqualTo(DemandeTache.Nature.CREER);
        assertThat(c2.getCorps()).contains("\"responsableId\":\"b\"");
        assertThat(c2.getNumero()).isNull();

        // Au prochain passage, la création repoussée n'est pas encore à réessayer : la clôture reste retenue.
        when(demandes.verrouillerEnAttente(RelaisTachesPilotage.LOT)).thenReturn(List.of(c1, k1));
        new RelaisTachesPilotage(demandes, pilotage, Clock.fixed(T.plusSeconds(1), ZoneOffset.UTC)).relayer();
        assertThat(c1.getTentatives()).isEqualTo(1);
        verify(pilotage, never()).cloturer(any());
    }

    @Test
    @DisplayName("Exception du port : traitée comme une indisponibilité ; abandon (ECHEC) au-delà du nombre maximal")
    void abandon() {
        DemandeTache k = cloture("a");
        when(demandes.verrouillerEnAttente(RelaisTachesPilotage.LOT)).thenReturn(List.of(k));
        when(pilotage.cloturer(any())).thenThrow(new IllegalStateException("jeton"));
        Instant t = T;
        for (int i = 0; i < RelaisTachesPilotage.MAXIMUM; i++) {
            t = k.getProchainEssai();
            new RelaisTachesPilotage(demandes, pilotage, Clock.fixed(t, ZoneOffset.UTC)).relayer();
        }
        assertThat(k.getStatut()).isEqualTo(DemandeTache.Statut.ECHEC);
        assertThat(k.getTentatives()).isEqualTo(RelaisTachesPilotage.MAXIMUM);
        assertThat(k.getTraiteeLe()).isEqualTo(t);
        k.echec("x".repeat(600), t, 100);
        assertThat(k.getDerniereErreur()).hasSize(500);
    }

    @Test
    @DisplayName("D-93 : création refusée (compte inactif) avec remontée : tâche « Réaffecter » au conducteur ; sans remontée : rien")
    void remontee() {
        DemandeTache refusee = creation("parti");
        refusee.escaladerVers("conducteur");
        DemandeTache sans = creation("autre");
        when(demandes.verrouillerEnAttente(RelaisTachesPilotage.LOT)).thenReturn(List.of(refusee, sans));
        when(pilotage.creer(any())).thenReturn(Resultat.REFUSE);
        new RelaisTachesPilotage(demandes, pilotage, Clock.fixed(T, ZoneOffset.UTC)).relayer();
        org.mockito.ArgumentCaptor<DemandeTache> c = org.mockito.ArgumentCaptor.forClass(DemandeTache.class);
        org.mockito.Mockito.verify(demandes).save(c.capture());
        Tache t = JsonTexte.lire(c.getValue().getCorps(), Tache.class);
        assertThat(t.responsableId()).isEqualTo("conducteur");
        assertThat(t.titre()).isEqualTo("Réaffecter : t");
        assertThat(t.origine().reference()).isEqualTo("LIA/3/R/REFUS");
        assertThat(t.description()).contains("parti");
        assertThat(c.getValue().getCle()).isEqualTo("LIA/3/R/REFUS|conducteur");
        DemandeTache soi = creation("conducteur");
        soi.escaladerVers("conducteur");
        new RelaisTachesPilotage(demandes, pilotage, Clock.fixed(T, ZoneOffset.UTC)).remonter(soi, T);
        new RelaisTachesPilotage(demandes, pilotage, Clock.fixed(T, ZoneOffset.UTC)).remonter(cloture("a"), T);
        org.mockito.Mockito.verify(demandes).save(any());
    }
}
