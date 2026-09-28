package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.RoleGrant;
import cm.gimac.des.iam.repository.RoleGrantRepository;
import cm.gimac.des.iam.service.dto.RoleGrantDTO;
import cm.gimac.des.iam.service.mapper.RoleGrantMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.iam.domain.RoleGrant}.
 */
@Service
@Transactional
public class RoleGrantService {

    private static final Logger LOG = LoggerFactory.getLogger(RoleGrantService.class);

    private final RoleGrantRepository roleGrantRepository;

    private final RoleGrantMapper roleGrantMapper;

    public RoleGrantService(RoleGrantRepository roleGrantRepository, RoleGrantMapper roleGrantMapper) {
        this.roleGrantRepository = roleGrantRepository;
        this.roleGrantMapper = roleGrantMapper;
    }

    /**
     * Save a roleGrant.
     *
     * @param roleGrantDTO the entity to save.
     * @return the persisted entity.
     */
    public RoleGrantDTO save(RoleGrantDTO roleGrantDTO) {
        LOG.debug("Request to save RoleGrant : {}", roleGrantDTO);
        RoleGrant roleGrant = roleGrantMapper.toEntity(roleGrantDTO);
        roleGrant = roleGrantRepository.save(roleGrant);
        return roleGrantMapper.toDto(roleGrant);
    }

    /**
     * Update a roleGrant.
     *
     * @param roleGrantDTO the entity to save.
     * @return the persisted entity.
     */
    public RoleGrantDTO update(RoleGrantDTO roleGrantDTO) {
        LOG.debug("Request to update RoleGrant : {}", roleGrantDTO);
        RoleGrant roleGrant = roleGrantMapper.toEntity(roleGrantDTO);
        roleGrant = roleGrantRepository.save(roleGrant);
        return roleGrantMapper.toDto(roleGrant);
    }

    /**
     * Partially update a roleGrant.
     *
     * @param roleGrantDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RoleGrantDTO> partialUpdate(RoleGrantDTO roleGrantDTO) {
        LOG.debug("Request to partially update RoleGrant : {}", roleGrantDTO);

        return roleGrantRepository
            .findById(roleGrantDTO.getId())
            .map(existingRoleGrant -> {
                roleGrantMapper.partialUpdate(existingRoleGrant, roleGrantDTO);

                return existingRoleGrant;
            })
            .map(roleGrantRepository::save)
            .map(roleGrantMapper::toDto);
    }

    /**
     * Get all the roleGrants with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RoleGrantDTO> findAllWithEagerRelationships(Pageable pageable) {
        return roleGrantRepository.findAllWithEagerRelationships(pageable).map(roleGrantMapper::toDto);
    }

    /**
     * Get one roleGrant by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RoleGrantDTO> findOne(UUID id) {
        LOG.debug("Request to get RoleGrant : {}", id);
        return roleGrantRepository.findOneWithEagerRelationships(id).map(roleGrantMapper::toDto);
    }

    /**
     * Delete the roleGrant by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete RoleGrant : {}", id);
        roleGrantRepository.deleteById(id);
    }
}
