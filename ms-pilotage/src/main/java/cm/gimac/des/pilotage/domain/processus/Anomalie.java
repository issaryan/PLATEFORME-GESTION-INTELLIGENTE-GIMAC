package cm.gimac.des.pilotage.domain.processus;

/**
 * Anomalie relevée par le contrôle d'une définition (§1) : code stable (CYCLE, PREDECESSEUR_INCONNU…), gravité,
 * étape et élément concernés (code de livrable, de signature, de champ, rôle de l'intervenant), message.
 */
public record Anomalie(String code, GraviteAnomalie gravite, String etape, String element, String message) {
    public static Anomalie bloquante(String code, String etape, String element, String message) {
        return new Anomalie(code, GraviteAnomalie.BLOQUANTE, etape, element, message);
    }

    public static Anomalie levable(String code, String etape, String element, String message) {
        return new Anomalie(code, GraviteAnomalie.LEVABLE, etape, element, message);
    }

    public static Anomalie avertissement(String code, String etape, String element, String message) {
        return new Anomalie(code, GraviteAnomalie.AVERTISSEMENT, etape, element, message);
    }
}
