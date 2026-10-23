package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.sandbox.DelegationsSandbox;
import cm.gimac.des.iam.application.port.out.sandbox.LecteurJeton;
import cm.gimac.des.iam.application.port.out.sandbox.PerimetresSuspendus;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.sandbox.Delegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatEffectif;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
import cm.gimac.des.iam.domain.sandbox.Introspection;
import cm.gimac.des.iam.security.SecurityUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Introspection des jetons pour la sandbox (D-119, US-EXI-06, US-EXI-04) : MS-IAM décide, à l'instant de la requête et sans
 * cache, si un jeton du fournisseur d'identité du GIMAC est encore admis, et rend sa portée effective pour le jeton
 * interne réduit. Refus : jeton invalide ou échu, compte inconnu ou inactif, périmètre de l'institution suspendu,
 * institution demandée hors de celle du compte.
 */
@Service
public class IntrospectionSandboxService {

    private final LecteurJeton lecteur;
    private final Annuaire annuaire;
    private final PerimetresSuspendus perimetres;
    private final DelegationsSandbox delegations;
    private final Clock clock;

    public IntrospectionSandboxService(LecteurJeton lecteur, Annuaire annuaire, PerimetresSuspendus perimetres, DelegationsSandbox delegations, Clock clock) {
        this.lecteur = lecteur;
        this.annuaire = annuaire;
        this.perimetres = perimetres;
        this.delegations = delegations;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Introspection introspecter(String jeton, String institutionDemandee) {
        Optional<Map<String, Object>> claims = lecteur.lire(jeton);
        if (claims.isEmpty()) {
            return Introspection.refusee(null, "JETON_INVALIDE");
        }
        Map<String, Object> c = claims.orElseThrow();
        String sujet = c.get("sub") == null ? null : c.get("sub").toString();
        Optional<Compte> compte = sujet == null ? Optional.empty() : annuaire.lire(sujet);
        if (compte.isEmpty()) {
            return Introspection.refusee(sujet, "COMPTE_INCONNU");
        }
        Compte k = compte.orElseThrow();
        if (!k.actif()) {
            return Introspection.refusee(sujet, "COMPTE_INACTIF");
        }
        if (!k.interne() && perimetres.suspendu(k.institutionId())) {
            return Introspection.refusee(sujet, "PERIMETRE_SUSPENDU");
        }
        String demandee = institutionDemandee == null || institutionDemandee.isBlank() ? null : institutionDemandee.strip();
        if (demandee != null && !demandee.equals(k.institutionId())) {
            return mandat(sujet, demandee, expiration(c));
        }
        return new Introspection(true, sujet, k.institutionId(), permissions(c), Introspection.COMPTE, null, expiration(c), Set.of(), null);
    }

    /**
     * Jeton délégué (US-EXI-04) : institution demandée autre que celle du compte. Admis seulement sous un mandat actif à
     * l'instant de la requête (la date de fin s'applique sans le planificateur de SBX-ESPACE), le périmètre mandant non
     * suspendu ; projeté avec l'institution mandante et les seuls droits du mandat, jusqu'à la fin du mandat au plus.
     */
    Introspection mandat(String sujet, String demandee, Instant expJeton) {
        if (perimetres.suspendu(demandee)) {
            return Introspection.refusee(sujet, "PERIMETRE_SUSPENDU");
        }
        Instant t = clock.instant();
        Optional<Delegation> m = delegations
            .enCours(sujet, TypeDelegation.MANDAT, t)
            .stream()
            .filter(d -> demandee.equals(d.institutionId()) && d.etatA(t) == EtatEffectif.ACTIVE)
            .findFirst();
        if (m.isEmpty()) {
            return Introspection.refusee(sujet, "HORS_MANDAT");
        }
        Delegation d = m.orElseThrow();
        Instant exp = expJeton == null || d.fin().isBefore(expJeton) ? d.fin() : expJeton;
        return new Introspection(true, sujet, demandee, d.permissions(), TypeDelegation.MANDAT.name(), d.id(), exp, Set.of(), null);
    }

    static Set<String> permissions(Map<String, Object> claims) {
        return SecurityUtils.extractAuthorityFromClaims(claims).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    static Instant expiration(Map<String, Object> claims) {
        return claims.get("exp") instanceof Instant i ? i : null;
    }
}
