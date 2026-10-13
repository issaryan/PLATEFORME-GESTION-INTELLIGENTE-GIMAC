package cm.gimac.des.homologation.infrastructure.client;

import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Adaptateur réel : GET /api/v1/interne/institutions/{id} de MS-PARTICIPANT, jeton de service. */
@Component
@ConditionalOnProperty(name = "gimac.adapters.participant-reader", havingValue = "real")
public class ParticipantRestAdapter implements ParticipantDirectoryPort {

    private final RestClient client;

    public ParticipantRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.participant.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public InstitutionView getInstitution(UUID institutionId) {
        try {
            return client.get().uri("/api/v1/interne/institutions/{id}", institutionId).retrieve().body(InstitutionView.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw HomologationException.conflict("HOM_LIAISON_INSTITUTION_UNKNOWN", "Institution inconnue du registre", Map.of("institutionId", institutionId.toString()));
        } catch (RestClientException | IllegalStateException e) {
            throw HomologationException.unavailable("HOM_PARTICIPANT_UNAVAILABLE", "Registre des institutions indisponible ; aucun statut n'est supposé");
        }
    }

    @Override
    public java.util.Optional<InstitutionView> parCodeMembre(String codeMembre) {
        try {
            return java.util.Optional.ofNullable(
                client.get().uri(u -> u.path("/api/v1/interne/institutions").queryParam("codeParticipant", codeMembre).build()).retrieve().body(InstitutionView.class)
            );
        } catch (HttpClientErrorException.NotFound e) {
            return java.util.Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            throw HomologationException.unavailable("HOM_PARTICIPANT_UNAVAILABLE", "Registre des institutions indisponible ; aucun statut n'est supposé");
        }
    }
}
