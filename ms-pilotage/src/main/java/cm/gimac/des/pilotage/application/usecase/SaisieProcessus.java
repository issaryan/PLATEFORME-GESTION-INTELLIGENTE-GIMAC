package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.ChampFormulaireSaisie;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.ChampSaisie;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.EtapeSaisie;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.FormulaireSaisie;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.InitiateursSaisie;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.IntervenantSaisie;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.LivrableSaisie;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.SignatureSaisie;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.processus.Champ;
import cm.gimac.des.pilotage.domain.processus.ChampFormulaire;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.Formulaire;
import cm.gimac.des.pilotage.domain.processus.Initiateurs;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.processus.Livrable;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.processus.Position;
import cm.gimac.des.pilotage.domain.processus.RegleInitiateur;
import cm.gimac.des.pilotage.domain.processus.SignatureRequise;
import cm.gimac.des.pilotage.domain.processus.TypeChamp;
import cm.gimac.des.pilotage.domain.processus.TypeChampFormulaire;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lecture d'une définition saisie (§1) : formats et bornes (400 PIL_REQUETE_INVALIDE avec le chemin du champ,
 * par exemple {@code etapes[1].livrables[0].code}). Un brouillon peut être incomplet (intervenant absent,
 * prédécesseur inconnu…) : ces anomalies relèvent du contrôle de publication.
 */
final class SaisieProcessus {

    static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,39}$");
    static final Pattern UNITE = Pattern.compile("^[A-Z][A-Z0-9_]{1,31}$");
    static final Pattern PROFIL = Pattern.compile("^ROLE_[A-Z][A-Z0-9_]{1,60}$");
    static final Pattern TYPE_MIME = Pattern.compile("^[a-z0-9.+-]+/[a-z0-9.+*-]+$");
    static final int ETAPES_MAX = 100;
    static final int ELEMENTS_MAX = 30;
    static final int TYPES_MAX = 20;
    static final int CHOIX_MAX = 50;
    static final int DELAI_MAX = 365;
    static final int ORDRE_MAX = 99;
    static final int LIBELLE_MAX = 200;
    static final int TEXTE_MAX = 4000;
    static final int CATEGORIE_MAX = 80;

    private SaisieProcessus() {}

    static String unite(String champ, String unite) {
        return format(champ, unite, UNITE, "Code d'unité attendu (par exemple POLE_RESEAU)");
    }

    static String profil(String champ, String profil) {
        return format(champ, profil, PROFIL, "Profil ROLE_… attendu");
    }

    static Integer delaiCible(Integer d) {
        if (d != null && (d < 0 || d > DELAI_MAX)) {
            throw PilotageException.champ("delaiCibleJours", "Délai de 0 à " + DELAI_MAX + " jours");
        }
        return d;
    }

    /**
     * Délai maximal (D-106) : facultatif, 0 à 365 jours, au moins le délai cible quand celui-ci est défini ;
     * 400 PIL_REQUETE_INVALIDE ({@code details.champ} = {@code champ}) sinon.
     */
    static Integer delaiMax(String champ, Integer max, Integer cible) {
        if (max == null) {
            return null;
        }
        if (max < 0 || max > DELAI_MAX) {
            throw PilotageException.champ(champ, "Délai de 0 à " + DELAI_MAX + " jours");
        }
        if (cible != null && max < cible) {
            throw PilotageException.champ(champ, "Le délai maximal (" + max + " j) ne peut être inférieur au délai cible (" + cible + " j)");
        }
        return max;
    }

    static List<Etape> etapes(List<EtapeSaisie> saisies) {
        if (saisies == null) {
            return List.of();
        }
        if (saisies.size() > ETAPES_MAX) {
            throw PilotageException.champ("etapes", ETAPES_MAX + " étapes au plus");
        }
        List<Etape> l = new ArrayList<>();
        for (int i = 0; i < saisies.size(); i++) {
            l.add(etape("etapes[" + i + "]", saisies.get(i)));
        }
        return List.copyOf(l);
    }

    private static Etape etape(String c, EtapeSaisie s) {
        if (s == null) {
            throw PilotageException.champ(c, "Étape absente");
        }
        String code = format(c + ".code", s.code(), CODE, "Code ^[A-Z0-9][A-Z0-9_-]{0,39}$ attendu");
        String libelle = ReglesActivite.texteObligatoire(c + ".libelle", s.libelle(), LIBELLE_MAX);
        String action = ReglesActivite.texteFacultatif(c + ".action", s.action(), TEXTE_MAX);
        List<String> preds = new ArrayList<>();
        if (s.predecesseurs() != null) {
            if (s.predecesseurs().size() > ETAPES_MAX) {
                throw PilotageException.champ(c + ".predecesseurs", ETAPES_MAX + " prédécesseurs au plus");
            }
            for (int i = 0; i < s.predecesseurs().size(); i++) {
                String p = format(c + ".predecesseurs[" + i + "]", s.predecesseurs().get(i), CODE, "Code d'étape attendu");
                if (!preds.contains(p)) {
                    preds.add(p);
                }
            }
        }
        int delai = s.delaiJours() == null ? 0 : s.delaiJours();
        if (delai < 0 || delai > DELAI_MAX) {
            throw PilotageException.champ(c + ".delaiJours", "Délai de 0 à " + DELAI_MAX + " jours");
        }
        Integer delaiMax = delaiMax(c + ".delaiMaxJours", s.delaiMaxJours(), delai);
        List<Livrable> livrables = liste(c + ".livrables", s.livrables()).stream().map(x -> livrable(x.chemin(), (LivrableSaisie) x.valeur())).toList();
        List<SignatureRequise> signatures = liste(c + ".signatures", s.signatures()).stream().map(x -> signature(x.chemin(), (SignatureSaisie) x.valeur())).toList();
        List<Champ> champs = liste(c + ".champs", s.champs()).stream().map(x -> champ(x.chemin(), (ChampSaisie) x.valeur())).toList();
        Position position = s.position() == null ? new Position(0, 0) : new Position(fini(c + ".position.x", s.position().x()), fini(c + ".position.y", s.position().y()));
        return new Etape(
            code,
            libelle,
            action,
            intervenant(c + ".intervenant", s.intervenant()),
            intervenant(c + ".validateur", s.validateur()),
            preds,
            delai,
            livrables,
            signatures,
            champs,
            position,
            delaiMax
        );
    }

    private record Element(String chemin, Object valeur) {}

    private static List<Element> liste(String c, List<?> l) {
        if (l == null) {
            return List.of();
        }
        if (l.size() > ELEMENTS_MAX) {
            throw PilotageException.champ(c, ELEMENTS_MAX + " éléments au plus");
        }
        List<Element> r = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i) == null) {
                throw PilotageException.champ(c + "[" + i + "]", "Élément absent");
            }
            r.add(new Element(c + "[" + i + "]", l.get(i)));
        }
        return r;
    }

    static Intervenant intervenant(String c, IntervenantSaisie s) {
        if (s == null || s.mode() == null || s.mode().isBlank()) {
            return null;
        }
        ModeIntervenant mode = ReglesActivite.valeur(c + ".mode", ModeIntervenant.class, s.mode(), null);
        String profil = s.profil() == null || s.profil().isBlank() ? null : format(c + ".profil", s.profil(), PROFIL, "Profil ROLE_… attendu");
        String personne = ReglesActivite.texteFacultatif(c + ".personneId", s.personneId(), 80);
        String unite = s.unite() == null || s.unite().isBlank() ? null : unite(c + ".unite", s.unite());
        String remplacement = s.remplacement() == null || s.remplacement().isBlank() ? null : format(c + ".remplacement", s.remplacement(), PROFIL, "Profil ROLE_… attendu");
        // Seuls les attributs du mode sont gardés : un profil n'a pas de sens pour un intervenant PERSONNE.
        return new Intervenant(mode, mode == ModeIntervenant.PROFIL ? profil : null, mode == ModeIntervenant.PERSONNE ? personne : null, unite, remplacement);
    }

    private static Livrable livrable(String c, LivrableSaisie s) {
        String code = format(c + ".code", s.code(), CODE, "Code ^[A-Z0-9][A-Z0-9_-]{0,39}$ attendu");
        String libelle = ReglesActivite.texteObligatoire(c + ".libelle", s.libelle(), LIBELLE_MAX);
        List<String> types = new ArrayList<>();
        if (s.typesPieces() != null) {
            if (s.typesPieces().size() > TYPES_MAX) {
                throw PilotageException.champ(c + ".typesPieces", TYPES_MAX + " types au plus");
            }
            for (int i = 0; i < s.typesPieces().size(); i++) {
                String t = format(c + ".typesPieces[" + i + "]", s.typesPieces().get(i) == null ? null : s.typesPieces().get(i).toLowerCase(java.util.Locale.ROOT), TYPE_MIME, "Type MIME attendu (par exemple application/pdf)");
                if (!types.contains(t)) {
                    types.add(t);
                }
            }
        }
        String modele = ReglesActivite.texteFacultatif(c + ".modele", s.modele(), 80);
        if (modele != null) {
            uuid(c + ".modele", modele);
        }
        return new Livrable(code, libelle, types, !Boolean.FALSE.equals(s.obligatoire()), modele);
    }

    private static SignatureRequise signature(String c, SignatureSaisie s) {
        String code = format(c + ".code", s.code(), CODE, "Code ^[A-Z0-9][A-Z0-9_-]{0,39}$ attendu");
        String libelle = ReglesActivite.texteObligatoire(c + ".libelle", s.libelle(), LIBELLE_MAX);
        List<String> porte = new ArrayList<>();
        if (s.porte() != null) {
            if (s.porte().size() > ELEMENTS_MAX) {
                throw PilotageException.champ(c + ".porte", ELEMENTS_MAX + " livrables au plus");
            }
            for (int i = 0; i < s.porte().size(); i++) {
                String l = format(c + ".porte[" + i + "]", s.porte().get(i), CODE, "Code de livrable attendu");
                if (!porte.contains(l)) {
                    porte.add(l);
                }
            }
        }
        int ordre = s.ordre() == null ? 1 : s.ordre();
        if (ordre < 1 || ordre > ORDRE_MAX) {
            throw PilotageException.champ(c + ".ordre", "Ordre de 1 à " + ORDRE_MAX);
        }
        return new SignatureRequise(code, libelle, intervenant(c + ".signataire", s.signataire()), porte, ordre);
    }

    private static Champ champ(String c, ChampSaisie s) {
        String code = format(c + ".code", s.code(), CODE, "Code ^[A-Z0-9][A-Z0-9_-]{0,39}$ attendu");
        String libelle = ReglesActivite.texteObligatoire(c + ".libelle", s.libelle(), LIBELLE_MAX);
        TypeChamp type = ReglesActivite.valeur(c + ".type", TypeChamp.class, s.type(), TypeChamp.TEXTE);
        List<String> choix = new ArrayList<>();
        if (s.choix() != null && type == TypeChamp.CHOIX) {
            if (s.choix().size() > CHOIX_MAX) {
                throw PilotageException.champ(c + ".choix", CHOIX_MAX + " choix au plus");
            }
            for (int i = 0; i < s.choix().size(); i++) {
                String v = ReglesActivite.texteObligatoire(c + ".choix[" + i + "]", s.choix().get(i), LIBELLE_MAX);
                if (!choix.contains(v)) {
                    choix.add(v);
                }
            }
        }
        return new Champ(code, libelle, type, choix, Boolean.TRUE.equals(s.obligatoire()));
    }

    // ------------------------------------------------------------------ lancement (D-98 §1, §2)

    static final Pattern PERMISSION = Pattern.compile("^[A-Z][A-Z0-9_]{1,79}$");
    static final int AIDE_MAX = 500;
    static final int PARAMETRES_MAX = 20;
    static final int CHAMPS_FORMULAIRE_MAX = 30;
    /** Personnes désignées nommément (D-100) : 50 au plus. */
    static final int PERSONNES_MAX = 50;
    /** Identifiant de compte : texte non vide de 80 caractères au plus, comme ailleurs dans MS-PILOTAGE. */
    static final int IDENTIFIANT_MAX = 80;

    /**
     * Public saisi (D-98 §1, D-100) : règles connues, sans doublon ; unités, profils et permissions au bon format (20 au
     * plus) ; personnes désignées : identifiants de comptes (50 au plus, sans doublon). L'existence des unités, profils et
     * personnes relève du contrôle de publication.
     */
    static Initiateurs initiateurs(InitiateursSaisie s) {
        if (s == null) {
            return Initiateurs.parDefaut();
        }
        List<RegleInitiateur> regles = new ArrayList<>();
        if (s.regles() != null) {
            if (s.regles().size() > PARAMETRES_MAX) {
                throw PilotageException.champ("initiateurs.regles", PARAMETRES_MAX + " règles au plus");
            }
            for (int i = 0; i < s.regles().size(); i++) {
                RegleInitiateur r = ReglesActivite.valeur("initiateurs.regles[" + i + "]", RegleInitiateur.class, s.regles().get(i), null);
                if (!regles.contains(r)) {
                    regles.add(r);
                }
            }
        }
        List<String> unites = codes("initiateurs.unites", s.unites(), UNITE, "Code d'unité attendu (par exemple POLE_RESEAU)");
        List<String> profils = codes("initiateurs.profils", s.profils(), PROFIL, "Profil ROLE_… attendu");
        List<String> permissions = codes("initiateurs.permissions", s.permissions(), PERMISSION, "Permission ^[A-Z][A-Z0-9_]{1,79}$ attendue");
        List<String> personnes = personnes(s.personnes());
        return new Initiateurs(regles, unites, profils, permissions, personnes, Boolean.TRUE.equals(s.initiateurIntervenant()));
    }

    /** Identifiants des personnes désignées (D-100) : non vides, 80 caractères au plus, 50 au plus, sans doublon. */
    private static List<String> personnes(List<String> l) {
        List<String> r = new ArrayList<>();
        if (l == null) {
            return r;
        }
        if (l.size() > PERSONNES_MAX) {
            throw PilotageException.champ("initiateurs.personnes", PERSONNES_MAX + " personnes au plus");
        }
        for (int i = 0; i < l.size(); i++) {
            String v = ReglesActivite.texteObligatoire("initiateurs.personnes[" + i + "]", l.get(i), IDENTIFIANT_MAX);
            if (!r.contains(v)) {
                r.add(v);
            }
        }
        return r;
    }

    private static List<String> codes(String c, List<String> l, Pattern motif, String message) {
        List<String> r = new ArrayList<>();
        if (l == null) {
            return r;
        }
        if (l.size() > PARAMETRES_MAX) {
            throw PilotageException.champ(c, PARAMETRES_MAX + " éléments au plus");
        }
        for (int i = 0; i < l.size(); i++) {
            String v = format(c + "[" + i + "]", l.get(i), motif, message);
            if (!r.contains(v)) {
                r.add(v);
            }
        }
        return r;
    }

    /** Formulaire de lancement saisi : 30 champs au plus, codes et types, choix d'une LISTE, types de pièces. */
    static Formulaire formulaire(FormulaireSaisie s) {
        if (s == null || s.champs() == null) {
            return Formulaire.vide();
        }
        if (s.champs().size() > CHAMPS_FORMULAIRE_MAX) {
            throw PilotageException.champ("formulaire.champs", CHAMPS_FORMULAIRE_MAX + " champs au plus");
        }
        List<ChampFormulaire> l = new ArrayList<>();
        for (int i = 0; i < s.champs().size(); i++) {
            String c = "formulaire.champs[" + i + "]";
            ChampFormulaireSaisie x = s.champs().get(i);
            if (x == null) {
                throw PilotageException.champ(c, "Élément absent");
            }
            String code = format(c + ".code", x.code(), CODE, "Code ^[A-Z0-9][A-Z0-9_-]{0,39}$ attendu");
            String libelle = ReglesActivite.texteObligatoire(c + ".libelle", x.libelle(), LIBELLE_MAX);
            TypeChampFormulaire type = ReglesActivite.valeur(c + ".type", TypeChampFormulaire.class, x.type(), TypeChampFormulaire.TEXTE);
            List<String> choix = new ArrayList<>();
            if (x.choix() != null && type == TypeChampFormulaire.LISTE) {
                if (x.choix().size() > CHOIX_MAX) {
                    throw PilotageException.champ(c + ".choix", CHOIX_MAX + " choix au plus");
                }
                for (int j = 0; j < x.choix().size(); j++) {
                    String v = ReglesActivite.texteObligatoire(c + ".choix[" + j + "]", x.choix().get(j), LIBELLE_MAX);
                    if (!choix.contains(v)) {
                        choix.add(v);
                    }
                }
            }
            List<String> types = new ArrayList<>();
            if (x.typesPieces() != null && type == TypeChampFormulaire.PIECE_JOINTE) {
                if (x.typesPieces().size() > TYPES_MAX) {
                    throw PilotageException.champ(c + ".typesPieces", TYPES_MAX + " types au plus");
                }
                for (int j = 0; j < x.typesPieces().size(); j++) {
                    String t = format(c + ".typesPieces[" + j + "]", x.typesPieces().get(j) == null ? null : x.typesPieces().get(j).toLowerCase(java.util.Locale.ROOT), TYPE_MIME, "Type MIME attendu (par exemple application/pdf)");
                    if (!types.contains(t)) {
                        types.add(t);
                    }
                }
            }
            String aide = ReglesActivite.texteFacultatif(c + ".aide", x.aide(), AIDE_MAX);
            l.add(new ChampFormulaire(code, libelle, type, choix, Boolean.TRUE.equals(x.obligatoire()), aide, types));
        }
        return new Formulaire(l);
    }

    static String format(String champ, String valeur, Pattern motif, String message) {
        String v = valeur == null ? null : valeur.strip();
        if (v == null || !motif.matcher(v).matches()) {
            throw PilotageException.champ(champ, message);
        }
        return v;
    }

    static void uuid(String champ, String valeur) {
        try {
            java.util.UUID.fromString(valeur);
        } catch (IllegalArgumentException e) {
            throw PilotageException.champ(champ, "Identifiant de document attendu");
        }
    }

    private static double fini(String champ, Double v) {
        if (v == null) {
            return 0;
        }
        if (v.isNaN() || v.isInfinite() || Math.abs(v) > 1_000_000) {
            throw PilotageException.champ(champ, "Coordonnée finie attendue");
        }
        return v;
    }

    /** Motif d'un acte : obligatoire ({@code exige}) ou facultatif, 1000 caractères au plus. */
    static String motif(String motif, boolean exige) {
        if (exige) {
            return ReglesActivite.texteObligatoire("motif", motif, 1000);
        }
        return ReglesActivite.texteFacultatif("motif", motif, 1000);
    }

    /** Motif d'un changement de public (D-103) : 3 à 500 caractères. */
    static final int MOTIF_PUBLIC_MIN = 3;
    static final int MOTIF_PUBLIC_MAX = 500;

    /**
     * Motif d'un changement de public (D-103) : obligatoire ({@code exige}, version publiée) ou facultatif (brouillon) ;
     * présent, de 3 à 500 caractères (400 PIL_REQUETE_INVALIDE, {@code details.champ} = motif).
     */
    static String motifPublic(String motif, boolean exige) {
        String v = motif == null ? "" : motif.strip();
        if (v.isEmpty() && !exige) {
            return null;
        }
        if (v.length() < MOTIF_PUBLIC_MIN || v.length() > MOTIF_PUBLIC_MAX) {
            throw PilotageException.champ("motif", "Motif de " + MOTIF_PUBLIC_MIN + " à " + MOTIF_PUBLIC_MAX + " caractères attendu");
        }
        return v;
    }

    /** Valeur d'un champ saisi, normalisée selon son type (422 PIL_PROCESSUS_CHAMP_INVALIDE). */
    static String valeurChamp(Champ champ, Object valeur) {
        if (valeur == null || (valeur instanceof String t && t.isBlank())) {
            return null;
        }
        String texte = valeur instanceof String t ? t.strip() : String.valueOf(valeur);
        Map<String, Object> details = Map.of("champ", champ.code(), "type", champ.type().name());
        switch (champ.type()) {
            case NOMBRE -> {
                try {
                    return new java.math.BigDecimal(texte).stripTrailingZeros().toPlainString();
                } catch (NumberFormatException e) {
                    throw PilotageException.regle("PIL_PROCESSUS_CHAMP_INVALIDE", "Nombre attendu", details);
                }
            }
            case DATE -> {
                try {
                    return java.time.LocalDate.parse(texte).toString();
                } catch (java.time.format.DateTimeParseException e) {
                    throw PilotageException.regle("PIL_PROCESSUS_CHAMP_INVALIDE", "Date AAAA-MM-JJ attendue", details);
                }
            }
            case CHOIX -> {
                if (!champ.choix().contains(texte)) {
                    throw PilotageException.regle("PIL_PROCESSUS_CHAMP_INVALIDE", "Valeur hors des choix admis", details);
                }
                return texte;
            }
            default -> {
                if (texte.length() > TEXTE_MAX) {
                    throw PilotageException.regle("PIL_PROCESSUS_CHAMP_INVALIDE", "Texte de " + TEXTE_MAX + " caractères au plus", details);
                }
                return texte;
            }
        }
    }
}
