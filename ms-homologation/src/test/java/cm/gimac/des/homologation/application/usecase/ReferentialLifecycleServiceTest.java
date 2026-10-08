package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperationSamples;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.ReferentialVersionSamples;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.event.CatalogPublishedPayload;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferentialLifecycleServiceTest {

    @Mock
    ReferentialVersionRepository versions;

    @Mock
    ReferentialGraphValidator graphValidator;

    @Mock
    OutboxPublisher outboxPublisher;

    ReferentialLifecycleService service;

    @BeforeEach
    void init() {
        service = new ReferentialLifecycleService(
            versions,
            new ReferentialCompletenessChecker(),
            graphValidator,
            outboxPublisher,
            Clock.fixed(Instant.parse("2026-10-05T08:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("HOM-01 : la publication numérote à la suite, retire la version précédente et écrit l'outbox")
    void publicationNumeroteRetireEtPublie() {
        ReferentialVersion precedente = ReferentialVersionSamples.publiee(TypeProjet.DELEGATAIRE, 2);
        ReferentialVersion brouillon = ReferentialVersionSamples.brouillonComplet(TypeProjet.DELEGATAIRE);
        when(versions.findByIdForUpdate(brouillon.getId())).thenReturn(Optional.of(brouillon));
        when(graphValidator.validate(anyList(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(List.of("DL-AMONT-01", "DL-BR-01", "DL-AVAL-01"));
        when(versions.findFirstByTypeProjetAndEtat(TypeProjet.DELEGATAIRE, EtatVersionReferentiel.PUBLIEE)).thenReturn(Optional.of(precedente));
        when(versions.maxNumero(TypeProjet.DELEGATAIRE)).thenReturn(Optional.of(2));

        ReferentialVersion publiee = service.publier(brouillon.getId());

        assertThat(publiee.getNumero()).isEqualTo(3);
        assertThat(publiee.getEtat()).isEqualTo(EtatVersionReferentiel.PUBLIEE);
        assertThat(publiee.ordreTopologiqueListe()).containsExactly("DL-AMONT-01", "DL-BR-01", "DL-AVAL-01");
        assertThat(precedente.getEtat()).isEqualTo(EtatVersionReferentiel.RETIREE);
        verify(outboxPublisher).publish(eq("catalog"), eq(brouillon.getId().toString()), eq(CatalogPublishedPayload.TYPE), any(CatalogPublishedPayload.class));
    }

    @Test
    @DisplayName("HOM-01 : une publication incomplète est refusée sans contrôle du graphe ni outbox")
    void publicationIncompleteRefusee() {
        ReferentialVersion brouillon = ReferentialVersion.nouveauBrouillon(TypeProjet.PAIEMENT_MOBILE, "Mobile", Instant.EPOCH);
        brouillon.setId(java.util.UUID.randomUUID());
        brouillon.upsertOperation("PM-BR-04", ReferenceOperationSamples.complete(Segment.BRANCHE).sansPartie().sansPreuveAttendue().data());
        when(versions.findByIdForUpdate(brouillon.getId())).thenReturn(Optional.of(brouillon));

        assertThatThrownBy(() -> service.publier(brouillon.getId()))
            .isInstanceOfSatisfying(HomologationException.class, ex -> assertThat(ex.code()).isEqualTo("HOM_REFERENTIAL_INCOMPLETE"));
        verifyNoInteractions(graphValidator, outboxPublisher);
        assertThat(brouillon.getEtat()).isEqualTo(EtatVersionReferentiel.BROUILLON);
    }
}
