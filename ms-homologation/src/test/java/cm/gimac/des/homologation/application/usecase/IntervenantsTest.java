package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Personne;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Intervenants lus dans l'annuaire : fonction, éligibilité, annuaire injoignable. */
@ExtendWith(MockitoExtension.class)
class IntervenantsTest {

    static final UUID ID = UUID.randomUUID();

    @Mock
    CollaboratorDirectoryPort annuaire;

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Intervenants d'une fonction par ses profils, actifs seulement ; titulaire ou fonction à affecter")
    void deFonction() {
        Intervenants i = new Intervenants(annuaire);
        when(annuaire.parProfil("ROLE_SNA")).thenReturn(List.of(new CollaboratorView(ID, "Ingénieur SNA", Set.of("ROLE_SNA"), null, true)));
        when(annuaire.parProfil("ROLE_INGENIEUR_HOMOLOGATION")).thenReturn(List.of(new CollaboratorView(UUID.randomUUID(), "Inactif", Set.of(), null, false)));
        assertThat(i.deFonction(Fonction.SNA)).containsExactly(new Personne(ID.toString(), "Ingénieur SNA", Fonction.SNA));
        assertThat(i.titulaire(Fonction.SNA).nom()).isEqualTo("Ingénieur SNA");
        assertThat(i.deFonction(Fonction.PERSONNALISATEUR)).isEmpty();
        when(annuaire.parProfil("ROLE_RSSI")).thenReturn(List.of());
        assertThat(i.titulaire(Fonction.RSSI)).isEqualTo(Personne.aAffecter(Fonction.RSSI));
        when(annuaire.parProfil("ROLE_DG")).thenThrow(HomologationException.unavailable("HOM_IAM_UNAVAILABLE", "x"));
        assertThat(i.titulaire(Fonction.DG)).isEqualTo(Personne.aAffecter(Fonction.DG));
    }

    @Test
    @DisplayName("Personne désignée : inconnue, inactive, sans profil du parcours, non éligible à la fonction ; nom affiché")
    void personne() {
        Intervenants i = new Intervenants(annuaire);
        refus(() -> i.personne("pas-un-uuid"), "HOM_PERSON_UNKNOWN");
        refus(() -> i.personne(null), "HOM_PERSON_UNKNOWN");
        when(annuaire.findCollaborator(ID)).thenReturn(Optional.of(new CollaboratorView(ID, "RSSI", Set.of("ROLE_RSSI", "ROLE_X"), null, true)));
        assertThat(i.personne(ID.toString())).isEqualTo(new Personne(ID.toString(), "RSSI", Fonction.RSSI));
        assertThat(i.pourFonction(ID.toString(), Fonction.RSSI).fonction()).isEqualTo(Fonction.RSSI);
        refus(() -> i.pourFonction(ID.toString(), Fonction.SNA), "HOM_PERSON_NOT_ELIGIBLE");
        UUID sans = UUID.randomUUID();
        when(annuaire.findCollaborator(sans)).thenReturn(Optional.of(new CollaboratorView(sans, "X", Set.of("ROLE_X"), null, true)));
        refus(() -> i.personne(sans.toString()), "HOM_PERSON_NOT_ELIGIBLE");
        UUID inactif = UUID.randomUUID();
        when(annuaire.findCollaborator(inactif)).thenReturn(Optional.of(new CollaboratorView(inactif, "Y", Set.of("ROLE_RSSI"), null, false)));
        refus(() -> i.personne(inactif.toString()), "HOM_PERSON_UNKNOWN");
        assertThat(i.nom(ID)).isEqualTo("RSSI");
        when(annuaire.findCollaborator(sans)).thenThrow(HomologationException.unavailable("HOM_IAM_UNAVAILABLE", "x"));
        assertThat(i.nom(sans)).isNull();
    }
}
