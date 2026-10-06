package cm.gimac.des.pilotage.domain.processus;

/**
 * Intervenant d'une étape (exécutant, validateur ou signataire, §1) : {@code profil} pour PROFIL,
 * {@code personneId} pour PERSONNE ; {@code unite} : unité où chercher (null : unité propriétaire, sous-unités
 * comprises) ; {@code remplacement} : profil servant à choisir une nouvelle personne si la personne nommée ou
 * désignée quitte le GIMAC (obligatoire pour PERSONNE et AU_LANCEMENT).
 */
public record Intervenant(ModeIntervenant mode, String profil, String personneId, String unite, String remplacement) {
    /** Unité effective : celle de l'intervenant, à défaut l'unité propriétaire du processus. */
    public String uniteOu(String proprietaire) {
        return unite == null || unite.isBlank() ? proprietaire : unite;
    }
}
