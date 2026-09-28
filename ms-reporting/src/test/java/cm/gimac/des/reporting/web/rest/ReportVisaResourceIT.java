package cm.gimac.des.reporting.web.rest;

import static cm.gimac.des.reporting.domain.ReportVisaAsserts.*;
import static cm.gimac.des.reporting.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.reporting.IntegrationTest;
import cm.gimac.des.reporting.domain.PeriodicReport;
import cm.gimac.des.reporting.domain.ReportVisa;
import cm.gimac.des.reporting.repository.ReportVisaRepository;
import cm.gimac.des.reporting.service.dto.ReportVisaDTO;
import cm.gimac.des.reporting.service.mapper.ReportVisaMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ReportVisaResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ReportVisaResourceIT {

    private static final UUID DEFAULT_APPROVER_ID = UUID.randomUUID();
    private static final UUID UPDATED_APPROVER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_APPROVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_APPROVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/report-visas";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReportVisaRepository reportVisaRepository;

    @Autowired
    private ReportVisaMapper reportVisaMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReportVisaMockMvc;

    private ReportVisa reportVisa;

    private ReportVisa insertedReportVisa;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReportVisa createEntity(EntityManager em) {
        ReportVisa reportVisa = new ReportVisa().approverId(DEFAULT_APPROVER_ID).approvedAt(DEFAULT_APPROVED_AT).comment(DEFAULT_COMMENT);
        // Add required entity
        PeriodicReport periodicReport;
        if (TestUtil.findAll(em, PeriodicReport.class).isEmpty()) {
            periodicReport = PeriodicReportResourceIT.createEntity();
            em.persist(periodicReport);
            em.flush();
        } else {
            periodicReport = TestUtil.findAll(em, PeriodicReport.class).get(0);
        }
        reportVisa.setReport(periodicReport);
        return reportVisa;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReportVisa createUpdatedEntity(EntityManager em) {
        ReportVisa updatedReportVisa = new ReportVisa()
            .approverId(UPDATED_APPROVER_ID)
            .approvedAt(UPDATED_APPROVED_AT)
            .comment(UPDATED_COMMENT);
        // Add required entity
        PeriodicReport periodicReport;
        if (TestUtil.findAll(em, PeriodicReport.class).isEmpty()) {
            periodicReport = PeriodicReportResourceIT.createUpdatedEntity();
            em.persist(periodicReport);
            em.flush();
        } else {
            periodicReport = TestUtil.findAll(em, PeriodicReport.class).get(0);
        }
        updatedReportVisa.setReport(periodicReport);
        return updatedReportVisa;
    }

    @BeforeEach
    void initTest() {
        reportVisa = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedReportVisa != null) {
            reportVisaRepository.delete(insertedReportVisa);
            insertedReportVisa = null;
        }
    }

    @Test
    @Transactional
    void createReportVisa() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ReportVisa
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(reportVisa);
        var returnedReportVisaDTO = om.readValue(
            restReportVisaMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reportVisaDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ReportVisaDTO.class
        );

        // Validate the ReportVisa in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedReportVisa = reportVisaMapper.toEntity(returnedReportVisaDTO);
        assertReportVisaUpdatableFieldsEquals(returnedReportVisa, getPersistedReportVisa(returnedReportVisa));

        insertedReportVisa = returnedReportVisa;
    }

    @Test
    @Transactional
    void createReportVisaWithExistingId() throws Exception {
        // Create the ReportVisa with an existing ID
        insertedReportVisa = reportVisaRepository.saveAndFlush(reportVisa);
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(reportVisa);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReportVisaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reportVisaDTO)))
            .andExpect(status().isBadRequest());

        // Validate the ReportVisa in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkApproverIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        reportVisa.setApproverId(null);

        // Create the ReportVisa, which fails.
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(reportVisa);

        restReportVisaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reportVisaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkApprovedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        reportVisa.setApprovedAt(null);

        // Create the ReportVisa, which fails.
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(reportVisa);

        restReportVisaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reportVisaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllReportVisas() throws Exception {
        // Initialize the database
        insertedReportVisa = reportVisaRepository.saveAndFlush(reportVisa);

        // Get all the reportVisaList
        restReportVisaMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(reportVisa.getId().toString())))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].approvedAt").value(hasItem(DEFAULT_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)));
    }

    @Test
    @Transactional
    void getReportVisa() throws Exception {
        // Initialize the database
        insertedReportVisa = reportVisaRepository.saveAndFlush(reportVisa);

        // Get the reportVisa
        restReportVisaMockMvc
            .perform(get(ENTITY_API_URL_ID, reportVisa.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(reportVisa.getId().toString()))
            .andExpect(jsonPath("$.approverId").value(DEFAULT_APPROVER_ID.toString()))
            .andExpect(jsonPath("$.approvedAt").value(DEFAULT_APPROVED_AT.toString()))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT));
    }

    @Test
    @Transactional
    void getNonExistingReportVisa() throws Exception {
        // Get the reportVisa
        restReportVisaMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReportVisa() throws Exception {
        // Initialize the database
        insertedReportVisa = reportVisaRepository.saveAndFlush(reportVisa);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the reportVisa
        ReportVisa updatedReportVisa = reportVisaRepository.findById(reportVisa.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReportVisa are not directly saved in db
        em.detach(updatedReportVisa);
        updatedReportVisa.approverId(UPDATED_APPROVER_ID).approvedAt(UPDATED_APPROVED_AT).comment(UPDATED_COMMENT);
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(updatedReportVisa);

        restReportVisaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, reportVisaDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(reportVisaDTO))
            )
            .andExpect(status().isOk());

        // Validate the ReportVisa in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReportVisaToMatchAllProperties(updatedReportVisa);
    }

    @Test
    @Transactional
    void putNonExistingReportVisa() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reportVisa.setId(UUID.randomUUID());

        // Create the ReportVisa
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(reportVisa);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReportVisaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, reportVisaDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(reportVisaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReportVisa in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReportVisa() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reportVisa.setId(UUID.randomUUID());

        // Create the ReportVisa
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(reportVisa);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReportVisaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(reportVisaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReportVisa in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReportVisa() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reportVisa.setId(UUID.randomUUID());

        // Create the ReportVisa
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(reportVisa);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReportVisaMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reportVisaDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReportVisa in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReportVisaWithPatch() throws Exception {
        // Initialize the database
        insertedReportVisa = reportVisaRepository.saveAndFlush(reportVisa);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the reportVisa using partial update
        ReportVisa partialUpdatedReportVisa = new ReportVisa();
        partialUpdatedReportVisa.setId(reportVisa.getId());

        partialUpdatedReportVisa.approverId(UPDATED_APPROVER_ID).approvedAt(UPDATED_APPROVED_AT).comment(UPDATED_COMMENT);

        restReportVisaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReportVisa.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReportVisa))
            )
            .andExpect(status().isOk());

        // Validate the ReportVisa in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReportVisaUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedReportVisa, reportVisa),
            getPersistedReportVisa(reportVisa)
        );
    }

    @Test
    @Transactional
    void fullUpdateReportVisaWithPatch() throws Exception {
        // Initialize the database
        insertedReportVisa = reportVisaRepository.saveAndFlush(reportVisa);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the reportVisa using partial update
        ReportVisa partialUpdatedReportVisa = new ReportVisa();
        partialUpdatedReportVisa.setId(reportVisa.getId());

        partialUpdatedReportVisa.approverId(UPDATED_APPROVER_ID).approvedAt(UPDATED_APPROVED_AT).comment(UPDATED_COMMENT);

        restReportVisaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReportVisa.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReportVisa))
            )
            .andExpect(status().isOk());

        // Validate the ReportVisa in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReportVisaUpdatableFieldsEquals(partialUpdatedReportVisa, getPersistedReportVisa(partialUpdatedReportVisa));
    }

    @Test
    @Transactional
    void patchNonExistingReportVisa() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reportVisa.setId(UUID.randomUUID());

        // Create the ReportVisa
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(reportVisa);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReportVisaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, reportVisaDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(reportVisaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReportVisa in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReportVisa() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reportVisa.setId(UUID.randomUUID());

        // Create the ReportVisa
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(reportVisa);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReportVisaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(reportVisaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReportVisa in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReportVisa() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reportVisa.setId(UUID.randomUUID());

        // Create the ReportVisa
        ReportVisaDTO reportVisaDTO = reportVisaMapper.toDto(reportVisa);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReportVisaMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(reportVisaDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReportVisa in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReportVisa() throws Exception {
        // Initialize the database
        insertedReportVisa = reportVisaRepository.saveAndFlush(reportVisa);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the reportVisa
        restReportVisaMockMvc
            .perform(delete(ENTITY_API_URL_ID, reportVisa.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return reportVisaRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected ReportVisa getPersistedReportVisa(ReportVisa reportVisa) {
        return reportVisaRepository.findById(reportVisa.getId()).orElseThrow();
    }

    protected void assertPersistedReportVisaToMatchAllProperties(ReportVisa expectedReportVisa) {
        assertReportVisaAllPropertiesEquals(expectedReportVisa, getPersistedReportVisa(expectedReportVisa));
    }

    protected void assertPersistedReportVisaToMatchUpdatableProperties(ReportVisa expectedReportVisa) {
        assertReportVisaAllUpdatablePropertiesEquals(expectedReportVisa, getPersistedReportVisa(expectedReportVisa));
    }
}
