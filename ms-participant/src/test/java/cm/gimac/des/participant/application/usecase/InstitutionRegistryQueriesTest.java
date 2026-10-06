package cm.gimac.des.participant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.StatusTransitionRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/** Lectures des écrans et du tableau de bord de la DDRM. */
@ExtendWith(MockitoExtension.class)
class InstitutionRegistryQueriesTest {

    @Mock
    InstitutionRepository institutions;

    @Mock
    AdhesionPieceRepository pieces;

    @Mock
    StatusTransitionRepository transitions;

    InstitutionRegistryQueries queries;

    @BeforeEach
    void init() {
        queries = new InstitutionRegistryQueries(institutions, pieces, transitions, new DossierSnapshots(pieces), java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Pagination bornée (1 à 100), tri par date de création décroissante")
    void paginationBornee() {
        when(institutions.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        ArgumentCaptor<Pageable> p = ArgumentCaptor.forClass(Pageable.class);
        queries.lister(new InstitutionRegistryQueries.Filtre(null, null, null), -3, 500);
        queries.lister(new InstitutionRegistryQueries.Filtre(Set.of(), " ", null), 2, 0);
        verify(institutions, org.mockito.Mockito.times(2)).findAll(any(Specification.class), p.capture());
        assertThat(p.getAllValues().get(0).getPageNumber()).isZero();
        assertThat(p.getAllValues().get(0).getPageSize()).isEqualTo(100);
        assertThat(p.getAllValues().get(1).getPageSize()).isEqualTo(1);
        assertThat(p.getAllValues().get(1).getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    @DisplayName("Filtre : statuts, chargé de la DDRM et recherche libre échappée")
    @SuppressWarnings("unchecked")
    void specificationComplete() {
        Root<Institution> root = org.mockito.Mockito.mock(Root.class, Answers.RETURNS_DEEP_STUBS);
        CriteriaQuery<?> q = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class, Answers.RETURNS_DEEP_STUBS);
        UUID charge = UUID.randomUUID();
        Specification<Institution> s = InstitutionRegistryQueries.specification(
            new InstitutionRegistryQueries.Filtre(Set.of(InstitutionStatus.CANDIDAT), "50%_banque", charge)
        );
        s.toPredicate(root, q, cb);
        verify(cb).equal(any(), org.mockito.ArgumentMatchers.eq(charge));
        verify(cb, org.mockito.Mockito.times(3)).like(any(), org.mockito.ArgumentMatchers.eq("%50\\%\\_banque%"), org.mockito.ArgumentMatchers.eq('\\'));
    }

    @Test
    @DisplayName("Dossier numérique : pièces, transitions, prérequis de l'adhésion et transitions permises")
    void dossier() {
        Institution i = new Institution().status(InstitutionStatus.CANDIDAT).complianceState(ComplianceState.A_REGULARISER);
        i.setId(UUID.randomUUID());
        when(institutions.findById(i.getId())).thenReturn(Optional.of(i));
        when(pieces.findByInstitutionIdOrderByCode(i.getId())).thenReturn(List.of());
        when(transitions.findByInstitutionIdOrderByRequestedAtDesc(i.getId())).thenReturn(List.of());
        InstitutionRegistryQueries.Dossier d = queries.dossier(i.getId());
        assertThat(d.institution()).isSameAs(i);
        assertThat(d.prerequisAdhesion()).extracting("code").containsExactly("CODE_PARTICIPANT", "MODE_ADHESION");
        assertThat(d.transitionsPermises()).containsExactlyInAnyOrder(InstitutionStatus.ADHERENT, InstitutionStatus.CLOS_SANS_SUITE);
    }

    @Test
    void dossierInconnu() {
        assertThatThrownBy(() -> queries.dossier(UUID.randomUUID())).isInstanceOf(ParticipantException.class);
    }

    @Test
    @DisplayName("Compteurs du tableau de bord de la DDRM")
    void indicateurs() {
        when(institutions.countByStatus(InstitutionStatus.CANDIDAT)).thenReturn(7L);
        when(institutions.countByStatus(InstitutionStatus.ADHERENT)).thenReturn(4L);
        when(institutions.countByStatus(InstitutionStatus.EN_HOMOLOGATION)).thenReturn(2L);
        when(institutions.countByStatus(InstitutionStatus.CLOS_SANS_SUITE)).thenReturn(1L);
        when(pieces.countByStateAndInstitutionStatus(AdhesionPieceState.RECUE, InstitutionStatus.CANDIDAT)).thenReturn(5L);
        when(transitions.countByState(TransitionState.PROPOSEE)).thenReturn(3L);
        var i = queries.indicateurs();
        assertThat(new InstitutionRegistryQueries.Indicateurs(i.candidats(), i.piecesAExaminer(), i.propositionsEnAttente(), i.adherents(), i.enHomologation(), i.closSansSuite()))
            .isEqualTo(new InstitutionRegistryQueries.Indicateurs(7, 5, 3, 4, 2, 1));
        assertThat(i.fluxParMois()).hasSize(InstitutionRegistryQueries.MOIS_FLUX);
    }

    @Test
    @DisplayName("Flux mensuel : 12 mois denses au fuseau de Douala, depuis le 1er novembre 2025, mois vides à zéro")
    void fluxParMois() {
        // 2026-09-30T23:30Z est déjà le 1er octobre à Douala (UTC+1).
        when(institutions.creationsDepuis(java.time.Instant.parse("2025-10-31T23:00:00Z")))
            .thenReturn(List.of(java.time.Instant.parse("2026-09-30T23:30:00Z"), java.time.Instant.parse("2026-10-02T09:00:00Z"), java.time.Instant.parse("2025-11-03T09:00:00Z")));
        when(institutions.adhesionsDepuis(any())).thenReturn(java.util.Arrays.asList(java.time.Instant.parse("2026-09-15T09:00:00Z"), null));
        when(institutions.cloturesDepuis(any())).thenReturn(List.of(java.time.Instant.parse("2026-03-10T09:00:00Z")));
        var f = queries.fluxParMois();
        assertThat(f).hasSize(12);
        assertThat(f.get(0)).isEqualTo(new InstitutionRegistryQueries.FluxMensuel(java.time.YearMonth.of(2025, 11), 1, 0, 0));
        assertThat(f.get(11)).isEqualTo(new InstitutionRegistryQueries.FluxMensuel(java.time.YearMonth.of(2026, 10), 2, 0, 0));
        assertThat(f.get(10)).isEqualTo(new InstitutionRegistryQueries.FluxMensuel(java.time.YearMonth.of(2026, 9), 0, 1, 0));
        assertThat(f.get(4).clotures()).isEqualTo(1);
        assertThat(new PageImpl<>(List.of()).getTotalElements()).isZero();
    }
}
