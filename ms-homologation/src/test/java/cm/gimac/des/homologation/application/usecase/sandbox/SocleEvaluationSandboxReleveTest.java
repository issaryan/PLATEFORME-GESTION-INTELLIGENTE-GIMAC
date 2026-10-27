package cm.gimac.des.homologation.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Campagne;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.PageExecutions;
import cm.gimac.des.homologation.domain.EvaluationBase;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.event.CampaignResultImportedPayload;
import cm.gimac.des.homologation.domain.sandbox.CampagneSandbox;
import cm.gimac.des.homologation.domain.sandbox.PieceSandbox;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.CampagneSandboxRepository;
import cm.gimac.des.homologation.repository.EvaluationBaseRepository;
import cm.gimac.des.homologation.repository.ExecutionSandboxRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.PieceSandboxRepository;
import cm.gimac.des.homologation.repository.ReleveLiaisonSandboxRepository;
import cm.gimac.des.homologation.repository.ScenariosSandboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-EXI-25 : socle historisé et empreinte propagée par la relève existante. */
class SocleEvaluationSandboxReleveTest {

    static final Instant T = Instant.parse("2026-10-27T08:00:00Z");
    static final String EMPREINTE = "a".repeat(64);

    LectureSandboxPort lecture;
    EvaluationBaseRepository evaluationBases;
    PieceSandboxRepository piecesSandbox;
    ConservationPiecesSandbox conservation;
    OutboxPublisher outbox;
    ReleveSandbox releve;
    UUID liaisonId;
    Liaison liaison;

    @BeforeEach
    void init() {
        lecture = mock(LectureSandboxPort.class);
        evaluationBases = mock(EvaluationBaseRepository.class);
        piecesSandbox = mock(PieceSandboxRepository.class);
        conservation = mock(ConservationPiecesSandbox.class);
        outbox = mock(OutboxPublisher.class);
        LiaisonRepository liaisons = mock(LiaisonRepository.class);
        CampagneSandboxRepository campagnes = mock(CampagneSandboxRepository.class);
        liaisonId = UUID.randomUUID();
        liaison = new Liaison().id(liaisonId).etat(EtatLiaison.EN_HOMOLOGATION);
        when(liaisons.findById(liaisonId)).thenReturn(Optional.of(liaison));
        when(campagnes.verrouiller("C1")).thenReturn(Optional.of(new CampagneSandbox("C1", liaisonId)));
        when(lecture.campagne("C1")).thenReturn(campagne());
        when(lecture.executions(eq("C1"), any(), anyInt())).thenReturn(new PageExecutions(List.of(), "p0", false, "e"));
        when(conservation.conserver("C1")).thenReturn(1);
        when(evaluationBases.findFirstByLiaison_IdOrderByCycleDesc(liaisonId)).thenReturn(Optional.empty());
        releve = new ReleveSandbox(
            lecture,
            liaisons,
            campagnes,
            mock(ExecutionSandboxRepository.class),
            mock(ScenariosSandboxRepository.class),
            mock(ReleveLiaisonSandboxRepository.class),
            evaluationBases,
            piecesSandbox,
            conservation,
            mock(CouverturesSandbox.class),
            outbox,
            ReleveSandboxTest.transactions(),
            Clock.fixed(T, ZoneOffset.UTC)
        );
    }

    static Campagne campagne() {
        return new Campagne("C1", "CMP", null, "99001", "MOB", null, "TERMINEE", null, 1, "1.0");
    }

    static PieceSandbox bilan(PieceSandbox.Statut statut) {
        return new PieceSandbox("C1", "BILAN", 1, EMPREINTE, 12, "bilan.json", UUID.randomUUID(), statut, T);
    }

    @Test
    @DisplayName("Le bilan contrôlé crée le premier socle et son empreinte part dans l'événement")
    void empreinteAjouteeAuSocleEtALevenement() {
        // L'identifiant de liaison est injecté dans la campagne par le test pour garder la fixture lisible.
        Campagne c = new Campagne("C1", "CMP", liaisonId.toString(), "99001", "MOB", null, "TERMINEE", null, 1, "1.0");
        when(lecture.campagne("C1")).thenReturn(c);
        when(piecesSandbox.findByCampagneIdAndNatureAndVersion("C1", "BILAN", 1)).thenReturn(Optional.of(bilan(PieceSandbox.Statut.DEPOSEE)));

        releve.releverCampagne(liaisonId, "C1");

        verify(evaluationBases).save(org.mockito.ArgumentMatchers.argThat((EvaluationBase b) ->
            b.getCycle() == 1 && b.getEmpreinte().equals(EMPREINTE) && b.getLiaison() == liaison && b.getComposition().contains("SANDBOX")
        ));
        verify(outbox).publish(eq("campaign"), eq("C1"), eq(CampaignResultImportedPayload.TYPE), org.mockito.ArgumentMatchers.argThat((CampaignResultImportedPayload p) ->
            p.empreinteBilan().equals(EMPREINTE) && p.versionBilan() == 1
        ));
    }

    @Test
    @DisplayName("Même empreinte : événement propagé mais aucun nouveau socle")
    void memeEmpreinteSansNouveauSocle() {
        when(lecture.campagne("C1")).thenReturn(new Campagne("C1", "CMP", liaisonId.toString(), "99001", "MOB", null, "TERMINEE", null, 1, "1.0"));
        when(piecesSandbox.findByCampagneIdAndNatureAndVersion("C1", "BILAN", 1)).thenReturn(Optional.of(bilan(PieceSandbox.Statut.DEJA_CONSERVEE)));
        when(evaluationBases.findFirstByLiaison_IdOrderByCycleDesc(liaisonId)).thenReturn(Optional.of(new EvaluationBase().empreinte(EMPREINTE).cycle(3)));

        releve.releverCampagne(liaisonId, "C1");

        verify(evaluationBases, never()).save(any());
        verify(outbox).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Bilan divergent ou absent : événement rétrocompatible sans empreinte et sans socle")
    void bilanNonValideSansSocle() {
        when(lecture.campagne("C1")).thenReturn(new Campagne("C1", "CMP", liaisonId.toString(), "99001", "MOB", null, "TERMINEE", null, 1, "1.0"));
        when(piecesSandbox.findByCampagneIdAndNatureAndVersion("C1", "BILAN", 1))
            .thenReturn(Optional.of(bilan(PieceSandbox.Statut.EMPREINTE_DIVERGENTE)))
            .thenReturn(Optional.empty());

        releve.releverCampagne(liaisonId, "C1");
        releve.releverCampagne(liaisonId, "C1");

        verify(evaluationBases, never()).save(any());
        verify(outbox, org.mockito.Mockito.times(2)).publish(eq("campaign"), eq("C1"), eq(CampaignResultImportedPayload.TYPE), org.mockito.ArgumentMatchers.argThat((CampaignResultImportedPayload p) ->
            p.empreinteBilan() == null
        ));
    }

    @Test
    @DisplayName("L'ancien constructeur du contrat reste utilisable et produit une empreinte nulle")
    void constructeurHistorique() {
        CampaignResultImportedPayload p = new CampaignResultImportedPayload(liaisonId, "C1", "CMP", "TERMINEE", 0, 0, 0, 1, 1, T);
        assertThat(p.empreinteBilan()).isNull();
        assertThat(p.releveLe()).isEqualTo(T);
    }
}
