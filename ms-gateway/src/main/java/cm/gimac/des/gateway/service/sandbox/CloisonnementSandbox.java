package cm.gimac.des.gateway.service.sandbox;

import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox;
import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox.Motif;
import cm.gimac.des.gateway.domain.sandbox.JetonReduit;
import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import cm.gimac.des.gateway.domain.sandbox.PolitiqueCloisonnement;
import cm.gimac.des.gateway.domain.sandbox.RouteSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.EmetteurJetonReduit;
import cm.gimac.des.gateway.domain.sandbox.port.JournalDecisionsSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.ProjectionPerimetres;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * Cas d'utilisation de la passerelle sur une route de la sandbox (US-EXI-07) : institution effective (propre ou
 * mandante d'un mandat actif), jeton interne réduit, quota de débit par couche, institution et route, décision
 * journalisée vers MS-AUDIT. Un refus de mandat rend 404 sans révéler l'existence de la ressource.
 */
public class CloisonnementSandbox {

    /** Préfixe des permissions de la sandbox, seules portées par le jeton réduit. */
    static final String PREFIXE_PERMISSION = "SBX_";

    private final ProjectionPerimetres projection;
    private final JournalDecisionsSandbox journal;
    private final Map<String, LimiteurTentatives> quotas;
    private final EmetteurJetonReduit emetteur;
    private final Clock horloge;

    /**
     * @param quotas limiteur par couche (portail, console, des) ; une couche absente n'est pas limitée
     * @param emetteur émetteur du jeton réduit, nul quand le jeton interne de la passerelle n'est pas actif
     */
    public CloisonnementSandbox(
        ProjectionPerimetres projection,
        JournalDecisionsSandbox journal,
        Map<String, LimiteurTentatives> quotas,
        EmetteurJetonReduit emetteur,
        Clock horloge
    ) {
        this.projection = projection;
        this.journal = journal;
        this.quotas = Map.copyOf(quotas);
        this.emetteur = emetteur;
        this.horloge = horloge;
    }

    /** Requête reçue par la passerelle sur une route de la sandbox, après la décision d'OPA. */
    public record Demande(
        String compte,
        String institutionJeton,
        String demandee,
        RouteSandbox route,
        String methode,
        String chemin,
        List<String> permissions,
        String acr,
        String ip
    ) {}

    /** Décision, statut HTTP (200 relayée, 404 hors mandat, 429 quota) et jeton réduit à relayer s'il existe. */
    public record Resultat(DecisionSandbox decision, int statut, Optional<String> jeton) {}

    public Mono<Resultat> evaluer(Demande d) {
        return perimetre(d)
            .map(p -> PolitiqueCloisonnement.decider(d.institutionJeton(), d.demandee(), p.orElse(null), horloge.instant()))
            .flatMap(decision -> decision.admise() ? admettre(d, decision) : Mono.just(new Resultat(decision, 404, Optional.empty())))
            .doOnNext(r -> consigner(d, r));
    }

    /** Le périmètre n'est lu que si une institution mandante, différente de celle du jeton, est demandée. */
    private Mono<Optional<PerimetreMandats>> perimetre(Demande d) {
        boolean mandat = !vide(d.institutionJeton()) && !vide(d.demandee()) && !d.demandee().equals(d.institutionJeton());
        if (!mandat) {
            return Mono.just(Optional.empty());
        }
        return projection.lire(d.compte()).map(Optional::of).defaultIfEmpty(Optional.empty());
    }

    private Mono<Resultat> admettre(Demande d, DecisionSandbox decision) {
        Optional<String> jeton = emetteur == null ? Optional.empty() : emetteur.emettre(jetonReduit(d, decision));
        if (decision.motif() == Motif.MANDAT_ACTIF && jeton.isEmpty()) {
            return Mono.just(new Resultat(DecisionSandbox.de(Motif.JETON_REDUIT_INDISPONIBLE, d.institutionJeton()), 404, Optional.empty()));
        }
        LimiteurTentatives limiteur = quotas.get(d.route().couche());
        if (limiteur == null) {
            return Mono.just(new Resultat(decision, 200, jeton));
        }
        String titulaire = vide(decision.institutionEffective()) ? "gimac:" + d.compte() : decision.institutionEffective();
        String cle = "sbx:" + d.route().couche() + ":" + titulaire + ":" + d.route().service();
        return limiteur
            .consommer(cle)
            .map(admis ->
                Boolean.TRUE.equals(admis)
                    ? new Resultat(decision, 200, jeton)
                    : new Resultat(DecisionSandbox.de(Motif.QUOTA_DEPASSE, decision.institutionEffective()), 429, Optional.empty())
            );
    }

    static JetonReduit jetonReduit(Demande d, DecisionSandbox decision) {
        List<String> permissions = d.permissions().stream().filter(p -> p.startsWith(PREFIXE_PERMISSION)).sorted().toList();
        String propre = vide(d.institutionJeton()) ? "" : d.institutionJeton();
        return new JetonReduit(d.compte(), decision.institutionEffective(), propre, permissions, d.acr(), d.route().nomService());
    }

    private void consigner(Demande d, Resultat r) {
        journal.consigner(
            new JournalDecisionsSandbox.Entree(d.compte(), d.route(), d.methode(), d.chemin(), d.institutionJeton(), d.demandee(), r.decision(), r.statut(), d.ip())
        );
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
