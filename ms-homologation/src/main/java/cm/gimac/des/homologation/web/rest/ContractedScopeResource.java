package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.service.ContractedScopeQueryService;
import cm.gimac.des.homologation.service.ContractedScopeService;
import cm.gimac.des.homologation.service.criteria.ContractedScopeCriteria;
import cm.gimac.des.homologation.service.dto.ContractedScopeDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.homologation.domain.ContractedScope}.
 */
@RestController
@RequestMapping("/api/contracted-scopes")
public class ContractedScopeResource {

    private static final Logger LOG = LoggerFactory.getLogger(ContractedScopeResource.class);

    private final ContractedScopeService contractedScopeService;

    private final ContractedScopeQueryService contractedScopeQueryService;

    public ContractedScopeResource(ContractedScopeService contractedScopeService, ContractedScopeQueryService contractedScopeQueryService) {
        this.contractedScopeService = contractedScopeService;
        this.contractedScopeQueryService = contractedScopeQueryService;
    }

    /**
     * {@code GET  /contracted-scopes} : get all the contractedScopes.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of contractedScopes in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ContractedScopeDTO>> getAllContractedScopes(ContractedScopeCriteria criteria) {
        LOG.debug("REST request to get ContractedScopes by criteria: {}", criteria);

        List<ContractedScopeDTO> entityList = contractedScopeQueryService.findByCriteria(criteria);
        return ResponseEntity.ok().body(entityList);
    }

    /**
     * {@code GET  /contracted-scopes/count} : count all the contractedScopes.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countContractedScopes(ContractedScopeCriteria criteria) {
        LOG.debug("REST request to count ContractedScopes by criteria: {}", criteria);
        return ResponseEntity.ok().body(contractedScopeQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /contracted-scopes/:id} : get the "id" contractedScope.
     *
     * @param id the id of the contractedScopeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the contractedScopeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContractedScopeDTO> getContractedScope(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ContractedScope : {}", id);
        Optional<ContractedScopeDTO> contractedScopeDTO = contractedScopeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(contractedScopeDTO);
    }
}
