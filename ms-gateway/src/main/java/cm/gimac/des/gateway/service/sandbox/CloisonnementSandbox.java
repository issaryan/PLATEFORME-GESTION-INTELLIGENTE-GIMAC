package cm.gimac.des.gateway.service.sandbox;

import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import cm.gimac.des.gateway.domain.sandbox.ContexteAction;
import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox;
import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox.Motif;
import cm.gimac.des.gateway.domain.sandbox.JetonReduit;
import cm.gimac.des.gateway.domain.sandbox.RouteSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.EmetteurJetonReduit;
import cm.gimac.des.gateway.domain.sandbox.port.IntrospectionSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.IntrospectionSandbox.Portee;
import cm.gimac.des.gateway.domain.sandbox.port.JournalDecisionsSandbox;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * Cas d'utilisation de la passerelle sur une route de la sandbox (US-EXI-07) : MS-IAM décide à chaque requête si le
 * jeton original est encore actif et rend la portée effective (compte propre, mandat ou prestataire). Aucun cache local
 * ne participe à cette décision. Le jeton interne réduit, le quota et le journal de décision sont appliqués ensuite.
 */
public class CloisonnementSandbox {

    /** Préfixe des permissions de la sandbox, seules portées par le jeton réduit. */
    static final String PREFIXE_PERMISSION = "SBX_";

    private final IntrospectionSandbox introspection;
    private final JournalDecisionsSandbox journal;
    private final Map<String, LimiteurTentatives> quotas;
    private final EmetteurJetonReduit emetteur;
    private final boolean jetonReduitObligatoire;

    /**
     * @param quotas limiteur par couche (portail, console, des) ; une couche absente n'est pas limitée
     * @param emetteur émetteur du jeton réduit, nul quand le jeton interne de la passerelle n'est pas actif
     */
    public CloisonnementSandbox(
        IntrospectionSandbox introspection,
        JournalDecisionsSandbox journal,
        Map<String, LimiteurTentatives> quotas,
        EmetteurJetonReduit emetteur
    ) {
        this(introspection, journal, quotas, emetteur, true);
    }

    /**
     * @param jetonReduitObligatoire D-134 : sans jeton réduit, toute requête est refusée. Faux seulement en attendant
     *     l'émetteur du jeton interne en déploiement Docker (gimac.sandbox.jeton-reduit.obligatoire, par défaut égal à
     *     gimac.securite.jeton-fantome.actif) : le jeton original reste alors relayé pour un compte du GIMAC ou d'une
     *     institution, jamais pour un mandat ni un prestataire, comme avant D-134.
     */
    public CloisonnementSandbox(
        IntrospectionSandbox introspection,
        JournalDecisionsSandbox journal,
        Map<String, LimiteurTentatives> quotas,
        EmetteurJetonReduit emetteur,
        boolean jetonReduitObligatoire
    ) {
        this.introspection = introspection;
        this.journal = journal;
        this.quotas = Map.copyOf(quotas);
        this.emetteur = emetteur;
        this.jetonReduitObligatoire = jetonReduitObligatoire;
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
        String ip,
        String jetonOriginal,
        String contexte
    ) {
        /** Demande sans contexte d'action choisi (D-133) : INTEGRATION pour un compte à double rôle. */
        public Demande(
            String compte,
            String institutionJeton,
            String demandee,
            RouteSandbox route,
            String methode,
            String chemin,
            List<String> permissions,
            String acr,
            String ip,
            String jetonOriginal
        ) {
            this(compte, institutionJeton, demandee, route, methode, chemin, permissions, acr, ip, jetonOriginal, null);
        }

        @Override
        public String toString() {
            return "Demande[compte=" + compte + ", institutionJeton=" + institutionJeton + ", demandee=" + demandee +
                ", route=" + route + ", methode=" + methode + ", chemin=" + chemin + ", permissions=" + permissions +
                ", acr=" + acr + ", ip=" + ip + ", contexte=" + contexte + ", jetonOriginal=****]";
        }
    }

    /** Décision, statut HTTP (200 relayée, 404 refus indiscernable, 429 quota) et jeton réduit éventuel. */
    public record Resultat(DecisionSandbox decision, int statut, Optional<String> jeton) {}

    public Mono<Resultat> evaluer(Demande d) {
        return introspection
            .introspecter(d.jetonOriginal(), d.demandee(), composant(d.route()))
            .switchIfEmpty(Mono.error(() -> new IllegalStateException("Réponse d'introspection vide")))
            .flatMap(portee -> {
                if (!portee.active()) {
                    return Mono.just(refus(d, Motif.INTROSPECTION_REFUSEE));
                }
                DecisionSandbox decision = decision(portee);
                return admettre(d, portee, decision);
            })
            .onErrorResume(e -> Mono.just(refus(d, Motif.IAM_INDISPONIBLE)))
            .doOnNext(r -> consigner(d, r));
    }

    private static String composant(RouteSandbox route) {
        return route.nomService().toUpperCase(java.util.Locale.ROOT);
    }

    private static DecisionSandbox decision(Portee portee) {
        if ("MANDAT".equals(portee.type())) {
            return DecisionSandbox.de(Motif.MANDAT_ACTIF, portee.institution());
        }
        if ("PRESTATAIRE".equals(portee.type())) {
            return DecisionSandbox.de(Motif.PRESTATAIRE_AUTORISE, "");
        }
        return DecisionSandbox.de(vide(portee.institution()) ? Motif.COMPTE_GIMAC : Motif.INSTITUTION_PROPRE, portee.institution());
    }

    private static Resultat refus(Demande d, Motif motif) {
        return new Resultat(DecisionSandbox.de(motif, d.institutionJeton()), 404, Optional.empty());
    }

    private Mono<Resultat> admettre(Demande d, Portee portee, DecisionSandbox decision) {
        if (ContexteAction.lire(d.contexte()).isEmpty()) {
            return Mono.just(new Resultat(DecisionSandbox.de(Motif.CONTEXTE_INVALIDE, d.institutionJeton()), 404, Optional.empty()));
        }
        Optional<String> jeton = emetteur == null ? Optional.empty() : emetteur.emettre(jetonReduit(d, portee, decision));
        // D-134 : le jeton original (institution_id au registre) n'est jamais relayé à la sandbox ; sans jeton réduit,
        // portant le code participant, la requête est refusée quand l'émetteur est actif. Un mandat, un prestataire ou,
        // D-133, un compte à double rôle exigent toujours le jeton réduit : son contexte d'action n'est jamais un masquage.
        boolean delegue = decision.motif() == Motif.MANDAT_ACTIF || decision.motif() == Motif.PRESTATAIRE_AUTORISE;
        boolean doubleRole = !vide(portee.institution()) && ContexteAction.doubleRole(sandbox(portee.permissions()));
        if (jeton.isEmpty() && (jetonReduitObligatoire || delegue || doubleRole)) {
            return Mono.just(new Resultat(DecisionSandbox.de(Motif.JETON_REDUIT_INDISPONIBLE, d.institutionJeton()), 404, Optional.empty()));
        }
        LimiteurTentatives limiteur = quotas.get(d.route().couche());
        if (limiteur == null) {
            return Mono.just(new Resultat(decision, 200, jeton));
        }
        String titulaire = vide(decision.institutionEffective()) ? "gimac:" + portee.sujet() : decision.institutionEffective();
        String cle = "sbx:" + d.route().couche() + ":" + titulaire + ":" + d.route().service();
        return limiteur
            .consommer(cle)
            .map(admis ->
                Boolean.TRUE.equals(admis)
                    ? new Resultat(decision, 200, jeton)
                    : new Resultat(DecisionSandbox.de(Motif.QUOTA_DEPASSE, decision.institutionEffective()), 429, Optional.empty())
            );
    }

    static JetonReduit jetonReduit(Demande d, Portee portee, DecisionSandbox decision) {
        List<String> permissions = sandbox(portee.permissions());
        // D-134 : codes participants traduits par MS-IAM, jamais l'institution_id du jeton original (identifiant au registre).
        String propre = !vide(portee.institutionPropre()) ? portee.institutionPropre() : "COMPTE".equals(portee.type()) && !vide(portee.institution()) ? portee.institution() : "";
        if (!vide(portee.institution()) && ContexteAction.doubleRole(permissions)) {
            ContexteAction contexte = ContexteAction.lire(d.contexte()).orElseThrow();
            return new JetonReduit(portee.sujet(), decision.institutionEffective(), propre, contexte.reduire(permissions), d.acr(), d.route().nomService(), contexte.name());
        }
        return new JetonReduit(portee.sujet(), decision.institutionEffective(), propre, permissions, d.acr(), d.route().nomService());
    }

    private static List<String> sandbox(List<String> permissions) {
        return permissions.stream().filter(p -> p.startsWith(PREFIXE_PERMISSION)).sorted().toList();
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
