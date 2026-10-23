package cm.gimac.des.audit.domain.sandbox;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enveloppe d'un événement à effet de la Sandbox GIMAC (US-EXI-10, CC-SBX-GIMAC-02 §4.2.2) : CloudEvents 1.0 publiée par
 * l'un des onze services (sujet Kafka nommé par le type), type {@code sbx-<service>.<agrégat>-<événement>.v<n>}, source
 * du même service ({@code /gimac/sandbox/sbx-<service>}). Validation pure,
 * sans Spring : une enveloppe non conforme rend son motif, et part au rebut sans jamais être inscrite.
 *
 * @param id identifiant CloudEvents
 * @param type type de l'événement
 * @param service service producteur (sbx-trace…)
 * @param survenueLe champ {@code time}
 * @param institution extension {@code institutionid}, sinon {@code codeParticipant} ou {@code institutionId} des
 *     données ; nulle si l'événement ne concerne aucune institution
 * @param acteur auteur de l'effet ({@code acteurId}, {@code auteurId} ou {@code sub} des données), sinon le service
 * @param correlationId extension {@code correlationid}
 */
public record EnveloppeSandbox(String id, String type, String service, Instant survenueLe, String institution, String acteur, String correlationId) {
    /** Les onze services de la sandbox, producteurs admis. */
    public static final List<String> SERVICES = List.of(
        "sbx-espace",
        "sbx-raccordement",
        "sbx-referentiel",
        "sbx-donnees",
        "sbx-simulation",
        "sbx-mobile",
        "sbx-iso20022",
        "sbx-carte",
        "sbx-campagne",
        "sbx-trace",
        "sbx-lecture"
    );

    /** Domaine des entrées de la sandbox au registre (filtre « source sandbox »). */
    public static final String DOMAINE = "sandbox";

    /** Demandes de notification : destinées à MS-NOTIFICATION, sans effet à inscrire. */
    static final String NOTIFICATION = ".notification-requested.v";

    private static final Pattern TYPE = Pattern.compile("^(sbx-[a-z0-9]+)\\.[a-z0-9]+(-[a-z0-9]+)+\\.v[1-9][0-9]*$");
    private static final int LONGUEUR_MAX = 100;

    /** Résultat de la lecture : enveloppe conforme, événement sans effet (ignoré), ou motif du rebut. */
    public record Lecture(EnveloppeSandbox enveloppe, boolean ignore, String motif) {
        static Lecture rebut(String motif) {
            return new Lecture(null, false, motif);
        }
    }

    /** Champs bruts lus dans l'enveloppe (JSON déjà analysé par l'appelant). */
    public record Champs(
        String specversion,
        String id,
        String type,
        String source,
        String time,
        boolean donneesObjet,
        String institutionid,
        String institutionDonnees,
        String acteurDonnees,
        String correlationid
    ) {}

    /** Valide l'enveloppe ; l'ordre des contrôles fixe le motif retenu. */
    public static Lecture lire(Champs c) {
        if (!"1.0".equals(c.specversion())) {
            return Lecture.rebut("SPECVERSION_INVALIDE");
        }
        if (vide(c.id()) || c.id().length() > 200) {
            return Lecture.rebut("ID_ABSENT");
        }
        Matcher t = c.type() == null ? null : TYPE.matcher(c.type());
        if (t == null || !t.matches() || c.type().length() > LONGUEUR_MAX || !SERVICES.contains(t.group(1))) {
            return Lecture.rebut("TYPE_INVALIDE");
        }
        if (c.type().contains(NOTIFICATION)) {
            return new Lecture(null, true, null);
        }
        if (!sourceDuService(c.source(), t.group(1))) {
            return Lecture.rebut("SOURCE_INVALIDE");
        }
        Optional<Instant> le = instant(c.time());
        if (le.isEmpty()) {
            return Lecture.rebut("TIME_INVALIDE");
        }
        if (!c.donneesObjet()) {
            return Lecture.rebut("DATA_ABSENTES");
        }
        String institution = !vide(c.institutionid()) ? c.institutionid().strip() : vide(c.institutionDonnees()) ? null : c.institutionDonnees().strip();
        if (institution != null && institution.length() > 64) {
            return Lecture.rebut("INSTITUTION_INVALIDE");
        }
        String acteur = vide(c.acteurDonnees()) ? t.group(1) : borne(c.acteurDonnees().strip());
        return new Lecture(new EnveloppeSandbox(c.id().strip(), c.type(), t.group(1), le.orElseThrow(), institution, acteur, c.correlationid()), false, null);
    }

    /** Source URI-référence dont un segment nomme le service du type ({@code /gimac/sandbox/sbx-espace}, {@code /sbx-espace}). */
    static boolean sourceDuService(String source, String service) {
        if (vide(source) || source.contains("://")) {
            return false;
        }
        for (String segment : source.split("/")) {
            if (segment.equals(service)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<Instant> instant(String s) {
        if (vide(s)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(s.strip()));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private static String borne(String s) {
        return s.length() <= LONGUEUR_MAX ? s : s.substring(0, LONGUEUR_MAX);
    }
}
