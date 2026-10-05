package cm.gimac.des.notification.domain.service;

import cm.gimac.des.notification.domain.boite.TypeNotification;
import cm.gimac.des.notification.domain.exception.NotificationException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Règles de la boîte de notifications (docs/lot1/notifications.md §2 et §3), sans dépendance technique :
 * destinataires (jamais l'auteur ni l'acteur), titres, liens dans la couche, pagination, conservation.
 */
public final class ReglesNotifications {

    /** Conservation des notifications (§2) : 90 jours, puis purge. */
    public static final Duration CONSERVATION = Duration.ofDays(90);

    public static final int TAILLE_DEFAUT = 20;
    public static final int TAILLE_MAX = 100;

    /** Filtre de la liste : non-lues ou toutes (§3). */
    public enum Etat {
        NON_LUES,
        TOUTES,
    }

    /** Valeurs par défaut des préférences (§3) : système actif, son inactif, tous les types actifs. */
    public static final boolean SYSTEME_DEFAUT = true;
    public static final boolean SON_DEFAUT = false;

    public ReglesNotifications() {}

    /** « non-lues » ou « toutes » ; absent : toutes ; autre valeur : 400 NOT_REQUETE_INVALIDE. */
    public Etat etat(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return Etat.TOUTES;
        }
        return switch (valeur.strip().toLowerCase(Locale.ROOT)) {
            case "non-lues" -> Etat.NON_LUES;
            case "toutes" -> Etat.TOUTES;
            default -> throw NotificationException.champ("etat", "État attendu : non-lues ou toutes");
        };
    }

    /** Page à partir de 0 ; négative : 400. */
    public int page(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw NotificationException.champ("page", "La page commence à 0");
        }
        return page;
    }

    /** Taille de page entre 1 et 100 (20 par défaut) ; hors bornes : 400. */
    public int taille(Integer taille) {
        if (taille == null) {
            return TAILLE_DEFAUT;
        }
        if (taille < 1 || taille > TAILLE_MAX) {
            throw NotificationException.champ("taille", "Taille de page entre 1 et " + TAILLE_MAX);
        }
        return taille;
    }

    /** Type nommé dans une requête (MESSAGE, TACHE, RAPPORT) ; inconnu : 400. */
    public TypeNotification type(String valeur, String champ) {
        try {
            return TypeNotification.valueOf(valeur.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw NotificationException.champ(champ, "Type attendu : MESSAGE, TACHE ou RAPPORT");
        }
    }

    /** Types d'une modification des préférences ; une clé inconnue est refusée (400). */
    public Map<TypeNotification, Boolean> types(Map<String, Boolean> types) {
        Map<TypeNotification, Boolean> r = new java.util.EnumMap<>(TypeNotification.class);
        if (types != null) {
            types.forEach((cle, actif) -> r.put(type(cle, "types"), actif));
        }
        return r;
    }

    /** Destinataires d'un événement, sans doublon, sans valeur vide, jamais l'auteur ou l'acteur. */
    public List<String> destinataires(Collection<String> destinataires, String auteurOuActeur) {
        Set<String> r = new LinkedHashSet<>();
        if (destinataires != null) {
            for (String d : destinataires) {
                if (d != null && !d.isBlank() && !d.strip().equals(auteurOuActeur)) {
                    r.add(d.strip());
                }
            }
        }
        return new ArrayList<>(r);
    }

    /** Lien relatif dans la couche interne (§2) vers la conversation. */
    public String lienConversation(String conversationId) {
        return "/des/messagerie?conversation=" + conversationId;
    }

    public String lienTache(String tacheId) {
        return "/des/taches?tache=" + tacheId;
    }

    public String lienRapport(String rapportId) {
        return "/des/rapports?vue=recus&rapport=" + rapportId;
    }

    /** Titre d'un message : l'expéditeur, ou à défaut un libellé neutre (annuaire injoignable). */
    public String titreMessage(String nomAuteur) {
        return vide(nomAuteur) ? "Nouveau message" : nomAuteur;
    }

    /** Titre d'une tâche : son titre s'il est transmis, sinon qui l'a confiée. */
    public String titreTache(String titreTache, String nomActeur) {
        if (!vide(titreTache)) {
            return titreTache;
        }
        return vide(nomActeur) ? "Nouvelle tâche confiée" : "Tâche confiée par " + nomActeur;
    }

    /** Sous-titre d'une tâche : échéance si elle est transmise. */
    public String sousTitreTache(String echeance) {
        return vide(echeance) ? null : "Échéance : " + echeance;
    }

    /** « Rapport de … » (§2). */
    public String titreRapport(String nomAuteur) {
        return vide(nomAuteur) ? "Rapport soumis" : "Rapport de " + nomAuteur;
    }

    /** Sous-titre d'un rapport : type et période (quotidien AAAA-MM-JJ, hebdomadaire AAAA-Www). */
    public String sousTitreRapport(String type, String periode) {
        String p = vide(periode) ? "" : " " + periode;
        if ("QUOTIDIEN".equals(type)) {
            return "Rapport quotidien" + p;
        }
        if ("HEBDOMADAIRE".equals(type)) {
            return "Rapport hebdomadaire" + p;
        }
        return vide(periode) ? null : periode;
    }

    /** Instant avant lequel une notification est purgée. */
    public Instant limiteConservation(Instant maintenant) {
        return maintenant.minus(CONSERVATION);
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
