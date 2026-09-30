package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.service.ConferenceParticipantService;
import cm.gimac.des.collaboration.service.dto.ConferenceParticipantDTO;
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
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.ConferenceParticipant}.
 */
@RestController
@RequestMapping("/api/conference-participants")
public class ConferenceParticipantResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConferenceParticipantResource.class);

    private final ConferenceParticipantService conferenceParticipantService;

    public ConferenceParticipantResource(ConferenceParticipantService conferenceParticipantService) {
        this.conferenceParticipantService = conferenceParticipantService;
    }

    /**
     * {@code GET  /conference-participants} : get all the Conference Participants.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Conference Participants in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ConferenceParticipantDTO>> getAllConferenceParticipants(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of ConferenceParticipants");
        Page<ConferenceParticipantDTO> page;
        if (eagerload) {
            page = conferenceParticipantService.findAllWithEagerRelationships(pageable);
        } else {
            page = conferenceParticipantService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /conference-participants/:id} : get the "id" conferenceParticipant.
     *
     * @param id the id of the conferenceParticipantDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the conferenceParticipantDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConferenceParticipantDTO> getConferenceParticipant(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ConferenceParticipant : {}", id);
        Optional<ConferenceParticipantDTO> conferenceParticipantDTO = conferenceParticipantService.findOne(id);
        return ResponseUtil.wrapOrNotFound(conferenceParticipantDTO);
    }
}
