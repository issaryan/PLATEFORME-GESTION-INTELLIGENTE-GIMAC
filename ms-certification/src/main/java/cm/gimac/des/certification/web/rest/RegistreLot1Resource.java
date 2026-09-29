package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.service.CertificationRegisterEntryService;
import cm.gimac.des.certification.service.dto.CertificationRegisterEntryDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Registre paginé des certifications (point de lecture de la Phase 0, enrichi par CER-14).
 * Exposé par la passerelle sous /bff/des/certification/registre (PI-GIMAC-01 §0.6, §0.9).
 */
@RestController
@RequestMapping("/api/v1/registre")
public class RegistreLot1Resource {

    private final CertificationRegisterEntryService service;

    public RegistreLot1Resource(CertificationRegisterEntryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CER_REGISTER_READ')")
    public ResponseEntity<List<CertificationRegisterEntryDTO>> lister(Pageable pageable) {
        Page<CertificationRegisterEntryDTO> page = service.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
