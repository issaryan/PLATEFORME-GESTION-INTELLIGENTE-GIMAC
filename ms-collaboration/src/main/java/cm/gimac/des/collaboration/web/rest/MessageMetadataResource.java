package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.service.MessageMetadataQueryService;
import cm.gimac.des.collaboration.service.MessageMetadataService;
import cm.gimac.des.collaboration.service.criteria.MessageMetadataCriteria;
import cm.gimac.des.collaboration.service.dto.MessageMetadataDTO;
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
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.MessageMetadata}.
 */
@RestController
@RequestMapping("/api/message-metadata")
public class MessageMetadataResource {

    private static final Logger LOG = LoggerFactory.getLogger(MessageMetadataResource.class);

    private final MessageMetadataService messageMetadataService;

    private final MessageMetadataQueryService messageMetadataQueryService;

    public MessageMetadataResource(MessageMetadataService messageMetadataService, MessageMetadataQueryService messageMetadataQueryService) {
        this.messageMetadataService = messageMetadataService;
        this.messageMetadataQueryService = messageMetadataQueryService;
    }

    /**
     * {@code GET  /message-metadata} : get all the Message Metadata.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Message Metadata in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MessageMetadataDTO>> getAllMessageMetadatas(
        MessageMetadataCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get MessageMetadatas by criteria: {}", criteria);

        Page<MessageMetadataDTO> page = messageMetadataQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /message-metadata/count} : count all the messageMetadatas.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countMessageMetadatas(MessageMetadataCriteria criteria) {
        LOG.debug("REST request to count MessageMetadatas by criteria: {}", criteria);
        return ResponseEntity.ok().body(messageMetadataQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /message-metadata/:id} : get the "id" messageMetadata.
     *
     * @param id the id of the messageMetadataDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the messageMetadataDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageMetadataDTO> getMessageMetadata(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get MessageMetadata : {}", id);
        Optional<MessageMetadataDTO> messageMetadataDTO = messageMetadataService.findOne(id);
        return ResponseUtil.wrapOrNotFound(messageMetadataDTO);
    }
}
