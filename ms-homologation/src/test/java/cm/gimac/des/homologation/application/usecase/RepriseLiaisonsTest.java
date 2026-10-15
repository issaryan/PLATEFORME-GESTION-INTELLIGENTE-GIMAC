package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.Acteur;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.EssaisRepris;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.LiaisonReprise;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.Ligne;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.OperationReprise;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.Rapport;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.RessourceReprise;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.Resultat;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.ServiceRepris;
import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.DefinitionTypeProjet;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.parcours.AvancementParcours;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.event.LiaisonOpenedPayload;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursLiaison;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.ServiceParcours;
import cm.gimac.des.homologation.domain.parcours.ModeleTypeProjet;
import cm.gimac.des.homologation.domain.service.LiaisonOpeningPolicy;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.repository.DefinitionTypeProjetRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Reprise des liaisons (D-107), sans base : création, mise à jour, simulation, refus. */
@ExtendWith(MockitoExtension.class)
class RepriseLiaisonsTest {

    static final Acteur REPRISE = new Acteur(UUID.randomUUID(), "service-account-reprise-service");
    static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-10-13T08:00:00Z"), ZoneOffset.UTC);
    static final UUID INSTITUTION = UUID.randomUUID();
    static final UUID BANQUE = UUID.randomUUID();
    static final UUID SNA = UUID.randomUUID();

    @Mock
    LiaisonRepository liaisons;

    @Mock
    ParticipantDirectoryPort participants;

    @Mock
    CollaboratorDirectoryPort collaborateurs;

    @Mock
    ReferentialVersionRepository versions;

    @Mock
    DefinitionTypeProjetRepository typesProjet;

    @Mock
    ContractualScopeRepository perimetres;

    @Mock
    ScopeItemRepository elements;

    @Mock
    ParcoursInstanciation instanciation;

    @Mock
    ParcoursDepot depot;

    @Mock
    Intervenants intervenants;

    @Mock
    OutboxPublisher outbox;

    @Mock
    EntityManager em;

    @Mock
    PlatformTransactionManager tm;

    @Mock
    PorteursPermission porteurs;

    @Mock
    CoverageRepository couvertures;

    RepriseLiaisons reprise;
    final List<SimpleTransactionStatus> statuts = new ArrayList<>();
    final List<Parcours> crees = new ArrayList<>();

    @BeforeEach
    void preparer() {
        reprise = new RepriseLiaisons(liaisons, participants, collaborateurs, new LiaisonOpeningPolicy(), versions, typesProjet, perimetres, elements, instanciation, depot, intervenants, outbox, em, tm, HORLOGE);
        reprise.setPorteurs(porteurs);
        lenient().when(tm.getTransaction(any())).thenAnswer(inv -> {
            SimpleTransactionStatus s = new SimpleTransactionStatus();
            statuts.add(s);
            return s;
        });
        lenient().when(participants.parCodeMembre("BQ-1")).thenReturn(Optional.of(new InstitutionView(INSTITUTION, "Banque fictive", "ACTIF", "BANQUE", List.of(new InstitutionView.ServiceSouscrit("PAIEMENT_MOBILE", "2024-01-01")))));
        lenient().when(participants.parCodeMembre("BQ-P")).thenReturn(Optional.of(new InstitutionView(BANQUE, "Banque partenaire", "ACTIF", "BANQUE", List.of())));
        lenient().when(participants.parCodeMembre("EP-P")).thenReturn(Optional.of(new InstitutionView(UUID.randomUUID(), "EP", "ACTIF", "ETABLISSEMENT_PAIEMENT", List.of())));
        lenient().when(participants.parCodeMembre("BQ-C")).thenReturn(Optional.of(new InstitutionView(UUID.randomUUID(), "Candidat", "CANDIDAT", "BANQUE", List.of())));
        lenient().when(liaisons.findByReferenceOrigine(anyString())).thenReturn(Optional.empty());
        lenient().when(typesProjet.findById("PAIEMENT_MOBILE")).thenReturn(Optional.of(DefinitionTypeProjet.creer("PAIEMENT_MOBILE", "Paiement mobile", null, ModeleTypeProjet.PAIEMENT_MOBILE, null, Instant.now())));
        ReferentialVersion v = ParcoursSamples.version1(TypeProjet.PAIEMENT_MOBILE);
        lenient().when(versions.findFirstByTypeProjetAndEtat(TypeProjet.PAIEMENT_MOBILE, EtatVersionReferentiel.PUBLIEE)).thenReturn(Optional.of(v));
        lenient().when(liaisons.saveAndFlush(any())).thenAnswer(inv -> {
            Liaison l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        lenient().when(instanciation.instancier(any(), any(), anyBoolean(), any(), anyBoolean())).thenAnswer(inv -> {
            Liaison l = inv.getArgument(0);
            List<String> services = inv.getArgument(1);
            List<ServiceParcours> svc = services.stream().filter(c -> !"TRANSVERSE".equals(c)).map(c -> new ServiceParcours(l.getId(), c, c, true)).toList();
            Parcours p = Parcours.instancier(l, new ParcoursLiaison(l.getId(), "LIA-2026-101", LocalDate.of(2026, 4, 1)), l.getVersion().getOperations(), svc, ParcoursSamples.designation(l));
            crees.add(p);
            return p;
        });
        lenient().when(collaborateurs.findCollaborator(ParcoursSamples.CONDUCTEUR)).thenReturn(Optional.of(new CollaboratorView(ParcoursSamples.CONDUCTEUR, "Conducteur fictif", Set.of("ROLE_CONDUCTEUR_PROJET"), null, true)));
        lenient().when(porteurs.porte(any(UUID.class), eq(LiaisonOpeningPolicy.PERMISSION_CONDUITE))).thenReturn(true);
        lenient().when(intervenants.pourFonction(SNA.toString(), Fonction.SNA)).thenReturn(new Personne(SNA.toString(), "Ingénieur SNA fictif", Fonction.SNA));
    }

    static LiaisonReprise liaison(String ref, String code, String avancement, UUID conducteur, List<OperationReprise> ops) {
        return new LiaisonReprise(ref, code, "PAIEMENT_MOBILE", "Projet fictif", "Suivi Excel", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 1, 1), LocalDate.of(2025, 11, 30), conducteur, List.of(), null, List.of(), avancement, ops, null);
    }

    Ligne une(LiaisonReprise l, boolean simulation) {
        Rapport r = reprise.importer(List.of(l), simulation, REPRISE);
        return r.lignes().get(0);
    }

    @Test
    @DisplayName("Création à qualifier, sans conducteur : liaison ouverte, conducteur à désigner, événement d'ouverture")
    void creationAQualifier() {
        Ligne r = une(liaison("REF-1", "BQ-1", null, null, null), false);
        assertThat(r.resultat()).isEqualTo(Resultat.CREEE);
        assertThat(r.reference()).isEqualTo("LIA-2026-101");
        assertThat(r.statut()).isEqualTo("CADREE");
        assertThat(r.avertissements()).anyMatch(a -> a.contains("Conducteur à désigner"));
        Parcours p = crees.get(0);
        assertThat(p.etat().isConducteurADesigner()).isTrue();
        assertThat(p.liaison().getOrigineEtat()).isEqualTo("Suivi Excel");
        assertThat(p.liaison().getChefDeProjetId()).isEqualTo(REPRISE.id());
        assertThat(p.liaison().getDateOuverture()).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
        verify(outbox).publish(eq("liaison"), any(UUID.class), eq(LiaisonOpenedPayload.TYPE), any());
        verify(perimetres).save(any());
        assertThat(statuts.get(0).isRollbackOnly()).isFalse();
    }

    @Test
    @DisplayName("Création en production, conducteur, ressource, banque partenaire et services repris ; simulation annulée")
    void creationEnProduction() {
        LiaisonReprise l = new LiaisonReprise(
            "REF-2", "BQ-1", "paiement_mobile", null, null, null, null, LocalDate.of(2025, 11, 30), ParcoursSamples.CONDUCTEUR,
            List.of(new RessourceReprise("SNA", SNA)), "BQ-P",
            List.of(new ServiceRepris("PAIEMENT_MOBILE", LocalDate.of(2025, 10, 1), true, "Ouvert"), new ServiceRepris("AUTRE_SERVICE", null, null, null)),
            "EN_PRODUCTION", List.of()
        , null);
        Ligne r = une(l, true);
        assertThat(r.resultat()).isEqualTo(Resultat.CREEE);
        assertThat(r.statut()).isEqualTo("EN_PRODUCTION");
        assertThat(r.etat()).isEqualTo("CLOSE");
        Parcours p = crees.get(0);
        assertThat(p.liaison().getBanquePartenaireId()).isEqualTo(BANQUE);
        assertThat(p.etat().getRessources().get(Fonction.SNA).id()).isEqualTo(SNA.toString());
        assertThat(p.services()).filteredOn(s -> s.getServiceCode().equals("PAIEMENT_MOBILE")).singleElement().satisfies(s -> {
            assertThat(s.getOuvert()).isTrue();
            assertThat(s.getCommentaire()).isEqualTo("Ouvert");
        });
        assertThat(statuts).allMatch(TransactionStatus::isRollbackOnly);
    }

    @Test
    @DisplayName("Mise à jour : en-tête, conducteur, ressource, avancement ; inchangée au second passage ; divergente refusée")
    void miseAJour() {
        une(liaison("REF-3", "BQ-1", null, null, null), false);
        Parcours p = crees.get(0);
        Liaison existante = p.liaison();
        when(liaisons.findByReferenceOrigine("REF-3")).thenReturn(Optional.of(existante));
        when(depot.trouver(existante.getId(), true)).thenReturn(Optional.of(p));
        String r0 = p.operations().get(0).getRang();
        LiaisonReprise maj = new LiaisonReprise(
            "REF-3", "BQ-1", "PAIEMENT_MOBILE", "Nouvel intitulé", "Portail sandbox", null, null, null, ParcoursSamples.CONDUCTEUR,
            List.of(new RessourceReprise("SNA", SNA)), null, List.of(new ServiceRepris("HORS_PERIMETRE", null, null, null)), "DETAILLE",
            List.of(new OperationReprise(r0, "BLOQUEE", null, null, "Attente", null, null, null, null))
        , null);
        Ligne r = une(maj, false);
        assertThat(r.resultat()).isEqualTo(Resultat.MISE_A_JOUR);
        assertThat(r.avertissements()).anyMatch(a -> a.contains("HORS_PERIMETRE"));
        assertThat(existante.getIntituleProjet()).isEqualTo("Nouvel intitulé");
        assertThat(existante.getChefDeProjetId()).isEqualTo(ParcoursSamples.CONDUCTEUR);
        assertThat(p.etat().isConducteurADesigner()).isFalse();
        verify(depot).ajouter(any(cm.gimac.des.homologation.domain.parcours.BlocageParcours.class));
        assertThat(une(maj, false).resultat()).isEqualTo(Resultat.INCHANGEE);
        LiaisonReprise autre = new LiaisonReprise("REF-3", "BQ-P", "PAIEMENT_MOBILE", null, null, null, null, null, null, null, null, null, null, null, null);
        assertThat(une(autre, false).code()).isEqualTo("HOM_REPRISE_LIAISON_DIVERGENTE");
    }

    @Test
    @DisplayName("Refus par ligne : institution inconnue ou candidate, type sans référentiel, banque non bancaire ou elle-même, conducteur non habilité, base")
    void refus() {
        assertThat(une(liaison("R-1", "BQ-X", null, null, null), false).code()).isEqualTo("HOM_LIAISON_INSTITUTION_UNKNOWN");
        assertThat(une(liaison("R-2", "BQ-C", null, null, null), false).code()).isEqualTo("HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE");
        LiaisonReprise carte = new LiaisonReprise("R-3", "BQ-1", "CARTE_INTERBANCAIRE", null, null, null, null, null, null, null, null, null, null, null, null);
        assertThat(une(carte, false).code()).isEqualTo("HOM_TYPE_PROJET_INTROUVABLE");
        when(typesProjet.findById("CARTE_INTERBANCAIRE")).thenReturn(Optional.of(DefinitionTypeProjet.creer("CARTE_INTERBANCAIRE", "Carte", null, ModeleTypeProjet.CARTE_INTERBANCAIRE, null, Instant.now())));
        assertThat(une(carte, false).code()).isEqualTo("HOM_REFERENTIAL_NONE_PUBLISHED");
        assertThat(une(avecBanque("R-4", "EP-P"), false).code()).isEqualTo("HOM_LIAISON_CARRIER_NOT_BANK");
        assertThat(une(avecBanque("R-5", "BQ-1"), false).code()).isEqualTo("HOM_LIAISON_CARRIER_INVALID");
        assertThat(une(avecBanque("R-6", "BQ-INCONNUE"), false).code()).isEqualTo("HOM_LIAISON_INSTITUTION_UNKNOWN");
        assertThat(une(liaison("R-7", "BQ-1", null, UUID.randomUUID(), null), false).code()).isEqualTo("HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED");
        doThrow(new DataIntegrityViolationException("uk")).when(em).flush();
        assertThat(une(liaison("R-8", "BQ-1", null, null, null), false).code()).isEqualTo("HOM_REPRISE_ECRITURE_REFUSEE");
    }

    static LiaisonReprise avecBanque(String ref, String banque) {
        return new LiaisonReprise(ref, "BQ-1", "PAIEMENT_MOBILE", null, null, null, null, null, null, null, banque, null, null, null, null);
    }

    @Test
    @DisplayName("Lot : vide, trop volumineux, sans acteur, ligne vide, doublon de fichier")
    void lot() {
        assertThatThrownBy(() -> reprise.importer(List.of(), true, REPRISE)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_REPRISE_VIDE"));
        assertThatThrownBy(() -> reprise.importer(null, true, REPRISE)).isInstanceOf(HomologationException.class);
        List<LiaisonReprise> trop = Collections.nCopies(RepriseLiaisons.LIGNES_MAX + 1, liaison("T", "BQ-1", null, null, null));
        assertThatThrownBy(() -> reprise.importer(trop, true, REPRISE)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_REPRISE_TROP_DE_LIGNES"));
        assertThatThrownBy(() -> reprise.importer(List.of(liaison("T", "BQ-1", null, null, null)), true, new Acteur(null, null))).isInstanceOf(HomologationException.class);
        List<LiaisonReprise> lignes = new ArrayList<>();
        lignes.add(null);
        lignes.add(liaison("D-1", "BQ-1", null, null, null));
        lignes.add(liaison("D-1", "BQ-1", null, null, null));
        Rapport r = reprise.importer(lignes, true, REPRISE);
        assertThat(r.lignes()).extracting(Ligne::code).containsExactly("HOM_REPRISE_LIGNE_INVALIDE", null, "HOM_REPRISE_DOUBLON_FICHIER");
        verify(liaisons, never()).findByReferenceOrigine("D-2");
    }

    @Test
    @DisplayName("Essais repris (D-110) : mesure globale créée, mise à jour, inchangée ; sans dépôt de mesures, ignorée")
    void mesureDesEssais() {
        Liaison l = new Liaison();
        l.setId(UUID.randomUUID());
        Instant t = HORLOGE.instant();
        assertThat(reprise.mesurerEssais(l, new EssaisRepris(37, 109, null), t)).isFalse();
        reprise.setCouvertures(couvertures);
        assertThat(reprise.mesurerEssais(l, null, t)).isFalse();
        when(couvertures.findFirstByLiaison_IdAndNatureAndServiceCode(l.getId(), NatureCouverture.CERTIFICATION, AvancementParcours.ESSAIS_GLOBAL)).thenReturn(Optional.empty());
        assertThat(reprise.mesurerEssais(l, new EssaisRepris(37, 109, LocalDate.of(2026, 10, 9)), t)).isTrue();
        var capture = org.mockito.ArgumentCaptor.forClass(Coverage.class);
        verify(couvertures).save(capture.capture());
        Coverage c = capture.getValue();
        assertThat(c.getServiceCode()).isEqualTo(AvancementParcours.ESSAIS_GLOBAL);
        assertThat(c.getTaux().intValue()).isEqualTo(34);
        assertThat(c.getDateCalcul()).isEqualTo(Instant.parse("2026-10-09T00:00:00Z"));
        assertThat(c.getLiaison()).isSameAs(l);
        when(couvertures.findFirstByLiaison_IdAndNatureAndServiceCode(l.getId(), NatureCouverture.CERTIFICATION, AvancementParcours.ESSAIS_GLOBAL)).thenReturn(Optional.of(c));
        assertThat(reprise.mesurerEssais(l, new EssaisRepris(37, 109, null), t)).isFalse();
        assertThat(reprise.mesurerEssais(l, new EssaisRepris(50, 109, null), t)).isTrue();
        assertThat(c.getNumerateur()).isEqualTo(50);
        assertThat(c.getDateCalcul()).isEqualTo(t);
    }

    @Test
    @DisplayName("Essais repris à la création et à la mise à jour d'une liaison")
    void essaisALaReprise() {
        reprise.setCouvertures(couvertures);
        when(couvertures.findFirstByLiaison_IdAndNatureAndServiceCode(any(), eq(NatureCouverture.CERTIFICATION), eq(AvancementParcours.ESSAIS_GLOBAL))).thenReturn(Optional.empty());
        LiaisonReprise l = new LiaisonReprise("REF-E", "BQ-1", "PAIEMENT_MOBILE", null, null, null, null, null, ParcoursSamples.CONDUCTEUR, null, null, null, null, null, new EssaisRepris(8, 186, null));
        assertThat(une(l, false).resultat()).isEqualTo(Resultat.CREEE);
        verify(couvertures).save(any());
    }
}
