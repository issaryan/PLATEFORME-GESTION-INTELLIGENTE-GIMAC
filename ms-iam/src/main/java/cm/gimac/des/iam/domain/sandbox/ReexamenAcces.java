package cm.gimac.des.iam.domain.sandbox;

import cm.gimac.des.iam.domain.AccessReviewDecision;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;

/** Période et empreinte canonique d'une campagne de réexamen (IAM-18, US-EXI-23). */
public final class ReexamenAcces {

    public static final ZoneId FUSEAU = ZoneId.of("Africa/Douala");

    private ReexamenAcces() {}

    /** Trimestre calendaire, par exemple 2026-T4. */
    public static String periode(Clock clock) {
        LocalDate d = LocalDate.now(clock.withZone(FUSEAU));
        return d.getYear() + "-T" + ((d.getMonthValue() - 1) / 3 + 1);
    }

    /** Listes canoniques : triées, une valeur par ligne ; jamais de valeur nulle. */
    public static String canonique(Collection<String> valeurs) {
        return valeurs.stream().sorted().reduce("", (a, b) -> a + b + "\n");
    }

    public static List<String> liste(String canonique) {
        return canonique == null || canonique.isEmpty() ? List.of() : canonique.lines().toList();
    }

    /** SHA-256 stable : valeurs préfixées par leur longueur, décisions triées par compte. */
    public static String empreinte(String periode, Collection<AccessReviewDecision> decisions) {
        StringBuilder canonique = new StringBuilder();
        ajouter(canonique, periode);
        decisions.stream().sorted(java.util.Comparator.comparing(d -> d.getCollaborator().getFederatedId())).forEach(d -> {
            ajouter(canonique, d.getCollaborator().getFederatedId());
            ajouter(canonique, d.getProfilesSnapshot());
            ajouter(canonique, d.getPermissionsSnapshot());
            ajouter(canonique, d.getDecision().name());
            ajouter(canonique, d.getComment());
            ajouter(canonique, String.valueOf(d.getDecidedBy()));
            ajouter(canonique, String.valueOf(d.getDecidedAt()));
        });
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonique.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private static void ajouter(StringBuilder cible, String valeur) {
        String v = valeur == null ? "" : valeur;
        cible.append(v.length()).append(':').append(v).append(';');
    }
}
