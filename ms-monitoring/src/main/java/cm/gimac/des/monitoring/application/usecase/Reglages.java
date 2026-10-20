package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages {@code gimac.monitoring.*} (D-114) : fuseau de la plateforme (plages de vacation), fenêtres de
 * déduplication (30 min) et de corrélation (10 min) de l'algorithme 6.44, délai de contresignature (30 min,
 * algorithme 6.46), paramètres de la grille (algorithme 6.45, limite de 300 s de D-15).
 */
@ConfigurationProperties(prefix = "gimac.monitoring")
public record Reglages(ZoneId fuseau, Duration deduplication, Duration correlation, Duration contresignature, Grille grille) {
    public Reglages {
        fuseau = fuseau == null ? ZoneId.of("Africa/Douala") : fuseau;
        deduplication = deduplication == null ? Duration.ofMinutes(30) : deduplication;
        correlation = correlation == null ? Duration.ofMinutes(10) : correlation;
        contresignature = contresignature == null ? Duration.ofMinutes(30) : contresignature;
        grille = grille == null ? new Grille(null, null, null, null, null, null) : grille;
    }

    public static Reglages defaut() {
        return new Reglages(null, null, null, null, null);
    }

    /** Paramètres de la grille ; une valeur absente prend celle du CC. */
    public record Grille(Integer limiteSecondes, Integer besoinSalle, Integer besoinAstreinte, Integer reposHeures, Integer nuitsMax, Integer plafondHeures) {
        public Duration limite() {
            return Duration.ofSeconds(limiteSecondes == null ? 300 : limiteSecondes);
        }

        /** Contraintes générales de D-117 ; effectifs et chef de quart relèvent désormais de chaque créneau. */
        public ProblemeGrille.Parametres parametres() {
            ProblemeGrille.Parametres d = ProblemeGrille.Parametres.DEFAUT;
            return new ProblemeGrille.Parametres(
                reposHeures == null ? d.reposHeures() : reposHeures,
                nuitsMax == null ? d.nuitsMax() : nuitsMax,
                plafondHeures == null ? d.plafondHeures() : plafondHeures
            );
        }
    }
}
