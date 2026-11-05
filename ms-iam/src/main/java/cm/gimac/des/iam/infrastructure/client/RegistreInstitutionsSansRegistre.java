package cm.gimac.des.iam.infrastructure.client;

import cm.gimac.des.iam.application.port.out.sandbox.RegistreInstitutions;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut du registre (PI §0.8, développement sans MS-PARTICIPANT) : aucune correspondance, jamais une valeur
 * inventée ; seuls les comptes dont l'attribut institution_id est déjà un code participant sont traduits.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.registre-institutions", havingValue = "stub", matchIfMissing = true)
public class RegistreInstitutionsSansRegistre implements RegistreInstitutions {

    @Override
    public Optional<String> codeParticipant(UUID institutionId) {
        return Optional.empty();
    }

    @Override
    public Optional<UUID> identifiant(String codeParticipant) {
        return Optional.empty();
    }
}
