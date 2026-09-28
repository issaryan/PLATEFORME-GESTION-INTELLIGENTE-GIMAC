package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.repository.ShiftRepository;
import cm.gimac.des.monitoring.service.dto.ShiftDTO;
import cm.gimac.des.monitoring.service.mapper.ShiftMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.Shift}.
 */
@Service
@Transactional
public class ShiftService {

    private static final Logger LOG = LoggerFactory.getLogger(ShiftService.class);

    private final ShiftRepository shiftRepository;

    private final ShiftMapper shiftMapper;

    public ShiftService(ShiftRepository shiftRepository, ShiftMapper shiftMapper) {
        this.shiftRepository = shiftRepository;
        this.shiftMapper = shiftMapper;
    }

    /**
     * Save a shift.
     *
     * @param shiftDTO the entity to save.
     * @return the persisted entity.
     */
    public ShiftDTO save(ShiftDTO shiftDTO) {
        LOG.debug("Request to save Shift : {}", shiftDTO);
        Shift shift = shiftMapper.toEntity(shiftDTO);
        shift = shiftRepository.save(shift);
        return shiftMapper.toDto(shift);
    }

    /**
     * Update a shift.
     *
     * @param shiftDTO the entity to save.
     * @return the persisted entity.
     */
    public ShiftDTO update(ShiftDTO shiftDTO) {
        LOG.debug("Request to update Shift : {}", shiftDTO);
        Shift shift = shiftMapper.toEntity(shiftDTO);
        shift = shiftRepository.save(shift);
        return shiftMapper.toDto(shift);
    }

    /**
     * Partially update a shift.
     *
     * @param shiftDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ShiftDTO> partialUpdate(ShiftDTO shiftDTO) {
        LOG.debug("Request to partially update Shift : {}", shiftDTO);

        return shiftRepository
            .findById(shiftDTO.getId())
            .map(existingShift -> {
                shiftMapper.partialUpdate(existingShift, shiftDTO);

                return existingShift;
            })
            .map(shiftRepository::save)
            .map(shiftMapper::toDto);
    }

    /**
     * Get all the shifts with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ShiftDTO> findAllWithEagerRelationships(Pageable pageable) {
        return shiftRepository.findAllWithEagerRelationships(pageable).map(shiftMapper::toDto);
    }

    /**
     *  Get all the shifts where ShiftReport is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> findAllWhereShiftReportIsNull() {
        LOG.debug("Request to get all shifts where ShiftReport is null");
        return StreamSupport.stream(shiftRepository.findAll().spliterator(), false)
            .filter(shift -> shift.getShiftReport() == null)
            .map(shiftMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one shift by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ShiftDTO> findOne(UUID id) {
        LOG.debug("Request to get Shift : {}", id);
        return shiftRepository.findOneWithEagerRelationships(id).map(shiftMapper::toDto);
    }

    /**
     * Delete the shift by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Shift : {}", id);
        shiftRepository.deleteById(id);
    }
}
