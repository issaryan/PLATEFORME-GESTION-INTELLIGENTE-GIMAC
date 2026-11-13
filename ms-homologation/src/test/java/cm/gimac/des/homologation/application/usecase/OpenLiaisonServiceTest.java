package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase.Acteur;
import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase.OpenLiaisonCommand;
import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersionSamples;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.event.LiaisonOpenedPayload;
import cm.gimac.des.homologation.domain.event.ProjectLeadDesignatedPayload;
import cm.gimac.des.homologation.domain.service.LiaisonOpeningPolicy;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/** Cadrage (étape 3, HOM-04 adapté par D-57 et D-58). */
@ExtendWith(MockitoExtension.class)
class OpenLiaisonServiceTest {

    static final UUID EP_A = UUID.randomUUID();
    static final UUID BANQUE_B = UUID.randomUUID();
    static final UUID CONDUCTEUR = UUID.randomUUID();
    static final Acteur CHEF = new Acteur(UUID.randomUUID(), true);
    static final Instant T = Instant.parse("2026-10-06T09:00:00Z");

    @Mock
    ParticipantDirectoryPort participants;

    @Mock
    CollaboratorDirectoryPort collaborateurs;

    @Mock
    LiaisonRepository liaisons;

    @Mock
    ReferentialVersionRepository versions;

    @Mock
    ContractualScopeRepository perimetres;

    @Mock
    ScopeItemRepository elements;

    @Mock
    OutboxPublisher outbox;

    @Mock
    ParcoursInstanciation instanciation;

    @Mock
    ParcoursCommandes conduite;

    OpenLiaisonService service;

    @BeforeEach
    void init() {
        service = new OpenLiaisonService(participants, collaborateurs, new LiaisonOpeningPolicy(), liaisons, versions, perimetres, elements, outbox, Clock.fixed(T, ZoneOffset.UTC), instanciation, conduite);
        lenient().when(participants.getInstitution(EP_A)).thenReturn(
            new InstitutionView(EP_A, "Établissement de paiement A", "ADHERENT", "ETABLISSEMENT_PAIEMENT", List.of(new InstitutionView.ServiceSouscrit("PAIEMENT_MOBILE", "2026-09-01"), new InstitutionView.ServiceSouscrit("TRANSVERSE", null)))
        );
        lenient().when(participants.getInstitution(BANQUE_B)).thenReturn(new InstitutionView(BANQUE_B, "Banque B", "ADHERENT", "BANQUE", List.of()));
        lenient().when(collaborateurs.findCollaborator(CONDUCTEUR)).thenReturn(Optional.of(new CollaboratorView(CONDUCTEUR, "Conducteur", Set.of("ROLE_CONDUCTEUR_PROJET"), null, true)));
        lenient().when(versions.findFirstByTypeProjetAndEtat(TypeProjet.PAIEMENT_MOBILE, EtatVersionReferentiel.PUBLIEE)).thenReturn(Optional.of(avecId(ReferentialVersionSamples.publiee(TypeProjet.PAIEMENT_MOBILE, 2))));
        lenient().when(liaisons.saveAndFlush(any())).thenAnswer(i -> {
            Liaison l = i.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        lenient().when(perimetres.save(any())).thenAnswer(i -> {
            ContractualScope p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
    }

    static cm.gimac.des.homologation.domain.ReferentialVersion avecId(cm.gimac.des.homologation.domain.ReferentialVersion v) {
        v.setId(UUID.randomUUID());
        return v;
    }

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Ouverture sur un adhérent : OUVERTE, version en vigueur, périmètre TRANSVERSE et services souscrits, événement")
    void nominal() {
        Liaison l = service.ouvrir(new OpenLiaisonCommand(EP_A, TypeProjet.PAIEMENT_MOBILE, CONDUCTEUR, BANQUE_B), CHEF);
        assertThat(l.getEtat()).isEqualTo(EtatLiaison.OUVERTE);
        assertThat(l.getTypeProjet()).isEqualTo(TypeProjet.PAIEMENT_MOBILE);
        assertThat(l.getBanquePartenaireId()).isEqualTo(BANQUE_B);
        assertThat(l.getInstitutionNom()).isEqualTo("Établissement de paiement A");
        assertThat(l.getDateOuverture()).isEqualTo(T);
        assertThat(l.getVersion().getNumero()).isEqualTo(2);
        ArgumentCaptor<ScopeItem> e = ArgumentCaptor.forClass(ScopeItem.class);
        verify(elements, times(2)).save(e.capture());
        assertThat(e.getAllValues()).extracting(ScopeItem::getServiceCode).containsExactly("TRANSVERSE", "PAIEMENT_MOBILE");
        assertThat(e.getAllValues()).allMatch(x -> x.getEtat() == EtatScopeItem.A_ENGAGER);
        ArgumentCaptor<LiaisonOpenedPayload> p = ArgumentCaptor.forClass(LiaisonOpenedPayload.class);
        verify(outbox).publish(eq("liaison"), eq(l.getId()), eq(LiaisonOpenedPayload.TYPE), p.capture());
        assertThat(p.getValue().institutionId()).isEqualTo(EP_A);
        assertThat(p.getValue().perimetre()).containsExactly("TRANSVERSE", "PAIEMENT_MOBILE");
        assertThat(p.getValue().ouvertPar()).isEqualTo(CHEF.id());
    }

    @Test
    @DisplayName("D-145 : périmètre défini par le gestionnaire à la création, journalisé en version 1 et publié ; refus sans enregistrement")
    void perimetreDefiniALaCreation() {
        cm.gimac.des.homologation.repository.PerimetreModificationRepository journal = org.mockito.Mockito.mock(cm.gimac.des.homologation.repository.PerimetreModificationRepository.class);
        service.setJournal(journal);
        UUID ep = UUID.randomUUID();
        when(participants.getInstitution(ep)).thenReturn(
            new InstitutionView(ep, "EP C", "ADHERENT", "ETABLISSEMENT_PAIEMENT", List.of(new InstitutionView.ServiceSouscrit("W2W", null), new InstitutionView.ServiceSouscrit("ISO.W2W", null), new InstitutionView.ServiceSouscrit("CRT.NET", null)))
        );
        Liaison l = service.ouvrir(new OpenLiaisonCommand(ep, TypeProjet.PAIEMENT_MOBILE, CONDUCTEUR, null, null, null, List.of("ISO.W2W")), CHEF);
        ArgumentCaptor<ScopeItem> e = ArgumentCaptor.forClass(ScopeItem.class);
        verify(elements, times(2)).save(e.capture());
        assertThat(e.getAllValues()).extracting(ScopeItem::getServiceCode).containsExactly("TRANSVERSE", "ISO.W2W");
        ArgumentCaptor<LiaisonOpenedPayload> p = ArgumentCaptor.forClass(LiaisonOpenedPayload.class);
        verify(outbox).publish(eq("liaison"), eq(l.getId()), eq(LiaisonOpenedPayload.TYPE), p.capture());
        assertThat(p.getValue().perimetre()).containsExactly("TRANSVERSE", "ISO.W2W");
        ArgumentCaptor<cm.gimac.des.homologation.domain.PerimetreModification> m = ArgumentCaptor.forClass(cm.gimac.des.homologation.domain.PerimetreModification.class);
        verify(journal).save(m.capture());
        assertThat(m.getValue().getVersionPerimetre()).isEqualTo(1);
        assertThat(m.getValue().getAuteurId()).isEqualTo(CHEF.id());
        assertThat(m.getValue().getApres()).isEqualTo("TRANSVERSE,ISO.W2W");

        refus(() -> service.ouvrir(new OpenLiaisonCommand(ep, TypeProjet.PAIEMENT_MOBILE, CONDUCTEUR, null, null, null, List.of("BILL")), CHEF), "HOM_PERIMETRE_SERVICE_NON_SOUSCRIT");
        verify(liaisons, times(1)).saveAndFlush(any());

        var proposable = service.perimetreProposable(ep, TypeProjet.PAIEMENT_MOBILE);
        assertThat(proposable.souscrits()).isTrue();
        assertThat(proposable.services()).extracting(cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase.ServiceProposable::code).containsExactly("W2W", "ISO.W2W", "CRT.NET");
    }

    @Test
    @DisplayName("D-104 : deux liaisons actives du même type pour la même institution, distinguées par l'intitulé")
    void plusieursLiaisonsDuMemeType() {
        Liaison lot1 = service.ouvrir(new OpenLiaisonCommand(EP_A, TypeProjet.PAIEMENT_MOBILE, CONDUCTEUR, null, " Projet Mobile EP A Lot 1 ", null), CHEF);
        Liaison lot2 = service.ouvrir(new OpenLiaisonCommand(EP_A, TypeProjet.PAIEMENT_MOBILE, CONDUCTEUR, null, "Projet Mobile EP A Lot 2", "  "), CHEF);
        assertThat(lot1.getId()).isNotEqualTo(lot2.getId());
        assertThat(lot1.getIntituleProjet()).isEqualTo("Projet Mobile EP A Lot 1");
        assertThat(lot2.getIntituleProjet()).isEqualTo("Projet Mobile EP A Lot 2");
        assertThat(lot2.getReferenceOrigine()).isNull();
        verify(liaisons, never()).existsByReferenceOrigine(any());
        verify(outbox, times(2)).publish(eq("liaison"), any(UUID.class), eq(LiaisonOpenedPayload.TYPE), any());
    }

    @Test
    @DisplayName("D-104 : référence d'origine retenue sans espaces ; déjà portée par une liaison : HOM_LIAISON_REFERENCE_ORIGINE_DUPLICATE, rien créé")
    void referenceOrigine() {
        Liaison l = service.ouvrir(new OpenLiaisonCommand(EP_A, TypeProjet.PAIEMENT_MOBILE, CONDUCTEUR, null, "Projet Mobile", " 202604-BCEG-01 "), CHEF);
        assertThat(l.getReferenceOrigine()).isEqualTo("202604-BCEG-01");
        org.mockito.Mockito.clearInvocations(liaisons, outbox);
        when(liaisons.existsByReferenceOrigine("202604-BCEG-01")).thenReturn(true);
        refus(() -> service.ouvrir(new OpenLiaisonCommand(EP_A, TypeProjet.CARTE_INTERBANCAIRE, CONDUCTEUR, null, null, "202604-BCEG-01"), CHEF), "HOM_LIAISON_REFERENCE_ORIGINE_DUPLICATE");
        verify(liaisons, never()).saveAndFlush(any());
        verifyNoInteractions(outbox);
    }

    @Test
    @DisplayName("Reprise concurrente de la même référence d'origine : l'index unique donne le même motif ; sans référence, l'erreur remonte")
    void concurrence() {
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("uk_liaison_reference_origine")).when(liaisons).saveAndFlush(any());
        refus(() -> service.ouvrir(new OpenLiaisonCommand(EP_A, TypeProjet.PAIEMENT_MOBILE, CONDUCTEUR, null, null, "202604-BCEG-01"), CHEF), "HOM_LIAISON_REFERENCE_ORIGINE_DUPLICATE");
        assertThatThrownBy(() -> service.ouvrir(new OpenLiaisonCommand(EP_A, TypeProjet.PAIEMENT_MOBILE, CONDUCTEUR, null), CHEF)).isInstanceOf(DataIntegrityViolationException.class);
        verifyNoInteractions(outbox);
    }

    @Test
    @DisplayName("Sans version publiée, sans droit de désignation, conducteur non habilité : refus")
    void refus() {
        refus(() -> service.ouvrir(new OpenLiaisonCommand(EP_A, TypeProjet.CARTE_INTERBANCAIRE, CONDUCTEUR, null), CHEF), "HOM_REFERENTIAL_NONE_PUBLISHED");
        refus(() -> service.ouvrir(new OpenLiaisonCommand(EP_A, TypeProjet.PAIEMENT_MOBILE, CONDUCTEUR, null), new Acteur(UUID.randomUUID(), false)), "HOM_PROJECT_DESIGNATION_FORBIDDEN");
        refus(() -> service.ouvrir(new OpenLiaisonCommand(EP_A, TypeProjet.PAIEMENT_MOBILE, UUID.randomUUID(), null), CHEF), "HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED");
        verifyNoInteractions(outbox);
    }

    @Test
    @DisplayName("Désignation d'un autre conducteur : événement ; même conducteur : sans effet ; liaison close ou inconnue : refus")
    void designation() {
        UUID autre = UUID.randomUUID();
        when(collaborateurs.findCollaborator(autre)).thenReturn(Optional.of(new CollaboratorView(autre, "Autre", Set.of("ROLE_CONDUCTEUR_PROJET"), null, true)));
        Liaison l = new Liaison().etat(EtatLiaison.OUVERTE).chefDeProjetId(CONDUCTEUR).institutionId(EP_A);
        l.setId(UUID.randomUUID());
        when(liaisons.findByIdForUpdate(l.getId())).thenReturn(Optional.of(l));
        service.designerChefDeProjet(l.getId(), autre, "Réaffectation", CHEF);
        assertThat(l.getChefDeProjetId()).isEqualTo(autre);
        verify(outbox).publish(eq("liaison"), eq(l.getId()), eq(ProjectLeadDesignatedPayload.TYPE), eq(new ProjectLeadDesignatedPayload(l.getId(), EP_A, CONDUCTEUR, autre, CHEF.id(), "Réaffectation")));
        service.designerChefDeProjet(l.getId(), autre, null, CHEF);
        verify(outbox, times(1)).publish(any(), any(UUID.class), any(), any());
        refus(() -> service.designerChefDeProjet(l.getId(), autre, null, new Acteur(UUID.randomUUID(), false)), "HOM_PROJECT_DESIGNATION_FORBIDDEN");
        l.setEtat(EtatLiaison.CLOSE);
        refus(() -> service.designerChefDeProjet(l.getId(), autre, null, CHEF), "HOM_LIAISON_CLOSED");
        refus(() -> service.designerChefDeProjet(UUID.randomUUID(), autre, null, CHEF), "HOM_LIAISON_NOT_FOUND");
    }
}
