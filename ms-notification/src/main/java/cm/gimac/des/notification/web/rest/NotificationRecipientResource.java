package cm.gimac.des.notification.web.rest;

import cm.gimac.des.notification.service.NotificationRecipientQueryService;
import cm.gimac.des.notification.service.NotificationRecipientService;
import cm.gimac.des.notification.service.criteria.NotificationRecipientCriteria;
import cm.gimac.des.notification.service.dto.NotificationRecipientDTO;
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
 * REST controller for managing {@link cm.gimac.des.notification.domain.NotificationRecipient}.
 */
@RestController
@RequestMapping("/api/notification-recipients")
public class NotificationRecipientResource {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationRecipientResource.class);

    private final NotificationRecipientService notificationRecipientService;

    private final NotificationRecipientQueryService notificationRecipientQueryService;

    public NotificationRecipientResource(
        NotificationRecipientService notificationRecipientService,
        NotificationRecipientQueryService notificationRecipientQueryService
    ) {
        this.notificationRecipientService = notificationRecipientService;
        this.notificationRecipientQueryService = notificationRecipientQueryService;
    }

    /**
     * {@code GET  /notification-recipients} : get all the notificationRecipients.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of notificationRecipients in body.
     */
    @GetMapping("")
    public ResponseEntity<List<NotificationRecipientDTO>> getAllNotificationRecipients(
        NotificationRecipientCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get NotificationRecipients by criteria: {}", criteria);

        Page<NotificationRecipientDTO> page = notificationRecipientQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /notification-recipients/count} : count all the notificationRecipients.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countNotificationRecipients(NotificationRecipientCriteria criteria) {
        LOG.debug("REST request to count NotificationRecipients by criteria: {}", criteria);
        return ResponseEntity.ok().body(notificationRecipientQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /notification-recipients/:id} : get the "id" notificationRecipient.
     *
     * @param id the id of the notificationRecipientDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the notificationRecipientDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationRecipientDTO> getNotificationRecipient(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get NotificationRecipient : {}", id);
        Optional<NotificationRecipientDTO> notificationRecipientDTO = notificationRecipientService.findOne(id);
        return ResponseUtil.wrapOrNotFound(notificationRecipientDTO);
    }
}
