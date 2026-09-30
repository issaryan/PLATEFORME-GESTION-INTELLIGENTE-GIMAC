package cm.gimac.des.certification.web.rest.lot1;

import cm.gimac.des.certification.application.port.out.DocumentPort.Fichier;
import cm.gimac.des.certification.application.usecase.ApprobationSecuriteService;
import cm.gimac.des.certification.application.usecase.DecisionVues.ApprobationVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.ProcesVerbalVue;
import cm.gimac.des.certification.application.usecase.ProcesVerbalService;
import cm.gimac.des.certification.domain.CertificationException;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Portail du participant, verrous de la décision (D-56) : lecture et signature du procès-verbal, lecture de
 * l'approbation de la sécurité, dépôt des attestations. Cloisonné à l'institution du jeton (404 pour une
 * autre institution), projection sans nom interne. Exposé par la passerelle sous
 * {@code /bff/portail/certification/liaisons/...} (portailParcoursHttp de l'interface).
 */
@RestController
@RequestMapping("/api/v1/portail/liaisons/{liaisonId}")
public class PortailDecisionResource {

    private final ProcesVerbalService procesVerbaux;
    private final ApprobationSecuriteService approbations;

    public PortailDecisionResource(ProcesVerbalService procesVerbaux, ApprobationSecuriteService approbations) {
        this.procesVerbaux = procesVerbaux;
        this.approbations = approbations;
    }

    @GetMapping("/proces-verbal")
    @PreAuthorize("hasAuthority('CER_PORTAL_READ')")
    public ProcesVerbalVue procesVerbal(@PathVariable UUID liaisonId, Authentication auth) {
        return procesVerbaux.consulterPortail(liaisonId, ActeurCourant.de(auth));
    }

    @PostMapping("/proces-verbal/signature")
    @PreAuthorize("hasAuthority('CER_PORTAL_SIGN')")
    public ProcesVerbalVue signer(
        @PathVariable UUID liaisonId,
        @RequestHeader(name = DecisionResource.REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        return procesVerbaux.signerParticipant(liaisonId, ActeurCourant.de(auth), assertion);
    }

    @GetMapping("/approbation-securite")
    @PreAuthorize("hasAuthority('CER_PORTAL_READ')")
    public ApprobationVue approbation(@PathVariable UUID liaisonId, Authentication auth) {
        return approbations.consulterPortail(liaisonId, ActeurCourant.de(auth));
    }

    @PostMapping(path = "/attestations/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('HOM_PORTAL_SUBMIT')")
    public ApprobationVue deposerAttestation(
        @PathVariable UUID liaisonId,
        @PathVariable String type,
        @RequestPart(name = "fichier", required = false) MultipartFile fichier,
        Authentication auth
    ) {
        return approbations.deposerAttestation(liaisonId, type, lire(fichier), ActeurCourant.de(auth));
    }

    private static Fichier lire(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            return null;
        }
        try {
            return new Fichier(fichier.getOriginalFilename(), fichier.getContentType(), fichier.getBytes());
        } catch (IOException e) {
            throw CertificationException.irrecevable("CER_ATTESTATION_FORMAT", "Pièce illisible.");
        }
    }
}
