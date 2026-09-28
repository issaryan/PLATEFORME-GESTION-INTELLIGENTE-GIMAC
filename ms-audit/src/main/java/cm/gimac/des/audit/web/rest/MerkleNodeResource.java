package cm.gimac.des.audit.web.rest;

import cm.gimac.des.audit.service.MerkleNodeService;
import cm.gimac.des.audit.service.dto.MerkleNodeDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.audit.domain.MerkleNode}.
 */
@RestController
@RequestMapping("/api/merkle-nodes")
public class MerkleNodeResource {

    private static final Logger LOG = LoggerFactory.getLogger(MerkleNodeResource.class);

    private final MerkleNodeService merkleNodeService;

    public MerkleNodeResource(MerkleNodeService merkleNodeService) {
        this.merkleNodeService = merkleNodeService;
    }

    /**
     * {@code GET  /merkle-nodes} : get all the Merkle Nodes.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Merkle Nodes in body.
     */
    @GetMapping("")
    public List<MerkleNodeDTO> getAllMerkleNodes() {
        LOG.debug("REST request to get all MerkleNodes");
        return merkleNodeService.findAll();
    }

    /**
     * {@code GET  /merkle-nodes/:id} : get the "id" merkleNode.
     *
     * @param id the id of the merkleNodeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the merkleNodeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MerkleNodeDTO> getMerkleNode(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get MerkleNode : {}", id);
        Optional<MerkleNodeDTO> merkleNodeDTO = merkleNodeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(merkleNodeDTO);
    }
}
