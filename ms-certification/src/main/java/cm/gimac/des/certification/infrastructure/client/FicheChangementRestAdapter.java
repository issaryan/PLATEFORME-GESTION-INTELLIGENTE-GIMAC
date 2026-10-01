package cm.gimac.des.certification.infrastructure.client;

import cm.gimac.des.certification.application.port.out.FicheChangementPort;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.enumeration.EtatFicheChangement;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel : lecture de la fiche de changement dans le dispositif existant de gestion des changements
 * ({@code GET /api/v1/changements/{reference}}, contrat du simulacre infra/docker-compose/simulacres/changements),
 * lue et jamais refaite (D-56, RG-34). Fiche inconnue : 422 CER_CHANGE_RECORD_UNKNOWN ; dispositif injoignable
 * ou réponse illisible : 503 CER_CHANGE_RECORD_UNAVAILABLE. Aucun état n'est jamais supposé.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.change-record-reader", havingValue = "real")
public class FicheChangementRestAdapter implements FicheChangementPort {

    record FicheRecue(String reference, String etat) {}

    private final RestClient client;

    public FicheChangementRestAdapter(RestClient.Builder builder, @Value("${gimac.services.gestion-changements.url}") String url) {
        this.client = builder.baseUrl(url).build();
    }

    @Override
    public EtatFicheChangement lire(String reference) {
        FicheRecue f;
        try {
            f = client.get().uri("/api/v1/changements/{reference}", reference).retrieve().body(FicheRecue.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw CertificationException.irrecevable("CER_CHANGE_RECORD_UNKNOWN", "Fiche de changement inconnue du dispositif de gestion des changements");
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
        if (f == null || f.etat() == null) {
            throw indisponible();
        }
        return etat(f.etat());
    }

    /** États du dispositif (APPROUVE, REFUSE, EN_EXAMEN, OUVERT, au masculin ou au féminin) vers ceux de la plateforme. */
    static EtatFicheChangement etat(String brut) {
        String e = brut.trim().toUpperCase(Locale.ROOT);
        if (e.startsWith("APPROUV")) return EtatFicheChangement.APPROUVEE;
        if (e.startsWith("REFUS") || e.startsWith("REJET")) return EtatFicheChangement.REFUSEE;
        if (e.startsWith("EN_EXAMEN") || e.startsWith("EXAMEN")) return EtatFicheChangement.EN_EXAMEN;
        if (e.startsWith("OUVERT")) return EtatFicheChangement.OUVERTE;
        throw indisponible();
    }

    private static CertificationException indisponible() {
        return CertificationException.indisponible("CER_CHANGE_RECORD_UNAVAILABLE", "Dispositif de gestion des changements indisponible");
    }
}
