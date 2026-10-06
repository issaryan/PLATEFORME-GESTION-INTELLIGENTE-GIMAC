package cm.gimac.des.pilotage.domain.processus;

/**
 * Affectation résolue d'un validateur ou d'un signataire : une personne ({@code personneId}), ou, à défaut, tout
 * membre actif de {@code unite} portant {@code profil} (profil null : tout membre de l'unité).
 */
public record Affectation(String personneId, String profil, String unite) {
    public static Affectation personne(String id, String unite) {
        return new Affectation(id, null, unite);
    }

    public static Affectation profil(String profil, String unite) {
        return new Affectation(null, profil, unite);
    }

    /** Affectation nominative à {@code id}. */
    public boolean nomme(String id) {
        return personneId != null && personneId.equals(id);
    }
}
