package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.sandbox.LecteurJeton;
import cm.gimac.des.iam.application.port.out.sandbox.PerimetresSuspendus;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.sandbox.Introspection;
import cm.gimac.des.iam.security.SecurityUtils;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Introspection des jetons pour la sandbox (D-119, US-EXI-06) : MS-IAM décide, à l'instant de la requête et sans
 * cache, si un jeton du fournisseur d'identité du GIMAC est encore admis, et rend sa portée effective pour le jeton
 * interne réduit. Refus : jeton invalide ou échu, compte inconnu ou inactif, périmètre de l'institution suspendu,
 * institution demandée hors de celle du compte.
 */
@Service
public class IntrospectionSandboxService {

    private final LecteurJeton lecteur;
    private final Annuaire annuaire;
    private final PerimetresSuspendus perimetres;

    public IntrospectionSandboxService(LecteurJeton lecteur, Annuaire annuaire, PerimetresSuspendus perimetres) {
        this.lecteur = lecteur;
        this.annuaire = annuaire;
        this.perimetres = perimetres;
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
            return Introspection.refusee(sujet, "HORS_PERIMETRE");
        }
        return new Introspection(true, sujet, k.institutionId(), permissions(c), Introspection.COMPTE, null, expiration(c), Set.of(), null);
    }

    static Set<String> permissions(Map<String, Object> claims) {
        return SecurityUtils.extractAuthorityFromClaims(claims).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    static Instant expiration(Map<String, Object> claims) {
        return claims.get("exp") instanceof Instant i ? i : null;
    }
}
