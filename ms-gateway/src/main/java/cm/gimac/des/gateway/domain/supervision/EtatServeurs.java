package cm.gimac.des.gateway.domain.supervision;

import java.time.Instant;
import java.util.List;

/** État des serveurs d'après Prometheus : cibles de collecte, hôtes (node-exporter), conteneurs (cAdvisor). */
public record EtatServeurs(
    Instant horodatage,
    boolean prometheusJoignable,
    String erreur,
    int ciblesUp,
    int ciblesDown,
    List<CibleCollecte> cibles,
    boolean nodeExporter,
    List<EtatHote> hotes,
    List<EtatConteneur> conteneurs
) {
    public EtatServeurs {
        cibles = List.copyOf(cibles);
        hotes = List.copyOf(hotes);
        conteneurs = List.copyOf(conteneurs);
    }
}
