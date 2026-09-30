package cm.gimac.des.certification.web.rest.lot1;

import cm.gimac.des.certification.application.usecase.ApprobationSecuriteService;
import cm.gimac.des.certification.application.usecase.DecisionVues.ApprobationVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.ProcesVerbalVue;
import cm.gimac.des.certification.application.usecase.ProcesVerbalService;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verrous de la décision de mise en production, côté DES (D-56 ; docs/lot1/parcours-backend.md §1) :
 * procès-verbal de certification (verrou 1) et approbation de la sécurité (verrou 2). Exposées par la
 * passerelle sous {@code /bff/des/certification/liaisons/...} ; chemins et corps de
 * frontend/packages/lot1/src/services/http/parcours.ts (decisionHttp). Chaque route exige sa permission ;
 * les signatures exigent en plus l'assertion FIDO2 (en-tête X-Reauth-Assertion).
 */
@RestController
@RequestMapping("/api/v1/liaisons/{liaisonId}")
public class DecisionResource {

    static final String REAUTH = "X-Reauth-Assertion";

    public record DossierChangement(
        @NotNull UUID documentId,
        @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{64}$") String empreinte,
        @NotBlank @Size(max = 255) String nom
    ) {}

    public record FicheChangement(@Size(max = 40) String reference) {}

    public record Verification(boolean conforme, @Size(max = 1000) String motif) {}

    public record Avis(boolean favorable, @Size(max = 1000) String motif) {}

    private final ProcesVerbalService procesVerbaux;
    private final ApprobationSecuriteService approbations;

    public DecisionResource(ProcesVerbalService procesVerbaux, ApprobationSecuriteService approbations) {
        this.procesVerbaux = procesVerbaux;
        this.approbations = approbations;
    }

    // ------------------------------------------------------------------ Verrou 1

    @GetMapping("/proces-verbal")
    @PreAuthorize("hasAuthority('CER_DOSSIER_READ')")
    public ProcesVerbalVue procesVerbal(@PathVariable UUID liaisonId, Authentication auth) {
        return procesVerbaux.consulter(liaisonId, ActeurCourant.de(auth));
    }

    @PostMapping("/proces-verbal")
    @PreAuthorize("hasAuthority('CER_PV_ESTABLISH')")
    public ProcesVerbalVue etablir(@PathVariable UUID liaisonId, Authentication auth) {
        return procesVerbaux.etablir(liaisonId, ActeurCourant.de(auth));
    }

    @PostMapping("/proces-verbal/signature-gimac")
    @PreAuthorize("hasAuthority('CER_QUITUS_TECHNIQUE_SIGN')")
    public ProcesVerbalVue signerGimac(
        @PathVariable UUID liaisonId,
        @RequestHeader(name = REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        return procesVerbaux.signerGimac(liaisonId, ActeurCourant.de(auth), assertion);
    }

    // ------------------------------------------------------------------ Verrou 2

    @GetMapping("/approbation-securite")
    @PreAuthorize("hasAuthority('CER_DOSSIER_READ')")
    public ApprobationVue approbation(@PathVariable UUID liaisonId, Authentication auth) {
        return approbations.consulter(liaisonId, ActeurCourant.de(auth));
    }

    @PostMapping("/approbation-securite/dossier")
    @PreAuthorize("hasAuthority('CER_CHANGE_RECORD_LINK')")
    public ApprobationVue constituerDossier(@PathVariable UUID liaisonId, @Valid @RequestBody DossierChangement d, Authentication auth) {
        return approbations.constituerDossier(liaisonId, d.documentId(), d.empreinte(), d.nom(), ActeurCourant.de(auth));
    }

    @PutMapping("/approbation-securite/fiche")
    @PreAuthorize("hasAuthority('CER_CHANGE_RECORD_LINK')")
    public ApprobationVue referencerFiche(@PathVariable UUID liaisonId, @Valid @RequestBody FicheChangement f, Authentication auth) {
        return approbations.referencerFiche(liaisonId, f.reference(), ActeurCourant.de(auth));
    }

    @PostMapping("/approbation-securite/fiche/lecture")
    @PreAuthorize("hasAnyAuthority('CER_CHANGE_RECORD_LINK', 'CER_SECURITY_VERIFY')")
    public ApprobationVue relireFiche(@PathVariable UUID liaisonId, Authentication auth) {
        return approbations.relireFiche(liaisonId, ActeurCourant.de(auth));
    }

    @PostMapping("/approbation-securite/attestations/{type}/verification")
    @PreAuthorize("hasAuthority('CER_SECURITY_VERIFY')")
    public ApprobationVue verifierAttestation(
        @PathVariable UUID liaisonId,
        @PathVariable String type,
        @Valid @RequestBody Verification v,
        Authentication auth
    ) {
        return approbations.verifierAttestation(liaisonId, type, v.conforme(), v.motif(), ActeurCourant.de(auth));
    }

    @PostMapping("/approbation-securite/archivage")
    @PreAuthorize("hasAuthority('CER_SECURITY_VERIFY')")
    public ApprobationVue archiver(@PathVariable UUID liaisonId, Authentication auth) {
        return approbations.archiver(liaisonId, ActeurCourant.de(auth));
    }

    @PostMapping("/approbation-securite/visas/CONFORMITE")
    @PreAuthorize("hasAuthority('CER_QUITUS_CONFORMITE_SIGN')")
    public ApprobationVue visaConformite(
        @PathVariable UUID liaisonId,
        @Valid @RequestBody Avis avis,
        @RequestHeader(name = REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        return approbations.signerVisa(liaisonId, NatureVisa.CONFORMITE, avis.favorable(), avis.motif(), ActeurCourant.de(auth), assertion);
    }

    @PostMapping("/approbation-securite/visas/DIRECTION_GENERALE")
    @PreAuthorize("hasAuthority('CER_QUITUS_DG_SIGN')")
    public ApprobationVue visaDirectionGenerale(
        @PathVariable UUID liaisonId,
        @Valid @RequestBody Avis avis,
        @RequestHeader(name = REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        return approbations.signerVisa(liaisonId, NatureVisa.DIRECTION_GENERALE, avis.favorable(), avis.motif(), ActeurCourant.de(auth), assertion);
    }
}
