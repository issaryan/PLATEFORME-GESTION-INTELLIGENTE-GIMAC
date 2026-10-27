package cm.gimac.des.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.config.RoutesPresentationTest.Route;
import cm.gimac.des.gateway.domain.sandbox.RouteSandbox;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Routes de la sandbox dans MS-GATEWAY (D-120, US-EXI-07), lues dans application.yml : revue de configuration de la
 * matrice des routes contre les onze services, de l'absence de route de conduite dans l'application du département
 * (hors sbx-espace, D-118) et de l'absence de route vers un système de production (GW-21).
 */
class RoutesSandboxTest {

    @BeforeAll
    static void lire() throws Exception {
        RoutesPresentationTest.lire();
    }

    static List<Route> sandbox() {
        return RoutesPresentationTest.routes.stream().filter(r -> r.chemins().stream().anyMatch(p -> p.getPatternString().matches("^/bff/[a-z]+/sbx-.*"))).toList();
    }

    @Test
    @DisplayName("Sécurité : matrice des routes déclarées contre les onze services, console et portail, OPA puis cloisonnement")
    void matriceDesRoutes() {
        for (String service : RouteSandbox.MODULES.keySet()) {
            for (String[] c : new String[][] { { "console", "console" }, { "portail", "portail" } }) {
                String chemin = "/bff/" + c[0] + "/sbx-" + service + "/ressources/r1";
                List<Route> r = RoutesPresentationTest.correspondantes(chemin);
                assertThat(r).as(chemin).hasSize(1);
                assertThat(r.get(0).id()).isEqualTo(c[0] + "-sbx-" + service);
                assertThat(r.get(0).uri()).isEqualTo("${gimac.services.sbx-" + service + "}");
                assertThat(r.get(0).filtres().subList(0, 2)).containsExactly("OpaAuthorize", "CloisonnementSandbox");
                assertThat(RoutesPresentationTest.reecrire(r.get(0), chemin)).isEqualTo("/api/v1/" + c[1] + "/ressources/r1");
            }
        }
        // Administration déléguée (D-115) : toujours vers MS-IAM, sans collision.
        assertThat(RoutesPresentationTest.correspondantes("/bff/portail/sbx-habilitations/comptes")).extracting(Route::id).containsExactly("portail-sbx-habilitations");
    }

    @Test
    @DisplayName("US-EXI-08, 09, 10 : routes de la console vers les services transverses adaptés pour la sandbox")
    void routesTransverses() {
        for (String[] c : new String[][] {
            { "/bff/console/document/sandbox/types-pieces", "console-document-sandbox", "${gimac.services.document}", "/api/v1/sandbox/types-pieces" },
            { "/bff/console/document/sandbox/types-pieces/SBX_TRACE_EXPORTEE/retention", "console-document-sandbox", "${gimac.services.document}", "/api/v1/sandbox/types-pieces/SBX_TRACE_EXPORTEE/retention" },
        }) {
            List<Route> r = RoutesPresentationTest.correspondantes(c[0]);
            assertThat(r).extracting(Route::id).containsExactly(c[1]);
            assertThat(r.get(0).uri()).isEqualTo(c[2]);
            assertThat(r.get(0).filtres().get(0)).isEqualTo("OpaAuthorize");
            assertThat(RoutesPresentationTest.reecrire(r.get(0), c[0])).isEqualTo(c[3]);
        }
    }

    @Test
    @DisplayName("Critère 3 : seules l'administration des espaces et la supervision D-121 sont ouvertes dans le département")
    void routeDeConduiteDansLeDepartement() {
        List<Route> des = sandbox().stream().filter(r -> r.chemins().stream().anyMatch(p -> p.getPatternString().startsWith("/bff/des/"))).toList();
        assertThat(des).extracting(Route::id).containsExactly("des-sbx-espace", "des-sbx-trace");
        Route espace = des.get(0);
        assertThat(espace.uri()).isEqualTo("${gimac.services.sbx-espace}");
        assertThat(RoutesPresentationTest.reecrire(espace, "/bff/des/sbx-espace/espaces/99001/suspension")).isEqualTo("/api/v1/console/espaces/99001/suspension");
        for (String service : RouteSandbox.MODULES.keySet()) {
            if (!"espace".equals(service)) {
                assertThat(RoutesPresentationTest.correspondantes("/bff/des/sbx-" + service + "/x")).as(service).isEmpty();
            }
        }
    }

    @Test
    @DisplayName("D-121 : quatre lectures exactes de SBX-TRACE, réécrites vers la console, aucun chemin voisin")
    void supervisionDansLeDepartement() {
        for (String chemin : new String[] { "etat-sante/detail", "elements-supervises", "alertes", "disponibilites" }) {
            String origine = "/bff/des/sbx-trace/" + chemin;
            List<Route> routes = RoutesPresentationTest.correspondantes(origine);
            assertThat(routes).extracting(Route::id).containsExactly("des-sbx-trace");
            assertThat(routes.get(0).uri()).isEqualTo("${gimac.services.sbx-trace}");
            assertThat(routes.get(0).filtres().subList(0, 2)).containsExactly("OpaAuthorize", "CloisonnementSandbox");
            assertThat(RoutesPresentationTest.reecrire(routes.get(0), origine)).isEqualTo("/api/v1/console/" + chemin);
        }
        for (String chemin : new String[] { "/bff/des/sbx-trace/traces", "/bff/des/sbx-trace/etat-sante", "/bff/des/sbx-trace/alertes/a1", "/bff/des/sbx-trace/disponibilites/2026-10" }) {
            assertThat(RoutesPresentationTest.correspondantes(chemin)).as(chemin).isEmpty();
        }
    }

    @Test
    @DisplayName("GW-21 : toute route de la sandbox vise un service de la sandbox, adresses sbx-* locales ou internes, jamais une adresse IP")
    @SuppressWarnings("unchecked")
    void aucuneRouteVersLaProduction() throws Exception {
        // Onze services en console et portail, espaces et supervision dans le département, administration déléguée.
        assertThat(sandbox()).hasSize(11 * 2 + 2 + 1);
        for (Route r : sandbox()) {
            if (r.id().equals("portail-sbx-habilitations")) {
                assertThat(r.uri()).isEqualTo("${gimac.services.iam}");
                continue;
            }
            assertThat(r.uri()).as(r.id()).matches("^\\$\\{gimac\\.services\\.sbx-[a-z0-9]+}$");
            assertThat(r.filtres()).as(r.id()).contains("CloisonnementSandbox");
        }
        for (String fichier : new String[] { "application.yml", "application-prod.yml" }) {
            try (InputStream in = Files.newInputStream(Path.of("src/main/resources/config/" + fichier))) {
                for (Object doc : new Yaml().loadAll(in)) {
                    if (!(doc instanceof Map<?, ?> racine)) continue;
                    Object services = RoutesPresentationTest.chemin(racine, "gimac", "services");
                    if (!(services instanceof Map<?, ?> m)) continue;
                    for (String service : RouteSandbox.MODULES.keySet()) {
                        String adresse = String.valueOf(((Map<String, Object>) m).get("sbx-" + service));
                        assertThat(adresse).as(fichier + " sbx-" + service).matches("^http://(localhost|sbx-" + service + "):81(0[1-9]|1[01])$");
                    }
                    assertThat(((Map<String, Object>) m).get("sbx-espace").toString()).endsWith(":8101");
                }
            }
        }
    }
}
