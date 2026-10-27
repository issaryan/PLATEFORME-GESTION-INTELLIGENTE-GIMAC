package cm.gimac.des.homologation.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Campagne;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.CampagneListee;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.PageExecutions;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Piece;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Taux;
import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.sandbox.CampagneSandbox;
import cm.gimac.des.homologation.domain.sandbox.ExecutionSandbox;
import cm.gimac.des.homologation.domain.sandbox.PieceSandbox;
import cm.gimac.des.homologation.domain.sandbox.ReleveLiaisonSandbox;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.CampagneSandboxRepository;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.repository.EvaluationBaseRepository;
import cm.gimac.des.homologation.repository.ExecutionSandboxRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.PieceSandboxRepository;
import cm.gimac.des.homologation.repository.ReleveLiaisonSandboxRepository;
import cm.gimac.des.homologation.repository.ScenariosSandboxRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/** Cas de bord de la relève, des pièces, des couvertures et des vues des résultats de la sandbox (US-EXI-14, US-EXI-17). */
class CouvertureSandboxComplementTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");

    @Test
    @DisplayName("Pièce déjà inscrite non relue ; octrets divergents de la liste ; nom fourni ; condensé d'un algorithme absent")
    void pieces() {
        byte[] octets = "bilan".getBytes(StandardCharsets.UTF_8);
        String hex = ConservationPiecesSandbox.sha256(octets);
        LectureSandboxPort lecture = mock(LectureSandboxPort.class);
        DocumentRegistryPort documents = mock(DocumentRegistryPort.class);
        PieceSandboxRepository repo = mock(PieceSandboxRepository.class);
        when(lecture.pieces("C")).thenReturn(List.of(new Piece("BILAN", 1, hex, 5, "b.json"), new Piece("RESULTATS", 1, hex, 5, "r.json")));
        when(repo.existsByCampagneIdAndNatureAndVersion("C", "BILAN", 1)).thenReturn(true);
        when(lecture.empreinte("C", "RESULTATS", 1)).thenReturn(hex);
        when(lecture.telecharger("C", "RESULTATS", 1)).thenReturn("autre".getBytes(StandardCharsets.UTF_8));
        when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(new ConservationPiecesSandbox(lecture, documents, repo, ReleveSandboxTest.transactions(), Clock.fixed(T, ZoneOffset.UTC)).conserver("C")).isEqualTo(1);
        verify(lecture, never()).telecharger("C", "BILAN", 1);
        verify(documents, never()).deposer(any(), anyString(), anyString(), anyString(), anyString(), any());
        assertThatThrownBy(() -> ConservationPiecesSandbox.condense(octets, "INCONNU")).isInstanceOf(IllegalStateException.class).hasMessageContaining("INCONNU");
    }

    @Test
    @DisplayName("Couverture plus ancienne remplacée par une mesure plus récente")
    void couvertureRemplacee() {
        CoverageRepository repo = mock(CoverageRepository.class);
        Liaison l = new Liaison().id(UUID.randomUUID());
        when(repo.findFirstByLiaison_IdAndNatureAndServiceCode(l.getId(), NatureCouverture.CERTIFICATION, "RETRAIT")).thenReturn(Optional.of(new Coverage().serviceCode("RETRAIT").dateCalcul(T.minusSeconds(60))));
        Taux t = new Taux("RETRAIT", "REUSSITE", 10, 10, "100.00", "1.0", T);
        assertThat(new CouverturesSandbox(repo).appliquer(l, new Campagne("C", "CMP", l.getId().toString(), "99001", "MOB", List.of(), "TERMINEE", List.of(t), 1, "1.0"))).isEqualTo(1);
    }

    @Test
    @DisplayName("Relève : liaison en échec non comptée, page close vide, liaison sans version de référentiel")
    void releve() {
        LectureSandboxPort lecture = mock(LectureSandboxPort.class);
        LiaisonRepository liaisons = mock(LiaisonRepository.class);
        CampagneSandboxRepository campagnes = mock(CampagneSandboxRepository.class);
        ReleveLiaisonSandboxRepository releves = mock(ReleveLiaisonSandboxRepository.class);
        ExecutionSandboxRepository executions = mock(ExecutionSandboxRepository.class);
        when(releves.findById(any())).thenReturn(Optional.empty());
        Liaison l = new Liaison().id(UUID.randomUUID()).etat(EtatLiaison.OUVERTE);
        Liaison autre = new Liaison().id(UUID.randomUUID()).etat(EtatLiaison.OUVERTE);
        when(liaisons.findAll()).thenReturn(List.of(l, autre));
        when(liaisons.findById(l.getId())).thenReturn(Optional.of(l));
        when(lecture.campagnes(autre.getId())).thenThrow(new LectureSandboxPort.Indisponible("503", null));
        when(lecture.campagnes(l.getId())).thenReturn(List.of(new CampagneListee("C1", "CMP", "EN_COURS", null)));
        when(lecture.campagne("C1")).thenReturn(new Campagne("C1", "CMP", l.getId().toString(), "99001", "MOB", null, "EN_COURS", null, null, "1.0"));
        when(campagnes.verrouiller("C1")).thenReturn(Optional.of(new CampagneSandbox("C1", l.getId())));
        when(lecture.executions(eq("C1"), any(), anyInt())).thenReturn(new PageExecutions(List.of(), "p1", true, "e"));
        ReleveSandbox r = new ReleveSandbox(lecture, liaisons, campagnes, executions, mock(ScenariosSandboxRepository.class), releves,
            mock(EvaluationBaseRepository.class), mock(PieceSandboxRepository.class), mock(ConservationPiecesSandbox.class),
            mock(CouverturesSandbox.class), mock(OutboxPublisher.class), ReleveSandboxTest.transactions(), Clock.fixed(T, ZoneOffset.UTC));
        assertThat(r.releverTout()).isEqualTo(1);
    }

    @Test
    @DisplayName("Vues des résultats : pièces, taux et services absents, exécution paginée, entités de la relève")
    void vues() {
        LiaisonRepository liaisons = mock(LiaisonRepository.class);
        CampagneSandboxRepository campagnes = mock(CampagneSandboxRepository.class);
        ExecutionSandboxRepository executions = mock(ExecutionSandboxRepository.class);
        PieceSandboxRepository pieces = mock(PieceSandboxRepository.class);
        ScenariosSandboxRepository scenarios = mock(ScenariosSandboxRepository.class);
        ReleveLiaisonSandboxRepository releves = mock(ReleveLiaisonSandboxRepository.class);
        UUID l = UUID.randomUUID();
        when(liaisons.existsById(l)).thenReturn(true);
        when(releves.findById(l)).thenReturn(Optional.of(new ReleveLiaisonSandbox(l)));
        CampagneSandbox c = new CampagneSandbox("C1", l);
        assertThat(c.getLiaisonId()).isEqualTo(l);
        when(campagnes.findByLiaisonIdOrderByCampagneId(l)).thenReturn(List.of(c));
        UUID doc = UUID.randomUUID();
        when(pieces.findByCampagneIdOrderByNatureAscVersionAsc("C1")).thenReturn(List.of(new PieceSandbox("C1", "BILAN", 1, "ab".repeat(32), 10, "b.json", doc, PieceSandbox.Statut.DEPOSEE, T)));
        when(scenarios.findByLiaison_IdOrderByDateDetectionAsc(l)).thenReturn(List.of());
        ResultatsSandbox s = new ResultatsSandbox(liaisons, campagnes, executions, pieces, scenarios, releves);
        ResultatsSandbox.Resultats r = s.resultats(l);
        assertThat(r.campagnes().get(0).services()).isEmpty();
        assertThat(r.campagnes().get(0).taux()).isEmpty();
        assertThat(r.campagnes().get(0).pieces().get(0).documentId()).isEqualTo(doc);
        // Liaison jamais relevée : relève vierge, aucune campagne.
        UUID jamais = UUID.randomUUID();
        when(liaisons.existsById(jamais)).thenReturn(true);
        ResultatsSandbox.Resultats vierge = s.resultats(jamais);
        assertThat(vierge.campagnes()).isEmpty();
        assertThat(vierge.enEchec()).isFalse();
        ExecutionSandbox x = new ExecutionSandbox("C1", l, "EXE-1");
        x.retenir("CAS", "SCN", "3", T, "PASSED", "favorable", null, "hybride", "TRC", "p1", "VALIDATION", "1.0", "PLATEFORME", "OP-1", T);
        assertThat(x.getId()).isNotNull();
        assertThat(x.getCampagneId()).isEqualTo("C1");
        assertThat(x.getLiaisonId()).isEqualTo(l);
        assertThat(x.getPosition()).isEqualTo("p1");
        assertThat(x.getReleveLe()).isEqualTo(T);
        when(executions.findByLiaisonIdAndCampagneId(eq(l), eq("C1"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(x)));
        assertThat(s.executions(l, "C1", -1, 500).contenu().get(0).operationCode()).isEqualTo("OP-1");
    }
}
