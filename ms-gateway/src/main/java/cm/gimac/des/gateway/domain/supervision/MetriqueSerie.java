package cm.gimac.des.gateway.domain.supervision;

/**
 * Métriques historisées exposées en séries temporelles : liste fermée. La passerelle ne relaie jamais une
 * requête PromQL fournie par le navigateur ; chaque métrique a sa requête, l'étiquette qui nomme ses séries
 * et son unité. {@code {S}} est remplacé par le sélecteur des conteneurs de la plateforme (projet Docker
 * Compose), {@code {F}} par le filtre facultatif sur une cible (service, conteneur ou hôte).
 */
public enum MetriqueSerie {
    /** Charge processeur de toute la plateforme, en % d'un cœur (somme des conteneurs). */
    PLATEFORME_CPU("100 * sum(rate(container_cpu_usage_seconds_total{S}[2m]))", null, "%"),
    /** Charge processeur par conteneur, en % d'un cœur. */
    CONTENEUR_CPU("100 * sum by (name) (rate(container_cpu_usage_seconds_total{S,F}[2m]))", "name", "%"),
    /** Mémoire résidente par conteneur, en Mo. */
    CONTENEUR_MEMOIRE("sum by (name) (container_memory_working_set_bytes{S,F}) / 1048576", "name", "Mo"),
    /**
     * Processeur par charge de travail Docker (service Compose, nom des charges de l'écran Infrastructure),
     * en % d'un cœur, toutes répliques confondues.
     */
    CHARGE_CPU(
        "100 * sum by (container_label_com_docker_compose_service) (rate(container_cpu_usage_seconds_total{S,F}[2m]))",
        "container_label_com_docker_compose_service",
        "%"
    ),
    /** Mémoire résidente par charge de travail Docker (service Compose), en Mo. */
    CHARGE_MEMOIRE(
        "sum by (container_label_com_docker_compose_service) (container_memory_working_set_bytes{S,F}) / 1048576",
        "container_label_com_docker_compose_service",
        "Mo"
    ),
    /** Processeur des hôtes (node-exporter), en %. */
    HOTE_CPU("100 * (1 - avg by (instance) (rate(node_cpu_seconds_total{mode=\"idle\",F}[2m])))", "instance", "%"),
    /** Mémoire occupée des hôtes, en %. */
    HOTE_MEMOIRE("100 * (1 - node_memory_MemAvailable_bytes{F} / node_memory_MemTotal_bytes{F})", "instance", "%"),
    /** Disque le plus plein de chaque hôte, en %. */
    HOTE_DISQUE(
        "100 * max by (instance) (1 - node_filesystem_avail_bytes{fstype!~\"tmpfs|overlay|squashfs|ramfs|nsfs\",F}" +
        " / node_filesystem_size_bytes{fstype!~\"tmpfs|overlay|squashfs|ramfs|nsfs\",F})",
        "instance",
        "%"
    ),
    /** Débit des requêtes HTTP par service, en requêtes par seconde. */
    SERVICE_DEBIT("sum by (application) (rate(http_server_requests_seconds_count{F}[2m]))", "application", "req/s"),
    /** Latence au 95e centile par service, en millisecondes. */
    SERVICE_LATENCE_P95(
        "1000 * histogram_quantile(0.95, sum by (application, le) (rate(http_server_requests_seconds_bucket{F}[5m])))",
        "application",
        "ms"
    ),
    /** Part des réponses en erreur serveur (5xx) par service, en %. */
    SERVICE_ERREURS(
        "100 * sum by (application) (rate(http_server_requests_seconds_count{status=~\"5..\",F}[5m]))" +
        " / sum by (application) (rate(http_server_requests_seconds_count{F}[5m]))",
        "application",
        "%"
    ),
    /** Tas JVM occupé par service, en Mo. */
    SERVICE_TAS_JVM("sum by (application) (jvm_memory_used_bytes{area=\"heap\",F}) / 1048576", "application", "Mo");

    private final String modele;
    private final String etiquette;
    private final String unite;

    MetriqueSerie(String modele, String etiquette, String unite) {
        this.modele = modele;
        this.etiquette = etiquette;
        this.unite = unite;
    }

    /** Étiquette qui nomme chaque série ; null pour une série unique (agrégat de la plateforme). */
    public String etiquette() {
        return etiquette;
    }

    public String unite() {
        return unite;
    }

    /**
     * Requête PromQL de la métrique.
     *
     * @param selecteurConteneurs matcher des conteneurs de la plateforme (ex. {@code name!=""}) ; jamais vide
     * @param cible valeur exacte de l'étiquette à retenir, déjà validée ; null ou vide : toutes les séries
     */
    public String requete(String selecteurConteneurs, String cible) {
        String filtre = etiquette == null || cible == null || cible.isBlank() ? "" : etiquette + "=\"" + cible + "\"";
        return modele.replace("{S,F}", "{" + joindre(selecteurConteneurs, filtre) + "}")
            .replace("{S}", "{" + selecteurConteneurs + "}")
            .replace(",F}", filtre.isEmpty() ? "}" : "," + filtre + "}")
            .replace("{F}", filtre.isEmpty() ? "" : "{" + filtre + "}");
    }

    private static String joindre(String a, String b) {
        return b.isEmpty() ? a : a + "," + b;
    }
}
