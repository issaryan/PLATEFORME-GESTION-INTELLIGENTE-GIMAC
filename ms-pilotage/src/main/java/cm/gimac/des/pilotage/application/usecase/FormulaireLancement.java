package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.out.DocumentPort;
import cm.gimac.des.pilotage.application.port.out.DocumentPort.Metadonnees;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.processus.ChampFormulaire;
import cm.gimac.des.pilotage.domain.processus.Formulaire;
import cm.gimac.des.pilotage.domain.service.EmpreinteSignature;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Valeurs du formulaire de lancement (D-98 §2), contrôlées au lancement : toutes les erreurs sont rendues ensemble en
 * 422 PIL_PROCESSUS_FORMULAIRE_INVALIDE, {@code details.fieldErrors} (et {@code fieldErrors} du problème) =
 * [ { objectName: "lancement", field: "formulaire.CODE", message, code } ], codes OBLIGATOIRE, INCONNU, FORMAT,
 * TROP_LONG, HORS_LISTE, PERIODE, PIECE_INVALIDE, TYPE_PIECE. Valeurs gardées : texte, nombre (texte décimal), date
 * AAAA-MM-JJ, période { debut, fin }, valeur de liste, booléen, pièce jointe { documentId, nomFichier, typeMime, taille,
 * sha256 } (dépôt fragmenté de MS-DOCUMENT, finalité PROCESSUS_UNITE, déposé par l'initiateur, référence absente ou
 * égale à l'identifiant de la définition lancée).
 */
final class FormulaireLancement {

    static final String CODE_ERREUR = "PIL_PROCESSUS_FORMULAIRE_INVALIDE";
    static final int TEXTE_COURT_MAX = 500;
    static final int TEXTE_LONG_MAX = 4000;

    /** Erreur d'un champ du formulaire (forme fieldErrors de l'ExceptionTranslator, plus un code stable). */
    public record ErreurChamp(String objectName, String field, String message, String code) {}

    private FormulaireLancement() {}

    /**
     * Valeurs normalisées, par code de champ, dans l'ordre du formulaire ; un champ facultatif vide est omis.
     *
     * @param processusId définition lancée (référence admise d'une pièce jointe)
     * @param deposant l'initiateur, seul déposant admis d'une pièce jointe
     * @param documents métadonnées d'un document de MS-DOCUMENT (vide : inconnu)
     */
    static Map<String, Object> valeurs(Formulaire f, Map<String, Object> saisies, UUID processusId, String deposant, Function<UUID, Optional<Metadonnees>> documents) {
        Map<String, Object> s = saisies == null ? Map.of() : saisies;
        List<ErreurChamp> erreurs = new ArrayList<>();
        for (String code : s.keySet()) {
            if (f.champs().stream().noneMatch(c -> c.code().equals(code))) {
                erreurs.add(erreur(code, "INCONNU", "Ce champ n'appartient pas au formulaire de lancement"));
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        for (ChampFormulaire c : f.champs()) {
            Object v = s.get(c.code());
            if (vide(v)) {
                if (c.obligatoire()) {
                    erreurs.add(erreur(c.code(), "OBLIGATOIRE", "Champ obligatoire"));
                }
                continue;
            }
            try {
                Object n = valeur(c, v, processusId, deposant, documents);
                if (n != null) {
                    r.put(c.code(), n);
                }
            } catch (Refus e) {
                erreurs.add(erreur(c.code(), e.code, e.getMessage()));
            }
        }
        if (!erreurs.isEmpty()) {
            throw PilotageException.regle(CODE_ERREUR, "Formulaire de lancement à corriger : " + erreurs.size() + " champ(s)", Map.of("fieldErrors", erreurs));
        }
        return r;
    }

    private static ErreurChamp erreur(String code, String raison, String message) {
        return new ErreurChamp("lancement", "formulaire." + code, message, raison);
    }

    private static boolean vide(Object v) {
        return v == null || (v instanceof String t && t.isBlank()) || (v instanceof Map<?, ?> m && m.isEmpty());
    }

    /** Refus d'une valeur, rendu en erreur de champ. */
    private static final class Refus extends RuntimeException {

        private final String code;

        Refus(String code, String message) {
            super(message, null, false, false);
            this.code = code;
        }
    }

    private static Object valeur(ChampFormulaire c, Object v, UUID processusId, String deposant, Function<UUID, Optional<Metadonnees>> documents) {
        return switch (c.type()) {
            case TEXTE -> texte(v, TEXTE_COURT_MAX);
            case TEXTE_LONG -> texte(v, TEXTE_LONG_MAX);
            case NOMBRE -> nombre(v);
            case DATE -> date(v);
            case PERIODE -> periode(v);
            case LISTE -> {
                String t = texte(v, TEXTE_COURT_MAX);
                if (!c.choix().contains(t)) {
                    throw new Refus("HORS_LISTE", "Valeur hors de la liste admise");
                }
                yield t;
            }
            case CASE -> {
                boolean coche = coche(v);
                if (c.obligatoire() && !coche) {
                    throw new Refus("OBLIGATOIRE", "Case à cocher");
                }
                yield coche;
            }
            case PIECE_JOINTE -> piece(c, v, processusId, deposant, documents);
        };
    }

    private static String texte(Object v, int max) {
        if (!(v instanceof String) && !(v instanceof Number)) {
            throw new Refus("FORMAT", "Texte attendu");
        }
        String t = String.valueOf(v).strip();
        if (t.length() > max) {
            throw new Refus("TROP_LONG", "Texte de " + max + " caractères au plus");
        }
        return t;
    }

    private static String nombre(Object v) {
        try {
            return new BigDecimal(String.valueOf(v).strip().replace(',', '.')).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            throw new Refus("FORMAT", "Nombre attendu");
        }
    }

    private static String date(Object v) {
        try {
            return LocalDate.parse(String.valueOf(v).strip()).toString();
        } catch (DateTimeParseException e) {
            throw new Refus("FORMAT", "Date AAAA-MM-JJ attendue");
        }
    }

    private static Map<String, String> periode(Object v) {
        if (!(v instanceof Map<?, ?> m)) {
            throw new Refus("FORMAT", "Période { debut, fin } attendue");
        }
        Object d = m.get("debut");
        Object f = m.get("fin");
        if (vide(d) || vide(f)) {
            throw new Refus("PERIODE", "Début et fin de la période attendus");
        }
        String debut = date(d);
        String fin = date(f);
        if (LocalDate.parse(fin).isBefore(LocalDate.parse(debut))) {
            throw new Refus("PERIODE", "La fin de la période précède son début");
        }
        Map<String, String> r = new LinkedHashMap<>();
        r.put("debut", debut);
        r.put("fin", fin);
        return r;
    }

    private static boolean coche(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        String t = String.valueOf(v).strip().toLowerCase(Locale.ROOT);
        if ("true".equals(t) || "false".equals(t)) {
            return Boolean.parseBoolean(t);
        }
        throw new Refus("FORMAT", "Vrai ou faux attendu");
    }

    private static Map<String, Object> piece(ChampFormulaire c, Object v, UUID processusId, String deposant, Function<UUID, Optional<Metadonnees>> documents) {
        Object id = v instanceof Map<?, ?> m ? m.get("documentId") : v;
        UUID doc;
        try {
            doc = UUID.fromString(String.valueOf(id).strip());
        } catch (IllegalArgumentException e) {
            throw new Refus("FORMAT", "Identifiant de document attendu");
        }
        Metadonnees m = documents
            .apply(doc)
            .filter(d -> DocumentPort.FINALITE_PROCESSUS.equals(d.finalite()))
            .filter(d -> deposant.equals(d.deposant()))
            .filter(d -> d.reference() == null || d.reference().equals(processusId.toString()))
            .orElseThrow(() -> new Refus("PIECE_INVALIDE", "Pièce inconnue, ou non déposée par vous pour ce processus (finalité " + DocumentPort.FINALITE_PROCESSUS + ")"));
        String type = m.typeMime() == null ? "" : m.typeMime().toLowerCase(Locale.ROOT);
        if (!c.typesPieces().isEmpty() && c.typesPieces().stream().noneMatch(t -> t.endsWith("/*") ? type.startsWith(t.substring(0, t.length() - 1)) : t.equals(type))) {
            throw new Refus("TYPE_PIECE", "Type de pièce non admis (" + String.join(", ", c.typesPieces()) + ")");
        }
        String sha = m.sha256() == null ? null : m.sha256().toLowerCase(Locale.ROOT);
        if (!EmpreinteSignature.valide(sha)) {
            throw new Refus("PIECE_INVALIDE", "Empreinte SHA-256 de la pièce absente");
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("documentId", doc.toString());
        r.put("nomFichier", m.nomFichier());
        r.put("typeMime", type);
        r.put("taille", m.taille());
        r.put("sha256", sha);
        return r;
    }
}
