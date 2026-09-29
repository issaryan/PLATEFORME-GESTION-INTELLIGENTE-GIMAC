package cm.gimac.des.gateway.domain.supervision;

import java.util.Map;

/** Réponse d'une sonde de santé : état global et état de chaque composant (db, kafka, redis…). */
public record Sante(EtatSante etat, Map<String, String> composants) {
    public Sante {
        composants = composants == null ? Map.of() : Map.copyOf(composants);
    }
}
