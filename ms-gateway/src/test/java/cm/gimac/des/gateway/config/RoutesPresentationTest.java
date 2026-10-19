package cm.gimac.des.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.server.PathContainer;
import org.springframework.util.unit.DataSize;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.yaml.snakeyaml.Yaml;

/**
 * Routes des couches de présentation (PI §0.6, D-62) lues dans application.yml : chaque chemin
 * /bff aboutit à une seule route, protégée par OpaAuthorize, et réécrit vers l'API v1 du service.
 * Les routes internes des services ne sont couvertes par aucune route.
 */
class RoutesPresentationTest {

    record Route(String id, String uri, List<PathPattern> chemins, List<String> filtres) {}

    static List<Route> routes;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void lire() throws Exception {
        routes = new ArrayList<>();
        // Fichier principal : les ressources de test ont leur propre application.yml.
        try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/main/resources/config/application.yml"))) {
            for (Object doc : new Yaml().loadAll(in)) {
                if (!(doc instanceof Map<?, ?> racine)) continue;
                Object liste = chemin(racine, "spring", "cloud", "gateway", "server", "webflux", "routes");
                if (!(liste instanceof List<?> l)) continue;
                for (Object o : l) {
                    Map<String, Object> r = (Map<String, Object>) o;
                    List<PathPattern> chemins = new ArrayList<>();
                    for (Object p : (List<Object>) r.getOrDefault("predicates", List.of())) {
                        String s = p.toString();
                        if (s.startsWith("Path=")) {
                            for (String motif : s.substring(5).split(",")) chemins.add(PathPatternParser.defaultInstance.parse(motif.trim()));
                        }
                    }
                    routes.add(new Route((String) r.get("id"), (String) r.get("uri"), chemins, ((List<Object>) r.getOrDefault("filters", List.of())).stream().map(Object::toString).toList()));
                }
            }
        }
        assertThat(routes).isNotEmpty();
    }

    @SuppressWarnings("unchecked")
    static Object chemin(Map<?, ?> m, String... cles) {
        Object o = m;
        for (String c : cles) {
            if (!(o instanceof Map<?, ?> x)) return null;
            o = ((Map<String, Object>) x).get(c);
        }
        return o;
    }

    static List<Route> correspondantes(String chemin) {
        PathContainer pc = PathContainer.parsePath(chemin);
        return routes.stream().filter(r -> r.chemins().stream().anyMatch(p -> p.matches(pc))).toList();
    }

    /** Applique RewritePath comme la passerelle (regex Java, groupes nommés). */
    static String reecrire(Route r, String chemin) {
        String f = r.filtres().stream().filter(x -> x.startsWith("RewritePath=")).findFirst().orElseThrow();
        String[] parties = f.substring("RewritePath=".length()).split(",\\s*", 2);
        return Pattern.compile(parties[0]).matcher(chemin).replaceAll(parties[1].replace("$\\", "$"));
    }

    static Stream<Arguments> chemins() {
        return Stream.of(
            Arguments.of("/bff/des/participant/institutions/42", "${gimac.services.participant}", "/api/v1/institutions/42"),
            Arguments.of("/bff/des/iam/moi", "${gimac.services.iam}", "/api/v1/moi"),
            Arguments.of("/bff/portail/iam/moi/premiere-configuration", "${gimac.services.iam}", "/api/v1/moi/premiere-configuration"),
            Arguments.of("/bff/console/iam/moi/cles-securite/defi", "${gimac.services.iam}", "/api/v1/moi/cles-securite/defi"),
            Arguments.of("/bff/des/iam/reauthentification/defi", "${gimac.services.iam}", "/api/v1/reauthentification/defi"),
            Arguments.of("/bff/console/iam/comptes", "${gimac.services.iam}", "/api/v1/comptes"),
            // Reprise de l'existant (D-107) : console du super administrateur, routes /api/v1/reprise des services.
            Arguments.of("/bff/console/participant/reprise/institutions", "${gimac.services.participant}", "/api/v1/reprise/institutions"),
            Arguments.of("/bff/console/homologation/reprise/liaisons", "${gimac.services.homologation}", "/api/v1/reprise/liaisons"),
            Arguments.of("/bff/console/iam/comptes/c1/profils", "${gimac.services.iam}", "/api/v1/comptes/c1/profils"),
            Arguments.of("/bff/console/iam/configuration/politique-securite", "${gimac.services.iam}", "/api/v1/configuration/politique-securite"),
            // Administration dans le département (D-69) : comptes, journal, politique et profils.
            Arguments.of("/bff/des/iam/comptes", "${gimac.services.iam}", "/api/v1/comptes"),
            Arguments.of("/bff/des/iam/comptes/journal", "${gimac.services.iam}", "/api/v1/comptes/journal"),
            Arguments.of("/bff/des/iam/comptes/c1/second-facteur/reinitialisation", "${gimac.services.iam}", "/api/v1/comptes/c1/second-facteur/reinitialisation"),
            Arguments.of("/bff/des/iam/configuration/politique-securite", "${gimac.services.iam}", "/api/v1/configuration/politique-securite"),
            Arguments.of("/bff/des/iam/configuration/profils", "${gimac.services.iam}", "/api/v1/configuration/profils"),
            Arguments.of("/bff/des/iam/configuration/profils/ROLE_SNA/permissions", "${gimac.services.iam}", "/api/v1/configuration/profils/ROLE_SNA/permissions"),
            Arguments.of("/bff/des/iam/configuration/permissions", "${gimac.services.iam}", "/api/v1/configuration/permissions"),
            // « Mon compte » (docs/lot1/mon-compte.md) : photo du titulaire depuis chaque couche, photos des collègues (DES, console).
            Arguments.of("/bff/des/iam/moi/avatar", "${gimac.services.iam}", "/api/v1/moi/avatar"),
            Arguments.of("/bff/portail/iam/moi/avatar", "${gimac.services.iam}", "/api/v1/moi/avatar"),
            Arguments.of("/bff/console/iam/moi/mot-de-passe", "${gimac.services.iam}", "/api/v1/moi/mot-de-passe"),
            Arguments.of("/bff/des/iam/avatars/u-123", "${gimac.services.iam}", "/api/v1/avatars/u-123"),
            Arguments.of("/bff/console/iam/avatars/u-123", "${gimac.services.iam}", "/api/v1/avatars/u-123"),
            Arguments.of("/bff/des/document/documents", "${gimac.services.document}", "/api/v1/documents"),
            Arguments.of("/bff/portail/document/documents", "${gimac.services.document}", "/api/v1/documents"),
            // Dépôts fragmentés (D-74, docs/lot1/depots-fragmentes.md §3) depuis les trois couches.
            Arguments.of("/bff/des/document/depots", "${gimac.services.document}", "/api/v1/depots"),
            Arguments.of("/bff/console/document/depots", "${gimac.services.document}", "/api/v1/depots"),
            Arguments.of("/bff/portail/document/depots", "${gimac.services.document}", "/api/v1/depots"),
            Arguments.of("/bff/des/document/depots/d1", "${gimac.services.document}", "/api/v1/depots/d1"),
            Arguments.of("/bff/console/document/depots/d1/parties/12", "${gimac.services.document}", "/api/v1/depots/d1/parties/12"),
            Arguments.of("/bff/portail/document/depots/d1/fin", "${gimac.services.document}", "/api/v1/depots/d1/fin"),
            // Aperçu des documents bureautiques (D-83, docs/lot1/apercu-bureautique.md §2) : département et console.
            Arguments.of("/bff/des/document/apercus", "${gimac.services.document}", "/api/v1/apercus"),
            Arguments.of("/bff/console/document/apercus", "${gimac.services.document}", "/api/v1/apercus"),
            Arguments.of("/bff/des/collaboration/messagerie/annuaire", "${gimac.services.collaboration}", "/api/v1/messagerie/annuaire"),
            Arguments.of(
                "/bff/des/collaboration/messagerie/conversations",
                "${gimac.services.collaboration}",
                "/api/v1/messagerie/conversations"
            ),
            Arguments.of(
                "/bff/des/collaboration/messagerie/conversations/c1/messages",
                "${gimac.services.collaboration}",
                "/api/v1/messagerie/conversations/c1/messages"
            ),
            Arguments.of(
                "/bff/des/collaboration/messagerie/conversations/c1/lu",
                "${gimac.services.collaboration}",
                "/api/v1/messagerie/conversations/c1/lu"
            ),
            Arguments.of("/bff/des/collaboration/messagerie/non-lus", "${gimac.services.collaboration}", "/api/v1/messagerie/non-lus"),
            Arguments.of("/bff/des/collaboration/messagerie/pieces/p1", "${gimac.services.collaboration}", "/api/v1/messagerie/pieces/p1"),
            Arguments.of(
                "/bff/console/collaboration/messagerie/conversations/c1/messages",
                "${gimac.services.collaboration}",
                "/api/v1/messagerie/conversations/c1/messages"
            ),
            Arguments.of("/bff/console/collaboration/messagerie/non-lus", "${gimac.services.collaboration}", "/api/v1/messagerie/non-lus"),
            // Processus d'unité (D-87, docs/lot1/processus-unite.md §5.1) : définitions et instances.
            Arguments.of("/bff/des/pilotage/processus", "${gimac.services.pilotage}", "/api/v1/processus"),
            Arguments.of("/bff/des/pilotage/processus/p1", "${gimac.services.pilotage}", "/api/v1/processus/p1"),
            Arguments.of("/bff/des/pilotage/processus/p1/publication", "${gimac.services.pilotage}", "/api/v1/processus/p1/publication"),
            Arguments.of("/bff/des/pilotage/processus/p1/instances", "${gimac.services.pilotage}", "/api/v1/processus/p1/instances"),
            Arguments.of("/bff/des/pilotage/processus/instances", "${gimac.services.pilotage}", "/api/v1/processus/instances"),
            Arguments.of(
                "/bff/des/pilotage/processus/instances/i1/etapes/RECETTE/signature",
                "${gimac.services.pilotage}",
                "/api/v1/processus/instances/i1/etapes/RECETTE/signature"
            ),
            Arguments.of("/bff/des/pilotage/processus/instances/i1/annulation", "${gimac.services.pilotage}", "/api/v1/processus/instances/i1/annulation"),
            // Supervision, astreintes et relève de quart (D-114, docs/lot1/astreintes-monitoring.md §8).
            Arguments.of("/bff/des/monitoring/alertes", "${gimac.services.monitoring}", "/api/v1/alertes"),
            Arguments.of("/bff/des/monitoring/alertes/a1/acquittement", "${gimac.services.monitoring}", "/api/v1/alertes/a1/acquittement"),
            Arguments.of("/bff/des/monitoring/grilles/generation", "${gimac.services.monitoring}", "/api/v1/grilles/generation"),
            Arguments.of("/bff/des/monitoring/rapports/r1/visa", "${gimac.services.monitoring}", "/api/v1/rapports/r1/visa"),
            Arguments.of("/bff/des/monitoring/vacations/v1/main-courante", "${gimac.services.monitoring}", "/api/v1/vacations/v1/main-courante")
        );
    }

    @Test
    @DisplayName("D-114 : supervision et astreintes par une seule route MS-MONITORING, OPA d'abord, sans couche console ni portail")
    void supervisionAstreintes() {
        for (String chemin : new String[] { "/bff/des/monitoring/liaisons", "/bff/des/monitoring/rapports/r1/signature", "/bff/des/monitoring/interne/alertes" }) {
            List<Route> r = correspondantes(chemin);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).id()).isEqualTo("des-monitoring");
            assertThat(r.get(0).filtres()).startsWith("OpaAuthorize");
            assertThat(r.get(0).filtres()).noneMatch(f -> f.contains("RemoveRequestHeader") || f.contains("Reauth"));
        }
        assertThat(correspondantes("/bff/console/monitoring/liaisons")).isEmpty();
        assertThat(correspondantes("/bff/portail/monitoring/liaisons")).isEmpty();
    }

    @Test
    @DisplayName("D-87 : processus d'unité par la route MS-PILOTAGE (une seule route, sans prédicat de méthode, OPA d'abord) ; aucun filtre ne retire X-Reauth-Assertion")
    @SuppressWarnings("unchecked")
    void processusUnite() throws Exception {
        for (String chemin : new String[] { "/bff/des/pilotage/processus", "/bff/des/pilotage/processus/instances/i1/etapes/E/signature" }) {
            List<Route> r = correspondantes(chemin);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).id()).isEqualTo("des-pilotage");
            assertThat(r.get(0).filtres()).startsWith("OpaAuthorize");
            assertThat(r.get(0).filtres()).noneMatch(f -> f.contains("RemoveRequestHeader") || f.contains("Reauth") || f.contains("SetRequestHeader"));
        }
        assertThat(correspondantes("/bff/console/pilotage/processus")).isEmpty();
        assertThat(correspondantes("/bff/portail/pilotage/processus")).isEmpty();
        try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/main/resources/config/application.yml"))) {
            for (Object doc : new Yaml().loadAll(in)) {
                if (!(doc instanceof Map<?, ?> racine)) continue;
                Object liste = chemin(racine, "spring", "cloud", "gateway", "server", "webflux", "routes");
                if (!(liste instanceof List<?> l)) continue;
                Map<String, Object> route = l.stream().map(o -> (Map<String, Object>) o).filter(m -> "des-pilotage".equals(m.get("id"))).findFirst().orElseThrow();
                // Aucune restriction de méthode : GET, POST, PUT et DELETE atteignent OPA, qui tranche.
                assertThat((List<Object>) route.get("predicates")).noneMatch(p -> p.toString().startsWith("Method"));
                Object defauts = chemin(racine, "spring", "cloud", "gateway", "server", "webflux", "default-filters");
                assertThat((List<Object>) defauts).noneMatch(f -> f.toString().contains("RemoveRequestHeader"));
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("chemins")
    @DisplayName("Une seule route, OPA d'abord, réécriture vers l'API v1 du service")
    void routage(String chemin, String service, String cible) {
        List<Route> r = correspondantes(chemin);
        assertThat(r).as("routes pour %s", chemin).hasSize(1);
        assertThat(r.get(0).uri()).isEqualTo(service);
        assertThat(r.get(0).filtres().get(0)).isEqualTo("OpaAuthorize");
        assertThat(reecrire(r.get(0), chemin)).isEqualTo(cible);
    }

    @Test
    @DisplayName("Messagerie : envois multipart de 5 pièces de 20 Mo admis, taille bornée")
    void tailleMessagerie() {
        Route r = correspondantes("/bff/des/collaboration/messagerie/conversations").get(0);
        String taille = r.filtres().stream().filter(f -> f.contains("RequestSize")).findFirst().orElseThrow();
        long max = DataSize.parse(taille.replaceAll(".*maxSize=([^,}]+).*", "$1")).toBytes();
        assertThat(max).isGreaterThan(5L * 20 * 1024 * 1024).isLessThanOrEqualTo(110L * 1024 * 1024);
    }

    @Test
    @DisplayName("Organigramme (D-102) : affectations et PDF par la seule route iam-organisation, réécrits vers /api/v1/organisation")
    void organigramme() {
        for (String couche : new String[] { "des", "console" }) {
            for (String[] c : new String[][] {
                { "/bff/" + couche + "/iam/organisation/unites/SNA/affectations", "/api/v1/organisation/unites/SNA/affectations" },
                { "/bff/" + couche + "/iam/organisation/unites/SNA/affectations/u-1", "/api/v1/organisation/unites/SNA/affectations/u-1" },
                { "/bff/" + couche + "/iam/organisation/organigramme.pdf", "/api/v1/organisation/organigramme.pdf" },
            }) {
                List<Route> l = correspondantes(c[0]);
                assertThat(l).extracting(Route::id).containsExactly("iam-organisation");
                assertThat(reecrire(l.get(0), c[0])).isEqualTo(c[1]);
            }
        }
        assertThat(correspondantes("/bff/portail/iam/organisation/organigramme.pdf")).isEmpty();
    }

    @Test
    @DisplayName("Dépôts fragmentés : une partie de 16 Mio admise, corps borné à 17 Mio")
    void tailleDepotsFragmentes() {
        for (String couche : new String[] { "des", "console", "portail" }) {
            Route r = correspondantes("/bff/" + couche + "/document/depots/d1/parties/1").get(0);
            assertThat(r.id()).isEqualTo("depot-fragmente");
            String taille = r.filtres().stream().filter(f -> f.contains("RequestSize")).findFirst().orElseThrow();
            long max = DataSize.parse(taille.replaceAll(".*maxSize=([^,}]+).*", "$1")).toBytes();
            assertThat(max).isEqualTo(17L * 1024 * 1024).isGreaterThan(16L * 1024 * 1024);
        }
    }

    @Test
    @DisplayName("Aperçu bureautique (D-83) : fichier de 20 Mo admis, corps borné à 20 Mo, délai de réponse d'au moins 70 s")
    @SuppressWarnings("unchecked")
    void apercuBureautique() throws Exception {
        for (String couche : new String[] { "des", "console" }) {
            Route r = correspondantes("/bff/" + couche + "/document/apercus").get(0);
            assertThat(r.id()).isEqualTo("document-apercus");
            String taille = r.filtres().stream().filter(f -> f.contains("RequestSize")).findFirst().orElseThrow();
            assertThat(DataSize.parse(taille.replaceAll(".*maxSize=([^,}]+).*", "$1")).toBytes()).isEqualTo(20L * 1024 * 1024);
        }
        try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/main/resources/config/application.yml"))) {
            for (Object doc : new Yaml().loadAll(in)) {
                if (!(doc instanceof Map<?, ?> racine)) continue;
                Object liste = chemin(racine, "spring", "cloud", "gateway", "server", "webflux", "routes");
                if (!(liste instanceof List<?> l)) continue;
                Map<String, Object> route = l.stream().map(o -> (Map<String, Object>) o).filter(m -> "document-apercus".equals(m.get("id"))).findFirst().orElseThrow();
                Object delai = ((Map<String, Object>) route.get("metadata")).get("response-timeout");
                assertThat(Long.parseLong(delai.toString())).isGreaterThanOrEqualTo(70_000L);
            }
        }
    }

    static Stream<String> fermes() {
        return Stream.of(
            // Aperçu bureautique : ni portail, ni chemin voisin.
            "/bff/portail/document/apercus",
            "/bff/des/document/apercus/1",
            // Dépôts fragmentés : ni autre service, ni chemin voisin.
            "/bff/des/document/depotsx",
            "/bff/portail/document/interne/documents/1",
            // Département (D-69) : ni serveur de courriels ni autre configuration ; portail : aucune administration.
            "/bff/des/iam/configuration/messagerie",
            "/bff/des/iam/configuration/messagerie/test",
            "/bff/portail/iam/comptes",
            "/bff/portail/iam/configuration/politique-securite",
            "/bff/des/iam/interne/reauthentifications/verification",
            "/bff/console/iam/interne/reauthentifications/verification",
            "/bff/des/document/interne/documents/1/empreinte",
            "/bff/des/document/documents/1",
            "/bff/portail/collaboration/messagerie/conversations",
            "/bff/des/live",
            "/bff/portail/live",
            "/bff/portail/iam/avatars/u-123",
            "/bff/des/iam/interne/collaborateurs/avatars",
            // Reprise (D-107) : console seulement, jamais les routes internes de reprise.
            "/bff/console/participant/interne/reprise/institutions",
            "/bff/console/homologation/interne/reprise/liaisons"
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fermes")
    @DisplayName("Administration hors console et département, courriels hors console, routes internes, lectures de documents, messagerie hors DES et canal temps réel : aucune route")
    void fermes(String chemin) {
        assertThat(correspondantes(chemin)).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
            "/services/iam/api/collaborators",
            "/services/iam/api/admin/users",
            "/services/participant/api/institutions",
            "/services/homologation/api/liaisons",
            "/services/certification/management/env",
            "/services/collaboration/management/loggers/ROOT",
            "/services/document/v3/api-docs",
            "/services/audit/management/health/readiness",
            "/services/gateway/api/admin/users",
        }
    )
    @DisplayName("Routes JHipster /services/{service}/** retirées : le CRUD généré et les points /management des services ne sont jamais relayés")
    void servicesNonRelayes(String chemin) {
        assertThat(correspondantes(chemin)).isEmpty();
    }

    @Test
    @DisplayName("Toute route déclarée est une route /bff des couches, filtrée par OpaAuthorize en premier")
    void toutesLesRoutesPassentParOpa() {
        assertThat(routes).allSatisfy(r -> {
            assertThat(r.filtres()).as("filtres de %s", r.id()).startsWith("OpaAuthorize");
            assertThat(r.chemins()).as("chemins de %s", r.id()).isNotEmpty().allSatisfy(p -> assertThat(p.getPatternString()).startsWith("/bff/"));
        });
    }

    @Test
    @DisplayName("mon-compte.md : « Mon compte » ferme les autres sessions (filtre passerelle) et borne la photo à 3 Mo ; avatars des collègues relus par OPA")
    void monCompte() {
        Route moi = correspondantes("/bff/des/iam/moi/mot-de-passe").get(0);
        assertThat(moi.filtres()).startsWith("OpaAuthorize").contains("FermetureAutresSessions");
        String taille = moi.filtres().stream().filter(f -> f.contains("RequestSize")).findFirst().orElseThrow();
        assertThat(DataSize.parse(taille.replaceAll(".*maxSize=([^,}]+).*", "$1")).toBytes()).isGreaterThanOrEqualTo(2L * 1024 * 1024).isLessThanOrEqualTo(4L * 1024 * 1024);
        Route avatars = correspondantes("/bff/des/iam/avatars/u-1").get(0);
        assertThat(avatars.id()).isEqualTo("iam-avatars");
        assertThat(avatars.filtres()).doesNotContain("FermetureAutresSessions");
    }
}
