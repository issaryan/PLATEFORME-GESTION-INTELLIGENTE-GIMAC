package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.EtapesIdempotentes;
import cm.gimac.des.iam.application.port.out.sandbox.DelegationsSandbox;
import cm.gimac.des.iam.application.port.out.sandbox.PerimetresSuspendus;
import cm.gimac.des.iam.application.usecase.CatalogueProfils;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.sandbox.Delegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatEffectif;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
import cm.gimac.des.iam.domain.sandbox.ReglesDelegation;
import cm.gimac.des.iam.domain.sandbox.ReglesPerimetre;
import cm.gimac.des.iam.domain.service.ReglesAdministrationDeleguee;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Délégations temporaires de la sandbox dans MS-IAM (D-119) : mandats de partenaire (US-EXI-04, saga MANDAT de
 * SBX-ESPACE, algorithme 6.3 du CC) ; révocation et prolongation communes aux accès de prestataire (US-EXI-24). Étapes
 * rejouables par leur clé d'idempotence ; chaque acte est journalisé sur le compte de l'intervenant
 * (iam.account-administered.v1).
 */
@Service
public class DelegationsSandboxService {

    /** Délégation rendue avec son état à l'instant et le nom de son titulaire. */
    public record VueDelegation(Delegation delegation, EtatEffectif etat, String nomCompte, boolean rejouee) {}

    /** Page de délégations. */
    public record PageDelegations(List<VueDelegation> contenu, long total, int page, int taille) {}

    static final String OCTROI = "DELEGATION_OCTROI";
    static final String REVOCATION = "DELEGATION_REVOCATION";
    static final String PROLONGATION = "DELEGATION_PROLONGATION";

    private final DelegationsSandbox delegations;
    private final PerimetresSuspendus perimetres;
    private final Annuaire annuaire;
    private final CatalogueProfils catalogue;
    private final EtapesIdempotentes etapes;
    private final JournalSandbox journal;
    private final ProprietesSandbox proprietes;
    private final Clock clock;

    public DelegationsSandboxService(
        DelegationsSandbox delegations,
        PerimetresSuspendus perimetres,
        Annuaire annuaire,
        CatalogueProfils catalogue,
        EtapesIdempotentes etapes,
        JournalSandbox journal,
        ProprietesSandbox proprietes,
        Clock clock
    ) {
        this.delegations = delegations;
        this.perimetres = perimetres;
        this.annuaire = annuaire;
        this.catalogue = catalogue;
        this.etapes = etapes;
        this.journal = journal;
        this.proprietes = proprietes;
        this.clock = clock;
    }

    Duration dureeMax(TypeDelegation type) {
        return type == TypeDelegation.MANDAT ? proprietes.mandatDureeMax() : proprietes.prestataireDureeMax();
    }

    private Optional<String> dejaFaite(String cle, String operation, String resultat, Acteur acteur) {
        return cle == null || cle.isBlank() ? Optional.empty() : etapes.reserver(cle.strip(), operation, resultat, acteur.id());
    }

    Compte compte(String id) {
        return annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id));
    }

    VueDelegation vue(Delegation d, boolean rejouee) {
        return new VueDelegation(d, d.etatA(clock.instant()), annuaire.lire(d.compteId()).map(Compte::nomAffiche).orElse(d.compteId()), rejouee);
    }

    Delegation exiger(String id) {
        return delegations.lire(id).orElseThrow(() -> IamException.introuvable("IAM_SBX_DELEGATION_INTROUVABLE", id));
    }

    /**
     * Mandat de partenaire : délégation temporaire de l'intervenant désigné sur l'institution mandante, aux bornes du
     * mandat. Compte inconnu : 404 ; compte inactif : 422 IAM_SBX_COMPTE_INACTIF ; compte de l'institution mandante : 422
     * IAM_SBX_MANDAT_PROPRE_INSTITUTION ; périmètre suspendu : 403 ; bornes et droits : {@link ReglesDelegation}.
     */
    @Transactional
    public VueDelegation accorderMandat(Acteur acteur, String compteId, String institution, Collection<String> permissions, Instant debut, Instant fin, String cle) {
        String inst = ReglesPerimetre.code(institution);
        Compte c = compte(compteId);
        if (!c.actif()) {
            throw IamException.nonTraitable("IAM_SBX_COMPTE_INACTIF", "Le compte désigné est inactif", Map.of("id", compteId));
        }
        if (inst.equals(c.institutionId())) {
            throw IamException.nonTraitable("IAM_SBX_MANDAT_PROPRE_INSTITUTION", "Un mandat désigne un intervenant extérieur à l'institution", Map.of("id", compteId));
        }
        ReglesPerimetre.verifierPerimetreOuvert(perimetres.suspendu(inst));
        Instant maintenant = clock.instant();
        ReglesDelegation.verifierBornes(debut, fin, maintenant, dureeMax(TypeDelegation.MANDAT));
        Set<String> accordables = ReglesDelegation.accordables(ReglesAdministrationDeleguee.rolesDelegables(catalogue.externes(), catalogue.compositions()), catalogue.compositions());
        Set<String> perimetre = ReglesDelegation.verifierPerimetre(permissions, accordables);
        String id = UUID.randomUUID().toString();
        Optional<String> deja = dejaFaite(cle, OCTROI, id, acteur);
        if (deja.isPresent()) {
            return vue(exiger(deja.orElseThrow()), true);
        }
        Delegation d = new Delegation(id, TypeDelegation.MANDAT, compteId, inst, perimetre, Set.of(), debut, fin, EtatDelegation.ACCORDEE, acteur.id(), null, null, maintenant, null);
        delegations.enregistrer(d);
        journal.compte(acteur, "MANDAT_ACCORDE", compteId, c.identifiant(), null, List.of(inst, debut + " / " + fin, String.join(" ", perimetre)));
        return vue(d, false);
    }

    /**
     * Révocation immédiate : la délégation est close, l'introspection de ses jetons refusée dès maintenant et les sessions
     * du titulaire révoquées. Déjà révoquée : sans effet.
     */
    @Transactional
    public VueDelegation revoquer(Acteur acteur, String id, String motif, String cle) {
        Delegation d = exiger(id);
        if (dejaFaite(cle, REVOCATION, id, acteur).isPresent() || d.etat() == EtatDelegation.REVOQUEE) {
            return vue(d, true);
        }
        Delegation r = d.avecEtat(EtatDelegation.REVOQUEE, acteur.id(), motif, clock.instant());
        delegations.mettreAJour(r);
        annuaire.lire(d.compteId()).ifPresent(c -> annuaire.fermerSessions(c.id()));
        journal.compte(acteur, "DELEGATION_REVOQUEE", d.compteId(), d.compteId(), motif, List.of(d.type().name(), String.valueOf(d.institutionId()), id));
        return vue(r, false);
    }

    /** Prolongation : la fin est reportée sous le même contrôle de durée. */
    @Transactional
    public VueDelegation prolonger(Acteur acteur, String id, Instant nouvelleFin, String cle) {
        Delegation d = exiger(id);
        if (dejaFaite(cle, PROLONGATION, id, acteur).isPresent()) {
            return vue(d, true);
        }
        ReglesDelegation.verifierProlongation(d, nouvelleFin, clock.instant(), dureeMax(d.type()));
        Delegation p = d.avecFin(nouvelleFin);
        delegations.mettreAJour(p);
        journal.compte(acteur, "DELEGATION_PROLONGEE", d.compteId(), d.compteId(), null, List.of(id, d.fin() + " -> " + nouvelleFin));
        return vue(p, false);
    }

    @Transactional(readOnly = true)
    public VueDelegation lire(String id) {
        return vue(exiger(id), false);
    }

    /** Délégations d'un type (toutes si nul), les plus récentes d'abord (console). */
    @Transactional(readOnly = true)
    public PageDelegations lister(TypeDelegation type, int page, int taille) {
        int p = Math.max(0, page);
        int t = Math.min(Math.max(1, taille), 100);
        return new PageDelegations(delegations.lister(type, p, t).stream().map(d -> vue(d, false)).toList(), delegations.compter(type), p, t);
    }
}
