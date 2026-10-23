package cm.gimac.des.homologation.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.usecase.sandbox.GelLiaisonsInstitution.Effet;
import cm.gimac.des.homologation.application.usecase.sandbox.GelLiaisonsInstitution.Resultat;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.sandbox.StatutInstitutionApplique;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.StatutInstitutionAppliqueRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** HOM-06 : gel des liaisons d'une institution suspendue ou retirée, rétablissement à son retour. */
class GelLiaisonsInstitutionTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");
    static final UUID INSTITUTION = UUID.randomUUID();

    StatutInstitutionAppliqueRepository statuts;
    LiaisonRepository liaisons;
    PublicationEtatsLiaison publication;
    GelLiaisonsInstitution gel;
    Liaison active;
    Liaison close;
    Liaison dejaGelee;

    static Liaison liaison(EtatLiaison etat) {
        return new Liaison().id(UUID.randomUUID()).institutionId(INSTITUTION).etat(etat);
    }

    @BeforeEach
    void init() {
        statuts = mock(StatutInstitutionAppliqueRepository.class);
        liaisons = mock(LiaisonRepository.class);
        publication = mock(PublicationEtatsLiaison.class);
        gel = new GelLiaisonsInstitution(statuts, liaisons, publication, Clock.fixed(T, ZoneOffset.UTC));
        active = liaison(EtatLiaison.EN_HOMOLOGATION);
        close = liaison(EtatLiaison.CLOSE);
        dejaGelee = liaison(EtatLiaison.GELEE);
        when(liaisons.findByInstitutionId(INSTITUTION)).thenReturn(List.of(active, close, dejaGelee));
        for (Liaison l : List.of(active, close, dejaGelee)) {
            when(liaisons.findByIdForUpdate(l.getId())).thenReturn(Optional.of(l));
        }
        when(statuts.verrouiller(INSTITUTION)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Institution suspendue : liaisons actives gelées, état d'avant le gel et motif retenus, publication observée")
    void gelSurSuspension() {
        Resultat r = gel.appliquer(INSTITUTION, "SUSPENDU", 3L, T, "Défaut de paiement");

        assertThat(r).isEqualTo(new Resultat(Effet.GEL, 1));
        assertThat(active.getEtat()).isEqualTo(EtatLiaison.GELEE);
        assertThat(active.getEtatAvantGel()).contains("\"etat\":\"EN_HOMOLOGATION\"").contains("\"statutInstitution\":\"SUSPENDU\"");
        assertThat(active.getMotifEtat()).isEqualTo("Liaison gelée : institution SUSPENDU au registre (Défaut de paiement)");
        assertThat(close.getEtat()).isEqualTo(EtatLiaison.CLOSE);
        verify(publication).observer(active);
        verify(publication, never()).observer(close);
        verify(publication, never()).observer(dejaGelee);
        ArgumentCaptor<StatutInstitutionApplique> s = ArgumentCaptor.forClass(StatutInstitutionApplique.class);
        verify(statuts).save(s.capture());
        assertThat(s.getValue().getVersionStatut()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Institution retirée sans motif : motif de gel sans parenthèse ; motif très long tronqué à 500")
    void gelSansMotifEtMotifLong() {
        gel.appliquer(INSTITUTION, "RETIRE", null, T, "  ");
        assertThat(active.getMotifEtat()).isEqualTo("Liaison gelée : institution RETIRE au registre");
        Liaison autre = liaison(EtatLiaison.OUVERTE);
        when(liaisons.findByInstitutionId(INSTITUTION)).thenReturn(List.of(autre));
        when(liaisons.findByIdForUpdate(autre.getId())).thenReturn(Optional.of(autre));
        gel.appliquer(INSTITUTION, "SUSPENDU", null, T.plusSeconds(1), null);
        assertThat(autre.getMotifEtat()).isEqualTo("Liaison gelée : institution SUSPENDU au registre");
        Liaison longue = liaison(EtatLiaison.OUVERTE);
        when(liaisons.findByInstitutionId(INSTITUTION)).thenReturn(List.of(longue));
        when(liaisons.findByIdForUpdate(longue.getId())).thenReturn(Optional.of(longue));
        gel.appliquer(INSTITUTION, "SUSPENDU", null, T.plusSeconds(2), "x".repeat(600));
        assertThat(longue.getMotifEtat()).hasSize(500);
    }

    @Test
    @DisplayName("Retour à un statut admis : chaque liaison gelée retrouve l'état d'avant le gel")
    void degel() {
        gel.appliquer(INSTITUTION, "SUSPENDU", 3L, T, null);
        when(statuts.verrouiller(INSTITUTION)).thenReturn(Optional.of(new StatutInstitutionApplique(INSTITUTION, "SUSPENDU", 3L, T, T)));

        Resultat r = gel.appliquer(INSTITUTION, "ACTIF", 4L, T.plusSeconds(60), null);

        assertThat(r).isEqualTo(new Resultat(Effet.DEGEL, 1));
        assertThat(active.getEtat()).isEqualTo(EtatLiaison.EN_HOMOLOGATION);
        assertThat(active.getEtatAvantGel()).isNull();
        assertThat(active.getMotifEtat()).isNull();
        // Liaison gelée sans état retenu (gel antérieur à HOM-06) : laissée telle quelle.
        assertThat(dejaGelee.getEtat()).isEqualTo(EtatLiaison.GELEE);
    }

    @Test
    @DisplayName("Événement périmé (version ou date antérieure) : sans effet")
    void perime() {
        StatutInstitutionApplique connu = new StatutInstitutionApplique(INSTITUTION, "ACTIF", 5L, T, T);
        when(statuts.verrouiller(INSTITUTION)).thenReturn(Optional.of(connu));

        assertThat(gel.appliquer(INSTITUTION, "SUSPENDU", 4L, T.plusSeconds(60), null)).isEqualTo(new Resultat(Effet.PERIME, 0));
        assertThat(active.getEtat()).isEqualTo(EtatLiaison.EN_HOMOLOGATION);
        assertThat(connu.getStatut()).isEqualTo("ACTIF");
        verify(publication, never()).observer(any());
        verify(liaisons, never()).findByInstitutionId(any());
    }

    @Test
    @DisplayName("Statut connu dépassé : retenu sur la ligne existante")
    void statutConnuMisAJour() {
        StatutInstitutionApplique connu = new StatutInstitutionApplique(INSTITUTION, "ACTIF", 5L, T, T);
        when(statuts.verrouiller(INSTITUTION)).thenReturn(Optional.of(connu));
        gel.appliquer(INSTITUTION, "SUSPENDU", 6L, T.plusSeconds(60), null);
        assertThat(connu.getStatut()).isEqualTo("SUSPENDU");
        assertThat(connu.getVersionStatut()).isEqualTo(6L);
        verify(statuts, never()).save(any());
    }
}
