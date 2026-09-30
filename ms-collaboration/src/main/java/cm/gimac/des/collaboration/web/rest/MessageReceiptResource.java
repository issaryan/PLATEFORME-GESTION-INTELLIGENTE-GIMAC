package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.service.MessageReceiptService;
import cm.gimac.des.collaboration.service.dto.MessageReceiptDTO;
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
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.MessageReceipt}.
 */
@RestController
@RequestMapping("/api/message-receipts")
public class MessageReceiptResource {

    private static final Logger LOG = LoggerFactory.getLogger(MessageReceiptResource.class);

    private final MessageReceiptService messageReceiptService;

    public MessageReceiptResource(MessageReceiptService messageReceiptService) {
        this.messageReceiptService = messageReceiptService;
    }

    /**
     * {@code GET  /message-receipts} : get all the Message Receipts.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Message Receipts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MessageReceiptDTO>> getAllMessageReceipts(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of MessageReceipts");
        Page<MessageReceiptDTO> page;
        if (eagerload) {
            page = messageReceiptService.findAllWithEagerRelationships(pageable);
        } else {
            page = messageReceiptService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /message-receipts/:id} : get the "id" messageReceipt.
     *
     * @param id the id of the messageReceiptDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the messageReceiptDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageReceiptDTO> getMessageReceipt(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get MessageReceipt : {}", id);
        Optional<MessageReceiptDTO> messageReceiptDTO = messageReceiptService.findOne(id);
        return ResponseUtil.wrapOrNotFound(messageReceiptDTO);
    }
}
