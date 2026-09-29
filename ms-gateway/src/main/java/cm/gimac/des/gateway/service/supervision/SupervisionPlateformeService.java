package cm.gimac.des.gateway.service.supervision;

import cm.gimac.des.gateway.domain.supervision.Alerte;
import cm.gimac.des.gateway.domain.supervision.ChangementJournalisation;
import cm.gimac.des.gateway.domain.supervision.CibleCollecte;
import cm.gimac.des.gateway.domain.supervision.ConfigurationPlateforme;
import cm.gimac.des.gateway.domain.supervision.ConfigurationService;
import cm.gimac.des.gateway.domain.supervision.EtatAlertes;
import cm.gimac.des.gateway.domain.supervision.EtatConteneur;
import cm.gimac.des.gateway.domain.supervision.EtatGlobal;
import cm.gimac.des.gateway.domain.supervision.EtatHote;
import cm.gimac.des.gateway.domain.supervision.EtatSante;
import cm.gimac.des.gateway.domain.supervision.EtatServeurs;
import cm.gimac.des.gateway.domain.supervision.EtatService;
import cm.gimac.des.gateway.domain.supervision.ImpulsionPlateforme;
import cm.gimac.des.gateway.domain.supervision.InfoService;
import cm.gimac.des.gateway.domain.supervision.InstantanePlateforme;
import cm.gimac.des.gateway.domain.supervision.MasqueSecrets;
import cm.gimac.des.gateway.domain.supervision.Metriques;
import cm.gimac.des.gateway.domain.supervision.NiveauLogger;
import cm.gimac.des.gateway.domain.supervision.NiveauxJournalisation;
import cm.gimac.des.gateway.domain.supervision.ProprietesExploitation;
import cm.gimac.des.gateway.domain.supervision.RouteVue;
import cm.gimac.des.gateway.domain.supervision.ServiceCible;
import cm.gimac.des.gateway.domain.supervision.ServiceInconnuException;
import cm.gimac.des.gateway.domain.supervision.port.SondeService;
import cm.gimac.des.gateway.domain.supervision.port.SourceAlertes;
import cm.gimac.des.gateway.domain.supervision.port.SourceRoutes;
import cm.gimac.des.gateway.domain.supervision.port.SourceServeurs;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service d'application de la supervision de la plateforme (D-61) : interroge en parallèle
 * chaque service, Prometheus et Alertmanager au travers de leurs ports, avec un délai court
 * par appel ; un service lent ou arrêté est rapporté INJOIGNABLE sans retarder les autres.
 */
public class SupervisionPlateformeService {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisionPlateformeService.class);

    /** Niveaux acceptés par /management/loggers ; « null » rétablit le niveau hérité. */
    public static final Set<String> NIVEAUX = Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF", "NULL");
    private static final Pattern NOM_LOGGER = Pattern.compile("^[A-Za-z0-9_$.-]{1,200}$");
    static final int MOTIF_MIN = 5;
    static final int MOTIF_MAX = 500;

    /** Paramètres : services supervisés (dans l'ordre), délai par appel, période du flux, sonde de Keycloak. */
    public record Parametres(List<ServiceCible> services, Duration delai, Duration intervalleFlux, String keycloakSanteUrl) {
        public Parametres {
            services = List.copyOf(services);
        }
    }

    private final Parametres parametres;
    private final SondeService sonde;
    private final SourceServeurs serveurs;
    private final SourceAlertes alertes;
    private final SourceRoutes routes;
    private final Clock horloge;

    public SupervisionPlateformeService(
        Parametres parametres,
        SondeService sonde,
        SourceServeurs serveurs,
        SourceAlertes alertes,
        SourceRoutes routes,
        Clock horloge
    ) {
        this.parametres = parametres;
        this.sonde = sonde;
        this.serveurs = serveurs;
        this.alertes = alertes;
        this.routes = routes;
        this.horloge = horloge;
    }

    public List<ServiceCible> services() {
        return parametres.services();
    }

    // ------------------------------------------------------------------ Services

    /** Instantané de tous les services, interrogés en parallèle, dans l'ordre de la configuration. */
    public Mono<InstantanePlateforme> instantane() {
        List<ServiceCible> cibles = parametres.services();
        Mono<List<EtatService>> etats = Flux.fromIterable(cibles)
            .flatMapSequential(this::etatService, Math.max(1, cibles.size()))
            .collectList();
        Mono<EtatService> keycloak = keycloak();
        return Mono.zip(etats, keycloak.map(Optional::of).defaultIfEmpty(Optional.empty())).map(t ->
            new InstantanePlateforme(horloge.instant(), EtatGlobal.de(t.getT1()), t.getT1(), t.getT2().orElse(null))
        );
    }

    /** État d'un service : santé d'abord (latence mesurée), puis construction et métriques en parallèle. */
    Mono<EtatService> etatService(ServiceCible cible) {
        return Mono.defer(() -> {
            long debut = System.nanoTime();
            return borne(sonde.sante(cible))
                .flatMap(sante -> {
                    long latence = Duration.ofNanos(System.nanoTime() - debut).toMillis();
                    Mono<InfoService> info = borne(sonde.info(cible)).onErrorResume(e -> Mono.just(InfoService.VIDE)).defaultIfEmpty(InfoService.VIDE);
                    Mono<Metriques> metriques = borne(sonde.metriques(cible))
                        .onErrorResume(e -> Mono.just(Metriques.VIDES))
                        .defaultIfEmpty(Metriques.VIDES);
                    return Mono.zip(info, metriques).map(t ->
                        new EtatService(
                            cible.nom(),
                            cible.url(),
                            sante.etat(),
                            latence,
                            sante.composants(),
                            t.getT1(),
                            t.getT2(),
                            null,
                            horloge.instant()
                        )
                    );
                })
                .switchIfEmpty(Mono.fromSupplier(() -> injoignable(cible, "réponse vide")))
                .onErrorResume(e -> Mono.just(injoignable(cible, cause(e))));
        });
    }

    private Mono<EtatService> keycloak() {
        String url = parametres.keycloakSanteUrl();
        if (url == null || url.isBlank()) {
            return Mono.empty();
        }
        ServiceCible cible = new ServiceCible("keycloak", url);
        return Mono.defer(() -> {
            long debut = System.nanoTime();
            return borne(sonde.santeExterne(url))
                .map(sante ->
                    new EtatService(
                        cible.nom(),
                        url,
                        sante.etat(),
                        Duration.ofNanos(System.nanoTime() - debut).toMillis(),
                        sante.composants(),
                        InfoService.VIDE,
                        Metriques.VIDES,
                        null,
                        horloge.instant()
                    )
                )
                .onErrorResume(e -> Mono.just(injoignable(cible, cause(e))));
        });
    }

    private EtatService injoignable(ServiceCible cible, String erreur) {
        return new EtatService(
            cible.nom(),
            cible.url(),
            EtatSante.INJOIGNABLE,
            null,
            Map.of(),
            InfoService.VIDE,
            Metriques.VIDES,
            erreur,
            horloge.instant()
        );
    }

    // ------------------------------------------------------------------ Serveurs et alertes

    /** Cibles de collecte, hôtes et conteneurs d'après Prometheus. */
    public Mono<EtatServeurs> etatServeurs() {
        Mono<List<EtatHote>> hotes = borne(serveurs.hotes()).onErrorResume(e -> Mono.just(List.of())).defaultIfEmpty(List.of());
        Mono<List<EtatConteneur>> conteneurs = borne(serveurs.conteneurs()).onErrorResume(e -> Mono.just(List.of())).defaultIfEmpty(List.of());
        return borne(serveurs.cibles())
            .defaultIfEmpty(List.of())
            .flatMap(cibles ->
                Mono.zip(hotes, conteneurs).map(t -> {
                    int up = (int) cibles.stream().filter(c -> "up".equals(c.etat())).count();
                    return new EtatServeurs(
                        horloge.instant(),
                        true,
                        null,
                        up,
                        cibles.size() - up,
                        cibles,
                        !t.getT1().isEmpty(),
                        t.getT1(),
                        t.getT2()
                    );
                })
            )
            .onErrorResume(e ->
                Mono.just(new EtatServeurs(horloge.instant(), false, "Prometheus injoignable : " + cause(e), 0, 0, List.<CibleCollecte>of(), false, List.of(), List.of()))
            );
    }

    /** Alertes actives d'Alertmanager. */
    public Mono<EtatAlertes> etatAlertes() {
        return borne(alertes.alertesActives())
            .defaultIfEmpty(List.of())
            .map(liste -> new EtatAlertes(horloge.instant(), true, null, liste.size(), liste))
            .onErrorResume(e ->
                Mono.just(new EtatAlertes(horloge.instant(), false, "Alertmanager injoignable : " + cause(e), 0, List.<Alerte>of()))
            );
    }

    // ------------------------------------------------------------------ Configuration

    /** Routes de la passerelle et configuration de chaque service, secrets masqués. */
    public Mono<ConfigurationPlateforme> configuration() {
        Mono<List<RouteVue>> lesRoutes = routes.routes().collectList();
        List<ServiceCible> cibles = parametres.services();
        Mono<List<ConfigurationService>> lesServices = Flux.fromIterable(cibles)
            .flatMapSequential(this::configurationService, Math.max(1, cibles.size()))
            .collectList();
        return Mono.zip(lesRoutes, lesServices).map(t -> new ConfigurationPlateforme(horloge.instant(), t.getT1(), t.getT2()));
    }

    Mono<ConfigurationService> configurationService(ServiceCible cible) {
        return borne(sonde.environnement(cible))
            .map(env ->
                new ConfigurationService(
                    cible.nom(),
                    cible.url(),
                    true,
                    null,
                    env.profilsActifs(),
                    ProprietesExploitation.selectionner(env.proprietes())
                )
            )
            .switchIfEmpty(Mono.fromSupplier(() -> new ConfigurationService(cible.nom(), cible.url(), false, "réponse vide", List.of(), Map.of())))
            .onErrorResume(e -> Mono.just(new ConfigurationService(cible.nom(), cible.url(), false, cause(e), List.of(), Map.of())));
    }

    // ------------------------------------------------------------------ Journalisation

    /** Niveaux de journalisation d'un service, filtrés sur un préfixe de nom de logger s'il est donné. */
    public Mono<NiveauxJournalisation> journalisation(String nomService, String filtre) {
        return Mono.defer(() -> borne(sonde.journalisation(cible(nomService)))).map(n -> {
            if (filtre == null || filtre.isBlank()) {
                return n;
            }
            List<NiveauLogger> retenus = n.loggers().stream().filter(l -> l.nom().startsWith(filtre.strip())).toList();
            return new NiveauxJournalisation(n.service(), n.niveauxDisponibles(), retenus);
        });
    }

    /** Change à chaud le niveau d'un logger ; le motif est obligatoire et le changement journalisé. */
    public Mono<ChangementJournalisation> modifierJournalisation(String nomService, String logger, String niveau, String motif, String auteur) {
        return Mono.defer(() -> modifier(cible(nomService), logger, niveau, motif, auteur));
    }

    private Mono<ChangementJournalisation> modifier(ServiceCible cible, String logger, String niveau, String motif, String auteur) {
        String nomLogger = logger == null ? "" : logger.strip();
        if (!NOM_LOGGER.matcher(nomLogger).matches()) {
            return Mono.error(new IllegalArgumentException("Nom de logger invalide"));
        }
        String n = niveau == null ? "" : niveau.strip().toUpperCase(Locale.ROOT);
        if (!NIVEAUX.contains(n)) {
            return Mono.error(new IllegalArgumentException("Niveau invalide : attendu TRACE, DEBUG, INFO, WARN, ERROR, OFF ou NULL"));
        }
        String m = motif == null ? "" : motif.strip();
        if (m.length() < MOTIF_MIN || m.length() > MOTIF_MAX) {
            return Mono.error(new IllegalArgumentException("Motif obligatoire (" + MOTIF_MIN + " à " + MOTIF_MAX + " caractères)"));
        }
        String niveauApplique = "NULL".equals(n) ? null : n;
        return borne(sonde.modifierNiveau(cible, nomLogger, niveauApplique)).then(
            Mono.fromSupplier(() -> {
                ChangementJournalisation c = new ChangementJournalisation(
                    cible.nom(),
                    nomLogger,
                    n,
                    MasqueSecrets.masquerValeur(m),
                    auteur,
                    horloge.instant()
                );
                LOG.info(
                    "Supervision : niveau de journalisation modifié, service={}, logger={}, niveau={}, auteur={}, motif={}",
                    c.service(),
                    c.logger(),
                    c.niveau(),
                    c.auteur(),
                    c.motif()
                );
                return c;
            })
        );
    }

    // ------------------------------------------------------------------ Temps réel

    /**
     * Flux périodique : instantané des services et nombre d'alertes actives (-1 si Alertmanager
     * est injoignable). Une collecte plus longue que la période fait sauter les échéances
     * manquées ; les impulsions restent numérotées sans trou.
     */
    public Flux<ImpulsionPlateforme> flux() {
        return Flux.interval(Duration.ZERO, parametres.intervalleFlux())
            .onBackpressureDrop()
            .concatMap(echeance -> Mono.zip(instantane(), etatAlertes()), 0)
            .index((sequence, t) -> new ImpulsionPlateforme(sequence, t.getT1(), t.getT2().joignable() ? t.getT2().nombre() : -1));
    }

    // ------------------------------------------------------------------ Outils

    ServiceCible cible(String nom) {
        return parametres
            .services()
            .stream()
            .filter(s -> s.nom().equals(nom))
            .findFirst()
            .orElseThrow(() -> new ServiceInconnuException(nom));
    }

    private <T> Mono<T> borne(Mono<T> appel) {
        return Mono.defer(() -> appel).timeout(parametres.delai());
    }

    static String cause(Throwable e) {
        if (e instanceof TimeoutException) {
            return "délai dépassé";
        }
        String message = e.getMessage();
        return MasqueSecrets.masquerValeur(message == null || message.isBlank() ? e.getClass().getSimpleName() : message);
    }
}
