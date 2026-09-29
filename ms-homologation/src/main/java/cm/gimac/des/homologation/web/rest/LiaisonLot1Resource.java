package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.service.LiaisonService;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
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
 * Liste paginée des liaisons (point de lecture de la Phase 0, enrichi par HOM-04 et HOM-20).
 * Exposé par la passerelle sous /bff/des/homologation/liaisons (PI-GIMAC-01 §0.6, §0.9).
 */
@RestController
@RequestMapping("/api/v1/liaisons")
public class LiaisonLot1Resource {

    private final LiaisonService service;

    public LiaisonLot1Resource(LiaisonService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public ResponseEntity<List<LiaisonDTO>> lister(Pageable pageable) {
        Page<LiaisonDTO> page = service.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
