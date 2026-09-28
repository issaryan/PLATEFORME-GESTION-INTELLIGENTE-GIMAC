package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.AlertSourceAsserts.*;
import static cm.gimac.des.monitoring.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.AlertSource;
import cm.gimac.des.monitoring.repository.AlertSourceRepository;
import cm.gimac.des.monitoring.service.dto.AlertSourceDTO;
import cm.gimac.des.monitoring.service.mapper.AlertSourceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link AlertSourceResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AlertSourceResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final String DEFAULT_CERTIFICATE_THUMBPRINT = "233f95dee7720e213b8fefe222b33cec2c21cf2dfbc0e7ea219823681ec4357d";
    private static final String UPDATED_CERTIFICATE_THUMBPRINT = "abca7b92f10297f542a01192b75a8ea9955e3d43c5f0f94ea15bf9a4ea1c8b0b";

    private static final String DEFAULT_ALLOWED_SCHEMA = "AAAAAAAAAA";
    private static final String UPDATED_ALLOWED_SCHEMA = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/alert-sources";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AlertSourceRepository alertSourceRepository;

    @Autowired
    private AlertSourceMapper alertSourceMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAlertSourceMockMvc;

    private AlertSource alertSource;

    private AlertSource insertedAlertSource;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AlertSource createEntity() {
        return new AlertSource()
            .code(DEFAULT_CODE)
            .label(DEFAULT_LABEL)
            .certificateThumbprint(DEFAULT_CERTIFICATE_THUMBPRINT)
            .allowedSchema(DEFAULT_ALLOWED_SCHEMA)
            .active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AlertSource createUpdatedEntity() {
        return new AlertSource()
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .certificateThumbprint(UPDATED_CERTIFICATE_THUMBPRINT)
            .allowedSchema(UPDATED_ALLOWED_SCHEMA)
            .active(UPDATED_ACTIVE);
    }

    @BeforeEach
    void initTest() {
        alertSource = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAlertSource != null) {
            alertSourceRepository.delete(insertedAlertSource);
            insertedAlertSource = null;
        }
    }

    @Test
    @Transactional
    void createAlertSource() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AlertSource
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);
        var returnedAlertSourceDTO = om.readValue(
            restAlertSourceMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(alertSourceDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AlertSourceDTO.class
        );

        // Validate the AlertSource in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAlertSource = alertSourceMapper.toEntity(returnedAlertSourceDTO);
        assertAlertSourceUpdatableFieldsEquals(returnedAlertSource, getPersistedAlertSource(returnedAlertSource));

        insertedAlertSource = returnedAlertSource;
    }

    @Test
    @Transactional
    void createAlertSourceWithExistingId() throws Exception {
        // Create the AlertSource with an existing ID
        insertedAlertSource = alertSourceRepository.saveAndFlush(alertSource);
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAlertSourceMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AlertSource in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        alertSource.setCode(null);

        // Create the AlertSource, which fails.
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        restAlertSourceMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        alertSource.setLabel(null);

        // Create the AlertSource, which fails.
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        restAlertSourceMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCertificateThumbprintIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        alertSource.setCertificateThumbprint(null);

        // Create the AlertSource, which fails.
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        restAlertSourceMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        alertSource.setActive(null);

        // Create the AlertSource, which fails.
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        restAlertSourceMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAlertSources() throws Exception {
        // Initialize the database
        insertedAlertSource = alertSourceRepository.saveAndFlush(alertSource);

        // Get all the alertSourceList
        restAlertSourceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(alertSource.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].certificateThumbprint").value(hasItem(DEFAULT_CERTIFICATE_THUMBPRINT)))
            .andExpect(jsonPath("$.[*].allowedSchema").value(hasItem(DEFAULT_ALLOWED_SCHEMA)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getAlertSource() throws Exception {
        // Initialize the database
        insertedAlertSource = alertSourceRepository.saveAndFlush(alertSource);

        // Get the alertSource
        restAlertSourceMockMvc
            .perform(get(ENTITY_API_URL_ID, alertSource.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(alertSource.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.certificateThumbprint").value(DEFAULT_CERTIFICATE_THUMBPRINT))
            .andExpect(jsonPath("$.allowedSchema").value(DEFAULT_ALLOWED_SCHEMA))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingAlertSource() throws Exception {
        // Get the alertSource
        restAlertSourceMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAlertSource() throws Exception {
        // Initialize the database
        insertedAlertSource = alertSourceRepository.saveAndFlush(alertSource);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the alertSource
        AlertSource updatedAlertSource = alertSourceRepository.findById(alertSource.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAlertSource are not directly saved in db
        em.detach(updatedAlertSource);
        updatedAlertSource
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .certificateThumbprint(UPDATED_CERTIFICATE_THUMBPRINT)
            .allowedSchema(UPDATED_ALLOWED_SCHEMA)
            .active(UPDATED_ACTIVE);
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(updatedAlertSource);

        restAlertSourceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, alertSourceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isOk());

        // Validate the AlertSource in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAlertSourceToMatchAllProperties(updatedAlertSource);
    }

    @Test
    @Transactional
    void putNonExistingAlertSource() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSource.setId(UUID.randomUUID());

        // Create the AlertSource
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAlertSourceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, alertSourceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AlertSource in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAlertSource() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSource.setId(UUID.randomUUID());

        // Create the AlertSource
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAlertSourceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AlertSource in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAlertSource() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSource.setId(UUID.randomUUID());

        // Create the AlertSource
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAlertSourceMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(alertSourceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the AlertSource in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAlertSourceWithPatch() throws Exception {
        // Initialize the database
        insertedAlertSource = alertSourceRepository.saveAndFlush(alertSource);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the alertSource using partial update
        AlertSource partialUpdatedAlertSource = new AlertSource();
        partialUpdatedAlertSource.setId(alertSource.getId());

        partialUpdatedAlertSource.certificateThumbprint(UPDATED_CERTIFICATE_THUMBPRINT).allowedSchema(UPDATED_ALLOWED_SCHEMA);

        restAlertSourceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAlertSource.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAlertSource))
            )
            .andExpect(status().isOk());

        // Validate the AlertSource in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAlertSourceUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAlertSource, alertSource),
            getPersistedAlertSource(alertSource)
        );
    }

    @Test
    @Transactional
    void fullUpdateAlertSourceWithPatch() throws Exception {
        // Initialize the database
        insertedAlertSource = alertSourceRepository.saveAndFlush(alertSource);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the alertSource using partial update
        AlertSource partialUpdatedAlertSource = new AlertSource();
        partialUpdatedAlertSource.setId(alertSource.getId());

        partialUpdatedAlertSource
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .certificateThumbprint(UPDATED_CERTIFICATE_THUMBPRINT)
            .allowedSchema(UPDATED_ALLOWED_SCHEMA)
            .active(UPDATED_ACTIVE);

        restAlertSourceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAlertSource.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAlertSource))
            )
            .andExpect(status().isOk());

        // Validate the AlertSource in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAlertSourceUpdatableFieldsEquals(partialUpdatedAlertSource, getPersistedAlertSource(partialUpdatedAlertSource));
    }

    @Test
    @Transactional
    void patchNonExistingAlertSource() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSource.setId(UUID.randomUUID());

        // Create the AlertSource
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAlertSourceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, alertSourceDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AlertSource in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAlertSource() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSource.setId(UUID.randomUUID());

        // Create the AlertSource
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAlertSourceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AlertSource in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAlertSource() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSource.setId(UUID.randomUUID());

        // Create the AlertSource
        AlertSourceDTO alertSourceDTO = alertSourceMapper.toDto(alertSource);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAlertSourceMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(alertSourceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AlertSource in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAlertSource() throws Exception {
        // Initialize the database
        insertedAlertSource = alertSourceRepository.saveAndFlush(alertSource);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the alertSource
        restAlertSourceMockMvc
            .perform(delete(ENTITY_API_URL_ID, alertSource.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return alertSourceRepository.count();
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

    protected AlertSource getPersistedAlertSource(AlertSource alertSource) {
        return alertSourceRepository.findById(alertSource.getId()).orElseThrow();
    }

    protected void assertPersistedAlertSourceToMatchAllProperties(AlertSource expectedAlertSource) {
        assertAlertSourceAllPropertiesEquals(expectedAlertSource, getPersistedAlertSource(expectedAlertSource));
    }

    protected void assertPersistedAlertSourceToMatchUpdatableProperties(AlertSource expectedAlertSource) {
        assertAlertSourceAllUpdatablePropertiesEquals(expectedAlertSource, getPersistedAlertSource(expectedAlertSource));
    }
}
