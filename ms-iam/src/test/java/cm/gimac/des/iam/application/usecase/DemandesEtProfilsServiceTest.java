package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.application.port.out.ProfilsCrees;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.ProfilCree;
import cm.gimac.des.iam.domain.demande.ContenuDemande;
import cm.gimac.des.iam.domain.demande.Demande;
import cm.gimac.des.iam.domain.demande.EtatDemande;
import cm.gimac.des.iam.domain.demande.TypeDemande;
import cm.gimac.des.iam.domain.event.DemandeDecideePayload;
import cm.gimac.des.iam.domain.event.DemandeSoumisePayload;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.domain.pouvoir.PouvoirsProfil;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import cm.gimac.des.iam.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.iam.infrastructure.persistence.DemandeAdministration;
import cm.gimac.des.iam.infrastructure.persistence.DemandeAdministrationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.json.JsonMapper;

/**
 * D-81 §4 et §5 : demandes à l'administration (périmètre, profils demandables, unicité, acceptation par
 * les cas d'usage, refus, annulation, événements) et profils créés (catalogue, matrice, suppression),
 * sur l'annuaire de substitution ; dépôt des demandes en mémoire.
 */
@ExtendWith(MockitoExtension.class)
class DemandesEtProfilsServiceTest {

    static final Instant T = Instant.parse("2026-10-06T08:00:00Z");
    static final Acteur ADMIN = new Acteur("admin", "Admin", Set.of("IAM_ACCOUNT_READ", "IAM_ACCOUNT_CREATE", "IAM_ACCOUNT_ROLES", "IAM_ACCOUNT_STATUS", "IAM_ROLE_PRIVILEGED_GRANT"));

    @Mock
    JournalActes journal;

    @Mock
    OutboxPublisher outbox;

    @Mock
    DemandeAdministrationRepository depot;

    @Mock
    AdministrationComptesService comptes;

    final Map<UUID, DemandeAdministration> stock = new LinkedHashMap<>();
    AnnuaireEnMemoire annuaire;
    OrganisationServiceTest.CatalogueEnMemoire unites;
    PouvoirsServiceTest.MatriceEnMemoire matrice;
    OrganisationService organisation;
    DemandesService service;
    String chef;
    String agent;
    String etranger;
    Acteur chefActeur;

    String creer(String identifiant, Set<String> profils) {
        return annuaire.creer(new NouveauCompte(identifiant, identifiant, "Mballa", identifiant + "@gimac.test", null, "Fonction", null, profils), List.of());
    }

    org.springframework.data.domain.Page<DemandeAdministration> page(Predicate<DemandeAdministration> filtre, Pageable p) {
        List<DemandeAdministration> l = stock.values().stream().filter(filtre).sorted(Comparator.comparing(DemandeAdministration::getCreeeLe).reversed()).toList();
        return new PageImpl<>(l, p, l.size());
    }

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI", "ROLE_CHEF_DES", "ROLE_SNA", "ROLE_COLLABORATEUR")), Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI"));
        unites = new OrganisationServiceTest.CatalogueEnMemoire();
        unites.enregistrer(new Unite("DG", "DG", TypeUnite.DIRECTION, null, true));
        unites.enregistrer(new Unite("DES", "DES", TypeUnite.DEPARTEMENT, "DG", true));
        unites.enregistrer(new Unite("SSI", "SSI", TypeUnite.SERVICE, "DG", true));
        matrice = new PouvoirsServiceTest.MatriceEnMemoire();
        Map<ActionPouvoir, Portee> a = new EnumMap<>(ActionPouvoir.class);
        a.put(ActionPouvoir.COMPTE_DEMANDER, Portee.UNITES);
        a.put(ActionPouvoir.PROFIL_DEMANDER, Portee.EQUIPE);
        a.put(ActionPouvoir.STATUT_DEMANDER, Portee.EQUIPE);
        matrice.enregistrer(new PouvoirsProfil("ROLE_CHEF_DES", a, Set.of("ROLE_SNA", "ROLE_COLLABORATEUR", "ROLE_RSSI")));
        var proprietes = new cm.gimac.des.iam.application.IamProprietes(
            null,
            null,
            new cm.gimac.des.iam.application.IamProprietes.Profils(Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI", "ROLE_CHEF_DES", "ROLE_SNA", "ROLE_COLLABORATEUR"), Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI")),
            null,
            null,
            null,
            null
        );
        organisation = new OrganisationService(unites, annuaire, journal);
        PouvoirsService pouvoirs = new PouvoirsService(matrice, CatalogueProfils.configuration(proprietes), annuaire, organisation, journal);
        service = new DemandesService(depot, pouvoirs, organisation, comptes, annuaire, journal, outbox, JsonMapper.builder().findAndAddModules().build(), Clock.fixed(T, ZoneOffset.UTC));

        chef = creer("chef", Set.of("ROLE_CHEF_DES"));
        agent = creer("agent", Set.of("ROLE_SNA"));
        etranger = creer("etranger", Set.of("ROLE_SNA"));
        annuaire.definirRattachement(chef, "DES", null);
        annuaire.definirRattachement(agent, "DES", chef);
        chefActeur = new Acteur(chef, "chef Mballa", Set.of("PIL_ACTIVITY_OWN"));

        lenient().when(depot.saveAndFlush(any())).thenAnswer(i -> {
            DemandeAdministration d = i.getArgument(0);
            stock.put(d.getId(), d);
            return d;
        });
        lenient().when(depot.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(depot.findById(any())).thenAnswer(i -> Optional.ofNullable(stock.get(i.<UUID>getArgument(0))));
        lenient().when(depot.existsByTypeAndCleCibleAndEtat(any(), anyString(), any())).thenAnswer(i ->
            stock.values().stream().anyMatch(d -> d.getType() == i.getArgument(0) && d.getCleCible().equals(i.getArgument(1)) && d.getEtat() == i.getArgument(2))
        );
        lenient().when(depot.findByDemandeurIdOrderByCreeeLeDesc(anyString(), any())).thenAnswer(i -> page(d -> d.getDemandeurId().equals(i.getArgument(0)), i.getArgument(1)));
        lenient().when(depot.findByDemandeurIdAndEtatOrderByCreeeLeDesc(anyString(), any(), any())).thenAnswer(i ->
            page(d -> d.getDemandeurId().equals(i.getArgument(0)) && d.getEtat() == i.getArgument(1), i.getArgument(2))
        );
        lenient().when(depot.findAllByOrderByCreeeLeDesc(any())).thenAnswer(i -> page(d -> true, i.getArgument(0)));
        lenient().when(depot.findByEtatOrderByCreeeLeDesc(any(), any())).thenAnswer(i -> page(d -> d.getEtat() == i.getArgument(0), i.getArgument(1)));
    }

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    ContenuDemande creation(String identifiant, String unite) {
        return ContenuDemande.creation(identifiant, "Nouvel", "Agent", identifiant + "@gimac.test", null, "Technicien", unite, chef, Set.of("ROLE_COLLABORATEUR"));
    }

    @Test
    @DisplayName("Soumission : périmètre (403), profils demandables (422, jamais privilégiés), unicité (409), sans effet (400), événement à l'administration")
    void soumission() {
        refus(() -> service.soumettre(chefActeur, TypeDemande.STATUT, etranger, ContenuDemande.statut(false), "Départ en congé"), "ORG_HORS_PERIMETRE", 403);
        refus(() -> service.soumettre(new Acteur(agent, "agent", Set.of()), TypeDemande.STATUT, chef, ContenuDemande.statut(false), "Départ en congé"), "ORG_HORS_PERIMETRE", 403);
        refus(() -> service.soumettre(chefActeur, TypeDemande.CREATION_COMPTE, null, creation("hors", "SSI"), "Renfort de l'équipe"), "ORG_HORS_PERIMETRE", 403);
        refus(() -> service.soumettre(chefActeur, TypeDemande.PROFILS, agent, ContenuDemande.profils(Set.of("ROLE_RSSI"), null), "Sécurité"), "ORG_PROFIL_NON_DEMANDABLE", 422);
        refus(() -> service.soumettre(chefActeur, TypeDemande.PROFILS, agent, ContenuDemande.profils(Set.of("ROLE_SNA"), null), "Sans effet"), "ORG_DEMANDE_INVALIDE", 400);
        refus(() -> service.soumettre(chefActeur, TypeDemande.STATUT, agent, ContenuDemande.statut(true), "Sans effet"), "ORG_DEMANDE_INVALIDE", 400);

        Demande d = service.soumettre(chefActeur, TypeDemande.PROFILS, agent, ContenuDemande.profils(Set.of("ROLE_COLLABORATEUR"), Set.of()), " Nouvelles missions ");
        assertThat(d.etat()).isEqualTo(EtatDemande.EN_ATTENTE);
        assertThat(d.demandeur()).isEqualTo(new Demande.Personne(chef, "chef Mballa"));
        assertThat(d.cible()).isEqualTo(new Demande.Personne(agent, "agent Mballa"));
        assertThat(d.contenu().ajouter()).containsExactly("ROLE_COLLABORATEUR");
        assertThat(d.motif()).isEqualTo("Nouvelles missions");
        assertThat(d.decision()).isNull();
        assertThat(d.creeeLe()).isEqualTo(T);
        refus(() -> service.soumettre(chefActeur, TypeDemande.PROFILS, agent, ContenuDemande.profils(Set.of("ROLE_COLLABORATEUR"), null), "Encore une fois"), "ORG_DEMANDE_EXISTANTE", 409);
        ArgumentCaptor<Object> p = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("request"), eq(d.id()), eq(DemandeSoumisePayload.TYPE), p.capture());
        DemandeSoumisePayload e = (DemandeSoumisePayload) p.getValue();
        assertThat(e.destinataires()).containsExactly("demo-rssi", "demo-super-admin");
        assertThat(e.cibleId()).isEqualTo(agent);
        verify(journal).demande(eq(chefActeur), eq("DEMANDE_SOUMISE"), eq(d.id().toString()), eq("PROFILS"), eq("Nouvelles missions"), any());

        // Identifiant déjà pris : 409 IAM_ACCOUNT_CONFLICT.
        refus(() -> service.soumettre(chefActeur, TypeDemande.CREATION_COMPTE, null, creation("agent", "DES"), "Renfort de l'équipe"), "IAM_ACCOUNT_CONFLICT", 409);
    }

    @Test
    @DisplayName("Acceptation : permission de l'acte, pas sa propre demande, création par le cas d'usage puis rattachement, événement au demandeur ; demande décidée immuable")
    void acceptationCreation() {
        Demande d = service.soumettre(chefActeur, TypeDemande.CREATION_COMPTE, null, creation("nouveau", "DES"), "Renfort de l'équipe");
        assertThat(d.cible()).isNull();
        String cree = creer("nouveau", Set.of("ROLE_COLLABORATEUR"));
        Compte compte = annuaire.lire(cree).orElseThrow();
        when(comptes.creer(eq(ADMIN), any(), eq(true), eq(false))).thenReturn(new AdministrationComptesService.Creation(compte, true, null));

        refus(() -> service.accepter(new Acteur("x", "x", Set.of("IAM_ACCOUNT_ROLES")), d.id(), null), "ORG_DEMANDE_INTERDITE", 403);
        refus(() -> service.accepter(new Acteur(chef, "chef", Set.of("IAM_ACCOUNT_CREATE")), d.id(), null), "IAM_SELF_ADMINISTRATION", 403);
        Demande acceptee = service.accepter(ADMIN, d.id(), "Validé");
        assertThat(acceptee.etat()).isEqualTo(EtatDemande.ACCEPTEE);
        assertThat(acceptee.decision().par().id()).isEqualTo("admin");
        assertThat(acceptee.decision().motif()).isEqualTo("Validé");
        assertThat(acceptee.decision().le()).isEqualTo(T);
        assertThat(acceptee.cible().id()).isEqualTo(cree);
        ArgumentCaptor<NouveauCompte> n = ArgumentCaptor.forClass(NouveauCompte.class);
        verify(comptes).creer(eq(ADMIN), n.capture(), eq(true), eq(false));
        assertThat(n.getValue().identifiant()).isEqualTo("nouveau");
        assertThat(n.getValue().institutionId()).isNull();
        assertThat(annuaire.lire(cree).orElseThrow().unite()).isEqualTo("DES");
        assertThat(annuaire.lire(cree).orElseThrow().responsableId()).isEqualTo(chef);
        ArgumentCaptor<Object> p = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("request"), eq(d.id()), eq(DemandeDecideePayload.TYPE), p.capture());
        assertThat(((DemandeDecideePayload) p.getValue()).destinataires()).containsExactly(chef);
        assertThat(((DemandeDecideePayload) p.getValue()).etat()).isEqualTo("ACCEPTEE");
        refus(() -> service.accepter(ADMIN, d.id(), null), "ORG_DEMANDE_TRAITEE", 409);
        refus(() -> service.accepter(ADMIN, UUID.randomUUID(), null), "ORG_DEMANDE_INTROUVABLE", 404);
    }

    @Test
    @DisplayName("Profils et statut acceptés par les cas d'usage ; refus motivé ; annulation par le demandeur seul ; lectures et vues")
    void profilsStatutRefusAnnulationLectures() {
        Demande profils = service.soumettre(chefActeur, TypeDemande.PROFILS, agent, ContenuDemande.profils(Set.of("ROLE_COLLABORATEUR"), Set.of("ROLE_SNA")), "Mutation interne");
        when(comptes.lire(agent)).thenReturn(annuaire.lire(agent).orElseThrow());
        service.accepter(ADMIN, profils.id(), null);
        verify(comptes).definirProfils(eq(ADMIN), eq(agent), eq(Set.of("ROLE_COLLABORATEUR")), org.mockito.ArgumentMatchers.startsWith("Demande " + profils.id()));

        Demande statut = service.soumettre(chefActeur, TypeDemande.STATUT, agent, ContenuDemande.statut(false), "Fin de mission");
        refus(() -> service.refuser(ADMIN, statut.id(), "non"), "ORG_DEMANDE_INVALIDE", 400);
        refus(() -> service.annuler(new Acteur("admin", "Admin", Set.of("IAM_ACCOUNT_READ")), statut.id()), "ORG_DEMANDE_INTERDITE", 403);
        refus(() -> service.annuler(new Acteur(etranger, "x", Set.of("PIL_ACTIVITY_OWN")), statut.id()), "ORG_DEMANDE_INTROUVABLE", 404);
        Demande annulee = service.annuler(chefActeur, statut.id());
        assertThat(annulee.etat()).isEqualTo(EtatDemande.ANNULEE);
        assertThat(annulee.decision()).isNull();
        Demande statut2 = service.soumettre(chefActeur, TypeDemande.STATUT, agent, ContenuDemande.statut(false), "Fin de mission");
        Demande refusee = service.refuser(ADMIN, statut2.id(), "Mission prolongée");
        assertThat(refusee.etat()).isEqualTo(EtatDemande.REFUSEE);
        assertThat(refusee.decision().motif()).isEqualTo("Mission prolongée");
        verify(comptes, never()).definirActif(any(), anyString(), org.mockito.ArgumentMatchers.anyBoolean(), anyString());
        Demande statut3 = service.soumettre(chefActeur, TypeDemande.STATUT, agent, ContenuDemande.statut(false), "Fin de mission");
        service.accepter(new Acteur("admin2", "Admin 2", Set.of("IAM_ACCOUNT_STATUS")), statut3.id(), " ");
        verify(comptes).definirActif(any(), eq(agent), eq(false), anyString());

        var miennes = service.lister(chefActeur, null, null, null, null);
        assertThat(miennes.total()).isEqualTo(4);
        assertThat(miennes.taille()).isEqualTo(25);
        assertThat(service.lister(chefActeur, "miennes", "annulee", 0, 10).contenu()).extracting(Demande::id).containsExactly(statut.id());
        refus(() -> service.lister(chefActeur, "a-traiter", null, null, null), "ORG_DEMANDE_INTERDITE", 403);
        assertThat(service.lister(ADMIN, "a-traiter", "REFUSEE", null, null).contenu()).extracting(Demande::id).containsExactly(statut2.id());
        assertThat(service.lister(ADMIN, "a-traiter", null, null, null).total()).isEqualTo(4);
        refus(() -> service.lister(ADMIN, "toutes", null, null, null), "ORG_DEMANDE_INVALIDE", 400);
        refus(() -> service.lister(ADMIN, "a-traiter", "INCONNU", null, null), "ORG_DEMANDE_INVALIDE", 400);
        refus(() -> service.lister(ADMIN, "a-traiter", null, -1, 500), "ORG_DEMANDE_INVALIDE", 400);
        assertThat(service.lire(chefActeur, profils.id()).etat()).isEqualTo(EtatDemande.ACCEPTEE);
        assertThat(service.lire(ADMIN, profils.id()).id()).isEqualTo(profils.id());
        refus(() -> service.lire(new Acteur(etranger, "x", Set.of()), profils.id()), "ORG_DEMANDE_INTROUVABLE", 404);
    }

    /** Profils créés en mémoire. */
    static final class ProfilsCreesEnMemoire implements ProfilsCrees {

        final Map<String, ProfilCree> profils = new LinkedHashMap<>();

        @Override
        public List<ProfilCree> tous() {
            return new ArrayList<>(profils.values());
        }

        @Override
        public Optional<ProfilCree> lire(String code) {
            return Optional.ofNullable(profils.get(code));
        }

        @Override
        public void enregistrer(ProfilCree profil) {
            profils.put(profil.code(), profil);
        }

        @Override
        public void supprimer(String code) {
            profils.remove(code);
        }
    }

    @Test
    @DisplayName("Profils créés : permissions communes, attribuables et privilégiés au catalogue, recomposition, matrice, suppression et refus")
    void profilsCrees() {
        ProfilsCreesEnMemoire crees = new ProfilsCreesEnMemoire();
        var proprietes = AdministrationComptesServiceTest.proprietes();
        CatalogueProfils catalogue = new CatalogueProfils(proprietes, crees);
        ProfilsService profils = new ProfilsService(annuaire, crees, catalogue, matrice, journal, Clock.fixed(T, ZoneOffset.UTC));
        Acteur rssi = new Acteur("rssi", "RSSI", Set.of());

        var p = profils.creer(ADMIN, "ROLE_QUALITE", " Qualité ", Set.of("HOM_DASHBOARD_READ"), " Revue ");
        assertThat(p.cree()).isTrue();
        assertThat(p.privilegie()).isFalse();
        assertThat(p.description()).isEqualTo("Qualité");
        assertThat(p.permissions()).contains("PIL_ACTIVITY_OWN", "NOT_INBOX_READ", "COL_MESSAGE_READ", "COL_MESSAGE_SEND", "HOM_DASHBOARD_READ");
        assertThat(crees.lire("ROLE_QUALITE").orElseThrow().creePar()).isEqualTo("admin");
        assertThat(catalogue.attribuables()).contains("ROLE_QUALITE", "ROLE_CHARGE_DDRM");
        assertThat(catalogue.cree("ROLE_QUALITE")).isTrue();
        assertThat(catalogue.cree("ROLE_RSSI")).isFalse();
        verify(journal).configuration(eq(ADMIN), eq("PROFIL_CREE"), eq("Revue"), any());

        refus(() -> profils.creer(ADMIN, "ROLE_QUALITE", "x", Set.of(), null), "IAM_PROFIL_EXISTANT", 409);
        refus(() -> profils.creer(ADMIN, "ROLE_RSSI", "x", Set.of(), null), "IAM_PROFIL_EXISTANT", 409);
        refus(() -> profils.creer(ADMIN, "ROLE_VIDE", " ", Set.of(), null), "IAM_PROFIL_INVALIDE", 400);
        refus(() -> profils.creer(ADMIN, "ROLE_SERVICE", "x", Set.of("INTERNAL_IAM_READ"), null), "IAM_PROFIL_PERMISSION_INTERNE", 422);
        refus(() -> profils.creer(rssi, "ROLE_AUDIT", "x", Set.of("IAM_ACCOUNT_READ"), null), "IAM_PRIVILEGED_PROFILE", 403);
        var audit = profils.creer(ADMIN, "ROLE_AUDIT", "Audit", null, null);
        assertThat(audit.privilegie()).isFalse();

        // Recomposition d'un profil créé : permissions communes gardées, privilégié recalculé.
        ConfigurationAuthentificationService configuration = new ConfigurationAuthentificationService(annuaire, journal, null, null, Clock.fixed(T, ZoneOffset.UTC), catalogue, crees);
        assertThat(configuration.definirPermissions(ADMIN, "ROLE_AUDIT", Set.of("IAM_ACCOUNT_READ"), "Audit des comptes").permissions()).contains("IAM_ACCOUNT_READ", "PIL_ACTIVITY_OWN");
        assertThat(crees.lire("ROLE_AUDIT").orElseThrow().privilegie()).isTrue();
        assertThat(catalogue.privilegies()).contains("ROLE_AUDIT");
        assertThat(configuration.profils()).filteredOn(x -> x.code().equals("ROLE_AUDIT")).singleElement().satisfies(x -> {
            assertThat(x.privilegie()).isTrue();
            assertThat(x.cree()).isTrue();
        });
        refus(() -> configuration.definirPermissions(rssi, "ROLE_AUDIT", Set.of(), "x"), "IAM_PRIVILEGED_PROFILE", 403);
        refus(() -> configuration.definirPermissions(ADMIN, "ROLE_QUALITE", Set.of("INTERNAL_IAM_READ"), "x"), "IAM_PROFIL_PERMISSION_INTERNE", 422);

        // Suppression.
        matrice.enregistrer(new PouvoirsProfil("ROLE_QUALITE", Map.of(ActionPouvoir.TACHE_SUIVRE, Portee.DIRECTS), Set.of()));
        String titulaire = creer("qualite", Set.of("ROLE_QUALITE"));
        refus(() -> profils.supprimer(ADMIN, "ROLE_QUALITE", null), "IAM_PROFIL_TITULAIRES", 409);
        annuaire.supprimer(titulaire);
        refus(() -> profils.supprimer(rssi, "ROLE_AUDIT", null), "IAM_PRIVILEGED_PROFILE", 403);
        refus(() -> profils.supprimer(ADMIN, "ROLE_RSSI", null), "IAM_PROFIL_SYSTEME", 409);
        refus(() -> profils.supprimer(ADMIN, "ROLE_INCONNU", null), "IAM_PROFILE_NOT_FOUND", 404);
        profils.supprimer(ADMIN, "ROLE_QUALITE", "Abandon");
        assertThat(catalogue.attribuables()).doesNotContain("ROLE_QUALITE");
        assertThat(matrice.toutes()).doesNotContainKey("ROLE_QUALITE");
        assertThat(annuaire.profils()).extracting(cm.gimac.des.iam.domain.compte.Profil::code).doesNotContain("ROLE_QUALITE");
        verify(journal).configuration(eq(ADMIN), eq("PROFIL_SUPPRIME"), eq("Abandon"), any());

        // Catalogue de la seule configuration : aucun profil créé, écriture refusée.
        CatalogueProfils config = CatalogueProfils.configuration(proprietes);
        assertThat(config.cree("ROLE_X")).isFalse();
        assertThat(config.privilegies()).containsExactlyInAnyOrderElementsOf(AdministrationComptesServiceTest.PRIVILEGIES);
        assertThat(config.attribuables()).containsExactlyInAnyOrderElementsOf(AdministrationComptesServiceTest.CATALOGUE);
    }
}
