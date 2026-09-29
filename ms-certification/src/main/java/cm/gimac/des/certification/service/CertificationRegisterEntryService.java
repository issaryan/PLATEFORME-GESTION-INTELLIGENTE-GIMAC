package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.CertificationRegisterEntryDTO;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.CertificationRegisterEntry}.
 */
public interface CertificationRegisterEntryService {
    /**
     * Save a certificationRegisterEntry.
     *
     * @param certificationRegisterEntryDTO the entity to save.
     * @return the persisted entity.
     */
    CertificationRegisterEntryDTO save(CertificationRegisterEntryDTO certificationRegisterEntryDTO);

    /**
     * Updates a certificationRegisterEntry.
     *
     * @param certificationRegisterEntryDTO the entity to update.
     * @return the persisted entity.
     */
    CertificationRegisterEntryDTO update(CertificationRegisterEntryDTO certificationRegisterEntryDTO);

    /**
     * Partially updates a certificationRegisterEntry.
     *
     * @param certificationRegisterEntryDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<CertificationRegisterEntryDTO> partialUpdate(CertificationRegisterEntryDTO certificationRegisterEntryDTO);

    /**
     * Get all the certificationRegisterEntries.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<CertificationRegisterEntryDTO> findAll(Pageable pageable);

    /**
     * Get the "id" certificationRegisterEntry.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<CertificationRegisterEntryDTO> findOne(UUID id);

    /**
     * Delete the "id" certificationRegisterEntry.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
