package cm.gimac.des.certification.application.usecase;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Contrôle des gestes par l'opération du parcours (D-93 §4, {@code gimac.certification.operations}).
 * <ul>
 * <li>{@code liaisonSansProjection} : ADMETTRE (défaut, transitoire) ou REFUSER un geste sur une liaison dont aucun
 * événement d'opération n'a encore été reçu (liaison ouverte avant le déploiement).</li>
 * </ul>
 * Le déclencheur d'une opération est lu dans les événements de MS-HOMOLOGATION (§8.2), jamais déduit de son code.
 */
@ConfigurationProperties(prefix = "gimac.certification.operations")
public record ParametresOperations(String liaisonSansProjection) {
    public ParametresOperations {
        liaisonSansProjection = liaisonSansProjection == null || liaisonSansProjection.isBlank() ? "ADMETTRE" : liaisonSansProjection.trim().toUpperCase(Locale.ROOT);
        if (!"ADMETTRE".equals(liaisonSansProjection) && !"REFUSER".equals(liaisonSansProjection)) {
            throw new IllegalArgumentException("gimac.certification.operations.liaison-sans-projection : ADMETTRE ou REFUSER, reçu " + liaisonSansProjection);
        }
    }

    public boolean refuserSansProjection() {
        return "REFUSER".equals(liaisonSansProjection);
    }
}
