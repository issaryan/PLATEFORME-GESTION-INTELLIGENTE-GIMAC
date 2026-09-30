package cm.gimac.des.homologation.application.usecase;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Vues;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import cm.gimac.des.homologation.domain.service.TableauDeBordCalculateur;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Lectures du parcours : dossier, opérations filtrées, tableaux de bord selon les fonctions du jeton, portail, contexte. */
@ExtendWith(MockitoExtension.class)
class ParcoursLecturesTest {

    static final Instant T = Instant.parse("2026-10-06T09:00:00Z");
    static final LocalDate J = LocalDate.parse("2026-10-06");

    @Mock
    ParcoursDepot depot;

    @Mock
    ParcoursInstanciation instanciation;

    @Mock
    Intervenants intervenants;

    @Mock
    ParticipantDirectoryPort participants;

    ParcoursLectures lectures;
    Parcours p;

    @BeforeEach
    void init() {
        ParcoursProjection projection = new ParcoursProjection();
        lectures = new ParcoursLectures(depot, instanciation, projection, new TableauDeBordCalculateur(projection), intervenants, participants, Clock.fixed(T, ZoneOffset.UTC));
        p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        lenient().when(depot.trouver(p.id(), false)).thenReturn(Optional.of(p));
        lenient().when(intervenants.nom(ParcoursSamples.CONDUCTEUR)).thenReturn("Conducteur de projet 1");
    }

    @Test
    @DisplayName("Dossier : conducteur et banque partenaire nommés ; registre injoignable, banque sans nom")
    void dossier() {
        UUID banque = UUID.randomUUID();
        p.liaison().banquePartenaireId(banque);
        when(participants.getInstitution(banque)).thenReturn(new InstitutionView(banque, "Banque B", "ADHERENT", "BANQUE", List.of()));
        Vues.Dossier d = lectures.dossier(p.id(), new Acteur(null, null, Set.of(), null));
        assertThat(d.conducteur().nom()).isEqualTo("Conducteur de projet 1");
        assertThat(d.banquePartenaireNom()).isEqualTo("Banque B");
        assertThat(d.versionReferentiel()).isEqualTo(2);
        when(participants.getInstitution(banque)).thenThrow(HomologationException.unavailable("X", "x"));
        assertThat(lectures.dossier(p.id(), new Acteur(null, null, Set.of(), null)).banquePartenaireNom()).isNull();
        p.liaison().version(null);
        p.liaison().banquePartenaireId(null);
        assertThat(lectures.dossier(p.id(), new Acteur(null, null, Set.of(), null)).versionReferentiel()).isNull();
    }

    @Test
    @DisplayName("Opérations filtrées par étape et par fonction du responsable ; fiche de suivi")
    void operations() {
        Acteur a = new Acteur(null, null, Set.of(), null);
        assertThat(lectures.operations(p.id(), null, null, a)).hasSize(43);
        assertThat(lectures.operations(p.id(), EtapeCode.SUIVI, null, a)).extracting(Vues.Operation::rang).containsExactly("32", "33", "34", "35");
        assertThat(lectures.operations(p.id(), null, Fonction.POLE_SYSTEME, a)).extracting(Vues.Operation::rang).containsExactly("27", "28", "29");
        assertThat(lectures.fiche(p.id(), a).etat()).isNotNull();
    }

    @Test
    @DisplayName("Liaison sans parcours : instanciée à la première lecture")
    void paresseux() {
        UUID id = UUID.randomUUID();
        Liaison l = ParcoursSamples.liaison(TypeProjet.CARTE_INTERBANCAIRE);
        when(depot.trouver(id, false)).thenReturn(Optional.empty());
        when(depot.liaison(id)).thenReturn(l);
        when(instanciation.instancierExistante(l)).thenReturn(p);
        assertThat(lectures.charger(id)).isSameAs(p);
    }

    @Test
    @DisplayName("Tableau de bord : seules les fonctions des profils du jeton ; aucune : 403 ; participant exclu du DES")
    void tableau() {
        when(depot.tous()).thenReturn(List.of(p));
        Acteur chef = new Acteur(UUID.randomUUID(), null, Set.of("ROLE_CHEF_DES", "HOM_DASHBOARD_READ"), null);
        Vues.TableauActeur t = lectures.tableauActeur(List.of(Fonction.SNA, Fonction.CHEF_DES), chef);
        assertThat(t.fonction()).isEqualTo(Fonction.CHEF_DES);
        assertThat(t.compteurs()).extracting(Vues.Compteur::cle).contains("pvASigner").doesNotContain("essaisEnCours");
        assertThat(lectures.tableauActeur(List.of(), chef).fonction()).isEqualTo(Fonction.CHEF_DES);
        assertThat(lectures.tableauActeur(null, chef).liaisons().get(0).conducteurNom()).isEqualTo("Conducteur de projet 1");
        assertThatThrownBy(() -> lectures.tableauActeur(List.of(Fonction.SNA), chef)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_FUNCTION_NOT_HELD"));
        Acteur correspondant = new Acteur(null, null, Set.of("ROLE_CORRESPONDANT"), null);
        assertThatThrownBy(() -> lectures.tableauActeur(List.of(Fonction.PARTICIPANT), correspondant)).isInstanceOf(HomologationException.class);
        assertThat(lectures.pilotage().projetsActifs()).isEqualTo(1);
    }

    @Test
    @DisplayName("Intervenants d'une fonction ; résumés d'une page de liaisons")
    void intervenantsEtResumes() {
        Personne sna = new Personne(UUID.randomUUID().toString(), "SNA", Fonction.SNA);
        when(intervenants.deFonction(Fonction.SNA)).thenReturn(List.of(sna));
        assertThat(lectures.intervenants(Fonction.SNA)).containsExactly(sna);
        Liaison sans = ParcoursSamples.liaison(TypeProjet.DELEGATAIRE);
        when(depot.trouver(sans.getId(), false)).thenReturn(Optional.empty());
        Map<UUID, Vues.LiaisonResume> r = lectures.resumes(List.of(p.liaison(), sans), Map.of(ParcoursSamples.CONDUCTEUR, "C1"));
        assertThat(r).containsOnlyKeys(p.id());
        assertThat(r.get(p.id()).conducteurNom()).isEqualTo("C1");
    }

    @Test
    @DisplayName("Portail : liaisons de l'institution, dossier projeté, tâches, fiche ; autre institution : 404")
    void portail() {
        String inst = p.liaison().getInstitutionId().toString();
        Acteur sien = new Acteur(null, null, Set.of("HOM_PORTAL_READ"), inst);
        when(depot.deLInstitution(inst)).thenReturn(List.of(p));
        assertThat(lectures.mesParcours(sien)).containsExactly(p);
        assertThat(lectures.resumePortail(p).conducteurNom()).isEqualTo("Conducteur de projet GIMAC");
        assertThat(lectures.dossierPortail(p.id(), sien).conducteur().nom()).isEqualTo("Conducteur de projet GIMAC");
        assertThat(lectures.tachesPortail(sien).fonction()).isEqualTo(Fonction.PARTICIPANT);
        assertThat(lectures.fichePortail(p.id(), sien).actionsPermises()).isEmpty();
        Acteur autre = new Acteur(null, null, Set.of("HOM_PORTAL_READ"), UUID.randomUUID().toString());
        assertThatThrownBy(() -> lectures.dossierPortail(p.id(), autre)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.status()).isEqualTo(404));
    }

    @Test
    @DisplayName("Contexte de certification pour MS-CERTIFICATION")
    void contexte() {
        avancer(p, "17", J);
        p.etat().constaterCouverture(J);
        Vues.ContexteCertification c = lectures.contexteCertification(p.id());
        assertThat(c.couvertureConstatee()).isTrue();
        assertThat(c.conducteurNom()).isEqualTo("Conducteur de projet 1");
        assertThat(c.couvertureCertification().taux()).isEqualTo(100);
        when(depot.tous()).thenReturn(List.of());
        assertThat(lectures.pilotage().projetsActifs()).isZero();
    }
}
