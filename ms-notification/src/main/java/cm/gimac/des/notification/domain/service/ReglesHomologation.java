package cm.gimac.des.notification.domain.service;

import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Règles des notifications du parcours d'homologation (D-93, docs/lot1/effet-reel-referentiels.md §8.2 et §11) : qui
 * est notifié d'une opération ouverte, d'une opération en validation ou d'un jalon atteint, avec quel rôle, quel titre
 * et quel lien. Service de domaine pur : ni Spring, ni annuaire ; les comptes d'une institution sont résolus par le cas
 * d'utilisation.
 * <ul>
 * <li>R (opération ouverte) et V (opération en validation) : action attendue ; C : avis demandé ; I : information ;
 * E : escalade ;</li>
 * <li>une fonction tenue par une institution (R ou E externes, institutions concernées) vise les comptes de cette
 * institution, sur le portail ;</li>
 * <li>une fonction « à affecter » ({@code A_AFFECTER:<FONCTION>}) n'est notifiée à personne nommément : la tâche
 * « Affecter » du conducteur et la tâche à prendre des porteurs de la fonction (MS-HOMOLOGATION, MS-PILOTAGE) en
 * tiennent lieu ;</li>
 * <li>un compte visé à plusieurs titres reçoit une seule notification, celle du rôle le plus fort.</li>
 * </ul>
 */
public final class ReglesHomologation {

    /** Préfixe des liens de la couche interne vers le dossier d'une liaison. */
    public static final String PREFIXE_LIEN_INTERNE = "/des/parcours/";

    /** Préfixe des liens du portail vers la liaison de l'institution (frontend/apps/portail, route liaisons/:id). */
    public static final String PREFIXE_LIEN_PORTAIL = "/portail/liaisons";

    /** Jalons notifiés par défaut aux comptes de l'institution (tous ceux du référentiel). */
    public static final Set<String> JALONS_PORTAIL_DEFAUT = Set.of("APTE_CERTIFICATION", "CERTIFIEE", "APPROUVEE_SECURITE", "EN_PRODUCTION");

    private static final Pattern IDENTIFIANT = Pattern.compile("[A-Za-z0-9-]{1,80}");

    /** Événement d'opération reçu. */
    public enum Nature {
        /** homologation.operation-activated.v1 : opération devenue à faire (ouverture, renvoi, reprise). */
        OPERATION_ACTIVEE,
        /** homologation.operation-validation-requested.v1 : opération passée en validation. */
        VALIDATION_DEMANDEE,
    }

    /** Rôle du destinataire dans la notification, du plus fort au plus faible. */
    public enum Role {
        ACTION,
        ESCALADE,
        AVIS,
        INFORMATION,
    }

    /**
     * Intervenant d'une opération tel que le publie MS-HOMOLOGATION ({@code Destinataire}) : qualité (R, V, C, I, E),
     * fonction, compte interne désigné, fonction à affecter, institution qui tient une fonction externe.
     */
    public record Intervenant(String qualite, String fonction, String compteId, boolean aAffecter, String institutionId) {
        /** Fonction tenue par personne : ni compte ni institution. */
        public boolean nonDesigne() {
            return aAffecter || (vide(compteId) && vide(institutionId));
        }
    }

    /** Faits d'une opération ouverte ou en validation. */
    public record FaitsOperation(
        Nature nature,
        String liaisonId,
        String reference,
        String institutionNom,
        String operationId,
        String code,
        String libelle,
        String geste,
        String echeance,
        String motif,
        Intervenant responsable,
        Intervenant validateur,
        List<Intervenant> destinataires,
        List<String> institutionsConcernees,
        String lien,
        String declencheur
    ) {
        public FaitsOperation {
            destinataires = destinataires == null ? List.of() : destinataires.stream().filter(d -> d != null).toList();
            institutionsConcernees = institutionsConcernees == null ? List.of() : institutionsConcernees.stream().filter(i -> !vide(i)).toList();
        }

        /** Faits sans déclencheur (opération qu'aucun geste de MS-CERTIFICATION ne clôt). */
        public FaitsOperation(
            Nature nature,
            String liaisonId,
            String reference,
            String institutionNom,
            String operationId,
            String code,
            String libelle,
            String geste,
            String echeance,
            String motif,
            Intervenant responsable,
            Intervenant validateur,
            List<Intervenant> destinataires,
            List<String> institutionsConcernees,
            String lien
        ) {
            this(nature, liaisonId, reference, institutionNom, operationId, code, libelle, geste, echeance, motif, responsable, validateur, destinataires, institutionsConcernees, lien, null);
        }
    }

    /** Faits d'un jalon atteint (homologation.liaison-milestone-reached.v1). */
    public record FaitsJalon(
        String liaisonId,
        String reference,
        String institutionId,
        String institutionNom,
        String jalon,
        String libelleJalon,
        String conducteurId,
        List<Intervenant> destinataires,
        String lien
    ) {
        public FaitsJalon {
            destinataires = destinataires == null ? List.of() : destinataires.stream().filter(d -> d != null).toList();
        }
    }

    /** Cible d'une notification : un compte interne nommé, ou une institution (ses comptes, sur le portail). */
    public record Cible(String compteId, String institutionId) {
        public static Cible compte(String id) {
            return new Cible(id.strip(), null);
        }

        public static Cible institution(String id) {
            return new Cible(null, id.strip());
        }

        public boolean estInstitution() {
            return institutionId != null;
        }
    }

    public ReglesHomologation() {}

    // ------------------------------------------------------------------ destinataires

    /** Cibles d'une opération et leur rôle, dans l'ordre R, V, C, I, E puis institutions concernées. */
    public Map<Cible, Role> ciblesOperation(FaitsOperation f) {
        Map<Cible, Role> r = new LinkedHashMap<>();
        boolean validation = f.nature() == Nature.VALIDATION_DEMANDEE;
        if (!validation) {
            ajouter(r, f.responsable(), Role.ACTION);
        } else {
            ajouter(r, f.validateur(), Role.ACTION);
            // Le dépôt de l'institution responsable passe en validation : elle en est informée sur le portail.
            if (f.responsable() != null && !f.responsable().nonDesigne() && !vide(f.responsable().institutionId())) {
                ajouter(r, Cible.institution(f.responsable().institutionId()), Role.INFORMATION);
            }
        }
        for (Intervenant d : f.destinataires()) {
            Role role = role(d.qualite());
            if (role != null) {
                ajouter(r, d, role);
            }
        }
        // Institutions concernées non encore visées : E tenu par une institution.
        for (String i : f.institutionsConcernees()) {
            ajouter(r, Cible.institution(i), validation ? Role.INFORMATION : Role.ESCALADE);
        }
        return r;
    }

    /**
     * Cibles d'un jalon atteint : le conducteur, les C et I internes de l'opération qui le porte (information) et,
     * si le jalon la touche, l'institution de la liaison.
     */
    public Map<Cible, Role> ciblesJalon(FaitsJalon f, Collection<String> jalonsPortail) {
        Map<Cible, Role> r = new LinkedHashMap<>();
        if (!vide(f.conducteurId())) {
            ajouter(r, Cible.compte(f.conducteurId()), Role.INFORMATION);
        }
        for (Intervenant d : f.destinataires()) {
            if (role(d.qualite()) != null) {
                ajouter(r, d, Role.INFORMATION);
            }
        }
        if (!vide(f.institutionId()) && jalonsPortail != null && f.jalon() != null && jalonsPortail.contains(f.jalon().strip())) {
            ajouter(r, Cible.institution(f.institutionId()), Role.INFORMATION);
        }
        return r;
    }

    /** Fonctions « à affecter » de l'opération, qui ne reçoivent aucune notification nominative. */
    public List<String> fonctionsAAffecter(FaitsOperation f) {
        Set<String> s = new LinkedHashSet<>();
        List<Intervenant> tous = new ArrayList<>();
        tous.add(f.responsable());
        tous.add(f.validateur());
        tous.addAll(f.destinataires());
        for (Intervenant x : tous) {
            if (x != null && x.aAffecter()) {
                s.add(vide(x.fonction()) ? "?" : x.fonction().strip());
            }
        }
        return new ArrayList<>(s);
    }

    /** Fusionne deux rôles d'un même compte : le plus fort l'emporte. */
    public static boolean plusFort(Role nouveau, Role existant) {
        return existant == null || nouveau.ordinal() < existant.ordinal();
    }

    private static void ajouter(Map<Cible, Role> r, Intervenant x, Role role) {
        if (x == null || x.nonDesigne()) {
            return;
        }
        ajouter(r, vide(x.compteId()) ? Cible.institution(x.institutionId()) : Cible.compte(x.compteId()), role);
    }

    private static void ajouter(Map<Cible, Role> r, Cible c, Role role) {
        if (plusFort(role, r.get(c))) {
            r.put(c, role);
        }
    }

    /** Rôle d'une qualité de la liste des destinataires (C, I, E) ; R et V y sont aussi admis par prudence. */
    private static Role role(String qualite) {
        if (qualite == null) {
            return null;
        }
        return switch (qualite.strip()) {
            case "R", "V", "A" -> Role.ACTION;
            case "C" -> Role.AVIS;
            case "I" -> Role.INFORMATION;
            case "E" -> Role.ESCALADE;
            default -> null;
        };
    }

    // ------------------------------------------------------------------ titres

    public String titreOperation(FaitsOperation f, Role role) {
        String objet = nomOperation(f);
        boolean validation = f.nature() == Nature.VALIDATION_DEMANDEE;
        return switch (role) {
            case ACTION -> validation ? "Validation attendue : " + objet : "RENVOI".equals(f.motif()) ? "Opération renvoyée : " + objet : "Action attendue : " + objet;
            case AVIS -> "Avis demandé : " + objet;
            case ESCALADE -> "Escalade : " + objet;
            case INFORMATION -> validation ? "Opération en validation : " + objet : "Opération ouverte : " + objet;
        };
    }

    /** « LIA-2026-001 (Banque A), échéance 2026-10-09 » ; éléments absents omis. */
    public String sousTitreOperation(FaitsOperation f) {
        String s = liaison(f.reference(), f.institutionNom());
        if (!vide(f.echeance())) {
            s = s == null ? "Échéance " + f.echeance().strip() : s + ", échéance " + f.echeance().strip();
        }
        return s;
    }

    public String titreJalon(FaitsJalon f) {
        String j = !vide(f.libelleJalon()) ? f.libelleJalon().strip() : vide(f.jalon()) ? null : f.jalon().strip();
        return j == null ? "Jalon atteint" : "Jalon atteint : " + j;
    }

    public String sousTitreJalon(FaitsJalon f) {
        return liaison(f.reference(), f.institutionNom());
    }

    private static String nomOperation(FaitsOperation f) {
        if (!vide(f.libelle())) {
            return f.libelle().strip();
        }
        return vide(f.code()) ? "opération" : "opération " + f.code().strip();
    }

    private static String liaison(String reference, String institution) {
        if (vide(reference)) {
            return vide(institution) ? null : institution.strip();
        }
        return vide(institution) ? reference.strip() : reference.strip() + " (" + institution.strip() + ")";
    }

    // ------------------------------------------------------------------ liens

    /**
     * Lien de la couche interne vers l'opération : celui de l'événement s'il est sous /des/parcours/ et sûr (sans
     * espace, caractère de contrôle ni « .. », 500 caractères au plus), sinon
     * {@code /des/parcours/<liaison>?onglet=operations&operation=<id>}.
     */
    public String lienInterneOperation(FaitsOperation f) {
        if (lienSur(f.lien())) {
            return f.lien();
        }
        if (!identifiant(f.liaisonId())) {
            return "/des/parcours";
        }
        String l = PREFIXE_LIEN_INTERNE + f.liaisonId();
        return identifiant(f.operationId()) ? l + "?onglet=operations&operation=" + f.operationId() : l + "?onglet=operations";
    }

    /**
     * Lien du portail (D-98, comme le geste du participant servi par MS-HOMOLOGATION) : l'écran où l'institution agit ou
     * suit l'opération : coordonnées techniques, procès-verbal (déclencheur {@code certification.pv-signed.v1}),
     * attestations ({@code certification.attestations-archived.v1}), fiche de suivi ; l'écran de dépôt du livrable
     * ({@code /portail/liaisons/<liaison>/livrables/<operation>}) quand l'institution doit déposer (action attendue sur un
     * geste DEPOSER_*) ; sinon la liaison de l'institution.
     */
    public String lienPortailOperation(FaitsOperation f, Role role) {
        if (!identifiant(f.liaisonId())) {
            return PREFIXE_LIEN_PORTAIL;
        }
        String l = PREFIXE_LIEN_PORTAIL + "/" + f.liaisonId();
        if ("COORDONNEES_TECHNIQUES".equals(f.geste())) {
            return l + "/coordonnees";
        }
        if ("certification.pv-signed.v1".equals(f.declencheur())) {
            return l + "/proces-verbal";
        }
        if ("certification.attestations-archived.v1".equals(f.declencheur())) {
            return l + "/attestations";
        }
        if ("FICHE_SUIVI".equals(f.geste())) {
            return l + "/fiche-suivi";
        }
        boolean depot = f.geste() != null && f.geste().startsWith("DEPOSER_");
        if (role == Role.ACTION && f.nature() == Nature.OPERATION_ACTIVEE && depot && identifiant(f.operationId())) {
            return l + "/livrables/" + f.operationId();
        }
        return l;
    }

    /** Lien interne du jalon : celui de l'événement s'il est sûr, sinon le dossier de la liaison. */
    public String lienInterneJalon(FaitsJalon f) {
        if (lienSur(f.lien())) {
            return f.lien();
        }
        return identifiant(f.liaisonId()) ? PREFIXE_LIEN_INTERNE + f.liaisonId() : "/des/parcours";
    }

    public String lienPortailJalon(FaitsJalon f) {
        return identifiant(f.liaisonId()) ? PREFIXE_LIEN_PORTAIL + "/" + f.liaisonId() : PREFIXE_LIEN_PORTAIL;
    }

    private static boolean lienSur(String lien) {
        return (
            lien != null &&
            lien.startsWith(PREFIXE_LIEN_INTERNE) &&
            lien.length() <= NotificationUtilisateur.LIEN_MAX &&
            lien.chars().noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c)) &&
            !lien.contains("..")
        );
    }

    private static boolean identifiant(String s) {
        return s != null && IDENTIFIANT.matcher(s).matches();
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
