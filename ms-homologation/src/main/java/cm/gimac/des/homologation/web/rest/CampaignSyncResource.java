package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.service.CampaignSyncService;
import cm.gimac.des.homologation.service.dto.CampaignSyncDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.homologation.domain.CampaignSync}.
 */
@RestController
@RequestMapping("/api/campaign-syncs")
public class CampaignSyncResource {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignSyncResource.class);

    private final CampaignSyncService campaignSyncService;

    public CampaignSyncResource(CampaignSyncService campaignSyncService) {
        this.campaignSyncService = campaignSyncService;
    }

    /**
     * {@code GET  /campaign-syncs} : get all the campaignSyncs.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of campaignSyncs in body.
     */
    @GetMapping("")
    public List<CampaignSyncDTO> getAllCampaignSyncs(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all CampaignSyncs");
        return campaignSyncService.findAll();
    }

    /**
     * {@code GET  /campaign-syncs/:id} : get the "id" campaignSync.
     *
     * @param id the id of the campaignSyncDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the campaignSyncDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CampaignSyncDTO> getCampaignSync(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CampaignSync : {}", id);
        Optional<CampaignSyncDTO> campaignSyncDTO = campaignSyncService.findOne(id);
        return ResponseUtil.wrapOrNotFound(campaignSyncDTO);
    }
}
