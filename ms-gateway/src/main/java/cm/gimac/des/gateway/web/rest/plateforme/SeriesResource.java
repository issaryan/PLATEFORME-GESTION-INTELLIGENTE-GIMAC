package cm.gimac.des.gateway.web.rest.plateforme;

import cm.gimac.des.gateway.domain.supervision.MetriqueSerie;
import cm.gimac.des.gateway.domain.supervision.PeriodeSerie;
import cm.gimac.des.gateway.domain.supervision.SeriesMetriques;
import cm.gimac.des.gateway.service.supervision.SeriesMetriquesService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Séries temporelles des métriques pour les graphiques de la supervision : métrique et période choisies
 * dans des listes fermées, cible facultative (service, conteneur ou hôte). Lecture réservée aux détenteurs
 * de la supervision (PLATFORM_SUPERVISION_READ), avec second facteur comme toute route /api/plateforme.
 */
@RestController
@RequestMapping("/api/plateforme")
public class SeriesResource {

    private final SeriesMetriquesService series;

    public SeriesResource(SeriesMetriquesService series) {
        this.series = series;
    }

    @GetMapping("/series")
    @PreAuthorize(PlateformeResource.LECTURE_SUPERVISION)
    public Mono<SeriesMetriques> series(
        @RequestParam MetriqueSerie metrique,
        @RequestParam(defaultValue = "UNE_HEURE") PeriodeSerie periode,
        @RequestParam(required = false) String cible
    ) {
        return series.series(metrique, periode, cible);
    }

    /** Cible hors du jeu de caractères admis : 400, sans écho de la valeur reçue. */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> cibleInvalide(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "PLATFORM_SERIES_TARGET_INVALID", "message", e.getMessage()));
    }
}
