package cm.gimac.des.gateway.infrastructure.sandbox;

import cm.gimac.des.gateway.audit.GatewayAuditEventPublisher;
import cm.gimac.des.gateway.domain.sandbox.port.JournalDecisionsSandbox;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Journal des décisions de la passerelle sur les routes de la sandbox : événement gateway.sandbox-decision.v1 publié
 * par le mécanisme d'audit de la passerelle (sujet gateway.security.events, scellé par MS-AUDIT) quand il est actif,
 * et toujours écrit au journal applicatif. Ni jeton ni secret : compte, route, institutions, décision et statut.
 */
public class JournalDecisionsSandboxAudit implements JournalDecisionsSandbox {

    private static final Logger LOG = LoggerFactory.getLogger("gimac.audit.sandbox");

    private final GatewayAuditEventPublisher publication;

    /** @param publication publication Kafka, nulle si le journal des décisions de la passerelle n'est pas actif */
    public JournalDecisionsSandboxAudit(GatewayAuditEventPublisher publication) {
        this.publication = publication;
    }

    @Override
    public void consigner(Entree e) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("source", "sandbox");
        details.put("couche", e.route().couche());
        details.put("service", e.route().nomService());
        details.put("methode", e.methode());
        details.put("chemin", e.chemin());
        details.put("institutionJeton", texte(e.institutionJeton()));
        details.put("institutionDemandee", texte(e.demandee()));
        details.put("institutionEffective", e.decision().institutionEffective());
        details.put("decision", e.decision().admise() ? "ADMISE" : "REFUSEE");
        details.put("motif", e.decision().motif().name());
        if (e.decision().admise()) {
            LOG.info("{} compte={} {} {} institution={} motif={}", EVENEMENT, e.compte(), e.methode(), e.chemin(), details.get("institutionEffective"), details.get("motif"));
        } else {
            LOG.warn("{} compte={} {} {} demandee={} motif={} statut={}", EVENEMENT, e.compte(), e.methode(), e.chemin(), details.get("institutionDemandee"), details.get("motif"), e.statut());
        }
        if (publication != null) {
            publication.publishSecurityEvent(EVENEMENT, e.compte(), e.ip(), e.statut(), details);
        }
    }

    private static String texte(String s) {
        return s == null ? "" : s;
    }
}
