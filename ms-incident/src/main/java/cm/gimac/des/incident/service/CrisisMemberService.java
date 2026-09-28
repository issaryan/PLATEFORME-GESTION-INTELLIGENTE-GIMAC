package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.CrisisMember;
import cm.gimac.des.incident.repository.CrisisMemberRepository;
import cm.gimac.des.incident.service.dto.CrisisMemberDTO;
import cm.gimac.des.incident.service.mapper.CrisisMemberMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.CrisisMember}.
 */
@Service
@Transactional
public class CrisisMemberService {

    private static final Logger LOG = LoggerFactory.getLogger(CrisisMemberService.class);

    private final CrisisMemberRepository crisisMemberRepository;

    private final CrisisMemberMapper crisisMemberMapper;

    public CrisisMemberService(CrisisMemberRepository crisisMemberRepository, CrisisMemberMapper crisisMemberMapper) {
        this.crisisMemberRepository = crisisMemberRepository;
        this.crisisMemberMapper = crisisMemberMapper;
    }

    /**
     * Save a crisisMember.
     *
     * @param crisisMemberDTO the entity to save.
     * @return the persisted entity.
     */
    public CrisisMemberDTO save(CrisisMemberDTO crisisMemberDTO) {
        LOG.debug("Request to save CrisisMember : {}", crisisMemberDTO);
        CrisisMember crisisMember = crisisMemberMapper.toEntity(crisisMemberDTO);
        crisisMember = crisisMemberRepository.save(crisisMember);
        return crisisMemberMapper.toDto(crisisMember);
    }

    /**
     * Update a crisisMember.
     *
     * @param crisisMemberDTO the entity to save.
     * @return the persisted entity.
     */
    public CrisisMemberDTO update(CrisisMemberDTO crisisMemberDTO) {
        LOG.debug("Request to update CrisisMember : {}", crisisMemberDTO);
        CrisisMember crisisMember = crisisMemberMapper.toEntity(crisisMemberDTO);
        crisisMember = crisisMemberRepository.save(crisisMember);
        return crisisMemberMapper.toDto(crisisMember);
    }

    /**
     * Partially update a crisisMember.
     *
     * @param crisisMemberDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CrisisMemberDTO> partialUpdate(CrisisMemberDTO crisisMemberDTO) {
        LOG.debug("Request to partially update CrisisMember : {}", crisisMemberDTO);

        return crisisMemberRepository
            .findById(crisisMemberDTO.getId())
            .map(existingCrisisMember -> {
                crisisMemberMapper.partialUpdate(existingCrisisMember, crisisMemberDTO);

                return existingCrisisMember;
            })
            .map(crisisMemberRepository::save)
            .map(crisisMemberMapper::toDto);
    }

    /**
     * Get all the crisisMembers.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<CrisisMemberDTO> findAll() {
        LOG.debug("Request to get all CrisisMembers");
        return crisisMemberRepository.findAll().stream().map(crisisMemberMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the crisisMembers with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<CrisisMemberDTO> findAllWithEagerRelationships(Pageable pageable) {
        return crisisMemberRepository.findAllWithEagerRelationships(pageable).map(crisisMemberMapper::toDto);
    }

    /**
     * Get one crisisMember by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CrisisMemberDTO> findOne(UUID id) {
        LOG.debug("Request to get CrisisMember : {}", id);
        return crisisMemberRepository.findOneWithEagerRelationships(id).map(crisisMemberMapper::toDto);
    }

    /**
     * Delete the crisisMember by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CrisisMember : {}", id);
        crisisMemberRepository.deleteById(id);
    }
}
