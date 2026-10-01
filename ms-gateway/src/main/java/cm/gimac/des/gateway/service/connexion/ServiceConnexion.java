package cm.gimac.des.gateway.service.connexion;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.ClientConnexion;
import cm.gimac.des.gateway.domain.connexion.DemandeConnexion;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.domain.connexion.EtapesPremierAcces;
import cm.gimac.des.gateway.domain.connexion.Identite;
import cm.gimac.des.gateway.domain.connexion.Jetons;
import cm.gimac.des.gateway.domain.connexion.RegleCouche;
import cm.gimac.des.gateway.domain.connexion.ResultatConnexion;
import cm.gimac.des.gateway.domain.connexion.port.AnnuaireComptes;
import cm.gimac.des.gateway.domain.connexion.port.FournisseurIdentite;
import cm.gimac.des.gateway.domain.connexion.port.JournalConnexion;
import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import cm.gimac.des.gateway.domain.live.Couche;
import cm.gimac.des.gateway.security.session.OuvertureSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Déroulé de POST /api/connexion (contrat D-65 §2) :
 * <ol>
 * <li>limitation : 5 tentatives par minute par adresse IP et par identifiant (429) ;</li>
 * <li>octroi direct sur gimac-connexion (mot de passe seul) ; échec 401, compte désactivé 403 ;</li>
 * <li>couche : institution_id n'ouvre que le portail, son absence le département et la console ;</li>
 * <li>état du compte sur MS-IAM ; inactif 403 ;</li>
 * <li>actions requises ou second facteur absent : session restreinte (acr 1), PREMIER_ACCES ;</li>
 * <li>sans code : CODE_REQUIS, aucune session ; avec code : octroi direct sur web_app (acr 2) ;</li>
 * <li>session ouverte (identifiant de session régénéré) ; journal d'audit à chaque issue.</li>
 * </ol>
 * Les jetons de gimac-connexion qui ne servent pas à une session restreinte sont révoqués aussitôt.
 */
public class ServiceConnexion {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceConnexion.class);

    private final FournisseurIdentite fournisseur;
    private final AnnuaireComptes annuaire;
    private final LimiteurTentatives limiteur;
    private final JournalConnexion journal;
    private final OuvertureSession ouverture;

    public ServiceConnexion(
        FournisseurIdentite fournisseur,
        AnnuaireComptes annuaire,
        LimiteurTentatives limiteur,
        JournalConnexion journal,
        OuvertureSession ouverture
    ) {
        this.fournisseur = fournisseur;
        this.annuaire = annuaire;
        this.limiteur = limiteur;
        this.journal = journal;
        this.ouverture = ouverture;
    }

    public Mono<IssueConnexion> connecter(DemandeConnexion demande, ServerWebExchange exchange) {
        return limiter("cnx", demande.adresseIp(), demande.identifiant())
            .flatMap(admise ->
                admise ? authentifier(demande, exchange) : Mono.<IssueConnexion>error(new EchecConnexion(CauseEchec.TROP_DE_TENTATIVES))
            )
            .doOnNext(issue -> {
                if (!(issue.resultat() instanceof ResultatConnexion.CodeRequis)) {
                    journal.reussie(demande, sujet(issue), etat(issue.resultat()));
                }
            })
            .onErrorMap(e -> !(e instanceof EchecConnexion), e -> new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, e.getMessage()))
            .doOnError(EchecConnexion.class, e -> journal.echouee(demande, e.causeEchec(), e.getMessage()));
    }

    private Mono<IssueConnexion> authentifier(DemandeConnexion d, ServerWebExchange exchange) {
        return fournisseur
            .octroyer(ClientConnexion.MOT_DE_PASSE, d.identifiant(), d.motDePasse(), null)
            .flatMap(jetons ->
                fournisseur
                    .valider(ClientConnexion.MOT_DE_PASSE, jetons)
                    .flatMap(identite -> poursuivre(d, jetons, identite, exchange))
                    // Tout échec après l'octroi : le jeton de gimac-connexion ne doit pas survivre.
                    .onErrorResume(e -> revoquer(jetons).then(Mono.error(e)))
            );
    }

    private Mono<IssueConnexion> poursuivre(DemandeConnexion d, Jetons jetons, Identite identite, ServerWebExchange exchange) {
        if (!RegleCouche.autorise(d.couche(), identite.texte("institution_id"))) {
            return Mono.error(new EchecConnexion(CauseEchec.COUCHE_REFUSEE, "couche " + d.couche() + " refusée"));
        }
        return annuaire
            .etat(identite.sujet())
            .flatMap(etat -> {
                if (!etat.actif()) {
                    return Mono.error(new EchecConnexion(CauseEchec.COMPTE_INACTIF, "compte inactif selon MS-IAM"));
                }
                List<String> etapes = EtapesPremierAcces.de(etat);
                if (!etapes.isEmpty()) {
                    return ouverture
                        .ouvrir(exchange, ClientConnexion.MOT_DE_PASSE, jetons, identite, true)
                        .map(auth -> new IssueConnexion(new ResultatConnexion.PremierAcces(etapes), auth));
                }
                if (!d.avecCode()) {
                    return revoquer(jetons).thenReturn(new IssueConnexion(new ResultatConnexion.CodeRequis(), null));
                }
                return fournisseur
                    .octroyer(ClientConnexion.DEUX_FACTEURS, d.identifiant(), d.motDePasse(), d.code())
                    .onErrorMap(EchecConnexion.class, e -> new EchecConnexion(e.causeEchec(), "second facteur : " + e.getMessage()))
                    .flatMap(complets ->
                        fournisseur
                            .valider(ClientConnexion.DEUX_FACTEURS, complets)
                            .flatMap(id2 -> ouverture.ouvrir(exchange, ClientConnexion.DEUX_FACTEURS, complets, id2, false))
                            .onErrorResume(e -> revoquer(complets).then(Mono.error(e)))
                    )
                    .flatMap(auth -> revoquer(jetons).thenReturn(new IssueConnexion(new ResultatConnexion.Connecte(), auth)));
            });
    }

    /**
     * Mot de passe oublié : toujours accepté (aucune information sur l'existence d'un compte) ;
     * au-delà de la limite, ou si MS-IAM ne répond pas, la demande est abandonnée sans le dire.
     */
    public Mono<Void> motDePasseOublie(String identifiant, String couche, String adresseIp) {
        if (identifiant == null || identifiant.isBlank()) {
            return Mono.empty();
        }
        String nomCouche = Couche.depuis(couche).map(c -> c.name().toLowerCase(Locale.ROOT)).orElse(null);
        return limiter("oubli", adresseIp, identifiant)
            .flatMap(admise -> admise ? annuaire.motDePasseOublie(identifiant.trim(), nomCouche) : Mono.empty())
            .onErrorResume(e -> {
                LOG.warn("Mot de passe oublié : demande non transmise à MS-IAM ({})", e.getMessage());
                return Mono.empty();
            });
    }

    public Mono<AnnuaireComptes.ReponseRelayee> nouveauMotDePasse(String jeton, String motDePasse) {
        return annuaire.nouveauMotDePasse(jeton, motDePasse);
    }

    /** Les deux limites (adresse IP puis identifiant) doivent admettre la tentative. */
    private Mono<Boolean> limiter(String espace, String adresseIp, String identifiant) {
        return limiteur
            .consommer(espace + ":ip:" + adresseIp)
            .flatMap(ip -> ip ? limiteur.consommer(espace + ":id:" + empreinte(identifiant)) : Mono.just(false));
    }

    /** L'identifiant n'apparaît pas en clair dans les clés ni dans les métriques de la limitation. */
    static String empreinte(String identifiant) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(identifiant.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Mono<Void> revoquer(Jetons jetons) {
        return fournisseur.revoquer(ClientConnexion.MOT_DE_PASSE.enregistrement(), jetons.jetonRafraichissement());
    }

    static String etat(ResultatConnexion r) {
        return switch (r) {
            case ResultatConnexion.CodeRequis c -> "CODE_REQUIS";
            case ResultatConnexion.PremierAcces p -> "PREMIER_ACCES";
            case ResultatConnexion.Connecte c -> "CONNECTE";
        };
    }

    private static String sujet(IssueConnexion issue) {
        return issue.authentification() == null ? null : cm.gimac.des.gateway.security.RevendicationsSession.claim(issue.authentification(), "sub");
    }
}
