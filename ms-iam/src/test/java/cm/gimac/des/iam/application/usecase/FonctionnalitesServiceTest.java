package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.application.port.out.CatalogueFonctionnalites;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite.Droit;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite.Predicat;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite.Widget;
import cm.gimac.des.iam.domain.fonctionnalite.NatureDroit;
import cm.gimac.des.iam.domain.fonctionnalite.ReglesFonctionnalites;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * D-94 (docs/lot1/ecrans-par-configuration.md §2) : catalogue des fonctionnalités servi et configurable ; valeurs de
 * départ chargées sans jamais écraser une modification faite depuis la plateforme ; fichier livré conforme au
 * contrat et au catalogue des permissions du domaine.
 */
@ExtendWith(MockitoExtension.class)
class FonctionnalitesServiceTest {

    static final Instant T = Instant.parse("2026-10-07T09:00:00Z");
    static final Acteur RSSI = new Acteur("rssi", "Carine RSSI", Set.of("IAM_PROFILE_EDIT"));
    static final ObjectMapper JSON = JsonMapper.builder().build();

    /** Table fonctionnalite en mémoire. */
    static final class CatalogueEnMemoire implements CatalogueFonctionnalites {

        final Map<String, Fonctionnalite> lignes = new TreeMap<>();

        @Override
        public List<Fonctionnalite> toutes() {
            return lignes.values().stream().sorted(Comparator.comparing(Fonctionnalite::application).thenComparing(Fonctionnalite::ordre).thenComparing(Fonctionnalite::cle)).toList();
        }

        @Override
        public Optional<Fonctionnalite> lire(String cle) {
            return Optional.ofNullable(lignes.get(cle));
        }

        @Override
        public boolean ajouterInitiale(Fonctionnalite f) {
            return lignes.putIfAbsent(f.cle(), f) == null;
        }

        @Override
        public boolean remplacerInitiale(Fonctionnalite f) {
            Fonctionnalite a = lignes.get(f.cle());
            if (a == null || a.modifieLe() != null) {
                return false;
            }
            lignes.put(f.cle(), f);
            return true;
        }

        @Override
        public boolean retirerInitiale(String cle) {
            Fonctionnalite a = lignes.get(cle);
            if (a == null || a.modifieLe() != null) {
                return false;
            }
            lignes.remove(cle);
            return true;
        }

        @Override
        public void modifier(Fonctionnalite f) {
            lignes.put(f.cle(), f);
        }
    }

    @Mock
    JournalActes journal;

    CatalogueEnMemoire catalogue;
    FonctionnalitesService service;

    static Fonctionnalite liaisons() {
        return new Fonctionnalite(
            "liaisons",
            "des",
            "homologation",
            "nav.liaisons",
            "homologation",
            110,
            "route",
            true,
            new Predicat(List.of(), List.of("HOM_LIAISON_LIST")),
            List.of(new Widget("cockpit", new Predicat(List.of("HOM_DASHBOARD_READ"), List.of("HOM_PROJECT_DESIGNATE")))),
            List.of(new Droit("HOM_LIAISON_LIST", NatureDroit.CONSULTER, "HOM_LIAISON_LIST", "droits.permission.HOM_LIAISON_LIST")),
            null,
            null
        );
    }

    static Fonctionnalite avec(Fonctionnalite f, String libelle, String groupe, int ordre, boolean visible, Predicat permissions) {
        return new Fonctionnalite(f.cle(), f.application(), f.module(), libelle, groupe, ordre, f.icone(), visible, permissions, f.widgets(), f.droits(), null, null);
    }

    @BeforeEach
    void init() {
        catalogue = new CatalogueEnMemoire();
        AnnuaireEnMemoire annuaire = new AnnuaireEnMemoire(Map.of("ROLE_CHEF_DES", Set.of("HOM_LIAISON_LIST", "HOM_DASHBOARD_READ", "HOM_PROJECT_DESIGNATE")));
        service = new FonctionnalitesService(catalogue, annuaire, journal, Clock.fixed(T, ZoneOffset.UTC));
    }

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Lecture par application, filtre inconnu refusé")
    void lecture() {
        service.charger(List.of(liaisons(), new Fonctionnalite("consoleComptes", "console", "administration", "console.nav.comptes", "comptes", 10, "users", true, null, null, null, null, null)));
        assertThat(service.lister(null)).extracting(Fonctionnalite::cle).containsExactly("consoleComptes", "liaisons");
        assertThat(service.lister(" DES ")).extracting(Fonctionnalite::cle).containsExactly("liaisons");
        assertThat(service.lister("portail")).isEmpty();
        refus(() -> service.lister("intranet"), "IAM_FONCTIONNALITE_INVALIDE");
    }

    @Test
    @DisplayName("Le RSSI renomme, regroupe, réordonne, masque et change la permission qui ouvre : enregistré avec la trace, journal avant/après")
    void modification() {
        service.charger(List.of(liaisons()));
        Fonctionnalite demandee = avec(liaisons(), " Projets d'homologation ", "pilotage", 5, false, new Predicat(List.of("HOM_DASHBOARD_READ"), List.of()));
        Fonctionnalite f = service.modifier(RSSI, "liaisons", demandee, " Revue de la navigation ");
        assertThat(f.libelle()).isEqualTo("Projets d'homologation");
        assertThat(f.groupe()).isEqualTo("pilotage");
        assertThat(f.ordre()).isEqualTo(5);
        assertThat(f.visible()).isFalse();
        assertThat(f.permissions().tous()).containsExactly("HOM_DASHBOARD_READ");
        assertThat(f.modifieLe()).isEqualTo(T);
        assertThat(f.modifiePar()).isEqualTo(new Fonctionnalite.Auteur("rssi", "Carine RSSI"));
        assertThat(service.lister("des")).singleElement().isEqualTo(f);
        verify(journal).fonctionnalite(
            eq(RSSI),
            eq("liaisons"),
            eq("Revue de la navigation"),
            eq("libelle=nav.liaisons ; groupe=homologation ; ordre=110 ; visible=true ; permissions=tous[] unDe[HOM_LIAISON_LIST]"),
            eq("libelle=Projets d'homologation ; groupe=pilotage ; ordre=5 ; visible=false ; permissions=tous[HOM_DASHBOARD_READ] unDe[]")
        );
        // Une modification de la plateforme n'est jamais écrasée par le registre livré.
        FonctionnalitesService.Chargement c = service.charger(List.of(liaisons()));
        assertThat(c.conservees()).isEqualTo(1);
        assertThat(c.remplacees()).isZero();
        assertThat(catalogue.lire("liaisons").orElseThrow().libelle()).isEqualTo("Projets d'homologation");
        // Sans changement : rien n'est écrit.
        service.modifier(RSSI, "liaisons", f, "Relecture");
        verify(journal, never()).fonctionnalite(any(), anyString(), eq("Relecture"), anyString(), anyString());
    }

    @Test
    @DisplayName("Refus : motif, clé inconnue ou changée, application changée, permission inconnue ou interne, forme invalide")
    void refus() {
        service.charger(List.of(liaisons()));
        Fonctionnalite l = liaisons();
        refus(() -> service.modifier(RSSI, "liaisons", l, " "), "IAM_REASON_REQUIRED");
        refus(() -> service.modifier(RSSI, "inconnue", l, "x"), "IAM_FONCTIONNALITE_INTROUVABLE");
        Fonctionnalite autreCle = new Fonctionnalite("mesProjets", "des", "homologation", "x", null, 1, "user", true, null, null, null, null, null);
        refus(() -> service.modifier(RSSI, "liaisons", autreCle, "x"), "IAM_FONCTIONNALITE_INVALIDE");
        Fonctionnalite console = new Fonctionnalite(null, "console", "homologation", "nav.liaisons", "homologation", 110, "route", true, l.permissions(), l.widgets(), l.droits(), null, null);
        refus(() -> service.modifier(RSSI, "liaisons", console, "x"), "IAM_FONCTIONNALITE_APPLICATION_IMMUABLE");
        refus(() -> service.modifier(RSSI, "liaisons", avec(l, "x", null, 1, true, new Predicat(List.of(), List.of("PERM_INEXISTANTE"))), "x"), "IAM_PERMISSION_UNKNOWN");
        refus(() -> service.modifier(RSSI, "liaisons", avec(l, "x", null, 1, true, new Predicat(List.of("INTERNAL_IAM_READ"), List.of())), "x"), "IAM_FONCTIONNALITE_PERMISSION_INTERNE");
        Fonctionnalite droitInconnu = new Fonctionnalite(null, "des", "homologation", "x", null, 1, "route", true, null, null, List.of(new Droit("D", NatureDroit.VALIDER, "PERM_INEXISTANTE", null)), null, null);
        refus(() -> service.modifier(RSSI, "liaisons", droitInconnu, "x"), "IAM_PERMISSION_UNKNOWN");
        refus(() -> service.modifier(RSSI, "liaisons", avec(l, " ", null, 1, true, null), "x"), "IAM_FONCTIONNALITE_INVALIDE");
        refus(() -> service.modifier(RSSI, "liaisons", avec(l, "x", "Groupe Invalide", 1, true, null), "x"), "IAM_FONCTIONNALITE_INVALIDE");
        refus(() -> service.modifier(RSSI, "liaisons", avec(l, "x", null, -1, true, null), "x"), "IAM_FONCTIONNALITE_INVALIDE");
        Fonctionnalite widgetsDoubles = new Fonctionnalite(null, "des", "homologation", "x", null, 1, "route", true, null, List.of(new Widget("cockpit", null), new Widget("cockpit", null)), null, null, null);
        refus(() -> service.modifier(RSSI, "liaisons", widgetsDoubles, "x"), "IAM_FONCTIONNALITE_INVALIDE");
        Fonctionnalite droitsDoubles = new Fonctionnalite(null, "des", "homologation", "x", null, 1, "route", true, null, null, List.of(new Droit("D", NatureDroit.CONSULTER, "HOM_LIAISON_LIST", null), new Droit("D", NatureDroit.CREER, "HOM_LIAISON_LIST", null)), null, null);
        refus(() -> service.modifier(RSSI, "liaisons", droitsDoubles, "x"), "IAM_FONCTIONNALITE_INVALIDE");
        Fonctionnalite sansNature = new Fonctionnalite(null, "des", "homologation", "x", null, 1, "route", true, null, null, List.of(new Droit("D", null, "HOM_LIAISON_LIST", null)), null, null);
        refus(() -> service.modifier(RSSI, "liaisons", sansNature, "x"), "IAM_FONCTIONNALITE_INVALIDE");
        verify(journal, never()).fonctionnalite(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Valeurs de départ : clés absentes ajoutées, valeur de départ suivie, retirée du registre, entrées invalides ou en double ignorées")
    void chargement() {
        Fonctionnalite invalide = new Fonctionnalite("Invalide", "des", "accueil", "x", null, 1, "gauge", true, null, null, null, null, null);
        Fonctionnalite ancienne = new Fonctionnalite("ancienne", "des", "accueil", "x", null, 1, "gauge", true, null, null, null, null, null);
        FonctionnalitesService.Chargement c = service.charger(List.of(liaisons(), ancienne, invalide, liaisons()));
        assertThat(c).isEqualTo(new FonctionnalitesService.Chargement(2, 0, 0, 0, 2));
        // Nouvelle version du registre : valeur de départ jamais modifiée mise à jour, entrée disparue retirée.
        Fonctionnalite nouvelle = avec(liaisons(), "nav.liaisons", "homologation", 120, true, liaisons().permissions());
        c = service.charger(List.of(nouvelle));
        assertThat(c).isEqualTo(new FonctionnalitesService.Chargement(0, 1, 1, 0, 0));
        assertThat(service.lister("des")).singleElement().extracting(Fonctionnalite::ordre).isEqualTo(120);
        // Idempotent.
        assertThat(service.charger(List.of(nouvelle))).isEqualTo(new FonctionnalitesService.Chargement(0, 0, 0, 0, 0));
        // Une modification de la plateforme absente du registre est conservée.
        service.modifier(RSSI, "liaisons", avec(nouvelle, "Liaisons", "homologation", 120, true, nouvelle.permissions()), "Renommage");
        assertThat(service.charger(List.of()).conservees()).isEqualTo(1);
        assertThat(service.lister("des")).singleElement().extracting(Fonctionnalite::libelle).isEqualTo("Liaisons");
    }

    @Test
    @DisplayName("Fichier livré config/fonctionnalites-initiales.json : forme du contrat, clés uniques, REF_METIER_ADMIN, permissions connues du domaine")
    void fichierLivre() throws IOException {
        InitialisationFonctionnalites init = new InitialisationFonctionnalites(service, JSON, new DefaultResourceLoader(), "classpath:config/fonctionnalites-initiales.json");
        List<Fonctionnalite> l = init.lire();
        assertThat(l).hasSizeGreaterThan(40);
        l.forEach(ReglesFonctionnalites::verifierForme);
        assertThat(l).extracting(Fonctionnalite::cle).doesNotHaveDuplicates();
        assertThat(l).extracting(Fonctionnalite::application).containsOnly("des", "console", "portail");
        assertThat(l).filteredOn(f -> f.cle().equals("referentielsMetierAdmin")).singleElement().satisfies(f -> {
            assertThat(f.module()).isEqualTo("referentielsMetier");
            assertThat(f.libelle()).isEqualTo("Administration de tous les domaines");
            assertThat(f.droits()).extracting(Droit::permission).containsExactly("REF_METIER_ADMIN");
        });
        // Matrice des pouvoirs : la règle la plus restrictive (lecture IAM_ACCOUNT_READ et écriture IAM_PROFILE_EDIT).
        assertThat(l).filteredOn(f -> f.cle().equals("adminMatrice")).singleElement().satisfies(f -> assertThat(f.permissions().tous()).containsExactly("IAM_ACCOUNT_READ", "IAM_PROFILE_EDIT"));
        // Toute permission citée existe dans le domaine Keycloak livré (rôles du realm).
        JsonNode realm = JSON.readTree(Files.readString(Path.of("../infra/keycloak/gimac-realm.json")));
        Set<String> roles = new TreeSet<>();
        realm.path("roles").path("realm").forEach(r -> roles.add(r.path("name").asText()));
        Set<String> citees = new TreeSet<>();
        l.forEach(f -> citees.addAll(ReglesFonctionnalites.permissionsCitees(f)));
        assertThat(roles).containsAll(citees);
        // Chargement complet, puis idempotent.
        assertThat(init.charger().ajoutees()).isEqualTo(l.size());
        assertThat(init.charger()).isEqualTo(new FonctionnalitesService.Chargement(0, 0, 0, 0, 0));
        init.auDemarrage();
        // Fichier absent : aucune valeur, démarrage maintenu ; fichier illisible : tracé.
        assertThat(new InitialisationFonctionnalites(service, JSON, new DefaultResourceLoader(), "classpath:config/absent.json").lire()).isEmpty();
        new InitialisationFonctionnalites(service, JSON, new DefaultResourceLoader(), "classpath:config/application.yml").auDemarrage();
        // Tableau seul accepté.
        List<Fonctionnalite> tableau = new ArrayList<>();
        Path tmp = Files.createTempFile("fonctionnalites", ".json");
        Files.writeString(tmp, JSON.writeValueAsString(List.of(liaisons())));
        tableau.addAll(new InitialisationFonctionnalites(service, JSON, new DefaultResourceLoader(), tmp.toUri().toString()).lire());
        Files.delete(tmp);
        assertThat(tableau).singleElement().extracting(Fonctionnalite::cle).isEqualTo("liaisons");
    }

    @Test
    @DisplayName("Prédicat : toutes les permissions de tous, l'une de unDe si non vide")
    void predicat() {
        Predicat p = new Predicat(List.of("A"), List.of("B", "C"));
        assertThat(ReglesFonctionnalites.satisfait(p, Set.of("A", "C"))).isTrue();
        assertThat(ReglesFonctionnalites.satisfait(p, Set.of("A"))).isFalse();
        assertThat(ReglesFonctionnalites.satisfait(p, Set.of("B", "C"))).isFalse();
        assertThat(ReglesFonctionnalites.satisfait(Predicat.aucun(), Set.of())).isTrue();
        assertThat(ReglesFonctionnalites.differences(liaisons(), liaisons())).containsExactly("", "");
    }
}
