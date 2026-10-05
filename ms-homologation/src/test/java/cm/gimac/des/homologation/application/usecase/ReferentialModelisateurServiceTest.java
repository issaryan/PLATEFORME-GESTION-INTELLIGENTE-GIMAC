package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperationSamples;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Position;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ReferenceOperationRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Modélisateur et tâche du pilote (D-77, §4 et §5) côté cas d'utilisation. */
class ReferentialModelisateurServiceTest {

    static final UUID CREATEUR = UUID.randomUUID();
    static final UUID INGENIEUR = UUID.randomUUID();
    static final UUID AUTRE = UUID.randomUUID();

    ReferentialVersionRepository versions;
    ReferenceOperationRepository operations;
    CollaboratorDirectoryPort annuaire;
    DemandesTaches taches;
    ReferentialLifecycleService service;

    @BeforeEach
    void init() {
        versions = mock(ReferentialVersionRepository.class);
        operations = mock(ReferenceOperationRepository.class);
        annuaire = mock(CollaboratorDirectoryPort.class);
        taches = mock(DemandesTaches.class);
        service = new ReferentialLifecycleService(
            versions,
            new ReferentialCompletenessChecker(),
            new ReferentialGraphValidator(),
            mock(OutboxPublisher.class),
            Clock.fixed(Instant.parse("2026-10-05T08:00:00Z"), ZoneOffset.UTC),
            operations,
            annuaire,
            taches,
            List.of("ROLE_INGENIEUR_HOMOLOGATION")
        );
        when(versions.save(any())).thenAnswer(i -> {
            ReferentialVersion v = i.getArgument(0);
            if (v.getId() == null) {
                v.setId(UUID.randomUUID());
            }
            return v;
        });
        lenient().when(versions.maxNumero(any())).thenReturn(Optional.of(2));
        lenient().when(annuaire.findCollaborator(INGENIEUR)).thenReturn(Optional.of(new CollaboratorView(INGENIEUR, "Ingénieure", Set.of("ROLE_INGENIEUR_HOMOLOGATION"), null, true)));
        lenient().when(annuaire.findCollaborator(AUTRE)).thenReturn(Optional.of(new CollaboratorView(AUTRE, "Autre", Set.of("ROLE_SNA"), null, true)));
    }

    ReferentialVersion enregistree(ReferentialVersion v) {
        when(versions.findByIdForUpdate(v.getId())).thenReturn(Optional.of(v));
        return v;
    }

    @Test
    @DisplayName("Brouillon : le créateur pilote par défaut et reçoit la tâche ; un pilote désigné doit pouvoir éditer ; sans créateur, aucune tâche")
    void brouillonEtPilote() {
        ReferentialVersion v = service.creerBrouillon(TypeProjet.PAIEMENT_MOBILE, "Mobile v3", null, CREATEUR, null);
        assertThat(v.getPiloteId()).isEqualTo(CREATEUR);
        ArgumentCaptor<PilotageTachesPort.Tache> t = ArgumentCaptor.forClass(PilotageTachesPort.Tache.class);
        verify(taches).creer(t.capture());
        assertThat(t.getValue().titre()).isEqualTo("Compléter et publier le référentiel Paiement mobile v3");
        assertThat(t.getValue().responsableId()).isEqualTo(CREATEUR.toString());
        assertThat(t.getValue().createurId()).isEqualTo(CREATEUR.toString());
        assertThat(t.getValue().origine()).isEqualTo(new PilotageTachesPort.Origine("REFERENTIEL", "referentiel/" + v.getId(), "homologation", "/des/referentiels/operations?version=" + v.getId()));

        assertThat(service.creerBrouillon(TypeProjet.DELEGATAIRE, "D", null, CREATEUR, INGENIEUR).getPiloteId()).isEqualTo(INGENIEUR);
        assertThatThrownBy(() -> service.creerBrouillon(TypeProjet.DELEGATAIRE, "D", null, CREATEUR, AUTRE))
            .isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_PILOTE_NOT_ELIGIBLE"));
        assertThat(service.creerBrouillon(TypeProjet.CARTE_INTERBANCAIRE, "C", null).getPiloteId()).isNull();
        assertThat(ReferentialLifecycleService.libelle(TypeProjet.CARTE_INTERBANCAIRE)).isEqualTo("Carte interbancaire");
        assertThat(ReferentialLifecycleService.libelle(TypeProjet.DELEGATAIRE)).isEqualTo("Délégataire");
    }

    @Test
    @DisplayName("Changement de pilote : tâche de l'ancien terminée, tâche du nouveau ; même pilote : rien ; publication : tâche terminée")
    void changementEtPublication() {
        ReferentialVersion v = enregistree(service.creerBrouillon(TypeProjet.DELEGATAIRE, "D", null, CREATEUR, null));
        service.designerPilote(v.getId(), CREATEUR, CREATEUR);
        verify(taches, never()).cloturer(any());
        service.designerPilote(v.getId(), INGENIEUR, CREATEUR);
        verify(taches).cloturer(new PilotageTachesPort.Designation(new PilotageTachesPort.Origine("REFERENTIEL", "referentiel/" + v.getId(), "homologation", "/des/referentiels/operations?version=" + v.getId()), CREATEUR.toString()));
        assertThat(v.getPiloteId()).isEqualTo(INGENIEUR);

        v.upsertOperation("DL-A", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AMONT).data());
        when(versions.findFirstByTypeProjetAndEtat(TypeProjet.DELEGATAIRE, EtatVersionReferentiel.PUBLIEE)).thenReturn(Optional.empty());
        service.publier(v.getId());
        verify(taches).cloturer(new PilotageTachesPort.Designation(new PilotageTachesPort.Origine("REFERENTIEL", "referentiel/" + v.getId(), "homologation", "/des/referentiels/operations?version=" + v.getId()), INGENIEUR.toString()));
    }

    @Test
    @DisplayName("Opérations : création, retrait (supprimée en base), positions ; arc qui fermerait un cycle : 409 HOM_REFERENTIAL_CYCLE")
    void operationsEtArcs() {
        ReferentialVersion v = enregistree(service.creerBrouillon(TypeProjet.DELEGATAIRE, "D", null));
        service.creerOperation(v.getId(), "A", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AMONT).data());
        service.creerOperation(v.getId(), "B", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AMONT).avecPredecesseurs("A").data());
        service.creerOperation(v.getId(), "C", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AMONT).avecPredecesseurs("B").data());
        assertThatThrownBy(() -> service.arc(v.getId(), "C", "A", true)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_REFERENTIAL_CYCLE"));
        assertThat(service.arc(v.getId(), "A", "C", true).predecesseursListe()).containsExactly("B", "A");
        assertThat(service.arc(v.getId(), "B", "C", false).predecesseursListe()).containsExactly("A");
        service.placer(v.getId(), Map.of("A", new Position(5, 6)));
        assertThat(v.operation("A").orElseThrow().getPosition()).isEqualTo(new Position(5, 6));
        service.supprimerOperation(v.getId(), "B");
        verify(operations).delete(any());
        assertThat(v.operation("B")).isEmpty();
        assertThatThrownBy(() -> service.creerOperation(UUID.randomUUID(), "Z", ReferenceOperationSamples.complete(Segment.BRANCHE).data()))
            .isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_REFERENTIAL_NOT_FOUND"));
        verify(annuaire, never()).findCollaborator(any());
    }
}
