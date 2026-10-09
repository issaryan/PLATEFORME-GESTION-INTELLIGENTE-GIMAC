package cm.gimac.des.iam.infrastructure.pdf;

import cm.gimac.des.iam.application.port.out.RenduPdf;
import cm.gimac.des.iam.domain.IamException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution du port {@link RenduPdf} (PI §0.8, développement sans MS-DOCUMENT) : aucun moteur
 * configuré, la production d'un PDF est refusée par 503 ORG_PDF_INDISPONIBLE, jamais par un faux document.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.rendu-pdf", havingValue = "stub", matchIfMissing = true)
public class RenduPdfIndisponible implements RenduPdf {

    @Override
    public byte[] rendre(String html, String piedDePage, Mise mise) {
        throw IamException.indisponible("ORG_PDF_INDISPONIBLE", "Le moteur de mise en page PDF n'est pas configuré (gimac.adapters.rendu-pdf=stub)");
    }
}
