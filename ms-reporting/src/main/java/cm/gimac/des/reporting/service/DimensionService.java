package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.Dimension;
import cm.gimac.des.reporting.repository.DimensionRepository;
import cm.gimac.des.reporting.service.dto.DimensionDTO;
import cm.gimac.des.reporting.service.mapper.DimensionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.Dimension}.
 */
@Service
@Transactional
public class DimensionService {

    private static final Logger LOG = LoggerFactory.getLogger(DimensionService.class);

    private final DimensionRepository dimensionRepository;

    private final DimensionMapper dimensionMapper;

    public DimensionService(DimensionRepository dimensionRepository, DimensionMapper dimensionMapper) {
        this.dimensionRepository = dimensionRepository;
        this.dimensionMapper = dimensionMapper;
    }

    /**
     * Save a dimension.
     *
     * @param dimensionDTO the entity to save.
     * @return the persisted entity.
     */
    public DimensionDTO save(DimensionDTO dimensionDTO) {
        LOG.debug("Request to save Dimension : {}", dimensionDTO);
        Dimension dimension = dimensionMapper.toEntity(dimensionDTO);
        dimension = dimensionRepository.save(dimension);
        return dimensionMapper.toDto(dimension);
    }

    /**
     * Update a dimension.
     *
     * @param dimensionDTO the entity to save.
     * @return the persisted entity.
     */
    public DimensionDTO update(DimensionDTO dimensionDTO) {
        LOG.debug("Request to update Dimension : {}", dimensionDTO);
        Dimension dimension = dimensionMapper.toEntity(dimensionDTO);
        dimension = dimensionRepository.save(dimension);
        return dimensionMapper.toDto(dimension);
    }

    /**
     * Partially update a dimension.
     *
     * @param dimensionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DimensionDTO> partialUpdate(DimensionDTO dimensionDTO) {
        LOG.debug("Request to partially update Dimension : {}", dimensionDTO);

        return dimensionRepository
            .findById(dimensionDTO.getId())
            .map(existingDimension -> {
                dimensionMapper.partialUpdate(existingDimension, dimensionDTO);

                return existingDimension;
            })
            .map(dimensionRepository::save)
            .map(dimensionMapper::toDto);
    }

    /**
     * Get one dimension by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DimensionDTO> findOne(UUID id) {
        LOG.debug("Request to get Dimension : {}", id);
        return dimensionRepository.findById(id).map(dimensionMapper::toDto);
    }

    /**
     * Delete the dimension by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Dimension : {}", id);
        dimensionRepository.deleteById(id);
    }
}
