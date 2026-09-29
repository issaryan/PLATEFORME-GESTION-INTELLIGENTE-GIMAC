package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.GoLiveReportDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.GoLiveReport}.
 */
public interface GoLiveReportService {
    /**
     * Save a goLiveReport.
     *
     * @param goLiveReportDTO the entity to save.
     * @return the persisted entity.
     */
    GoLiveReportDTO save(GoLiveReportDTO goLiveReportDTO);

    /**
     * Updates a goLiveReport.
     *
     * @param goLiveReportDTO the entity to update.
     * @return the persisted entity.
     */
    GoLiveReportDTO update(GoLiveReportDTO goLiveReportDTO);

    /**
     * Partially updates a goLiveReport.
     *
     * @param goLiveReportDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<GoLiveReportDTO> partialUpdate(GoLiveReportDTO goLiveReportDTO);

    /**
     * Get all the goLiveReports.
     *
     * @return the list of entities.
     */
    List<GoLiveReportDTO> findAll();

    /**
     * Get the "id" goLiveReport.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<GoLiveReportDTO> findOne(UUID id);

    /**
     * Delete the "id" goLiveReport.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
