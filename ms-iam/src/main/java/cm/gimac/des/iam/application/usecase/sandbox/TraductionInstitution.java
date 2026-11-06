package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.sandbox.RegistreInstitutions;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Traduction de l'attribut institution_id d'un compte en code participant pour la sandbox (D-134), faite une seule fois
 * par MS-IAM : la passerelle et SBX-ESPACE recopient le code rendu. Identifiant au registre (UUID) : correspondance lue
 * dans MS-PARTICIPANT, gardée dans un cache borné à durée de vie courte (seule la correspondance est gardée, jamais un
 * état : la suspension d'un espace reste lue sans cache dans MS-IAM). Toute autre valeur admise comme code par MS-IAM
 * (ReglesPerimetre) : rendue telle quelle, pour les comptes créés avant D-134. Valeur illisible ou correspondance
 * absente : vide, jamais deviné.
 */
@Component
public class TraductionInstitution {

    /** Code participant admis par MS-IAM pour la sandbox (même règle que ReglesPerimetre). */
    static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    static final Pattern UUID_TEXTE = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    static final int TAILLE_MAX = 10_000;

    private record Entree(String code, Instant expire) {}

    private final RegistreInstitutions registre;
    private final Clock clock;
    private final Duration duree;
    private final Map<UUID, Entree> cache = new ConcurrentHashMap<>();

    public TraductionInstitution(RegistreInstitutions registre, Clock clock, @Value("${gimac.sandbox.registre.cache-duree:PT5M}") Duration duree) {
        this.registre = registre;
        this.clock = clock;
        this.duree = duree;
    }

    /** Traduction sans registre (tests, développement) : seuls les codes participants déjà enregistrés sont admis. */
    public static TraductionInstitution sansRegistre(Clock clock) {
        return new TraductionInstitution(new cm.gimac.des.iam.infrastructure.client.RegistreInstitutionsSansRegistre(), clock, Duration.ofMinutes(5));
    }

    /**
     * Comptes de l'institution désignée par son code ou son identifiant : comptes dont l'attribut porte l'une ou l'autre
     * valeur (D-134), sans doublon.
     */
    public java.util.List<cm.gimac.des.iam.domain.compte.Compte> comptes(cm.gimac.des.iam.application.port.out.Annuaire annuaire, String institution) {
        java.util.Map<String, cm.gimac.des.iam.domain.compte.Compte> r = new java.util.LinkedHashMap<>();
        for (String v : valeurs(institution)) {
            annuaire.parInstitution(v).stream().filter(c -> memeInstitution(institution, c.institutionId())).forEach(c -> r.putIfAbsent(c.id(), c));
        }
        return java.util.List.copyOf(r.values());
    }

    /**
     * Code participant de l'institution du compte ; vide si l'attribut est absent, illisible ou sans correspondance.
     *
     * @throws RegistreInstitutions.RegistreIndisponible si MS-PARTICIPANT est injoignable
     */
    public Optional<String> code(String institutionId) {
        if (institutionId == null || institutionId.isBlank()) {
            return Optional.empty();
        }
        String v = institutionId.strip();
        if (!UUID_TEXTE.matcher(v).matches()) {
            return CODE.matcher(v).matches() ? Optional.of(v) : Optional.empty();
        }
        UUID id = UUID.fromString(v);
        Instant t = clock.instant();
        Entree e = cache.get(id);
        if (e != null && t.isBefore(e.expire())) {
            return Optional.of(e.code());
        }
        Optional<String> code = registre.codeParticipant(id).filter(c -> CODE.matcher(c).matches() && !UUID_TEXTE.matcher(c).matches());
        code.ifPresent(c -> {
            if (cache.size() >= TAILLE_MAX) {
                cache.clear();
            }
            cache.put(id, new Entree(c, t.plus(duree)));
        });
        return code;
    }

    /**
     * Vrai si deux valeurs de institution_id (identifiant au registre ou code participant) désignent la même institution.
     * Registre injoignable : seule l'égalité stricte est admise.
     */
    public boolean memeInstitution(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return false;
        }
        if (a.strip().equals(b.strip())) {
            return true;
        }
        try {
            Optional<String> ca = code(a);
            return ca.isPresent() && ca.equals(code(b));
        } catch (RegistreInstitutions.RegistreIndisponible e) {
            return false;
        }
    }

    /**
     * Valeurs possibles de l'attribut institution_id pour cette institution, désignée par son code ou son identifiant :
     * la valeur reçue, et sa correspondance au registre si elle est connue (comptes créés avant et après D-134).
     */
    public java.util.List<String> valeurs(String institution) {
        String v = institution.strip();
        java.util.Set<String> l = new java.util.LinkedHashSet<>(java.util.List.of(v));
        try {
            if (!UUID_TEXTE.matcher(v).matches()) {
                registre.identifiant(v).ifPresent(id -> l.add(id.toString()));
            } else {
                code(v).ifPresent(l::add);
            }
        } catch (RegistreInstitutions.RegistreIndisponible e) {
            // Registre injoignable : la valeur reçue seule.
        }
        return java.util.List.copyOf(l);
    }

    /** Valeur à enregistrer dans l'attribut d'un compte créé pour cette institution : l'identifiant au registre s'il est connu. */
    public String aEnregistrer(String institution) {
        if (institution == null || UUID_TEXTE.matcher(institution.strip()).matches() || !CODE.matcher(institution.strip()).matches()) {
            return institution;
        }
        try {
            return registre.identifiant(institution.strip()).map(UUID::toString).orElse(institution.strip());
        } catch (RegistreInstitutions.RegistreIndisponible e) {
            return institution.strip();
        }
    }
}
