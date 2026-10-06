package cm.gimac.des.gateway.infrastructure.supervision;

import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.carte;
import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.liste;
import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.nombre;
import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.texte;

import cm.gimac.des.gateway.domain.supervision.CibleCollecte;
import cm.gimac.des.gateway.domain.supervision.EtatConteneur;
import cm.gimac.des.gateway.domain.supervision.EtatHote;
import cm.gimac.des.gateway.domain.supervision.FormatPrometheus;
import cm.gimac.des.gateway.domain.supervision.port.SourceServeurs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adaptateur WebClient du port {@link SourceServeurs} : API HTTP de Prometheus. Les cibles
 * viennent de /api/v1/targets ; l'état des hôtes (node-exporter) et des conteneurs (cAdvisor),
 * de requêtes PromQL instantanées (/api/v1/query).
 */
public class PrometheusWebClientAdapter implements SourceServeurs {

    static final String CPU_HOTE = "100 * (1 - avg by (instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[1m])))";
    static final String MEMOIRE_HOTE = "100 * (1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)";
    static final String MEMOIRE_TOTALE_HOTE = "node_memory_MemTotal_bytes";
    static final String SYSTEMES_FICHIERS = "fstype!~\"tmpfs|overlay|squashfs|ramfs|nsfs\"";
    static final String DISQUE_HOTE =
        "100 * max by (instance) (1 - node_filesystem_avail_bytes{" +
        SYSTEMES_FICHIERS +
        "} / node_filesystem_size_bytes{" +
        SYSTEMES_FICHIERS +
        "})";
    static final String CHARGE_HOTE = "node_load1";
    static final String CPU_CONTENEUR = "sum by (name) (rate(container_cpu_usage_seconds_total{name!=\"\"}[1m]))";
    static final String MEMOIRE_CONTENEUR = "sum by (name) (container_memory_working_set_bytes{name!=\"\"})";

    private final WebClient client;
    private final String cpuConteneur;
    private final String memoireConteneur;

    /** @param client client dont l'adresse de base est celle de Prometheus */
    public PrometheusWebClientAdapter(WebClient client) {
        this(client, "");
    }

    /**
     * @param client client dont l'adresse de base est celle de Prometheus
     * @param projet projet Docker Compose dont les conteneurs sont restitués (étiquette
     *     com.docker.compose.project exportée par cAdvisor) ; vide : tous les conteneurs de l'hôte. Le serveur
     *     de démonstration héberge d'autres applications : seuls ceux de la plateforme y sont montrés.
     */
    public PrometheusWebClientAdapter(WebClient client, String projet) {
        this.client = client;
        String selecteur = projet == null || projet.isBlank()
            ? "name!=\"\""
            : "name!=\"\",container_label_com_docker_compose_project=\"" + projet.replace("\"", "") + "\"";
        this.cpuConteneur = CPU_CONTENEUR.replace("name!=\"\"", selecteur);
        this.memoireConteneur = MEMOIRE_CONTENEUR.replace("name!=\"\"", selecteur);
    }

    @Override
    public Mono<List<CibleCollecte>> cibles() {
        return client
            .get()
            .uri(b -> b.path("/api/v1/targets").queryParam("state", "active").build())
            .retrieve()
            .bodyToMono(ActuatorWebClientAdapter.CARTE)
            .map(PrometheusWebClientAdapter::lireCibles);
    }

    static List<CibleCollecte> lireCibles(Map<String, Object> corps) {
        List<CibleCollecte> cibles = new ArrayList<>();
        for (Object o : liste(carte(corps.get("data")).get("activeTargets"))) {
            Map<String, Object> cible = carte(o);
            Map<String, Object> etiquettes = carte(cible.get("labels"));
            String erreur = texte(cible.get("lastError"));
            cibles.add(
                new CibleCollecte(
                    texte(etiquettes.get("job")),
                    texte(Optional.ofNullable(etiquettes.get("instance")).orElse(cible.get("scrapeUrl"))),
                    texte(Optional.ofNullable(cible.get("health")).orElse("unknown")),
                    ClientsSupervision.instant(cible.get("lastScrape")),
                    nombre(cible.get("lastScrapeDuration")),
                    erreur == null || erreur.isEmpty() ? null : erreur
                )
            );
        }
        cibles.sort(Comparator.comparing((CibleCollecte c) -> String.valueOf(c.job())).thenComparing(c -> String.valueOf(c.instance())));
        return cibles;
    }

    @Override
    public Mono<List<EtatHote>> hotes() {
        return Mono.zip(
            requete(CPU_HOTE, "instance"),
            requete(MEMOIRE_HOTE, "instance"),
            requete(MEMOIRE_TOTALE_HOTE, "instance"),
            requete(DISQUE_HOTE, "instance"),
            requete(CHARGE_HOTE, "instance")
        ).map(t -> {
            TreeSet<String> instances = new TreeSet<>();
            instances.addAll(t.getT1().keySet());
            instances.addAll(t.getT2().keySet());
            instances.addAll(t.getT3().keySet());
            instances.addAll(t.getT4().keySet());
            instances.addAll(t.getT5().keySet());
            return instances
                .stream()
                .map(i ->
                    new EtatHote(
                        i,
                        arrondi(t.getT1().get(i)),
                        arrondi(t.getT2().get(i)),
                        t.getT3().get(i),
                        arrondi(t.getT4().get(i)),
                        t.getT5().get(i)
                    )
                )
                .toList();
        });
    }

    @Override
    public Mono<List<EtatConteneur>> conteneurs() {
        return Mono.zip(requete(cpuConteneur, "name"), requete(memoireConteneur, "name")).map(t -> {
            TreeSet<String> noms = new TreeSet<>(t.getT1().keySet());
            noms.addAll(t.getT2().keySet());
            return noms.stream().map(n -> new EtatConteneur(n, t.getT1().get(n), t.getT2().get(n))).toList();
        });
    }

    /** Requête PromQL instantanée : valeur de chaque série, indexée par l'étiquette donnée. */
    Mono<Map<String, Double>> requete(String promql, String etiquette) {
        return client
            .get()
            .uri(b -> b.path("/api/v1/query").queryParam("query", "{q}").build(promql))
            .retrieve()
            .bodyToMono(ActuatorWebClientAdapter.CARTE)
            .map(corps -> lireVecteur(corps, etiquette));
    }

    static Map<String, Double> lireVecteur(Map<String, Object> corps, String etiquette) {
        Map<String, Double> valeurs = new TreeMap<>();
        for (Object o : liste(carte(corps.get("data")).get("result"))) {
            Map<String, Object> serie = carte(o);
            Object cle = carte(serie.get("metric")).get(etiquette);
            List<?> valeur = liste(serie.get("value"));
            Double v = valeur.size() == 2 ? nombre(valeur.get(1)) : null;
            if (cle != null && v != null) {
                valeurs.put(cle.toString(), v);
            }
        }
        return valeurs;
    }

    private static Double arrondi(Double v) {
        return v == null ? null : FormatPrometheus.arrondi(v);
    }
}
