package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.Interaction;
import cm.gimac.des.participant.repository.InteractionRepository;
import cm.gimac.des.participant.service.dto.InteractionDTO;
import cm.gimac.des.participant.service.mapper.InteractionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.participant.domain.Interaction}.
 */
@Service
@Transactional
public class InteractionService {

    private static final Logger LOG = LoggerFactory.getLogger(InteractionService.class);

    private final InteractionRepository interactionRepository;

    private final InteractionMapper interactionMapper;

    public InteractionService(InteractionRepository interactionRepository, InteractionMapper interactionMapper) {
        this.interactionRepository = interactionRepository;
        this.interactionMapper = interactionMapper;
    }

    /**
     * Save a interaction.
     *
     * @param interactionDTO the entity to save.
     * @return the persisted entity.
     */
    public InteractionDTO save(InteractionDTO interactionDTO) {
        LOG.debug("Request to save Interaction : {}", interactionDTO);
        Interaction interaction = interactionMapper.toEntity(interactionDTO);
        interaction = interactionRepository.save(interaction);
        return interactionMapper.toDto(interaction);
    }

    /**
     * Update a interaction.
     *
     * @param interactionDTO the entity to save.
     * @return the persisted entity.
     */
    public InteractionDTO update(InteractionDTO interactionDTO) {
        LOG.debug("Request to update Interaction : {}", interactionDTO);
        Interaction interaction = interactionMapper.toEntity(interactionDTO);
        interaction = interactionRepository.save(interaction);
        return interactionMapper.toDto(interaction);
    }

    /**
     * Partially update a interaction.
     *
     * @param interactionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<InteractionDTO> partialUpdate(InteractionDTO interactionDTO) {
        LOG.debug("Request to partially update Interaction : {}", interactionDTO);

        return interactionRepository
            .findById(interactionDTO.getId())
            .map(existingInteraction -> {
                interactionMapper.partialUpdate(existingInteraction, interactionDTO);

                return existingInteraction;
            })
            .map(interactionRepository::save)
            .map(interactionMapper::toDto);
    }

    /**
     * Get all the interactions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<InteractionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return interactionRepository.findAllWithEagerRelationships(pageable).map(interactionMapper::toDto);
    }

    /**
     * Get one interaction by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<InteractionDTO> findOne(UUID id) {
        LOG.debug("Request to get Interaction : {}", id);
        return interactionRepository.findOneWithEagerRelationships(id).map(interactionMapper::toDto);
    }

    /**
     * Delete the interaction by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Interaction : {}", id);
        interactionRepository.deleteById(id);
    }
}
