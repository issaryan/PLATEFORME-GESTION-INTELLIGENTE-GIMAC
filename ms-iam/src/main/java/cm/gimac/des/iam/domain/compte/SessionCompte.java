package cm.gimac.des.iam.domain.compte;

import java.time.Instant;
import java.util.List;

/** Session ouverte d'un compte auprès du fournisseur d'identité. */
public record SessionCompte(String id, String adresseIp, Instant debut, Instant dernierAcces, List<String> applications) {
    public SessionCompte {
        applications = applications == null ? List.of() : List.copyOf(applications);
    }
}
