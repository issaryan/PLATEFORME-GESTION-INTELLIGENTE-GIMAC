package cm.gimac.des.certification.infrastructure.client;

import cm.gimac.des.certification.application.port.out.ContexteCertificationPort;
import cm.gimac.des.certification.domain.CertificationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel : contexte de certification servi par MS-HOMOLOGATION
 * ({@code GET /api/v1/interne/liaisons/{id}/contexte-certification}, jeton de service portant
 * INTERNAL_HOMOLOGATION_READ). 404 : liaison inconnue ; autre échec : 503, sans valeur par défaut.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.homologation-reader", havingValue = "real")
public class HomologationContexteAdapter implements ContexteCertificationPort {

    record CouvertureRecue(Integer taux, Integer numerateur, Integer denominateur) {}

    record ContexteRecu(
        UUID liaisonId,
        String reference,
        String institutionId,
        String institutionNom,
        String typeProjet,
        Boolean fintech,
        String statut,
        CouvertureRecue couvertureCertification,
        Boolean couvertureConstatee,
        List<String> servicesCouverts,
        String conducteurId,
        String conducteurNom,
        String modeleTypeProjet,
        List<String> attestationsExigees
    ) {
        ContexteCertification vers() {
            CouvertureRecue c = couvertureCertification == null ? new CouvertureRecue(null, 0, 0) : couvertureCertification;
            return new ContexteCertification(
                liaisonId,
                reference,
                institutionId,
                institutionNom,
                typeProjet,
                Boolean.TRUE.equals(fintech),
                statut,
                new Couverture(c.taux(), c.numerateur() == null ? 0 : c.numerateur(), c.denominateur() == null ? 0 : c.denominateur()),
                Boolean.TRUE.equals(couvertureConstatee),
                servicesCouverts,
                conducteurId,
                conducteurNom,
                modeleTypeProjet,
                attestationsExigees
            );
        }
    }

    private final RestClient client;

    public HomologationContexteAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.homologation.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public Optional<ContexteCertification> contexte(UUID liaisonId) {
        try {
            ContexteRecu recu = client.get().uri("/api/v1/interne/liaisons/{id}/contexte-certification", liaisonId).retrieve().body(ContexteRecu.class);
            return Optional.ofNullable(recu).map(ContexteRecu::vers);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            throw CertificationException.indisponible("CER_HOMOLOGATION_UNAVAILABLE", "Service d'homologation indisponible");
        }
    }
}
