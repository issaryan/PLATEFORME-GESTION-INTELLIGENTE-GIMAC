package cm.gimac.des.audit.service;

import cm.gimac.des.audit.domain.MerkleNode;
import cm.gimac.des.audit.repository.MerkleNodeRepository;
import cm.gimac.des.audit.service.dto.MerkleNodeDTO;
import cm.gimac.des.audit.service.mapper.MerkleNodeMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.audit.domain.MerkleNode}.
 */
@Service
@Transactional
public class MerkleNodeService {

    private static final Logger LOG = LoggerFactory.getLogger(MerkleNodeService.class);

    private final MerkleNodeRepository merkleNodeRepository;

    private final MerkleNodeMapper merkleNodeMapper;

    public MerkleNodeService(MerkleNodeRepository merkleNodeRepository, MerkleNodeMapper merkleNodeMapper) {
        this.merkleNodeRepository = merkleNodeRepository;
        this.merkleNodeMapper = merkleNodeMapper;
    }

    /**
     * Save a merkleNode.
     *
     * @param merkleNodeDTO the entity to save.
     * @return the persisted entity.
     */
    public MerkleNodeDTO save(MerkleNodeDTO merkleNodeDTO) {
        LOG.debug("Request to save MerkleNode : {}", merkleNodeDTO);
        MerkleNode merkleNode = merkleNodeMapper.toEntity(merkleNodeDTO);
        merkleNode = merkleNodeRepository.save(merkleNode);
        return merkleNodeMapper.toDto(merkleNode);
    }

    /**
     * Update a merkleNode.
     *
     * @param merkleNodeDTO the entity to save.
     * @return the persisted entity.
     */
    public MerkleNodeDTO update(MerkleNodeDTO merkleNodeDTO) {
        LOG.debug("Request to update MerkleNode : {}", merkleNodeDTO);
        MerkleNode merkleNode = merkleNodeMapper.toEntity(merkleNodeDTO);
        merkleNode = merkleNodeRepository.save(merkleNode);
        return merkleNodeMapper.toDto(merkleNode);
    }

    /**
     * Partially update a merkleNode.
     *
     * @param merkleNodeDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MerkleNodeDTO> partialUpdate(MerkleNodeDTO merkleNodeDTO) {
        LOG.debug("Request to partially update MerkleNode : {}", merkleNodeDTO);

        return merkleNodeRepository
            .findById(merkleNodeDTO.getId())
            .map(existingMerkleNode -> {
                merkleNodeMapper.partialUpdate(existingMerkleNode, merkleNodeDTO);

                return existingMerkleNode;
            })
            .map(merkleNodeRepository::save)
            .map(merkleNodeMapper::toDto);
    }

    /**
     * Get all the merkleNodes.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<MerkleNodeDTO> findAll() {
        LOG.debug("Request to get all MerkleNodes");
        return merkleNodeRepository.findAll().stream().map(merkleNodeMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one merkleNode by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MerkleNodeDTO> findOne(UUID id) {
        LOG.debug("Request to get MerkleNode : {}", id);
        return merkleNodeRepository.findById(id).map(merkleNodeMapper::toDto);
    }

    /**
     * Delete the merkleNode by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete MerkleNode : {}", id);
        merkleNodeRepository.deleteById(id);
    }
}
