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

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Campagne;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.CampagneListee;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.HorsContrat;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.PageExecutions;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.sandbox.ReleveLiaisonSandbox;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.CampagneSandboxRepository;
import cm.gimac.des.homologation.repository.ExecutionSandboxRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** Relève des résultats de la sandbox : cas d'erreur et de bord, sans base (US-EXI-14). */
class ReleveSandboxTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");

    /** Gestionnaire de transactions sans effet : les rappels s'exécutent tels quels. */
    static TransactionTemplate transactions() {
        return new TransactionTemplate(
            new PlatformTransactionManager() {
                @Override
                public TransactionStatus getTransaction(TransactionDefinition d) {
                    return new SimpleTransactionStatus();
                }

                @Override
                public void commit(TransactionStatus s) {
                    // sans effet
                }

                @Override
                public void rollback(TransactionStatus s) {
                    // sans effet
                }
            }
        );
    }

    LectureSandboxPort lecture;
    LiaisonRepository liaisons;
    CampagneSandboxRepository campagnes;
    ReleveLiaisonSandboxRepository releves;
    ConservationPiecesSandbox pieces;
    OutboxPublisher outbox;
    ReleveSandbox releve;
    UUID l;

    @BeforeEach
    void init() {
        lecture = mock(LectureSandboxPort.class);
        liaisons = mock(LiaisonRepository.class);
        campagnes = mock(CampagneSandboxRepository.class);
        releves = mock(ReleveLiaisonSandboxRepository.class);
        pieces = mock(ConservationPiecesSandbox.class);
        outbox = mock(OutboxPublisher.class);
        when(releves.findById(any())).thenReturn(Optional.empty());
        when(campagnes.verrouiller(anyString())).thenReturn(Optional.empty());
        releve = new ReleveSandbox(
            lecture,
            liaisons,
            campagnes,
            mock(ExecutionSandboxRepository.class),
            mock(ScenariosSandboxRepository.class),
            releves,
            pieces,
            mock(CouverturesSandbox.class),
            outbox,
            transactions(),
            Clock.fixed(T, ZoneOffset.UTC)
        );
        l = UUID.randomUUID();
    }

    static Campagne campagne(String id, UUID liaison) {
        return new Campagne(id, "CMP", liaison.toString(), "99001", "MOB", null, "EN_COURS", null, null, "1.0");
    }

    @Test
    @DisplayName("Liste des campagnes hors contrat : échec de la relève noté sans délai de reprise")
    void listeHorsContrat() {
        when(lecture.campagnes(l)).thenThrow(new HorsContrat("SBX-LECTURE répond 403"));
        assertThat(releve.releverLiaison(l).issue()).isEqualTo(ReleveSandbox.Issue.ECHEC);
        verify(releves).save(org.mockito.ArgumentMatchers.argThat((ReleveLiaisonSandbox r) -> r.enEchec() && r.getReprendreApres() == null));
    }

    @Test
    @DisplayName("Campagne d'une autre liaison, ou liaison inconnue : écartée, les autres campagnes sont relues")
    void campagnesEcartees() {
        when(lecture.campagnes(l)).thenReturn(List.of(new CampagneListee("C1", "CMP", "TERMINEE", null), new CampagneListee("C2", "CMP", "EN_COURS", null)));
        when(lecture.campagne("C1")).thenReturn(campagne("C1", UUID.randomUUID()));
        when(lecture.campagne("C2")).thenReturn(campagne("C2", l));
        when(liaisons.findById(l)).thenReturn(Optional.empty());

        ReleveSandbox.Bilan b = releve.releverLiaison(l);

        assertThat(b).isEqualTo(new ReleveSandbox.Bilan(ReleveSandbox.Issue.REUSSIE, 0, 0));
        verify(lecture, never()).executions(anyString(), any(), anyInt());
    }

    @Test
    @DisplayName("Campagne relue sans référentiel ni enregistrement nouveau : aucun événement publié")
    void sansNouveaute() {
        Liaison liaison = new Liaison().id(l).etat(EtatLiaison.OUVERTE);
        when(liaisons.findById(l)).thenReturn(Optional.of(liaison));
        when(lecture.campagne("C1")).thenReturn(campagne("C1", l));
        when(lecture.executions(eq("C1"), any(), anyInt())).thenReturn(new PageExecutions(List.of(), "p0", false, "e"));
        when(campagnes.verrouiller("C1")).thenReturn(Optional.of(new cm.gimac.des.homologation.domain.sandbox.CampagneSandbox("C1", l)));

        assertThat(releve.releverCampagne(l, "C1")).isZero();
        verify(outbox, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Relève périodique : les liaisons closes ne sont pas relues")
    void releveTout() {
        Liaison close = new Liaison().id(UUID.randomUUID()).etat(EtatLiaison.CLOSE);
        Liaison ouverte = new Liaison().id(l).etat(EtatLiaison.EN_HOMOLOGATION);
        when(liaisons.findAll()).thenReturn(List.of(close, ouverte));
        when(lecture.campagnes(l)).thenReturn(List.of());
        when(lecture.campagnes(close.getId())).thenThrow(new IllegalStateException("jamais appelé"));
        assertThat(releve.releverTout()).isEqualTo(1);
    }

    @Test
    @DisplayName("État de relève : report pendant le délai, échec le plus récent, motif tronqué")
    void etatDeReleve() {
        ReleveLiaisonSandbox r = new ReleveLiaisonSandbox(l);
        assertThat(r.enEchec()).isFalse();
        assertThat(r.reportee(T)).isFalse();
        r.echec(T, "x".repeat(400), java.time.Duration.ofMinutes(5));
        assertThat(r.getMotifEchec()).hasSize(300);
        assertThat(r.reportee(T.plusSeconds(60))).isTrue();
        assertThat(r.enEchec()).isTrue();
        r.reussie(T.plusSeconds(600));
        assertThat(r.enEchec()).isFalse();
        assertThat(r.getReprendreApres()).isNull();
        r.echec(T.plusSeconds(700), null, null);
        assertThat(r.enEchec()).isTrue();
        assertThat(r.getMotifEchec()).isNull();
        assertThat(r.getLiaisonId()).isEqualTo(l);
        assertThat(r.getDernierEchec()).isEqualTo(T.plusSeconds(700));
        assertThatThrownBy(() -> new ReleveLiaisonSandbox(null)).isInstanceOf(NullPointerException.class);
    }
}
