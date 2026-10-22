package cm.gimac.des.participant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.participant.application.AdhesionProperties;
import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Acteur;
import cm.gimac.des.participant.application.port.in.RepriseInstitutionsUseCase.InstitutionReprise;
import cm.gimac.des.participant.application.port.in.RepriseInstitutionsUseCase.Ligne;
import cm.gimac.des.participant.application.port.in.RepriseInstitutionsUseCase.Rapport;
import cm.gimac.des.participant.application.port.in.RepriseInstitutionsUseCase.Resultat;
import cm.gimac.des.participant.application.port.in.RepriseInstitutionsUseCase.ServiceRepris;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.domain.SubscribedService;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import cm.gimac.des.participant.domain.enumeration.SubscriptionState;
import cm.gimac.des.participant.domain.event.InstitutionRegisteredPayload;
import cm.gimac.des.participant.domain.event.InstitutionUpdatedPayload;
import cm.gimac.des.participant.domain.event.StatusChangedPayload;
import cm.gimac.des.participant.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.InteractionRepository;
import cm.gimac.des.participant.repository.StatusTransitionRepository;
import cm.gimac.des.participant.repository.SubscribedServiceRepository;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Reprise des institutions (D-107) : création, mise à jour, idempotence, simulation et refus, sans base. */
@ExtendWith(MockitoExtension.class)
class RepriseInstitutionsTest {

    static final Acteur REPRISE = new Acteur(UUID.randomUUID(), "service-account-reprise-service");
    static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-10-13T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    InstitutionRepository institutions;

    @Mock
    AdhesionPieceRepository pieces;

    @Mock
    StatusTransitionRepository transitions;

    @Mock
    InteractionRepository interactions;

    @Mock
    SubscribedServiceRepository services;

    @Mock
    OutboxPublisher outbox;

    @Mock
    EntityManager em;

    @Mock
    PlatformTransactionManager tm;

    final VersionsStatutMemoire versions = new VersionsStatutMemoire();
    RepriseInstitutions reprise;
    final List<SimpleTransactionStatus> statuts = new ArrayList<>();

    @BeforeEach
    void preparer() {
        reprise = new RepriseInstitutions(
            institutions, pieces, transitions, interactions, services, outbox,
            new AdhesionProperties(List.of(new AdhesionProperties.PieceDefinition("STATUTS", "Statuts", true))), em, tm, HORLOGE, versions
        );
        lenient().when(tm.getTransaction(any())).thenAnswer(inv -> {
            SimpleTransactionStatus s = new SimpleTransactionStatus();
            statuts.add(s);
            return s;
        });
        lenient().when(institutions.findByParticipantCode(anyString())).thenReturn(Optional.empty());
        lenient().when(transitions.save(any())).thenAnswer(inv -> {
            StatusTransition t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
    }

    static InstitutionReprise ligne(String code, String statut) {
        return new InstitutionReprise(code, null, "Institution " + code, null, "CM", "BANQUE", null, null, null, statut, null, null, null, null, null, null, null, null, null, null);
    }

    static Institution enregistree(String code, InstitutionStatus statut) {
        Institution i = new Institution().name("Institution " + code).status(statut).category(InstitutionCategory.BANQUE).country("CM").complianceState(ComplianceState.CONFORME);
        i.setId(UUID.randomUUID());
        i.setParticipantCode(code);
        return i;
    }

    Ligne une(InstitutionReprise l, boolean simulation) {
        Rapport r = reprise.importer(List.of(l), simulation, null, REPRISE);
        assertThat(r.total()).isEqualTo(1);
        return r.lignes().get(0);
    }

    @Test
    @DisplayName("Création ACTIF avec UUID, adhésion datée, services : persistée, transition de reprise et événements")
    void creation() {
        UUID id = UUID.randomUUID();
        InstitutionReprise l = new InstitutionReprise(
            "BQ-01", id, "Banque fictive", "BF", "CM", "BANQUE", "BDLGGALI", "B01", "Groupe", "ACTIF", LocalDate.of(2022, 1, 1), "PV-7",
            null, LocalDate.of(2021, 3, 1), "DIRECT", "CR-1", null, null, null, List.of(new ServiceRepris("PAIEMENT_MOBILE", null))
        );
        Ligne r = une(l, false);
        assertThat(r.resultat()).isEqualTo(Resultat.CREEE);
        ArgumentCaptor<Institution> creee = ArgumentCaptor.forClass(Institution.class);
        verify(em).persist(creee.capture());
        Institution i = creee.getValue();
        assertThat(i.getId()).isEqualTo(id);
        assertThat(i.getStatus()).isEqualTo(InstitutionStatus.ACTIF);
        assertThat(i.getJoinedOn()).isEqualTo(LocalDate.of(2021, 3, 1));
        assertThat(i.getContactedOn()).isEqualTo(LocalDate.of(2021, 3, 1));
        assertThat(i.getAdmittedById()).isEqualTo(REPRISE.id());
        assertThat(i.getMembershipMode()).isEqualTo(MembershipMode.DIRECT);
        assertThat(i.getGroupName()).isEqualTo("Groupe");
        ArgumentCaptor<SubscribedService> s = ArgumentCaptor.forClass(SubscribedService.class);
        verify(services).save(s.capture());
        assertThat(s.getValue().getSubscribedOn()).isEqualTo(LocalDate.of(2021, 3, 1));
        assertThat(s.getValue().getState()).isEqualTo(SubscriptionState.ACTIF);
        ArgumentCaptor<StatusTransition> t = ArgumentCaptor.forClass(StatusTransition.class);
        verify(transitions).save(t.capture());
        assertThat(t.getValue().getFromStatus()).isEqualTo(InstitutionStatus.CANDIDAT);
        assertThat(t.getValue().getAutomaticTransition()).isTrue();
        assertThat(t.getValue().getReason()).contains("Reprise de l'existant", "PV-7", "2022-01-01");
        verify(outbox).publish(eq("institution"), eq(id), eq(InstitutionRegisteredPayload.TYPE), any());
        verify(outbox).publish(eq("institution"), eq(id), eq(StatusChangedPayload.TYPE), any());
        verify(pieces).save(any());
        assertThat(statuts.get(0).isRollbackOnly()).isFalse();
    }

    @Test
    @DisplayName("Candidat : aucune transition ; clos sans suite : clôture datée ; adhérent : la décision date l'adhésion")
    void statutsParticuliers() {
        assertThat(une(ligne("C-1", "CANDIDAT"), false).resultat()).isEqualTo(Resultat.CREEE);
        verify(transitions, never()).save(any());
        InstitutionReprise clos = new InstitutionReprise("C-2", null, "Clos", null, "CM", "BANQUE", null, null, null, "CLOS_SANS_SUITE", LocalDate.of(2023, 5, 2), null, null, null, null, null, null, null, null, null);
        une(clos, false);
        InstitutionReprise adherent = new InstitutionReprise("C-3", null, "Adh", null, "CM", "BANQUE", null, null, null, "ADHERENT", LocalDate.of(2024, 6, 1), null, null, null, null, null, null, null, null, null);
        une(adherent, false);
        ArgumentCaptor<Institution> creees = ArgumentCaptor.forClass(Institution.class);
        verify(em, org.mockito.Mockito.times(3)).persist(creees.capture());
        assertThat(creees.getAllValues().get(0).getComplianceState()).isEqualTo(ComplianceState.A_REGULARISER);
        assertThat(creees.getAllValues().get(1).getClosedAt()).isEqualTo(LocalDate.of(2023, 5, 2).atStartOfDay(java.time.ZoneId.of("Africa/Douala")).toInstant());
        assertThat(creees.getAllValues().get(1).getClosureReason()).isEqualTo(RepriseInstitutions.MOTIF_DEFAUT);
        assertThat(creees.getAllValues().get(2).getJoinedOn()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    @Test
    @DisplayName("Simulation : chaque ligne est annulée ; une banque créée plus haut est signalée, non rattachée")
    void simulation() {
        InstitutionReprise ep = new InstitutionReprise("EP-1", null, "EP", null, "CM", "ETABLISSEMENT_PAIEMENT", null, null, null, "ACTIF", null, null, null, null, "INDIRECT", null, "CC", null, "BQ-1", null);
        Rapport r = reprise.importer(List.of(ligne("BQ-1", "ACTIF"), ep), true, "  ", REPRISE);
        assertThat(r.simulation()).isTrue();
        assertThat(r.lignes()).extracting(Ligne::resultat).containsExactly(Resultat.CREEE, Resultat.CREEE);
        assertThat(r.lignes().get(1).avertissements()).anyMatch(a -> a.contains("BQ-1"));
        assertThat(statuts).allMatch(TransactionStatus::isRollbackOnly);
        assertThat(r.totaux()).containsEntry(Resultat.CREEE, 2L).containsEntry(Resultat.REFUSEE, 0L);
    }

    @Test
    @DisplayName("Mise à jour : inchangée sans écart, champs et statut atteignable sinon ; statut en arrière refusé")
    void miseAJour() {
        Institution i = enregistree("BQ-2", InstitutionStatus.EN_HOMOLOGATION);
        i.setJoinedOn(LocalDate.of(2020, 1, 1));
        i.setAdmittedById(UUID.randomUUID());
        when(institutions.findByParticipantCode("BQ-2")).thenReturn(Optional.of(i));
        when(institutions.findByIdForUpdate(i.getId())).thenReturn(Optional.of(i));
        when(em.contains(i)).thenReturn(true);
        when(services.findByInstitution_IdAndStateOrderByServiceCode(i.getId(), SubscriptionState.ACTIF)).thenReturn(
            List.of(new SubscribedService().serviceCode("PAIEMENT_MOBILE"))
        );
        InstitutionReprise meme = new InstitutionReprise("BQ-2", i.getId(), "Institution BQ-2", null, "CM", "BANQUE", null, null, null, "EN_HOMOLOGATION", null, null, null, null, null, null, null, null, null, List.of(new ServiceRepris("PAIEMENT_MOBILE", null)));
        assertThat(une(meme, false).resultat()).isEqualTo(Resultat.INCHANGEE);
        verify(outbox, never()).publish(anyString(), any(UUID.class), anyString(), any());

        InstitutionReprise active = new InstitutionReprise("BQ-2", null, "Nouveau nom", "NN", "CM", "BANQUE", null, null, "G", "ACTIF", null, "PV-9", LocalDate.of(2019, 1, 1), LocalDate.of(2020, 2, 2), null, null, null, null, null, List.of(new ServiceRepris("CARTE", null)));
        Ligne r = une(active, false);
        assertThat(r.resultat()).isEqualTo(Resultat.MISE_A_JOUR);
        assertThat(r.changements()).contains("nom", "sigle", "groupe", "statut", "datePriseDeContact", "dateAdhesion", "servicesSouscrits");
        assertThat(i.getStatus()).isEqualTo(InstitutionStatus.ACTIF);
        assertThat(i.getJoinedOn()).isEqualTo(LocalDate.of(2020, 2, 2));
        verify(outbox).publish(eq("institution"), eq(i.getId()), eq(InstitutionUpdatedPayload.TYPE), any());
        verify(outbox).publish(eq("institution"), eq(i.getId()), eq(StatusChangedPayload.TYPE), any());

        Ligne arriere = une(ligne("BQ-2", "ADHERENT"), false);
        assertThat(arriere.resultat()).isEqualTo(Resultat.REFUSEE);
        assertThat(arriere.code()).isEqualTo("PAR_REPRISE_STATUT_INATTEIGNABLE");
    }

    @Test
    @DisplayName("Refus par ligne : UUID divergent ou déjà pris, code BIC ou BEAC déjà attribué, banque inconnue ou elle-même")
    void refus() {
        Institution autre = enregistree("BQ-3", InstitutionStatus.ACTIF);
        when(institutions.findByParticipantCode("BQ-3")).thenReturn(Optional.of(autre));
        assertThat(une(avecId(ligne("BQ-3", "ACTIF"), UUID.randomUUID()), false).code()).isEqualTo("PAR_REPRISE_ID_DIVERGENT");
        UUID pris = UUID.randomUUID();
        when(institutions.existsById(pris)).thenReturn(true);
        assertThat(une(avecId(ligne("BQ-4", "ACTIF"), pris), false).code()).isEqualTo("PAR_REPRISE_ID_DEJA_PRIS");
        when(institutions.existsByIdentificationCodeAndIdNot(eq("BDLGGALI"), any())).thenReturn(true);
        InstitutionReprise bic = new InstitutionReprise("BQ-5", null, "B", null, "CM", "BANQUE", "BDLGGALI", null, null, "ACTIF", null, null, null, null, null, null, null, null, null, null);
        assertThat(une(bic, false).code()).isEqualTo("PAR_CODE_ALREADY_USED");
        InstitutionReprise inconnue = new InstitutionReprise("BQ-6", null, "B", null, "CM", "BANQUE", null, null, null, "ACTIF", null, null, null, null, null, null, null, "BQ-ABS", null, null);
        assertThat(une(inconnue, false).code()).isEqualTo("PAR_REPRISE_BANQUE_INCONNUE");
        InstitutionReprise elleMeme = new InstitutionReprise("BQ-7", null, "B", null, "CM", "BANQUE", null, null, null, "ACTIF", null, null, null, null, null, null, null, null, "BQ-7", null);
        assertThat(une(elleMeme, false).code()).isEqualTo("PAR_SPONSOR_INVALID");
        Institution marraine = enregistree("BQ-8", InstitutionStatus.ACTIF);
        when(institutions.findByParticipantCode("BQ-8")).thenReturn(Optional.of(marraine));
        InstitutionReprise rattachee = new InstitutionReprise("EP-8", null, "EP", null, "CM", "BANQUE", null, null, null, "ACTIF", null, null, null, null, "INDIRECT", null, "CC", "BQ-8", "BQ-8", null);
        assertThat(une(rattachee, false).resultat()).isEqualTo(Resultat.CREEE);
    }

    static InstitutionReprise avecId(InstitutionReprise l, UUID id) {
        return new InstitutionReprise(l.codeMembre(), id, l.nom(), l.sigle(), l.pays(), l.categorie(), null, null, null, l.statut(), null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Lot : doublons du fichier, ligne vide, ligne invalide, écriture refusée par la base")
    void lot() {
        doThrow(new DataIntegrityViolationException("uk")).when(em).flush();
        List<InstitutionReprise> lignes = new ArrayList<>();
        lignes.add(ligne("D-1", "ACTIF"));
        lignes.add(ligne("d-1", "ACTIF"));
        UUID id = UUID.randomUUID();
        lignes.add(avecId(ligne("D-2", "ACTIF"), id));
        lignes.add(avecId(ligne("D-3", "ACTIF"), id));
        lignes.add(null);
        lignes.add(ligne("D-4", "INCONNU"));
        Rapport r = reprise.importer(lignes, false, "x".repeat(300), REPRISE);
        assertThat(r.lignes()).extracting(Ligne::code).containsExactly(
            "PAR_REPRISE_ECRITURE_REFUSEE",
            "PAR_REPRISE_DOUBLON_FICHIER",
            "PAR_REPRISE_ECRITURE_REFUSEE",
            "PAR_REPRISE_DOUBLON_FICHIER",
            "PAR_REPRISE_LIGNE_INVALIDE",
            "PAR_REPRISE_LIGNE_INVALIDE"
        );
        assertThat(r.totaux()).containsEntry(Resultat.REFUSEE, 6L);
    }

    @Test
    @DisplayName("Lot vide ou trop volumineux : refusé d'emblée")
    void bornes() {
        assertThatThrownBy(() -> reprise.importer(List.of(), true, null, REPRISE))
            .isInstanceOfSatisfying(ParticipantException.class, e -> assertThat(e.code()).isEqualTo("PAR_REPRISE_VIDE"));
        assertThatThrownBy(() -> reprise.importer(null, true, null, REPRISE)).isInstanceOf(ParticipantException.class);
        List<InstitutionReprise> trop = Collections.nCopies(RepriseInstitutions.LIGNES_MAX + 1, ligne("T-1", "ACTIF"));
        assertThatThrownBy(() -> reprise.importer(trop, true, null, REPRISE))
            .isInstanceOfSatisfying(ParticipantException.class, e -> assertThat(e.code()).isEqualTo("PAR_REPRISE_TROP_DE_LIGNES"));
    }
}
