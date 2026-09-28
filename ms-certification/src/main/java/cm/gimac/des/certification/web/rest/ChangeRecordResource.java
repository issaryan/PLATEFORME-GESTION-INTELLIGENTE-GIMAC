package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.service.ChangeRecordQueryService;
import cm.gimac.des.certification.service.ChangeRecordService;
import cm.gimac.des.certification.service.criteria.ChangeRecordCriteria;
import cm.gimac.des.certification.service.dto.ChangeRecordDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.certification.domain.ChangeRecord}.
 */
@RestController
@RequestMapping("/api/change-records")
public class ChangeRecordResource {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeRecordResource.class);

    private final ChangeRecordService changeRecordService;

    private final ChangeRecordQueryService changeRecordQueryService;

    public ChangeRecordResource(ChangeRecordService changeRecordService, ChangeRecordQueryService changeRecordQueryService) {
        this.changeRecordService = changeRecordService;
        this.changeRecordQueryService = changeRecordQueryService;
    }

    /**
     * {@code GET  /change-records} : get all the changeRecords.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of changeRecords in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ChangeRecordDTO>> getAllChangeRecords(ChangeRecordCriteria criteria) {
        LOG.debug("REST request to get ChangeRecords by criteria: {}", criteria);

        List<ChangeRecordDTO> entityList = changeRecordQueryService.findByCriteria(criteria);
        return ResponseEntity.ok().body(entityList);
    }

    /**
     * {@code GET  /change-records/count} : count all the changeRecords.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countChangeRecords(ChangeRecordCriteria criteria) {
        LOG.debug("REST request to count ChangeRecords by criteria: {}", criteria);
        return ResponseEntity.ok().body(changeRecordQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /change-records/:id} : get the "id" changeRecord.
     *
     * @param id the id of the changeRecordDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the changeRecordDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChangeRecordDTO> getChangeRecord(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ChangeRecord : {}", id);
        Optional<ChangeRecordDTO> changeRecordDTO = changeRecordService.findOne(id);
        return ResponseUtil.wrapOrNotFound(changeRecordDTO);
    }
}
