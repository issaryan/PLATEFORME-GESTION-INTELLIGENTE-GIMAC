package cm.gimac.des.homologation.infrastructure.client.sandbox;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Campagne;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.CampagneListee;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Enregistrement;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.HorsContrat;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.PageExecutions;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Piece;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Taux;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Couche anticorruption du contrat de lecture v1 (L04) : seule classe à connaître la forme JSON des réponses de
 * SBX-LECTURE (contrats/sandbox/sbx-lecture.v1.*.schema.json). Elle traduit sans jamais recalculer ; une réponse hors
 * contrat (élément exigé absent, vocabulaire inconnu, jeton altéré) est écartée en entier : jamais de page partielle.
 */
public final class TraducteurLectureSandbox {

    static final Set<String> VERDICTS = Set.of("PASSED", "FAILED", "PENDING", "SKIPPED", "NA");
    static final Set<String> NATURES = Set.of("CREATION", "VALIDATION", "RECTIFICATION");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private TraducteurLectureSandbox() {}

    public static List<CampagneListee> campagnes(byte[] brut) {
        List<CampagneListee> l = new ArrayList<>();
        for (JsonNode c : exiger(lire(brut), "campagnes")) {
            l.add(new CampagneListee(texte(c, "identifiantCampagne"), facultatif(c, "code"), texte(c, "etat"), facultatif(c, "signalement")));
        }
        return l;
    }

    public static Campagne campagne(byte[] brut) {
        JsonNode c = lire(brut);
        List<String> services = new ArrayList<>();
        exiger(c, "services").forEach(s -> services.add(s.asString()));
        List<Taux> taux = new ArrayList<>();
        for (JsonNode t : exiger(c, "taux")) {
            taux.add(new Taux(facultatif(t, "service"), texte(t, "nature"), entier(t, "numerateur"), entier(t, "denominateur"), texte(t, "valeur"), texte(t, "versionFormule"), instant(t, "horodatage")));
        }
        return new Campagne(
            texte(c, "identifiantCampagne"),
            facultatif(c, "code"),
            texte(c, "identifiantLiaison"),
            texte(c, "codeParticipant"),
            facultatif(c, "interfaceTest"),
            services,
            texte(c, "etat"),
            taux,
            c.path("versionBilan").isIntegralNumber() ? c.get("versionBilan").asInt() : null,
            facultatif(c, "versionTable")
        );
    }

    public static PageExecutions page(byte[] brut) {
        JsonNode p = lire(brut);
        List<Enregistrement> l = new ArrayList<>();
        for (JsonNode e : exiger(p, "enregistrements")) {
            String verdict = texte(e, "verdictNatif");
            String nature = texte(e, "nature");
            if (!VERDICTS.contains(verdict) || !NATURES.contains(nature)) {
                throw new HorsContrat("verdict ou nature hors vocabulaire : " + verdict + ", " + nature);
            }
            l.add(
                new Enregistrement(
                    texte(e, "identifiantCampagne"),
                    texte(e, "identifiantLiaison"),
                    texte(e, "codeParticipant"),
                    texte(e, "identifiantCas"),
                    texte(e, "identifiantScenario"),
                    texte(e, "versionScenario"),
                    texte(e, "identifiantExecution"),
                    instant(e, "dateExecution"),
                    verdict,
                    facultatif(e, "verdictNormalise"),
                    facultatif(e, "motif"),
                    texte(e, "modeExecution"),
                    texte(e, "referenceTrace"),
                    texte(e, "position"),
                    nature,
                    facultatif(e, "versionTable"),
                    texte(e, "origine")
                )
            );
        }
        if (!p.path("close").isBoolean()) {
            throw new HorsContrat("indicateur de page close absent");
        }
        return new PageExecutions(l, facultatif(p, "positionSuivante"), p.get("close").asBoolean(), texte(p, "etiquette"));
    }

    public static List<Piece> pieces(byte[] brut) {
        List<Piece> l = new ArrayList<>();
        for (JsonNode x : exiger(lire(brut), "pieces")) {
            l.add(new Piece(texte(x, "nature"), entier(x, "version"), texte(x, "empreinte").toLowerCase(java.util.Locale.ROOT), x.path("taille").asLong(-1), facultatif(x, "nom")));
        }
        return l;
    }

    /** En-tête Repr-Digest (RFC 9530) : {@code sha-256=:<base64>:} converti en hexadécimal. */
    public static String empreinteRepr(String entete) {
        if (entete == null) {
            throw new HorsContrat("en-tête Repr-Digest absent");
        }
        for (String partie : entete.split(",")) {
            String p = partie.trim();
            if (p.startsWith("sha-256=:") && p.endsWith(":") && p.length() > 10) {
                try {
                    return HexFormat.of().formatHex(Base64.getDecoder().decode(p.substring(9, p.length() - 1)));
                } catch (IllegalArgumentException e) {
                    throw new HorsContrat("empreinte Repr-Digest illisible");
                }
            }
        }
        throw new HorsContrat("empreinte sha-256 absente de Repr-Digest");
    }

    /** Forme de l'en-tête Repr-Digest d'une empreinte hexadécimale (substitut et tests). */
    public static String reprDigest(String hex) {
        return "sha-256=:" + Base64.getEncoder().encodeToString(HexFormat.of().parseHex(hex)) + ":";
    }

    private static JsonNode lire(byte[] brut) {
        try {
            // Contenu vide : MissingNode (Jackson 3), jamais null.
            JsonNode n = JSON.readTree(brut);
            if (!n.isObject()) {
                throw new HorsContrat("réponse qui n'est pas un objet JSON");
            }
            return n;
        } catch (JacksonException e) {
            throw new HorsContrat("réponse illisible : " + e.getOriginalMessage());
        }
    }

    private static JsonNode exiger(JsonNode n, String champ) {
        JsonNode v = n.path(champ);
        if (!v.isArray()) {
            throw new HorsContrat("liste " + champ + " absente");
        }
        return v;
    }

    private static String texte(JsonNode n, String champ) {
        JsonNode v = n.path(champ);
        if (!v.isString() || v.asString().isBlank()) {
            throw new HorsContrat("élément " + champ + " absent");
        }
        return v.asString();
    }

    private static String facultatif(JsonNode n, String champ) {
        JsonNode v = n.path(champ);
        return v.isString() ? v.asString() : null;
    }

    private static int entier(JsonNode n, String champ) {
        JsonNode v = n.path(champ);
        if (!v.isIntegralNumber()) {
            throw new HorsContrat("élément " + champ + " absent");
        }
        return v.asInt();
    }

    private static Instant instant(JsonNode n, String champ) {
        try {
            return Instant.parse(texte(n, champ));
        } catch (java.time.format.DateTimeParseException e) {
            throw new HorsContrat("date " + champ + " hors ISO 8601");
        }
    }
}
