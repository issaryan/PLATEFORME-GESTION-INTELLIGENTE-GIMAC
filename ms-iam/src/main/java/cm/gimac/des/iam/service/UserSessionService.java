package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.UserSession;
import cm.gimac.des.iam.repository.UserSessionRepository;
import cm.gimac.des.iam.service.dto.UserSessionDTO;
import cm.gimac.des.iam.service.mapper.UserSessionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.iam.domain.UserSession}.
 */
@Service
@Transactional
public class UserSessionService {

    private static final Logger LOG = LoggerFactory.getLogger(UserSessionService.class);

    private final UserSessionRepository userSessionRepository;

    private final UserSessionMapper userSessionMapper;

    public UserSessionService(UserSessionRepository userSessionRepository, UserSessionMapper userSessionMapper) {
        this.userSessionRepository = userSessionRepository;
        this.userSessionMapper = userSessionMapper;
    }

    /**
     * Save a userSession.
     *
     * @param userSessionDTO the entity to save.
     * @return the persisted entity.
     */
    public UserSessionDTO save(UserSessionDTO userSessionDTO) {
        LOG.debug("Request to save UserSession : {}", userSessionDTO);
        UserSession userSession = userSessionMapper.toEntity(userSessionDTO);
        userSession = userSessionRepository.save(userSession);
        return userSessionMapper.toDto(userSession);
    }

    /**
     * Update a userSession.
     *
     * @param userSessionDTO the entity to save.
     * @return the persisted entity.
     */
    public UserSessionDTO update(UserSessionDTO userSessionDTO) {
        LOG.debug("Request to update UserSession : {}", userSessionDTO);
        UserSession userSession = userSessionMapper.toEntity(userSessionDTO);
        userSession = userSessionRepository.save(userSession);
        return userSessionMapper.toDto(userSession);
    }

    /**
     * Partially update a userSession.
     *
     * @param userSessionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<UserSessionDTO> partialUpdate(UserSessionDTO userSessionDTO) {
        LOG.debug("Request to partially update UserSession : {}", userSessionDTO);

        return userSessionRepository
            .findById(userSessionDTO.getId())
            .map(existingUserSession -> {
                userSessionMapper.partialUpdate(existingUserSession, userSessionDTO);

                return existingUserSession;
            })
            .map(userSessionRepository::save)
            .map(userSessionMapper::toDto);
    }

    /**
     * Get all the userSessions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<UserSessionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return userSessionRepository.findAllWithEagerRelationships(pageable).map(userSessionMapper::toDto);
    }

    /**
     * Get one userSession by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<UserSessionDTO> findOne(UUID id) {
        LOG.debug("Request to get UserSession : {}", id);
        return userSessionRepository.findOneWithEagerRelationships(id).map(userSessionMapper::toDto);
    }

    /**
     * Delete the userSession by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete UserSession : {}", id);
        userSessionRepository.deleteById(id);
    }
}
