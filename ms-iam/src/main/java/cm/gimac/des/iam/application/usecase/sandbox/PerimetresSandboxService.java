package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.EtapesIdempotentes;
import cm.gimac.des.iam.application.port.out.sandbox.DelegationsSandbox;
import cm.gimac.des.iam.application.port.out.sandbox.PerimetresSuspendus;
import cm.gimac.des.iam.application.port.out.sandbox.ProtectionsAdministrateur;
import cm.gimac.des.iam.application.port.out.sandbox.ProtectionsAdministrateur.Protection;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.sandbox.PerimetreSuspendu;
import cm.gimac.des.iam.domain.sandbox.ReglesPerimetre;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suspension du périmètre d'une institution et protection du dernier administrateur (D-119, US-EXI-06, algorithme 6.2
 * du CC de la sandbox), points d'entrée des sagas SUSPENSION et RETABLISSEMENT de SBX-ESPACE.
 *
 * <ul>
 *   <li>Suspension : l'indicateur est posé sur le périmètre, les sessions de chaque compte rattaché et de chaque
 *       titulaire d'un mandat sur ce périmètre sont révoquées ; aucun état de compte ni de mandat n'est modifié.</li>
 *   <li>Rétablissement : l'indicateur est levé ; chaque compte retrouve son propre état, un compte suspendu avant la
 *       suspension le reste.</li>
 *   <li>Étape rejouée avec la même clé d'idempotence : résultat d'origine, sans second effet (scénario 4).</li>
 * </ul>
 */
@Service
public class PerimetresSandboxService {

    /** État d'un périmètre après une étape de saga. */
    public record EtatPerimetre(String code, boolean suspendu, Instant depuis, int sessionsRevoquees, boolean rejouee) {}

    /** Page de périmètres suspendus. */
    public record PagePerimetres(List<PerimetreSuspendu> contenu, long total, int page, int taille) {}

    /** Protection d'un compte après une étape de saga. */
    public record EtatProtection(String compteId, String institutionId, boolean protege, boolean rejouee) {}

    /** Compte protégé, avec son nom affiché. */
    public record CompteProtege(String compteId, String nom, String institutionId, Instant depuis) {}

    /** Page de comptes protégés. */
    public record PageProtections(List<CompteProtege> contenu, long total, int page, int taille) {}

    static final String SUSPENSION = "PERIMETRE_SUSPENSION";
    static final String RETABLISSEMENT = "PERIMETRE_RETABLISSEMENT";
    static final String PROTECTION = "PROTECTION_POSEE";
    static final String LEVEE = "PROTECTION_LEVEE";

    private final PerimetresSuspendus perimetres;
    private final ProtectionsAdministrateur protections;
    private final DelegationsSandbox delegations;
    private final Annuaire annuaire;
    private final EtapesIdempotentes etapes;
    private final JournalSandbox journal;
    private final Clock clock;

    public PerimetresSandboxService(
        PerimetresSuspendus perimetres,
        ProtectionsAdministrateur protections,
        DelegationsSandbox delegations,
        Annuaire annuaire,
        EtapesIdempotentes etapes,
        JournalSandbox journal,
        Clock clock
    ) {
        this.perimetres = perimetres;
        this.protections = protections;
        this.delegations = delegations;
        this.annuaire = annuaire;
        this.etapes = etapes;
        this.journal = journal;
        this.clock = clock;
    }

    private boolean rejouee(String cle, String operation, String resultat, Acteur acteur) {
        return cle != null && !cle.isBlank() && etapes.reserver(cle.strip(), operation, resultat, acteur.id()).isPresent();
    }

    /** Pose ou lève l'indicateur de suspension du périmètre (étape pivot de la saga SUSPENSION, ou RETABLISSEMENT). */
    @Transactional
    public EtatPerimetre suspendre(Acteur acteur, String code, boolean suspendu, String motif, String cle) {
        String c = ReglesPerimetre.code(code);
        if (rejouee(cle, suspendu ? SUSPENSION : RETABLISSEMENT, c, acteur)) {
            return etat(c, 0, true);
        }
        if (!suspendu) {
            if (perimetres.lever(c)) {
                journal.objet(acteur, "PERIMETRE_RETABLI", c, c, motif, List.of());
                annuaire.parInstitution(c).forEach(x -> journal.compte(acteur, "PERIMETRE_RETABLI", x.id(), x.identifiant(), motif, List.of(c)));
            }
            return etat(c, 0, false);
        }
        boolean pose = perimetres.poser(c, acteur.id(), motif, clock.instant());
        Set<String> touches = new LinkedHashSet<>();
        List<Compte> comptes = annuaire.parInstitution(c);
        comptes.forEach(x -> touches.add(x.id()));
        // Titulaires d'un mandat sur ce périmètre (US-EXI-04) : leurs sessions sont révoquées aussi.
        touches.addAll(delegations.titulaires(c, clock.instant()));
        touches.forEach(annuaire::fermerSessions);
        if (pose) {
            journal.objet(acteur, "PERIMETRE_SUSPENDU", c, c, motif, List.of("sessions révoquées : " + touches.size()));
            comptes.forEach(x -> journal.compte(acteur, "PERIMETRE_SUSPENDU", x.id(), x.identifiant(), motif, List.of(c)));
        }
        return etat(c, touches.size(), false);
    }

    private EtatPerimetre etat(String code, int sessions, boolean rejouee) {
        Optional<PerimetreSuspendu> p = perimetres.lire(code);
        return new EtatPerimetre(code, p.isPresent(), p.map(PerimetreSuspendu::depuis).orElse(null), sessions, rejouee);
    }

    /** État du périmètre. */
    @Transactional(readOnly = true)
    public EtatPerimetre lire(String code) {
        return etat(ReglesPerimetre.code(code), 0, false);
    }

    /** Périmètres suspendus, les plus récents d'abord (console). */
    @Transactional(readOnly = true)
    public PagePerimetres suspendus(int page, int taille) {
        int p = Math.max(0, page);
        int t = Math.min(Math.max(1, taille), 100);
        return new PagePerimetres(perimetres.lister(p, t), perimetres.compter(), p, t);
    }

    /** Pose ou lève l'attribut de protection du dernier administrateur actif d'un compte d'institution. */
    @Transactional
    public EtatProtection proteger(Acteur acteur, String compteId, boolean protege, String cle) {
        Compte c = annuaire.lire(compteId).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", compteId));
        if (c.interne()) {
            throw IamException.nonTraitable("IAM_SBX_COMPTE_HORS_INSTITUTION", "Seul un compte d'institution est protégé", Map.of("id", compteId));
        }
        boolean rejouee = rejouee(cle, protege ? PROTECTION : LEVEE, compteId, acteur);
        if (!rejouee && protections.definir(compteId, c.institutionId(), protege, acteur.id(), clock.instant())) {
            journal.compte(acteur, protege ? "PROTECTION_DERNIER_ADMINISTRATEUR_POSEE" : "PROTECTION_DERNIER_ADMINISTRATEUR_LEVEE", compteId, c.identifiant(), null, List.of(c.institutionId()));
        }
        return new EtatProtection(compteId, c.institutionId(), protections.protege(compteId), rejouee);
    }

    /** Comptes protégés (console), avec leur nom affiché. */
    @Transactional(readOnly = true)
    public PageProtections protections(int page, int taille) {
        int p = Math.max(0, page);
        int t = Math.min(Math.max(1, taille), 100);
        List<CompteProtege> contenu = protections
            .lister(p, t)
            .stream()
            .map((Protection x) -> new CompteProtege(x.compteId(), annuaire.lire(x.compteId()).map(Compte::nomAffiche).orElse(x.compteId()), x.institutionId(), x.depuis()))
            .toList();
        return new PageProtections(contenu, protections.compter(), p, t);
    }
}
