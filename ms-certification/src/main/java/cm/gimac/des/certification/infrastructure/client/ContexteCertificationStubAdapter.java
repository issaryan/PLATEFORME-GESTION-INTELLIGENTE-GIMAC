package cm.gimac.des.certification.infrastructure.client;

import cm.gimac.des.certification.application.port.out.ContexteCertificationPort;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution du contexte de certification (développement, tests) : les contextes sont
 * enregistrés par les tests ou le poste de développement ; une liaison non enregistrée est inconnue (404).
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.homologation-reader", havingValue = "stub", matchIfMissing = true)
public class ContexteCertificationStubAdapter implements ContexteCertificationPort {

    private final Map<UUID, ContexteCertification> contextes = new ConcurrentHashMap<>();

    public void enregistrer(ContexteCertification contexte) {
        contextes.put(contexte.liaisonId(), contexte);
    }

    public void oublier(UUID liaisonId) {
        contextes.remove(liaisonId);
    }

    @Override
    public Optional<ContexteCertification> contexte(UUID liaisonId) {
        return Optional.ofNullable(contextes.get(liaisonId));
    }
}
