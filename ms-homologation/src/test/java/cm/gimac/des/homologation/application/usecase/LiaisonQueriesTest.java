package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class LiaisonQueriesTest {

    @Mock
    LiaisonRepository liaisons;

    @Mock
    ScopeItemRepository elements;

    @Mock
    CollaboratorDirectoryPort collaborateurs;

    @Mock
    ParticipantDirectoryPort participants;

    static Liaison liaison(EtatLiaison etat, UUID conducteur, UUID banque) {
        Liaison l = new Liaison().etat(etat).typeProjet(TypeProjet.PAIEMENT_MOBILE).institutionId(UUID.randomUUID()).institutionNom("EP A").chefDeProjetId(conducteur).banquePartenaireId(banque);
        l.setId(UUID.randomUUID());
        return l;
    }

    @Test
    @DisplayName("Fiche : intervenants, périmètre, noms ; désignation offerte au seul détenteur de HOM_PROJECT_DESIGNATE")
    void fiche() {
        UUID conducteur = UUID.randomUUID();
        UUID banque = UUID.randomUUID();
        Liaison l = liaison(EtatLiaison.OUVERTE, conducteur, banque);
        ContractualScope p = new ContractualScope();
        p.setId(UUID.randomUUID());
        l.setContractualScope(p);
        when(liaisons.findById(l.getId())).thenReturn(Optional.of(l));
        when(elements.findByScope_IdOrderByServiceCode(p.getId())).thenReturn(List.of(new ScopeItem().serviceCode("PAIEMENT_MOBILE"), new ScopeItem().serviceCode("TRANSVERSE")));
        when(collaborateurs.findCollaborator(conducteur)).thenReturn(Optional.of(new CollaboratorView(conducteur, "Aminata Ngono", Set.of(), null, true)));
        when(participants.getInstitution(banque)).thenReturn(new InstitutionView(banque, "Banque B", "ADHERENT", "BANQUE", List.of()));
        var q = new LiaisonQueries(liaisons, elements, collaborateurs, participants);
        var f = q.fiche(l.getId(), Set.of("HOM_LIAISON_READ", "HOM_PROJECT_DESIGNATE"));
        assertThat(f.chefDeProjetNom()).isEqualTo("Aminata Ngono");
        assertThat(f.banquePartenaireNom()).isEqualTo("Banque B");
        assertThat(f.perimetre()).containsExactly("PAIEMENT_MOBILE", "TRANSVERSE");
        assertThat(f.intervenants()).extracting(LiaisonQueries.Intervenant::role).containsExactly("INSTITUTION", "BANQUE_PARTENAIRE", "CHEF_DE_PROJET");
        assertThat(f.actionsPermises()).containsExactly("DESIGNER_CHEF_DE_PROJET");
        assertThat(q.fiche(l.getId(), Set.of("HOM_LIAISON_READ")).actionsPermises()).isEmpty();
        l.setEtat(EtatLiaison.CLOSE);
        assertThat(q.fiche(l.getId(), Set.of("HOM_PROJECT_DESIGNATE")).actionsPermises()).isEmpty();
        UUID inconnue = UUID.randomUUID();
        assertThatThrownBy(() -> q.fiche(inconnue, Set.of())).isInstanceOf(HomologationException.class);
    }

    @Test
    @DisplayName("Liste : annuaires injoignables, la liste reste lisible (noms absents) ; conducteurs par profil")
    @SuppressWarnings("unchecked")
    void listeEtConducteurs() {
        UUID conducteur = UUID.randomUUID();
        UUID banque = UUID.randomUUID();
        when(liaisons.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(liaison(EtatLiaison.OUVERTE, conducteur, banque), liaison(EtatLiaison.OUVERTE, conducteur, null))));
        when(collaborateurs.findCollaborator(conducteur)).thenThrow(HomologationException.unavailable("HOM_IAM_UNAVAILABLE", "x"));
        when(participants.getInstitution(banque)).thenThrow(HomologationException.unavailable("HOM_PARTICIPANT_UNAVAILABLE", "x"));
        var q = new LiaisonQueries(liaisons, elements, collaborateurs, participants);
        var p = q.lister(new LiaisonQueries.Filtre(EtatLiaison.OUVERTE, TypeProjet.PAIEMENT_MOBILE, UUID.randomUUID(), "ep", conducteur), 0, 25, Set.of());
        assertThat(p.getContent()).hasSize(2).allMatch(f -> f.chefDeProjetNom() == null && f.perimetre().isEmpty());
        assertThat(p.getContent().get(0).banquePartenaireNom()).isNull();
        when(collaborateurs.parProfil(eq("ROLE_CONDUCTEUR_PROJET"))).thenReturn(List.of(new CollaboratorView(conducteur, "A", Set.of(), null, true)));
        assertThat(q.conducteurs()).hasSize(1);
        when(liaisons.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        assertThat(q.lister(new LiaisonQueries.Filtre(null, null, null, null, null), -1, 500, Set.of()).getContent()).isEmpty();
    }

    @Test
    @DisplayName("Filtre : un prédicat par critère renseigné ; recherche insensible à la casse ; aucun critère : aucun prédicat")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void filtre() {
        jakarta.persistence.criteria.Root racine = org.mockito.Mockito.mock(jakarta.persistence.criteria.Root.class);
        jakarta.persistence.criteria.CriteriaQuery requete = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaBuilder.class);
        LiaisonQueries.specification(new LiaisonQueries.Filtre(EtatLiaison.OUVERTE, TypeProjet.PAIEMENT_MOBILE, UUID.randomUUID(), " EP ", UUID.randomUUID())).toPredicate(racine, requete, cb);
        org.mockito.Mockito.verify(cb, org.mockito.Mockito.times(4)).equal(any(), any(Object.class));
        org.mockito.Mockito.verify(cb).like(any(), eq("%ep%"));
        org.mockito.Mockito.clearInvocations(cb);
        LiaisonQueries.specification(new LiaisonQueries.Filtre(null, null, null, " ", null)).toPredicate(racine, requete, cb);
        org.mockito.Mockito.verify(cb, org.mockito.Mockito.never()).equal(any(), any(Object.class));
        org.mockito.Mockito.verify(cb, org.mockito.Mockito.never()).like(any(), any(String.class));
    }

    @Test
    @DisplayName("Vues du registre et de l'annuaire : listes absentes lues comme vides")
    void vuesTolerantes() {
        assertThat(new InstitutionView(UUID.randomUUID(), "x", "ADHERENT", "BANQUE", null).servicesSouscrits()).isEmpty();
        assertThat(new CollaboratorView(UUID.randomUUID(), "x", null, null, true).profils()).isEmpty();
    }
}
