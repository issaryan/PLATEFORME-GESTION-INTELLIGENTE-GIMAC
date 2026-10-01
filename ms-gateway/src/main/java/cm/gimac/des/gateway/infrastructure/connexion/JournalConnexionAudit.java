package cm.gimac.des.gateway.infrastructure.connexion;

import cm.gimac.des.gateway.audit.GatewayAuditEventPublisher;
import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.DemandeConnexion;
import cm.gimac.des.gateway.domain.connexion.MasqueIdentifiant;
import cm.gimac.des.gateway.domain.connexion.port.JournalConnexion;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Journal d'audit de la connexion : événements gateway.login-succeeded.v1 et gateway.login-failed.v1
 * publiés par le mécanisme d'audit de la passerelle (GatewayAuditEventPublisher, sujet Kafka
 * gateway.security.events, scellé par MS-AUDIT) quand il est actif, et toujours écrits au journal
 * applicatif. Identifiant masqué ; jamais le mot de passe ni le code.
 */
public class JournalConnexionAudit implements JournalConnexion {

    private static final Logger LOG = LoggerFactory.getLogger("gimac.audit.connexion");

    private final GatewayAuditEventPublisher publication;

    /** @param publication publication Kafka, nulle si le journal des décisions n'est pas actif */
    public JournalConnexionAudit(GatewayAuditEventPublisher publication) {
        this.publication = publication;
    }

    @Override
    public void reussie(DemandeConnexion demande, String sujet, String etat) {
        Map<String, String> details = details(demande);
        details.put("etat", etat);
        LOG.info("{} identifiant={} ip={} couche={} etat={}", REUSSIE, details.get("identifiant"), demande.adresseIp(), details.get("couche"), etat);
        if (publication != null) {
            publier(REUSSIE, sujet, demande.adresseIp(), 200, details);
        }
    }

    @Override
    public void echouee(DemandeConnexion demande, CauseEchec cause, String detailTechnique) {
        Map<String, String> details = details(demande);
        details.put("code", cause.code());
        details.put("cause", detailTechnique == null ? cause.name() : detailTechnique);
        LOG.warn(
            "{} identifiant={} ip={} couche={} code={} cause={}",
            ECHOUEE,
            details.get("identifiant"),
            demande.adresseIp(),
            details.get("couche"),
            cause.code(),
            details.get("cause")
        );
        if (publication != null) {
            publier(ECHOUEE, null, demande.adresseIp(), cause.statut(), details);
        }
    }

    /**
     * Publication hors du fil d'événements : le producteur Kafka peut attendre les métadonnées du
     * courtier, ce qui ne doit ni bloquer Netty ni retarder la réponse de connexion.
     */
    private void publier(String type, String sujet, String ip, int statut, Map<String, String> details) {
        Mono.fromRunnable(() -> publication.publishSecurityEvent(type, sujet, ip, statut, details))
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(e -> Mono.empty())
            .subscribe();
    }

    private static Map<String, String> details(DemandeConnexion demande) {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("identifiant", MasqueIdentifiant.masquer(demande.identifiant()));
        d.put("couche", demande.couche() == null ? "" : demande.couche().name().toLowerCase(java.util.Locale.ROOT));
        return d;
    }
}
