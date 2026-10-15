package cm.gimac.des.homologation.application.usecase;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.BlocageParcours;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.repository.BlocageParcoursRepository;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.OperationParcoursRepository;
import cm.gimac.des.homologation.repository.ParcoursLiaisonRepository;
import cm.gimac.des.homologation.repository.ServiceParcoursRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Chargement et enregistrement de l'agrégat du parcours, taux repris du simulateur compris. */
@ExtendWith(MockitoExtension.class)
class ParcoursDepotTest {

    @Mock
    LiaisonRepository liaisons;

    @Mock
    ParcoursLiaisonRepository etats;

    @Mock
    OperationParcoursRepository operations;

    @Mock
    ServiceParcoursRepository services;

    @Mock
    BlocageParcoursRepository blocages;

    @Mock
    CoverageRepository couvertures;

    @Mock
    EntityManager em;

    ParcoursDepot depot;
    Parcours p;

    @BeforeEach
    void init() {
        depot = new ParcoursDepot(liaisons, etats, operations, services, blocages, couvertures, em);
        p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT", "PAIEMENT");
    }

    Coverage couverture(String service, int taux, boolean calculable) {
        Coverage c = new Coverage().serviceCode(service).nature(NatureCouverture.CERTIFICATION).calculable(calculable).taux(BigDecimal.valueOf(taux)).liaison(p.liaison());
        return c;
    }

    @Test
    @DisplayName("Parcours d'une liaison, verrouillé ou non ; taux repris du simulateur calculables seulement")
    void trouver() {
        UUID id = p.id();
        when(liaisons.findByIdForUpdate(id)).thenReturn(Optional.of(p.liaison()));
        when(etats.findById(id)).thenReturn(Optional.of(p.etat()));
        when(operations.findByLiaisonIdOrderByOrdre(id)).thenReturn(p.operations());
        when(services.findByLiaisonIdOrderByServiceCode(id)).thenReturn(p.services());
        when(blocages.findByLiaisonId(id)).thenReturn(List.of());
        when(couvertures.findByLiaison_IdInAndNature(List.of(id), NatureCouverture.CERTIFICATION)).thenReturn(List.of(couverture("RETRAIT", 100, true), couverture("PAIEMENT", 50, false)));
        Parcours charge = depot.trouver(id, true).orElseThrow();
        assertThat(charge.operations()).hasSize(43);
        assertThat(charge.mesures()).containsOnlyKeys("RETRAIT");
        when(liaisons.findById(id)).thenReturn(Optional.empty());
        assertThat(depot.trouver(id, false)).isEmpty();
        assertThatThrownBy(() -> depot.liaison(id)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_LIAISON_NOT_FOUND"));
    }

    @Test
    @DisplayName("Opération et blocage inconnus : 404 ; parcours de l'opération et du blocage")
    void parOperationEtBlocage() {
        UUID inconnu = UUID.randomUUID();
        when(operations.liaisonDe(inconnu)).thenReturn(Optional.empty());
        when(blocages.liaisonDe(inconnu)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> depot.deLOperation(inconnu)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_OPERATION_NOT_FOUND"));
        assertThatThrownBy(() -> depot.duBlocage(inconnu)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_BLOCKAGE_NOT_FOUND"));
        var o = p.operations().get(0);
        when(operations.liaisonDe(o.getId())).thenReturn(Optional.of(p.id()));
        when(liaisons.findByIdForUpdate(p.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> depot.deLOperation(o.getId())).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_LIAISON_NOT_FOUND"));
        // D-109 : même lecture sans verrou (pièces, chronologie).
        when(liaisons.findById(p.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> depot.deLOperationLecture(o.getId())).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_LIAISON_NOT_FOUND"));
        assertThatThrownBy(() -> depot.deLOperationLecture(inconnu)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_OPERATION_NOT_FOUND"));
        BlocageParcours b = new BlocageParcours(p.id(), "m", LocalDate.EPOCH, "a", new Personne("u", "n", Fonction.DDRM), LocalDate.EPOCH, EtapeCode.CADRAGE, null);
        when(blocages.liaisonDe(b.getId())).thenReturn(Optional.of(p.id()));
        assertThatThrownBy(() -> depot.duBlocage(b.getId())).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.status()).isEqualTo(404));
    }

    @Test
    @DisplayName("Tous les parcours et ceux d'une institution : lectures groupées ; institution invalide : aucun")
    void tous() {
        UUID id = p.id();
        Parcours orphelin = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        when(etats.findAll()).thenReturn(List.of(p.etat(), orphelin.etat()));
        when(liaisons.findAllById(anyCollection())).thenReturn(List.of(p.liaison()));
        when(operations.findByLiaisonIdIn(anyCollection())).thenReturn(p.operations());
        when(services.findByLiaisonIdIn(anyCollection())).thenReturn(p.services());
        when(blocages.findByLiaisonIdIn(anyCollection())).thenReturn(List.of());
        when(couvertures.findByLiaison_IdInAndNature(anyCollection(), eq(NatureCouverture.CERTIFICATION))).thenReturn(List.of(couverture("RETRAIT", 80, true)));
        List<Parcours> l = depot.tous();
        assertThat(l).hasSize(1);
        assertThat(l.get(0).mesures()).containsEntry("RETRAIT", 80);
        assertThat(depot.deLInstitution("pas-un-uuid")).isEmpty();
        assertThat(depot.deLInstitution(null)).isEmpty();
        Liaison liaison = p.liaison();
        when(liaisons.findByInstitutionId(liaison.getInstitutionId())).thenReturn(List.of(liaison));
        when(etats.findAllById(List.of(id))).thenReturn(List.of());
        assertThat(depot.deLInstitution(liaison.getInstitutionId().toString())).isEmpty();
    }

    @Test
    @DisplayName("Création et ajout d'un blocage par le contexte de persistance ; numéro de référence ; existence")
    void ecritures() {
        depot.creer(p);
        verify(em).persist(p.etat());
        verify(em, times(1 + 43 + 2)).persist(any());
        BlocageParcours b = new BlocageParcours(p.id(), "m", LocalDate.EPOCH, "a", new Personne("u", "n", Fonction.DDRM), LocalDate.EPOCH, EtapeCode.CADRAGE, null);
        depot.ajouter(b);
        verify(em).persist(b);
        when(etats.prochainNumero()).thenReturn(12L);
        assertThat(depot.prochainNumero()).isEqualTo(12L);
        when(etats.existsById(p.id())).thenReturn(true);
        assertThat(depot.existe(p.id())).isTrue();
    }
}
