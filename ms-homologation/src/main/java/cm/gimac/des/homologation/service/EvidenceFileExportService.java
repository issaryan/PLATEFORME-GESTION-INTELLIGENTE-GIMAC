package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.EvidenceFileExportDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.EvidenceFileExport}.
 */
public interface EvidenceFileExportService {
    /**
     * Save a evidenceFileExport.
     *
     * @param evidenceFileExportDTO the entity to save.
     * @return the persisted entity.
     */
    EvidenceFileExportDTO save(EvidenceFileExportDTO evidenceFileExportDTO);

    /**
     * Updates a evidenceFileExport.
     *
     * @param evidenceFileExportDTO the entity to update.
     * @return the persisted entity.
     */
    EvidenceFileExportDTO update(EvidenceFileExportDTO evidenceFileExportDTO);

    /**
     * Partially updates a evidenceFileExport.
     *
     * @param evidenceFileExportDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<EvidenceFileExportDTO> partialUpdate(EvidenceFileExportDTO evidenceFileExportDTO);

    /**
     * Get all the evidenceFileExports.
     *
     * @return the list of entities.
     */
    List<EvidenceFileExportDTO> findAll();

    /**
     * Get the "id" evidenceFileExport.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<EvidenceFileExportDTO> findOne(UUID id);

    /**
     * Delete the "id" evidenceFileExport.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
