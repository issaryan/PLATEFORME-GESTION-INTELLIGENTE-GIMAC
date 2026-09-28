package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.CockpitView;
import cm.gimac.des.reporting.repository.CockpitViewRepository;
import cm.gimac.des.reporting.service.dto.CockpitViewDTO;
import cm.gimac.des.reporting.service.mapper.CockpitViewMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.CockpitView}.
 */
@Service
@Transactional
public class CockpitViewService {

    private static final Logger LOG = LoggerFactory.getLogger(CockpitViewService.class);

    private final CockpitViewRepository cockpitViewRepository;

    private final CockpitViewMapper cockpitViewMapper;

    public CockpitViewService(CockpitViewRepository cockpitViewRepository, CockpitViewMapper cockpitViewMapper) {
        this.cockpitViewRepository = cockpitViewRepository;
        this.cockpitViewMapper = cockpitViewMapper;
    }

    /**
     * Save a cockpitView.
     *
     * @param cockpitViewDTO the entity to save.
     * @return the persisted entity.
     */
    public CockpitViewDTO save(CockpitViewDTO cockpitViewDTO) {
        LOG.debug("Request to save CockpitView : {}", cockpitViewDTO);
        CockpitView cockpitView = cockpitViewMapper.toEntity(cockpitViewDTO);
        cockpitView = cockpitViewRepository.save(cockpitView);
        return cockpitViewMapper.toDto(cockpitView);
    }

    /**
     * Update a cockpitView.
     *
     * @param cockpitViewDTO the entity to save.
     * @return the persisted entity.
     */
    public CockpitViewDTO update(CockpitViewDTO cockpitViewDTO) {
        LOG.debug("Request to update CockpitView : {}", cockpitViewDTO);
        CockpitView cockpitView = cockpitViewMapper.toEntity(cockpitViewDTO);
        cockpitView = cockpitViewRepository.save(cockpitView);
        return cockpitViewMapper.toDto(cockpitView);
    }

    /**
     * Partially update a cockpitView.
     *
     * @param cockpitViewDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CockpitViewDTO> partialUpdate(CockpitViewDTO cockpitViewDTO) {
        LOG.debug("Request to partially update CockpitView : {}", cockpitViewDTO);

        return cockpitViewRepository
            .findById(cockpitViewDTO.getId())
            .map(existingCockpitView -> {
                cockpitViewMapper.partialUpdate(existingCockpitView, cockpitViewDTO);

                return existingCockpitView;
            })
            .map(cockpitViewRepository::save)
            .map(cockpitViewMapper::toDto);
    }

    /**
     * Get all the cockpitViews.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<CockpitViewDTO> findAll() {
        LOG.debug("Request to get all CockpitViews");
        return cockpitViewRepository.findAll().stream().map(cockpitViewMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the cockpitViews with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<CockpitViewDTO> findAllWithEagerRelationships(Pageable pageable) {
        return cockpitViewRepository.findAllWithEagerRelationships(pageable).map(cockpitViewMapper::toDto);
    }

    /**
     * Get one cockpitView by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CockpitViewDTO> findOne(UUID id) {
        LOG.debug("Request to get CockpitView : {}", id);
        return cockpitViewRepository.findOneWithEagerRelationships(id).map(cockpitViewMapper::toDto);
    }

    /**
     * Delete the cockpitView by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CockpitView : {}", id);
        cockpitViewRepository.deleteById(id);
    }
}
