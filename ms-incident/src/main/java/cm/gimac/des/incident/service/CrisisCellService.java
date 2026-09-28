package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.CrisisCell;
import cm.gimac.des.incident.repository.CrisisCellRepository;
import cm.gimac.des.incident.service.dto.CrisisCellDTO;
import cm.gimac.des.incident.service.mapper.CrisisCellMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.CrisisCell}.
 */
@Service
@Transactional
public class CrisisCellService {

    private static final Logger LOG = LoggerFactory.getLogger(CrisisCellService.class);

    private final CrisisCellRepository crisisCellRepository;

    private final CrisisCellMapper crisisCellMapper;

    public CrisisCellService(CrisisCellRepository crisisCellRepository, CrisisCellMapper crisisCellMapper) {
        this.crisisCellRepository = crisisCellRepository;
        this.crisisCellMapper = crisisCellMapper;
    }

    /**
     * Save a crisisCell.
     *
     * @param crisisCellDTO the entity to save.
     * @return the persisted entity.
     */
    public CrisisCellDTO save(CrisisCellDTO crisisCellDTO) {
        LOG.debug("Request to save CrisisCell : {}", crisisCellDTO);
        CrisisCell crisisCell = crisisCellMapper.toEntity(crisisCellDTO);
        crisisCell = crisisCellRepository.save(crisisCell);
        return crisisCellMapper.toDto(crisisCell);
    }

    /**
     * Update a crisisCell.
     *
     * @param crisisCellDTO the entity to save.
     * @return the persisted entity.
     */
    public CrisisCellDTO update(CrisisCellDTO crisisCellDTO) {
        LOG.debug("Request to update CrisisCell : {}", crisisCellDTO);
        CrisisCell crisisCell = crisisCellMapper.toEntity(crisisCellDTO);
        crisisCell = crisisCellRepository.save(crisisCell);
        return crisisCellMapper.toDto(crisisCell);
    }

    /**
     * Partially update a crisisCell.
     *
     * @param crisisCellDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CrisisCellDTO> partialUpdate(CrisisCellDTO crisisCellDTO) {
        LOG.debug("Request to partially update CrisisCell : {}", crisisCellDTO);

        return crisisCellRepository
            .findById(crisisCellDTO.getId())
            .map(existingCrisisCell -> {
                crisisCellMapper.partialUpdate(existingCrisisCell, crisisCellDTO);

                return existingCrisisCell;
            })
            .map(crisisCellRepository::save)
            .map(crisisCellMapper::toDto);
    }

    /**
     * Get one crisisCell by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CrisisCellDTO> findOne(UUID id) {
        LOG.debug("Request to get CrisisCell : {}", id);
        return crisisCellRepository.findById(id).map(crisisCellMapper::toDto);
    }

    /**
     * Delete the crisisCell by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CrisisCell : {}", id);
        crisisCellRepository.deleteById(id);
    }
}
