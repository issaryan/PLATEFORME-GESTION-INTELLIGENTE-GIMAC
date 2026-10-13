package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.LiaisonReprise;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.OperationReprise;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.RessourceReprise;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.ServiceRepris;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.RepriseParcours;
import cm.gimac.des.homologation.domain.parcours.RepriseParcours.Avancement;
import cm.gimac.des.homologation.domain.parcours.RepriseParcours.EtatRepris;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Contrôle et normalisation d'une liaison reprise (D-107), sans base de données : un refus détaille chaque champ fautif
 * (422 HOM_REPRISE_LIGNE_INVALIDE, details.champs).
 */
final class RepriseLiaisonsValidation {

    /** Référence d'origine (D-104) : lettres, chiffres, « . », « _ », « / », « - », 50 caractères au plus. */
    static final Pattern REFERENCE_ORIGINE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._/-]{0,49}$");
    static final Pattern CODE_MEMBRE = Pattern.compile("^[A-Z0-9-]{1,20}$");
    static final Pattern CODE_SERVICE = Pattern.compile("^[A-Z][A-Z0-9_]{1,39}$");
    static final Pattern EMPREINTE = Pattern.compile("^[0-9a-f]{64}$");

    /** Liaison contrôlée. */
    record LiaisonControlee(
        String referenceOrigine,
        String codeMembre,
        TypeProjet typeProjet,
        String intitule,
        String origineEtat,
        LocalDate dateOuverture,
        LocalDate dateCibleProduction,
        LocalDate dateMiseEnProduction,
        java.util.UUID conducteurId,
        Map<Fonction, java.util.UUID> ressources,
        String banquePartenaire,
        List<ServiceRepris> services,
        Avancement avancement,
        List<RepriseParcours.OperationReprise> operations
    ) {}

    private RepriseLiaisonsValidation() {}

    static LiaisonControlee valider(LiaisonReprise l, LocalDate aujourdhui) {
        Map<String, String> champs = new LinkedHashMap<>();
        String reference = texte(l.referenceOrigine());
        if (reference == null) {
            champs.put("referenceOrigine", "Champ obligatoire");
        } else if (!REFERENCE_ORIGINE.matcher(reference).matches()) {
            champs.put("referenceOrigine", "Lettres, chiffres, « . », « _ », « / », « - », 50 caractères au plus");
        }
        String code = majuscules(l.codeMembre());
        if (code == null) {
            champs.put("codeMembre", "Champ obligatoire");
        } else if (!CODE_MEMBRE.matcher(code).matches()) {
            champs.put("codeMembre", "Code membre invalide");
        }
        TypeProjet type = null;
        String t = majuscules(l.typeProjet());
        if (t == null) {
            champs.put("typeProjet", "Champ obligatoire");
        } else if (!TypeProjet.CODE.matcher(t).matches()) {
            champs.put("typeProjet", "Code de type de projet invalide");
        } else {
            type = TypeProjet.valueOf(t);
        }
        String intitule = longueur(champs, "intitule", texte(l.intitule()), 200);
        String origine = longueur(champs, "origineEtat", texte(l.origineEtat()), 100);
        String banque = majuscules(l.banquePartenaire());
        if (banque != null && !CODE_MEMBRE.matcher(banque).matches()) {
            champs.put("banquePartenaire", "Code membre invalide");
        }
        passee(champs, "dateOuverture", l.dateOuverture(), aujourdhui);
        passee(champs, "dateMiseEnProduction", l.dateMiseEnProduction(), aujourdhui);
        Avancement avancement = avancement(champs, l);
        if (avancement == Avancement.EN_PRODUCTION && l.dateMiseEnProduction() == null) {
            champs.put("dateMiseEnProduction", "Obligatoire pour un projet repris en production");
        }
        Map<Fonction, java.util.UUID> ressources = ressources(champs, l.ressources());
        List<ServiceRepris> services = services(champs, l.services(), aujourdhui);
        List<RepriseParcours.OperationReprise> operations = operations(champs, l.operations(), aujourdhui);
        if (avancement == Avancement.A_QUALIFIER && !operations.isEmpty()) {
            champs.put("avancement", "Un projet à qualifier ne porte aucun état d'opération");
        }
        if (!champs.isEmpty()) {
            throw HomologationException.unprocessableDetails("HOM_REPRISE_LIGNE_INVALIDE", "Liaison reprise invalide", Map.of("champs", champs));
        }
        return new LiaisonControlee(
            reference,
            code,
            type,
            intitule,
            origine,
            l.dateOuverture(),
            l.dateCibleProduction(),
            l.dateMiseEnProduction(),
            l.conducteurId(),
            ressources,
            banque,
            services,
            avancement,
            operations
        );
    }

    private static Avancement avancement(Map<String, String> champs, LiaisonReprise l) {
        String a = majuscules(l.avancement());
        if (a == null) {
            return l.operations() == null || l.operations().isEmpty() ? Avancement.A_QUALIFIER : Avancement.DETAILLE;
        }
        try {
            return Avancement.valueOf(a);
        } catch (IllegalArgumentException e) {
            champs.put("avancement", "A_QUALIFIER, DETAILLE ou EN_PRODUCTION");
            return null;
        }
    }

    private static Map<Fonction, java.util.UUID> ressources(Map<String, String> champs, List<RessourceReprise> source) {
        Map<Fonction, java.util.UUID> r = new LinkedHashMap<>();
        if (source == null) {
            return r;
        }
        for (RessourceReprise x : source) {
            String f = x == null ? null : majuscules(x.fonction());
            Fonction fonction = f == null ? null : Fonction.duCatalogue(f).orElse(null);
            if (fonction == null || x.compteId() == null) {
                champs.put("ressources", "Fonction du catalogue et compte obligatoires : " + (f == null ? "vide" : f));
            } else if (fonction.externe() || Fonction.CONDUCTEUR.equals(fonction) || Fonction.PARTICIPANT.equals(fonction)) {
                champs.put("ressources", "Fonction non affectable à un compte : " + f);
            } else if (r.put(fonction, x.compteId()) != null) {
                champs.put("ressources", "Fonction en double : " + f);
            }
        }
        return r;
    }

    private static List<ServiceRepris> services(Map<String, String> champs, List<ServiceRepris> source, LocalDate aujourdhui) {
        List<ServiceRepris> r = new ArrayList<>();
        if (source == null) {
            return r;
        }
        Set<String> vus = new HashSet<>();
        for (ServiceRepris s : source) {
            String c = s == null ? null : majuscules(s.code());
            if (c == null || !CODE_SERVICE.matcher(c).matches()) {
                champs.put("services", "Code de service invalide : " + (c == null ? "vide" : c));
            } else if (!vus.add(c)) {
                champs.put("services", "Service en double : " + c);
            } else if (s.certifieLe() != null && s.certifieLe().isAfter(aujourdhui)) {
                champs.put("services", "Date de certification future : " + c);
            } else {
                r.add(new ServiceRepris(c, s.certifieLe(), s.ouvert(), tronquer(texte(s.commentaire()), 500)));
            }
        }
        return r;
    }

    private static List<RepriseParcours.OperationReprise> operations(Map<String, String> champs, List<OperationReprise> source, LocalDate aujourdhui) {
        List<RepriseParcours.OperationReprise> r = new ArrayList<>();
        if (source == null) {
            return r;
        }
        Set<String> vus = new HashSet<>();
        for (OperationReprise o : source) {
            String rang = o == null ? null : texte(o.rang());
            if (rang == null || rang.length() > 40) {
                champs.put("operations", "Rang d'opération obligatoire, 40 caractères au plus");
                continue;
            }
            String cle = "operations." + rang;
            EtatRepris etat = etat(o.etat());
            if (etat == null) {
                champs.put(cle, "État FAITE, EN_COURS, A_FAIRE, SANS_OBJET ou BLOQUEE");
            } else if (!vus.add(rang)) {
                champs.put(cle, "Opération en double");
            } else if ((o.faiteLe() != null && o.faiteLe().isAfter(aujourdhui)) || (o.debut() != null && o.debut().isAfter(aujourdhui))) {
                champs.put(cle, "Date future");
            } else if (o.empreinte() != null && !EMPREINTE.matcher(o.empreinte().trim()).matches()) {
                champs.put(cle, "Empreinte SHA-256 attendue (64 caractères hexadécimaux en minuscules)");
            } else if ((texte(o.documentRef()) == null) != (texte(o.empreinte()) == null)) {
                champs.put(cle, "Pièce et empreinte vont ensemble");
            } else if (o.commentaire() != null && o.commentaire().length() > 500) {
                champs.put(cle, "Commentaire de 500 caractères au plus");
            } else if (o.valeurReference() != null && (texte(o.valeurReference().valeur()) == null || o.valeurReference().valeur().length() > 200)) {
                champs.put(cle, "Valeur de référence vide ou trop longue");
            } else {
                r.add(new RepriseParcours.OperationReprise(rang, etat, o.debut(), o.faiteLe(), texte(o.commentaire()), o.valeurReference(), texte(o.documentRef()), texte(o.empreinte()), o.echeanceLevee()));
            }
        }
        return r;
    }

    private static EtatRepris etat(String s) {
        String e = majuscules(s);
        if (e == null) {
            return null;
        }
        try {
            return EtatRepris.valueOf(e);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void passee(Map<String, String> champs, String champ, LocalDate d, LocalDate aujourdhui) {
        if (d != null && d.isAfter(aujourdhui)) {
            champs.put(champ, "Date future");
        }
    }

    private static String longueur(Map<String, String> champs, String champ, String valeur, int max) {
        if (valeur != null && valeur.length() > max) {
            champs.put(champ, max + " caractères au plus");
        }
        return valeur;
    }

    static String texte(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String majuscules(String s) {
        String t = texte(s);
        return t == null ? null : t.toUpperCase(Locale.ROOT);
    }

    private static String tronquer(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
