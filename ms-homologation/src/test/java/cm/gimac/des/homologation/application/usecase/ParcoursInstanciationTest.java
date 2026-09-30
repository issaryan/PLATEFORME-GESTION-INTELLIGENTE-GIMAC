package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.ReferentielOperation;
import cm.gimac.des.homologation.domain.parcours.ServiceParcours;
import cm.gimac.des.homologation.repository.ReferentielOperationRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Instanciation du référentiel de l'annexe A à l'ouverture d'une liaison. */
@ExtendWith(MockitoExtension.class)
class ParcoursInstanciationTest {

    @Mock
    ReferentielOperationRepository referentiel;

    @Mock
    ScopeItemRepository elements;

    @Mock
    ParcoursDepot depot;

    @Mock
    Intervenants intervenants;

    ParcoursInstanciation instanciation;

    @BeforeEach
    void init() {
        instanciation = new ParcoursInstanciation(referentiel, elements, depot, intervenants, Clock.fixed(Instant.parse("2026-10-06T09:00:00Z"), ZoneOffset.UTC));
        when(referentiel.findByVersionCrOrderByOrdre(ReferentielOperation.VERSION_CR)).thenReturn(ParcoursSamples.referentiel());
        when(depot.prochainNumero()).thenReturn(7L);
        when(intervenants.titulaire(any())).thenAnswer(i -> Personne.aAffecter(i.getArgument(0)));
    }

    @Test
    @DisplayName("Référence lisible, périmètre sans TRANSVERSE, qualités par fonction (conducteur, correspondant, personnalisateur)")
    void instancier() {
        Liaison l = ParcoursSamples.liaison(TypeProjet.DELEGATAIRE);
        when(intervenants.nom(l.getChefDeProjetId())).thenReturn(null);
        Parcours p = instanciation.instancier(l, List.of("TRANSVERSE", "RETRAIT", "RETRAIT", " "), true, null, false);
        verify(depot).creer(p);
        assertThat(p.etat().getReference()).isEqualTo("LIA-2026-007");
        assertThat(p.etat().isFintech()).isTrue();
        assertThat(p.services()).extracting(ServiceParcours::getServiceCode).containsExactly("RETRAIT");
        assertThat(p.operation("D1").orElseThrow().getRaci().r().nom()).isEqualTo("Conducteur de projet");
        assertThat(p.operation("D1").orElseThrow().getRaci().e()).extracting(Personne::fonction).containsExactly(Fonction.PERSONNALISATEUR);
        assertThat(p.operation("3").orElseThrow().getRaci().e()).extracting(Personne::fonction).containsExactly(Fonction.PARTICIPANT);
        assertThat(p.operation("6").orElseThrow().getRaci().r()).isEqualTo(Personne.aAffecter(Fonction.CHEF_DES));
    }

    @Test
    @DisplayName("Liaison ouverte avant cette tranche : instanciée depuis son périmètre contractualisé, hors services écartés")
    void existante() {
        Liaison l = ParcoursSamples.liaison(TypeProjet.CARTE_INTERBANCAIRE);
        l.liaisonOrigineId(UUID.randomUUID());
        ContractualScope s = new ContractualScope();
        s.setId(UUID.randomUUID());
        l.setContractualScope(s);
        when(intervenants.nom(l.getChefDeProjetId())).thenReturn("Aminata Ngono");
        when(elements.findByScope_IdOrderByServiceCode(s.getId())).thenReturn(
            List.of(new ScopeItem().serviceCode("RETRAIT").etat(EtatScopeItem.A_ENGAGER), new ScopeItem().serviceCode("TRANSFERT").etat(EtatScopeItem.HORS_PERIMETRE))
        );
        Parcours p = instanciation.instancierExistante(l);
        assertThat(p.services()).extracting(ServiceParcours::getServiceCode).containsExactly("RETRAIT");
        assertThat(p.etat().getLiaisonOrigineId()).isEqualTo(l.getLiaisonOrigineId());
        assertThat(p.operation("18").orElseThrow().getRaci().r().nom()).isEqualTo("Aminata Ngono");
        Liaison sansPerimetre = ParcoursSamples.liaison(TypeProjet.PAIEMENT_MOBILE).dateOuverture(null);
        assertThat(instanciation.instancierExistante(sansPerimetre).services()).isEmpty();
    }
}
