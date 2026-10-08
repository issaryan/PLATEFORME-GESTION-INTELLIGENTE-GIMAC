package cm.gimac.des.notification.domain.service;

import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
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

    /** Type nommé dans une requête (MESSAGE, TACHE, RAPPORT, DEMANDE, MENTION, REPONSE, PROCESSUS) ; inconnu : 400. */
    public TypeNotification type(String valeur, String champ) {
        try {
            return TypeNotification.valueOf(valeur.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw NotificationException.champ(champ, "Type attendu : MESSAGE, TACHE, RAPPORT, DEMANDE, MENTION, REPONSE ou PROCESSUS");
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

    /**
     * Mentions et réponse d'un message (D-82, messagerie.md §4) : destinataire et type de la notification
     * propre, dans l'ordre des mentions puis de l'auteur cité. Seuls les membres destinataires du message
     * sont retenus, jamais l'auteur ; un compte à la fois mentionné et auteur du message cité ne reçoit
     * qu'une notification, de type MENTION.
     */
    public Map<String, TypeNotification> mentionsEtReponse(
        Collection<String> membres,
        Collection<String> mentionnes,
        String reponseAAuteurId,
        String auteurId
    ) {
        Set<String> admis = new LinkedHashSet<>(destinataires(membres, auteurId));
        Map<String, TypeNotification> r = new java.util.LinkedHashMap<>();
        for (String m : destinataires(mentionnes, auteurId)) {
            if (admis.contains(m)) {
                r.put(m, TypeNotification.MENTION);
            }
        }
        if (!vide(reponseAAuteurId)) {
            String cite = reponseAAuteurId.strip();
            if (!cite.equals(auteurId) && admis.contains(cite)) {
                r.putIfAbsent(cite, TypeNotification.REPONSE);
            }
        }
        return r;
    }

    /** Titre d'une mention : « {auteur} vous a mentionné », ou un libellé neutre. */
    public String titreMention(String nomAuteur) {
        return vide(nomAuteur) ? "Vous avez été mentionné" : nomAuteur + " vous a mentionné";
    }

    /** Titre d'une réponse : « {auteur} a répondu à votre message », ou un libellé neutre. */
    public String titreReponse(String nomAuteur) {
        return vide(nomAuteur) ? "Réponse à votre message" : nomAuteur + " a répondu à votre message";
    }

    /** Lien vers un message précis d'une conversation (mention, réponse ; D-82). */
    public String lienMessage(String conversationId, String messageId) {
        return lienConversation(conversationId) + "&message=" + messageId;
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

    /** Tâches à prendre de mes unités (docs/lot1/pouvoirs-hierarchiques.md §6). */
    public String lienTachesAPrendre() {
        return "/des/taches?vue=unite";
    }

    /**
     * Lien de la notification d'une tâche à prendre (D-98) : celui de l'offre s'il est un chemin de la couche interne
     * (sous /des/, 500 caractères au plus, sans espace, caractère de contrôle ni « .. ») : écran où l'acte se fait sans
     * prise préalable, par exemple prononcer une transition de statut ; sinon les tâches à prendre.
     */
    public String lienTacheOfferte(String lien) {
        if (
            lien != null &&
            lien.startsWith("/des/") &&
            lien.length() <= NotificationUtilisateur.LIEN_MAX &&
            lien.chars().noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c)) &&
            !lien.contains("..")
        ) {
            return lien;
        }
        return lienTachesAPrendre();
    }

    /** Demande à traiter, pour l'administration (docs/lot1/pouvoirs-hierarchiques.md §4). */
    public String lienDemandeATraiter(String demandeId) {
        return "/des/administration/demandes?demande=" + demandeId;
    }

    /** Décision d'une demande, pour son demandeur (« Mon équipe », onglet « Mes demandes »). */
    public String lienDemandeDecidee(String demandeId) {
        return "/des/equipe/demandes?demande=" + demandeId;
    }

    /** Libellé du type d'une demande (CREATION_COMPTE, PROFILS, STATUT) ; autre : nul. */
    public String libelleDemande(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "CREATION_COMPTE" -> "Création de compte";
            case "PROFILS" -> "Profils d'un collaborateur";
            case "STATUT" -> "Activation ou désactivation d'un compte";
            default -> null;
        };
    }

    /** Titre d'une demande soumise : « Demande de … », ou un libellé neutre. */
    public String titreDemandeSoumise(String nomDemandeur) {
        return vide(nomDemandeur) ? "Nouvelle demande à traiter" : "Demande de " + nomDemandeur;
    }

    /** Titre d'une demande décidée : acceptée ou refusée. */
    public String titreDemandeDecidee(String etat) {
        if ("ACCEPTEE".equals(etat)) {
            return "Demande acceptée";
        }
        return "REFUSEE".equals(etat) ? "Demande refusée" : "Demande traitée";
    }

    /** Titre d'une tâche à prendre : son titre, ou un libellé neutre. */
    public String titreTacheOfferte(String titreTache) {
        return vide(titreTache) ? "Tâche à prendre" : titreTache;
    }

    /** Sous-titre d'une tâche à prendre : l'unité visée. */
    public String sousTitreTacheOfferte(String uniteCible) {
        return vide(uniteCible) ? "À prendre" : "À prendre : " + uniteCible;
    }

    /** Titre d'une relance : « Relance : <titre> ». */
    public String titreRelance(String titreTache) {
        return vide(titreTache) ? "Relance d'une tâche" : "Relance : " + titreTache;
    }

    /** Sous-titre d'une relance : qui relance. */
    public String sousTitreRelance(String nomActeur) {
        return vide(nomActeur) ? null : "Relancée par " + nomActeur;
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

    // ------------------------------------------------------------------ processus d'unité (D-87)

    /** Préfixe des liens admis tels quels pour un processus d'unité (docs/lot1/processus-unite.md §5.5). */
    public static final String PREFIXE_LIEN_PROCESSUS = "/des/processus/";

    /** Nature d'un événement à destinataires du sujet pilotage.process (docs/lot1/processus-unite.md §5.5). */
    public enum NatureProcessus {
        /** pilotage.process-instance-started.v1 : au chef de l'unité. */
        INSTANCE_LANCEE,
        /** pilotage.process-step-activated.v1 : à l'exécutant, ou aux porteurs du profil (étape à prendre). */
        ETAPE_ACTIVEE,
        /** pilotage.process-step-submitted.v1 : aux signataires attendus ou au validateur. */
        ETAPE_SOUMISE,
        /** pilotage.process-step-signed.v1 : aux signataires suivants, au validateur ou à l'exécutant. */
        ETAPE_SIGNEE,
        /** pilotage.process-step-reassigned.v1 : au nouvel exécutant ; départ : porteurs du profil et chef. */
        ETAPE_REAFFECTEE,
        /** pilotage.process-instance-closed.v1 : au lanceur. */
        INSTANCE_CLOSE,
        /**
         * pilotage.process-step-activated.v1 vu de l'initiateur (D-98) : sa demande passe à une nouvelle étape, s'il n'en
         * est pas lui-même l'intervenant.
         */
        DEMANDE_AVANCEE,
    }

    /**
     * Ce qu'un événement de processus dit de lui-même pour son titre et son sous-titre ; jamais de contenu de
     * document ni de valeur de champ.
     *
     * @param precision selon la nature : attente (SIGNATURE, VALIDATION) d'une étape soumise, état de l'étape
     *     après une signature (A_SIGNER, A_VALIDER, TERMINEE), cause d'une réaffectation (REAFFECTATION,
     *     DEPART), état d'une instance close (CLOSE, ANNULEE) ; null sinon
     * @param renvoi étape rendue active par un renvoi du validateur
     * @param aPrendre étape à prendre par les porteurs d'un profil (pas d'exécutant nommé)
     */
    public record FaitsProcessus(
        NatureProcessus nature,
        String code,
        String intitule,
        String etape,
        String libelleEtape,
        String precision,
        boolean renvoi,
        boolean aPrendre,
        String echeance
    ) {}

    /** Titre d'une notification de processus (docs/lot1/notifications.md §2). */
    public String titreProcessus(FaitsProcessus f) {
        String etape = nomEtape(f);
        return switch (f.nature()) {
            case INSTANCE_LANCEE -> avec("Processus lancé", nomProcessus(f));
            case ETAPE_ACTIVEE -> {
                if (f.renvoi()) {
                    yield avec("Étape renvoyée", etape);
                }
                yield avec(f.aPrendre() ? "Étape à prendre" : "Étape à réaliser", etape);
            }
            case ETAPE_SOUMISE -> avec("VALIDATION".equals(f.precision()) ? "Validation attendue" : "Signature attendue", etape);
            case ETAPE_SIGNEE -> switch (f.precision() == null ? "" : f.precision()) {
                case "A_SIGNER" -> avec("Signature attendue", etape);
                case "A_VALIDER" -> avec("Validation attendue", etape);
                default -> avec("Étape signée", etape);
            };
            case ETAPE_REAFFECTEE -> avec(aReprendre(f.precision()) ? "Étape à reprendre" : "Étape réaffectée", etape);
            case INSTANCE_CLOSE -> avec("ANNULEE".equals(f.precision()) ? "Processus annulé" : "Processus terminé", nomProcessus(f));
            case DEMANDE_AVANCEE -> avec("Votre demande avance", etape);
        };
    }

    /** Réaffectation subie (départ, retrait de profil, changement d'unité) : l'étape est à reprendre. */
    private static boolean aReprendre(String cause) {
        return "DEPART".equals(cause) || "PROFIL_RETIRE".equals(cause) || "CHANGEMENT_UNITE".equals(cause);
    }

    /**
     * Sous-titre d'une notification de processus : l'instance (intitulé, à défaut code du processus) et, selon la
     * nature, l'échéance ou qui a agi ; null s'il n'y a rien à dire.
     *
     * @param nomActeur nom affiché de l'acteur (lanceur, exécutant qui soumet, signataire, chef qui réaffecte ou annule)
     * @param nomAncien nom affiché de la personne partie (réaffectation après un départ)
     */
    public String sousTitreProcessus(FaitsProcessus f, String nomActeur, String nomAncien) {
        String instance = nomProcessus(f);
        String suite =
            switch (f.nature()) {
                case INSTANCE_LANCEE -> vide(nomActeur) ? null : "lancé par " + nomActeur;
                case ETAPE_ACTIVEE -> vide(f.echeance()) ? null : "échéance " + f.echeance();
                case ETAPE_SOUMISE -> vide(nomActeur) ? null : "soumise par " + nomActeur;
                case ETAPE_SIGNEE -> vide(nomActeur) ? null : "signée par " + nomActeur;
                case ETAPE_REAFFECTEE -> switch (f.precision() == null ? "" : f.precision()) {
                    case "DEPART" -> vide(nomAncien) ? "après un départ" : "après le départ de " + nomAncien;
                    // D-93 §9.3 : réaction aux changements d'organisation.
                    case "PROFIL_RETIRE" -> vide(nomAncien) ? "après un retrait de profil" : "après le retrait d'un profil de " + nomAncien;
                    case "CHANGEMENT_UNITE" -> vide(nomAncien) ? "après un changement d'unité" : "après le changement d'unité de " + nomAncien;
                    case "CHANGEMENT_CHEF" -> "après un changement de chef d'unité";
                    default -> vide(nomActeur) ? null : "réaffectée par " + nomActeur;
                };
                case INSTANCE_CLOSE -> "ANNULEE".equals(f.precision()) && !vide(nomActeur) ? "annulé par " + nomActeur : null;
                // D-98 : l'initiateur sait si l'étape attend d'être prise ou quand elle doit être faite.
                case DEMANDE_AVANCEE -> f.aPrendre() ? "en attente de prise en charge" : vide(f.echeance()) ? "étape en cours" : "étape en cours, échéance " + f.echeance();
            };
        if (f.nature() == NatureProcessus.INSTANCE_LANCEE || f.nature() == NatureProcessus.INSTANCE_CLOSE) {
            // Le titre nomme déjà l'instance : le code du processus la situe.
            instance = vide(f.code()) || f.code().equals(nomProcessus(f)) ? null : f.code();
        }
        if (vide(instance)) {
            return vide(suite) ? null : majuscule(suite);
        }
        return vide(suite) ? instance : instance + ", " + suite;
    }

    /**
     * Lien d'une notification de processus : celui de l'événement, rendu tel quel s'il est un chemin de la couche
     * interne sous /des/processus/ (500 caractères au plus, sans espace ni caractère de contrôle) ; sinon la page de
     * l'instance.
     */
    public String lienProcessus(String lien, String instanceId) {
        if (
            lien != null &&
            lien.startsWith(PREFIXE_LIEN_PROCESSUS) &&
            lien.length() <= NotificationUtilisateur.LIEN_MAX &&
            lien.chars().noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c)) &&
            !lien.contains("..")
        ) {
            return lien;
        }
        return PREFIXE_LIEN_PROCESSUS + "instances/" + instanceId;
    }

    private static String nomProcessus(FaitsProcessus f) {
        if (!vide(f.intitule())) {
            return f.intitule().strip();
        }
        return vide(f.code()) ? null : f.code().strip();
    }

    private static String nomEtape(FaitsProcessus f) {
        if (!vide(f.libelleEtape())) {
            return f.libelleEtape().strip();
        }
        return vide(f.etape()) ? null : f.etape().strip();
    }

    private static String avec(String prefixe, String objet) {
        return vide(objet) ? prefixe : prefixe + " : " + objet;
    }

    private static String majuscule(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Instant avant lequel une notification est purgée. */
    public Instant limiteConservation(Instant maintenant) {
        return maintenant.minus(CONSERVATION);
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
