package cm.gimac.des.participant.application.usecase;

import cm.gimac.des.participant.application.port.in.RepriseInstitutionsUseCase.InstitutionReprise;
import cm.gimac.des.participant.application.port.in.RepriseInstitutionsUseCase.ServiceRepris;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import cm.gimac.des.participant.domain.service.InstitutionLifecycle;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Contrôle et normalisation d'une ligne de reprise des institutions (D-107), sans base de données : un refus détaille
 * chaque champ fautif (422 PAR_REPRISE_LIGNE_INVALIDE, details.champs) ; les lacunes admises à la reprise (conditions
 * d'adhésion incomplètes, pays hors CEMAC, date d'adhésion inconnue) deviennent des avertissements.
 */
final class RepriseInstitutionsValidation {

    static final Pattern CODE_MEMBRE = Pattern.compile("^[A-Z0-9-]{1,20}$");
    static final Pattern PAYS = Pattern.compile("^[A-Z]{2}$");
    static final Set<String> CEMAC = Set.of("CM", "CF", "CG", "GA", "GQ", "TD");
    static final Pattern BIC = Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$");
    static final Pattern CODE_BEAC = Pattern.compile("^[A-Z0-9]{1,10}$");
    static final Pattern CODE_SERVICE = Pattern.compile("^[A-Z][A-Z0-9_.]{1,49}$");

    /** Ligne contrôlée et normalisée. */
    record Fiche(
        String codeMembre,
        UUID id,
        String nom,
        String sigle,
        String pays,
        InstitutionCategory categorie,
        String codeIdentification,
        String codeBanqueBeac,
        String groupe,
        InstitutionStatus statut,
        LocalDate dateDecision,
        String referenceDecision,
        LocalDate datePriseDeContact,
        LocalDate dateAdhesion,
        MembershipMode mode,
        String compteReglement,
        String compteCantonnement,
        String banqueReglement,
        String banqueMarraine,
        List<ServiceRepris> services,
        List<String> avertissements
    ) {}

    private RepriseInstitutionsValidation() {}

    static Fiche valider(InstitutionReprise l, LocalDate aujourdhui) {
        Map<String, String> champs = new LinkedHashMap<>();
        List<String> avertissements = new ArrayList<>();
        String code = majuscules(l.codeMembre());
        if (code == null) {
            champs.put("codeMembre", "Champ obligatoire");
        } else if (!CODE_MEMBRE.matcher(code).matches()) {
            champs.put("codeMembre", "Majuscules, chiffres et tirets, 20 caractères au plus");
        }
        String nom = texte(l.nom());
        if (nom == null) {
            champs.put("nom", "Champ obligatoire");
        } else if (nom.length() > 255) {
            champs.put("nom", "255 caractères au plus");
        }
        String sigle = longueur(champs, "sigle", texte(l.sigle()), 50);
        String pays = majuscules(l.pays());
        if (pays == null) {
            champs.put("pays", "Champ obligatoire");
        } else if (!PAYS.matcher(pays).matches()) {
            champs.put("pays", "Code ISO 3166-1 alpha-2 (deux lettres)");
        } else if (!CEMAC.contains(pays)) {
            avertissements.add("Pays hors CEMAC (" + pays + ") : admis à la reprise, la fiche ne se modifie ensuite qu'avec un pays CEMAC");
        }
        InstitutionCategory categorie = enumeration(champs, "categorie", l.categorie(), InstitutionCategory.class);
        InstitutionStatus statut = enumeration(champs, "statut", l.statut(), InstitutionStatus.class);
        MembershipMode mode = l.modeAdhesion() == null || l.modeAdhesion().isBlank() ? null : enumeration(champs, "modeAdhesion", l.modeAdhesion(), MembershipMode.class);
        String bic = motif(champs, "codeIdentification", majuscules(l.codeIdentification()), BIC, "Code BIC (ISO 9362) invalide");
        String beac = motif(champs, "codeBanqueBeac", majuscules(l.codeBanqueBeac()), CODE_BEAC, "Majuscules et chiffres, 10 caractères au plus");
        String groupe = longueur(champs, "groupe", texte(l.groupe()) == null ? null : l.groupe().trim().replaceAll("\\s+", " "), 100);
        String reference = longueur(champs, "referenceDecision", texte(l.referenceDecision()), 100);
        String compteReglement = longueur(champs, "compteReglement", texte(l.compteReglement()), 50);
        String compteCantonnement = longueur(champs, "compteCantonnement", texte(l.compteCantonnement()), 50);
        String banqueReglement = motif(champs, "banqueReglement", majuscules(l.banqueReglement()), CODE_MEMBRE, "Code membre invalide");
        String banqueMarraine = motif(champs, "banqueMarraine", majuscules(l.banqueMarraine()), CODE_MEMBRE, "Code membre invalide");
        for (String champ : List.of("dateDecision", "datePriseDeContact", "dateAdhesion")) {
            LocalDate d = switch (champ) {
                case "dateDecision" -> l.dateDecision();
                case "datePriseDeContact" -> l.datePriseDeContact();
                default -> l.dateAdhesion();
            };
            if (d != null && d.isAfter(aujourdhui)) {
                champs.put(champ, "Date future");
            }
        }
        List<ServiceRepris> services = services(champs, l.servicesSouscrits(), aujourdhui);
        if (!champs.isEmpty()) {
            throw ParticipantException.unprocessable("PAR_REPRISE_LIGNE_INVALIDE", "Ligne de reprise invalide", Map.of("champs", champs));
        }
        if (InstitutionLifecycle.atteignable(InstitutionStatus.ADHERENT, statut) && statut != InstitutionStatus.CLOS_SANS_SUITE) {
            conditions(mode, compteReglement, compteCantonnement, banqueMarraine, avertissements);
            if (l.dateAdhesion() == null && !(statut == InstitutionStatus.ADHERENT && l.dateDecision() != null)) {
                avertissements.add("Date d'adhésion inconnue");
            }
        }
        return new Fiche(
            code,
            l.id(),
            nom,
            sigle,
            pays,
            categorie,
            bic,
            beac,
            groupe,
            statut,
            l.dateDecision(),
            reference,
            l.datePriseDeContact(),
            l.dateAdhesion(),
            mode,
            compteReglement,
            compteCantonnement,
            banqueReglement,
            banqueMarraine,
            services,
            avertissements
        );
    }

    /** Conditions d'adhésion (RG-11, RG-12) : incomplètes, elles sont reprises telles quelles et signalées. */
    private static void conditions(MembershipMode mode, String compteReglement, String compteCantonnement, String marraine, List<String> avertissements) {
        if (mode == null) {
            avertissements.add("Mode d'adhésion inconnu (conditions d'adhésion à compléter)");
        } else if (mode == MembershipMode.DIRECT && compteReglement == null) {
            avertissements.add("Adhésion directe sans compte de règlement (RG-11)");
        } else if (mode == MembershipMode.INDIRECT && (marraine == null || compteCantonnement == null)) {
            avertissements.add("Adhésion indirecte sans banque marraine ou compte de cantonnement (RG-12)");
        }
    }

    private static List<ServiceRepris> services(Map<String, String> champs, List<ServiceRepris> source, LocalDate aujourdhui) {
        if (source == null) {
            return List.of();
        }
        List<ServiceRepris> services = new ArrayList<>();
        Set<String> vus = new LinkedHashSet<>();
        for (ServiceRepris s : source) {
            String code = s == null ? null : majuscules(s.code());
            if (code == null || !CODE_SERVICE.matcher(code).matches()) {
                champs.put("servicesSouscrits", "Code de service invalide : " + (code == null ? "vide" : code));
            } else if (!vus.add(code)) {
                champs.put("servicesSouscrits", "Service en double : " + code);
            } else if (s.depuis() != null && s.depuis().isAfter(aujourdhui)) {
                champs.put("servicesSouscrits", "Date de souscription future : " + code);
            } else {
                services.add(new ServiceRepris(code, s.depuis()));
            }
        }
        return List.copyOf(services);
    }

    private static <E extends Enum<E>> E enumeration(Map<String, String> champs, String champ, String valeur, Class<E> type) {
        String v = majuscules(valeur);
        if (v == null) {
            champs.put(champ, "Champ obligatoire");
            return null;
        }
        try {
            return Enum.valueOf(type, v);
        } catch (IllegalArgumentException e) {
            champs.put(champ, "Valeur inconnue : " + v);
            return null;
        }
    }

    private static String motif(Map<String, String> champs, String champ, String valeur, Pattern motif, String message) {
        if (valeur != null && !motif.matcher(valeur).matches()) {
            champs.put(champ, message);
        }
        return valeur;
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
}
