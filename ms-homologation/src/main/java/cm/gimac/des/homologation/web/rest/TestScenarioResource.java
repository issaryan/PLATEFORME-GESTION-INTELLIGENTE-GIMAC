package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.service.TestScenarioQueryService;
import cm.gimac.des.homologation.service.TestScenarioService;
import cm.gimac.des.homologation.service.criteria.TestScenarioCriteria;
import cm.gimac.des.homologation.service.dto.TestScenarioDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.homologation.domain.TestScenario}.
 */
@RestController
@RequestMapping("/api/test-scenarios")
public class TestScenarioResource {

    private static final Logger LOG = LoggerFactory.getLogger(TestScenarioResource.class);

    private final TestScenarioService testScenarioService;

    private final TestScenarioQueryService testScenarioQueryService;

    public TestScenarioResource(TestScenarioService testScenarioService, TestScenarioQueryService testScenarioQueryService) {
        this.testScenarioService = testScenarioService;
        this.testScenarioQueryService = testScenarioQueryService;
    }

    /**
     * {@code GET  /test-scenarios} : get all the testScenarios.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of testScenarios in body.
     */
    @GetMapping("")
    public ResponseEntity<List<TestScenarioDTO>> getAllTestScenarios(
        TestScenarioCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get TestScenarios by criteria: {}", criteria);

        Page<TestScenarioDTO> page = testScenarioQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /test-scenarios/count} : count all the testScenarios.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countTestScenarios(TestScenarioCriteria criteria) {
        LOG.debug("REST request to count TestScenarios by criteria: {}", criteria);
        return ResponseEntity.ok().body(testScenarioQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /test-scenarios/:id} : get the "id" testScenario.
     *
     * @param id the id of the testScenarioDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the testScenarioDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TestScenarioDTO> getTestScenario(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get TestScenario : {}", id);
        Optional<TestScenarioDTO> testScenarioDTO = testScenarioService.findOne(id);
        return ResponseUtil.wrapOrNotFound(testScenarioDTO);
    }
}
