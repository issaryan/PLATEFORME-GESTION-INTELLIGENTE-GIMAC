package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.service.RoomArchiveQueryService;
import cm.gimac.des.collaboration.service.RoomArchiveService;
import cm.gimac.des.collaboration.service.criteria.RoomArchiveCriteria;
import cm.gimac.des.collaboration.service.dto.RoomArchiveDTO;
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
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.RoomArchive}.
 */
@RestController
@RequestMapping("/api/room-archives")
public class RoomArchiveResource {

    private static final Logger LOG = LoggerFactory.getLogger(RoomArchiveResource.class);

    private final RoomArchiveService roomArchiveService;

    private final RoomArchiveQueryService roomArchiveQueryService;

    public RoomArchiveResource(RoomArchiveService roomArchiveService, RoomArchiveQueryService roomArchiveQueryService) {
        this.roomArchiveService = roomArchiveService;
        this.roomArchiveQueryService = roomArchiveQueryService;
    }

    /**
     * {@code GET  /room-archives} : get all the roomArchives.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of roomArchives in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RoomArchiveDTO>> getAllRoomArchives(
        RoomArchiveCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get RoomArchives by criteria: {}", criteria);

        Page<RoomArchiveDTO> page = roomArchiveQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /room-archives/count} : count all the roomArchives.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countRoomArchives(RoomArchiveCriteria criteria) {
        LOG.debug("REST request to count RoomArchives by criteria: {}", criteria);
        return ResponseEntity.ok().body(roomArchiveQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /room-archives/:id} : get the "id" roomArchive.
     *
     * @param id the id of the roomArchiveDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the roomArchiveDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoomArchiveDTO> getRoomArchive(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RoomArchive : {}", id);
        Optional<RoomArchiveDTO> roomArchiveDTO = roomArchiveService.findOne(id);
        return ResponseUtil.wrapOrNotFound(roomArchiveDTO);
    }
}
