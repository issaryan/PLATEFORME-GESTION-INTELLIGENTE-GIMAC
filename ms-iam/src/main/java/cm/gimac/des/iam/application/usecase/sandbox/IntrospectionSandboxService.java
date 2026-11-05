package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.sandbox.DelegationsSandbox;
import cm.gimac.des.iam.application.port.out.sandbox.LecteurJeton;
import cm.gimac.des.iam.application.port.out.sandbox.PerimetresSuspendus;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.sandbox.Delegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatEffectif;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
import cm.gimac.des.iam.domain.sandbox.Introspection;
import cm.gimac.des.iam.security.SecurityUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Introspection des jetons pour la sandbox (D-119, US-EXI-06, US-EXI-04, US-EXI-24) : MS-IAM décide, à l'instant de la
 * requête et sans cache, si un jeton du fournisseur d'identité du GIMAC est encore admis, et rend sa portée effective pour
 * le jeton interne réduit. Refus : jeton invalide ou échu, compte inconnu ou inactif, périmètre de l'institution suspendu,
 * institution demandée hors de celle du compte, accès de prestataire échu ou hors de ses composants.
 */
@Service
public class IntrospectionSandboxService {

    private final LecteurJeton lecteur;
    private final Annuaire annuaire;
    private final PerimetresSuspendus perimetres;
    private final DelegationsSandbox delegations;
    private final JournalSandbox journal;
    private final TraductionInstitution traduction;
    private final Clock clock;

    public IntrospectionSandboxService(
        LecteurJeton lecteur,
        Annuaire annuaire,
        PerimetresSuspendus perimetres,
        DelegationsSandbox delegations,
        JournalSandbox journal,
        TraductionInstitution traduction,
        Clock clock
    ) {
        this.traduction = traduction;
        this.lecteur = lecteur;
        this.annuaire = annuaire;
        this.perimetres = perimetres;
        this.delegations = delegations;
        this.journal = journal;
        this.clock = clock;
    }

    @Transactional
    public Introspection introspecter(String jeton, String institutionDemandee) {
        return introspecter(jeton, institutionDemandee, null);
    }

    /**
     * @param composant composant technique appelé (SBX-TRACE…), présenté par la passerelle ou le service ; il borne un
     *     accès de prestataire (US-EXI-24)
     */
    @Transactional
    public Introspection introspecter(String jeton, String institutionDemandee, String composant) {
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
        // D-134 : institution_id du compte (identifiant au registre) traduit une seule fois en code participant, seule
        // clé des périmètres, des mandats et du jeton réduit de la sandbox.
        String code = null;
        if (!k.interne()) {
            try {
                code = traduction.code(k.institutionId()).orElse(null);
            } catch (cm.gimac.des.iam.application.port.out.sandbox.RegistreInstitutions.RegistreIndisponible e) {
                return Introspection.refusee(sujet, "REGISTRE_INDISPONIBLE");
            }
            if (code == null) {
                return Introspection.refusee(sujet, "INSTITUTION_NON_TRADUITE");
            }
            if (perimetres.suspendu(code)) {
                return Introspection.refusee(sujet, "PERIMETRE_SUSPENDU");
            }
        }
        String demandee = institutionDemandee == null || institutionDemandee.isBlank() ? null : institutionDemandee.strip();
        if (k.interne() && delegations.designe(sujet, TypeDelegation.PRESTATAIRE)) {
            return prestataire(k, demandee, composant, expiration(c));
        }
        if (demandee != null && !demandee.equals(code)) {
            Introspection m = mandat(sujet, demandee, expiration(c));
            return m.active() ? new Introspection(true, sujet, m.institutionId(), m.permissions(), m.type(), m.delegationId(), m.expiration(), m.composants(), null, code) : m;
        }
        return new Introspection(true, sujet, code, permissions(c), Introspection.COMPTE, null, expiration(c), Set.of(), null, code);
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

    /**
     * Accès de prestataire de maintenance (US-EXI-24) : admis seulement sous une délégation PRESTATAIRE active à l'instant
     * de la requête (la date de fin s'applique sans planificateur), pour un des composants désignés ; jamais pour les
     * données d'une institution (tentative refusée et journalisée), sans aucune permission projetée, jusqu'à la fin de
     * l'accès au plus.
     */
    Introspection prestataire(Compte k, String demandee, String composant, Instant expJeton) {
        String sujet = k.id();
        if (demandee != null) {
            journal.compte(new Acteur(sujet, k.identifiant(), Set.of()), "TENTATIVE_ACCES_PRESTATAIRE", sujet, k.identifiant(), "Données d'une institution", List.of(demandee, String.valueOf(composant)));
            return Introspection.refusee(sujet, "PRESTATAIRE_HORS_DONNEES");
        }
        Instant t = clock.instant();
        String cible = composant == null || composant.isBlank() ? null : composant.strip();
        Optional<Delegation> a = delegations
            .enCours(sujet, TypeDelegation.PRESTATAIRE, t)
            .stream()
            .filter(d -> d.etatA(t) == EtatEffectif.ACTIVE && (cible == null || d.composants().contains(cible)))
            .findFirst();
        if (a.isEmpty()) {
            return Introspection.refusee(sujet, cible == null ? "ACCES_PRESTATAIRE_INACTIF" : "COMPOSANT_HORS_ACCES");
        }
        Delegation d = a.orElseThrow();
        Instant exp = expJeton == null || d.fin().isBefore(expJeton) ? d.fin() : expJeton;
        return new Introspection(true, sujet, null, Set.of(), TypeDelegation.PRESTATAIRE.name(), d.id(), exp, d.composants(), null);
    }

    static Set<String> permissions(Map<String, Object> claims) {
        return SecurityUtils.extractAuthorityFromClaims(claims).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    static Instant expiration(Map<String, Object> claims) {
        return claims.get("exp") instanceof Instant i ? i : null;
    }
}
