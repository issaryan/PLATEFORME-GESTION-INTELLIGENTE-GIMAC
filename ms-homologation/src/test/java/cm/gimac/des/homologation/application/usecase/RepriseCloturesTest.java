package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.in.RepriseCloturesUseCase.ClotureReprise;
import cm.gimac.des.homologation.application.port.in.RepriseCloturesUseCase.Rapport;
import cm.gimac.des.homologation.application.port.in.RepriseCloturesUseCase.Resultat;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.Acteur;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.event.LiaisonClosedWithoutFollowUpPayload;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursLiaison;
import cm.gimac.des.homologation.domain.parcours.Preuve;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Clôture sans suite des liaisons reprises (D-137) : bornes, motif, idempotence, refus et effets, sans base. */
@ExtendWith(MockitoExtension.class)
class RepriseCloturesTest {

    static final Acteur REPRISE = new Acteur(UUID.randomUUID(), "service-account-reprise-service");
    static final String MOTIF = "Liaison par défaut sans projet (décision du 05/11/2026)";

    @Mock
    LiaisonRepository liaisons;

    @Mock
    ParcoursDepot depot;

    @Mock
    OutboxPublisher outbox;

    @Mock
    EntityManager em;

    @Mock
    PlatformTransactionManager tm;

    RepriseClotures clotures;
    final List<SimpleTransactionStatus> statuts = new ArrayList<>();

    @BeforeEach
    void preparer() {
        clotures = new RepriseClotures(liaisons, depot, outbox, em, tm, Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC));
        lenient().when(tm.getTransaction(any())).thenAnswer(inv -> {
            SimpleTransactionStatus s = new SimpleTransactionStatus();
            statuts.add(s);
            return s;
        });
        lenient().when(liaisons.findByReferenceOrigine(anyString())).thenReturn(Optional.empty());
    }

    /** Liaison reprise au statut donné, avec ses opérations. */
    Liaison liaison(String ref, StatutParcours statut, OperationParcours... operations) {
        Liaison l = new Liaison().etat(EtatLiaison.EN_HOMOLOGATION).institutionId(UUID.randomUUID()).referenceOrigine(ref);
        l.setId(UUID.randomUUID());
        ParcoursLiaison etat = new ParcoursLiaison(l.getId(), "LIA-2026-001", LocalDate.of(2026, 10, 13));
        etat.setStatut(statut);
        Parcours p = mock(Parcours.class);
        lenient().when(p.etat()).thenReturn(etat);
        lenient().when(p.liaison()).thenReturn(l);
        lenient().when(p.operations(any())).thenAnswer(inv -> {
            java.util.function.Predicate<OperationParcours> critere = inv.getArgument(0);
            return List.of(operations).stream().filter(critere).toList();
        });
        when(liaisons.findByReferenceOrigine(ref)).thenReturn(Optional.of(l));
        when(depot.trouver(l.getId(), true)).thenReturn(Optional.of(p));
        return l;
    }

    static OperationParcours operation(EtatOperationParcours etat, Preuve preuve) {
        OperationParcours o = mock(OperationParcours.class);
        lenient().when(o.getEtat()).thenReturn(etat);
        lenient().when(o.getPreuve()).thenReturn(preuve);
        return o;
    }

    static ClotureReprise ligne(String ref) {
        return new ClotureReprise(ref, null);
    }

    @Test
    @DisplayName("Lot vide, trop volumineux, motif absent ou trop court, acteur absent : refus du lot entier")
    void bornes() {
        assertThatThrownBy(() -> clotures.clore(List.of(), true, MOTIF, REPRISE)).isInstanceOf(HomologationException.class);
        assertThatThrownBy(() -> clotures.clore(null, true, MOTIF, REPRISE)).isInstanceOf(HomologationException.class);
        List<ClotureReprise> trop = Collections.nCopies(RepriseClotures.LIGNES_MAX + 1, ligne("R"));
        assertThatThrownBy(() -> clotures.clore(trop, true, MOTIF, REPRISE)).hasMessageContaining("volumineux");
        assertThatThrownBy(() -> clotures.clore(List.of(ligne("R")), true, " x ", REPRISE)).hasMessageContaining("Motif");
        assertThatThrownBy(() -> clotures.clore(List.of(ligne("R")), true, null, REPRISE)).hasMessageContaining("Motif");
        assertThatThrownBy(() -> clotures.clore(List.of(ligne("R")), true, MOTIF, null)).isInstanceOf(HomologationException.class);
        assertThatThrownBy(() -> clotures.clore(List.of(ligne("R")), true, MOTIF, new Acteur(null, "x"))).isInstanceOf(HomologationException.class);
    }

    @Test
    @DisplayName("Clôture : statut CLOSE_SANS_SUITE, état CLOSE, motif, trace dans l'outbox ; simulation annulée")
    void clotureEtSimulation() {
        Liaison l = liaison("REPRISE-99001-MOBILE", StatutParcours.CADREE, operation(EtatOperationParcours.FAITE, Preuve.reprise(null, null, LocalDate.of(2026, 10, 13), "reprise", null, "Repris")));
        Rapport r = clotures.clore(List.of(new ClotureReprise("REPRISE-99001-MOBILE", "PayWay test seul")), true, MOTIF, REPRISE);
        assertThat(r.lignes().get(0).resultat()).isEqualTo(Resultat.CLOSE);
        assertThat(r.lignes().get(0).statutPrecedent()).isEqualTo("CADREE");
        assertThat(statuts).singleElement().matches(SimpleTransactionStatus::isRollbackOnly);
        assertThat(l.getEtat()).isEqualTo(EtatLiaison.CLOSE);
        assertThat(l.getMotifEtat()).isEqualTo("Close sans suite par la reprise de l'existant : " + MOTIF + " : PayWay test seul");
        assertThat(l.getRepriseLe()).isEqualTo(Instant.parse("2026-11-05T12:00:00Z"));
        ArgumentCaptor<Object> charge = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("liaison"), eq(l.getId()), eq(LiaisonClosedWithoutFollowUpPayload.TYPE), charge.capture());
        LiaisonClosedWithoutFollowUpPayload p = (LiaisonClosedWithoutFollowUpPayload) charge.getValue();
        assertThat(p.statutPrecedent()).isEqualTo("CADREE");
        assertThat(p.acteurId()).isEqualTo(REPRISE.id());
    }

    @Test
    @DisplayName("Idempotence et refus par ligne : déjà close, en production, travail réel, inconnue, doublon, vide")
    void refus() {
        liaison("DEJA", StatutParcours.CLOSE_SANS_SUITE);
        liaison("PROD", StatutParcours.EN_PRODUCTION);
        liaison("TRV", StatutParcours.EN_TEST, operation(EtatOperationParcours.FAITE, null));
        liaison("SIG", StatutParcours.EN_TEST, operation(EtatOperationParcours.EN_SIGNATURE, null));
        List<ClotureReprise> lignes = new ArrayList<>(List.of(ligne("DEJA"), ligne("PROD"), ligne("TRV"), ligne("SIG"), ligne("INCONNUE"), ligne("DEJA"), ligne(" ")));
        lignes.add(null);
        Rapport r = clotures.clore(lignes, false, MOTIF, REPRISE);
        assertThat(r.lignes()).extracting(x -> x.resultat() == Resultat.INCHANGEE ? "INCHANGEE" : x.code()).containsExactly(
            "INCHANGEE", "HOM_REPRISE_CLOTURE_EN_PRODUCTION", "HOM_REPRISE_CLOTURE_TRAITEE", "HOM_REPRISE_CLOTURE_TRAITEE",
            "HOM_LIAISON_INTROUVABLE", "HOM_REPRISE_DOUBLON_FICHIER", "HOM_REPRISE_LIGNE_INVALIDE", "HOM_REPRISE_LIGNE_INVALIDE"
        );
        verify(outbox, never()).publish(any(), any(UUID.class), any(), any());
    }

    @Test
    @DisplayName("Écriture refusée par la base : ligne refusée, les autres continuent ; motif borné à 500 caractères")
    void refusBase() {
        liaison("A", StatutParcours.CADREE);
        liaison("B", StatutParcours.CADREE);
        doThrow(new DataIntegrityViolationException("contrainte")).doNothing().when(em).flush();
        Rapport r = clotures.clore(List.of(ligne("A"), ligne("B")), false, MOTIF, REPRISE);
        assertThat(r.lignes().get(0).code()).isEqualTo("HOM_REPRISE_ECRITURE_REFUSEE");
        assertThat(r.lignes().get(1).resultat()).isEqualTo(Resultat.CLOSE);
        assertThat(RepriseClotures.motifLigne("m".repeat(450), "p".repeat(100))).hasSize(500);
        assertThat(RepriseClotures.motifLigne("lot", " ")).isEqualTo("lot");
    }
}
