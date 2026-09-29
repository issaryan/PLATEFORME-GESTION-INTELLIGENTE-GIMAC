package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.DeadlineNotificationAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.DeadlineNotification;
import cm.gimac.des.homologation.repository.DeadlineNotificationRepository;
import cm.gimac.des.homologation.service.dto.DeadlineNotificationDTO;
import cm.gimac.des.homologation.service.mapper.DeadlineNotificationMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link DeadlineNotificationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DeadlineNotificationResourceIT {

    private static final UUID DEFAULT_OPERATION_ID = UUID.randomUUID();
    private static final UUID UPDATED_OPERATION_ID = UUID.randomUUID();

    private static final String DEFAULT_SEUIL = "AAAAAAAAAA";
    private static final String UPDATED_SEUIL = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_ENVOI = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_ENVOI = Instant.ofEpochMilli(1701958935371L);

    private static final String ENTITY_API_URL = "/api/deadline-notifications";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DeadlineNotificationRepository deadlineNotificationRepository;

    @Autowired
    private DeadlineNotificationMapper deadlineNotificationMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDeadlineNotificationMockMvc;

    private DeadlineNotification deadlineNotification;

    private DeadlineNotification insertedDeadlineNotification;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DeadlineNotification createEntity() {
        return new DeadlineNotification().operationId(DEFAULT_OPERATION_ID).seuil(DEFAULT_SEUIL).dateEnvoi(DEFAULT_DATE_ENVOI);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DeadlineNotification createUpdatedEntity() {
        return new DeadlineNotification().operationId(UPDATED_OPERATION_ID).seuil(UPDATED_SEUIL).dateEnvoi(UPDATED_DATE_ENVOI);
    }

    @BeforeEach
    void initTest() {
        deadlineNotification = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDeadlineNotification != null) {
            deadlineNotificationRepository.delete(insertedDeadlineNotification);
            insertedDeadlineNotification = null;
        }
    }

    @Test
    @Transactional
    void createDeadlineNotification() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the DeadlineNotification
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);
        var returnedDeadlineNotificationDTO = om.readValue(
            restDeadlineNotificationMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(deadlineNotificationDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DeadlineNotificationDTO.class
        );

        // Validate the DeadlineNotification in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDeadlineNotification = deadlineNotificationMapper.toEntity(returnedDeadlineNotificationDTO);
        assertDeadlineNotificationUpdatableFieldsEquals(
            returnedDeadlineNotification,
            getPersistedDeadlineNotification(returnedDeadlineNotification)
        );

        insertedDeadlineNotification = returnedDeadlineNotification;
    }

    @Test
    @Transactional
    void createDeadlineNotificationWithExistingId() throws Exception {
        // Create the DeadlineNotification with an existing ID
        insertedDeadlineNotification = deadlineNotificationRepository.saveAndFlush(deadlineNotification);
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDeadlineNotificationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DeadlineNotification in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkOperationIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        deadlineNotification.setOperationId(null);

        // Create the DeadlineNotification, which fails.
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);

        restDeadlineNotificationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSeuilIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        deadlineNotification.setSeuil(null);

        // Create the DeadlineNotification, which fails.
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);

        restDeadlineNotificationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateEnvoiIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        deadlineNotification.setDateEnvoi(null);

        // Create the DeadlineNotification, which fails.
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);

        restDeadlineNotificationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDeadlineNotifications() throws Exception {
        // Initialize the database
        insertedDeadlineNotification = deadlineNotificationRepository.saveAndFlush(deadlineNotification);

        // Get all the deadlineNotificationList
        restDeadlineNotificationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(deadlineNotification.getId().toString())))
            .andExpect(jsonPath("$.[*].operationId").value(hasItem(DEFAULT_OPERATION_ID.toString())))
            .andExpect(jsonPath("$.[*].seuil").value(hasItem(DEFAULT_SEUIL)))
            .andExpect(jsonPath("$.[*].dateEnvoi").value(hasItem(DEFAULT_DATE_ENVOI.toString())));
    }

    @Test
    @Transactional
    void getDeadlineNotification() throws Exception {
        // Initialize the database
        insertedDeadlineNotification = deadlineNotificationRepository.saveAndFlush(deadlineNotification);

        // Get the deadlineNotification
        restDeadlineNotificationMockMvc
            .perform(get(ENTITY_API_URL_ID, deadlineNotification.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(deadlineNotification.getId().toString()))
            .andExpect(jsonPath("$.operationId").value(DEFAULT_OPERATION_ID.toString()))
            .andExpect(jsonPath("$.seuil").value(DEFAULT_SEUIL))
            .andExpect(jsonPath("$.dateEnvoi").value(DEFAULT_DATE_ENVOI.toString()));
    }

    @Test
    @Transactional
    void getNonExistingDeadlineNotification() throws Exception {
        // Get the deadlineNotification
        restDeadlineNotificationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDeadlineNotification() throws Exception {
        // Initialize the database
        insertedDeadlineNotification = deadlineNotificationRepository.saveAndFlush(deadlineNotification);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the deadlineNotification
        DeadlineNotification updatedDeadlineNotification = deadlineNotificationRepository
            .findById(deadlineNotification.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedDeadlineNotification are not directly saved in db
        em.detach(updatedDeadlineNotification);
        updatedDeadlineNotification.operationId(UPDATED_OPERATION_ID).seuil(UPDATED_SEUIL).dateEnvoi(UPDATED_DATE_ENVOI);
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(updatedDeadlineNotification);

        restDeadlineNotificationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, deadlineNotificationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isOk());

        // Validate the DeadlineNotification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDeadlineNotificationToMatchAllProperties(updatedDeadlineNotification);
    }

    @Test
    @Transactional
    void putNonExistingDeadlineNotification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        deadlineNotification.setId(UUID.randomUUID());

        // Create the DeadlineNotification
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDeadlineNotificationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, deadlineNotificationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DeadlineNotification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDeadlineNotification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        deadlineNotification.setId(UUID.randomUUID());

        // Create the DeadlineNotification
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDeadlineNotificationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DeadlineNotification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDeadlineNotification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        deadlineNotification.setId(UUID.randomUUID());

        // Create the DeadlineNotification
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDeadlineNotificationMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the DeadlineNotification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDeadlineNotificationWithPatch() throws Exception {
        // Initialize the database
        insertedDeadlineNotification = deadlineNotificationRepository.saveAndFlush(deadlineNotification);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the deadlineNotification using partial update
        DeadlineNotification partialUpdatedDeadlineNotification = new DeadlineNotification();
        partialUpdatedDeadlineNotification.setId(deadlineNotification.getId());

        partialUpdatedDeadlineNotification.seuil(UPDATED_SEUIL).dateEnvoi(UPDATED_DATE_ENVOI);

        restDeadlineNotificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDeadlineNotification.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDeadlineNotification))
            )
            .andExpect(status().isOk());

        // Validate the DeadlineNotification in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDeadlineNotificationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedDeadlineNotification, deadlineNotification),
            getPersistedDeadlineNotification(deadlineNotification)
        );
    }

    @Test
    @Transactional
    void fullUpdateDeadlineNotificationWithPatch() throws Exception {
        // Initialize the database
        insertedDeadlineNotification = deadlineNotificationRepository.saveAndFlush(deadlineNotification);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the deadlineNotification using partial update
        DeadlineNotification partialUpdatedDeadlineNotification = new DeadlineNotification();
        partialUpdatedDeadlineNotification.setId(deadlineNotification.getId());

        partialUpdatedDeadlineNotification.operationId(UPDATED_OPERATION_ID).seuil(UPDATED_SEUIL).dateEnvoi(UPDATED_DATE_ENVOI);

        restDeadlineNotificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDeadlineNotification.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDeadlineNotification))
            )
            .andExpect(status().isOk());

        // Validate the DeadlineNotification in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDeadlineNotificationUpdatableFieldsEquals(
            partialUpdatedDeadlineNotification,
            getPersistedDeadlineNotification(partialUpdatedDeadlineNotification)
        );
    }

    @Test
    @Transactional
    void patchNonExistingDeadlineNotification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        deadlineNotification.setId(UUID.randomUUID());

        // Create the DeadlineNotification
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDeadlineNotificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, deadlineNotificationDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DeadlineNotification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDeadlineNotification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        deadlineNotification.setId(UUID.randomUUID());

        // Create the DeadlineNotification
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDeadlineNotificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DeadlineNotification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDeadlineNotification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        deadlineNotification.setId(UUID.randomUUID());

        // Create the DeadlineNotification
        DeadlineNotificationDTO deadlineNotificationDTO = deadlineNotificationMapper.toDto(deadlineNotification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDeadlineNotificationMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(deadlineNotificationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the DeadlineNotification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDeadlineNotification() throws Exception {
        // Initialize the database
        insertedDeadlineNotification = deadlineNotificationRepository.saveAndFlush(deadlineNotification);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the deadlineNotification
        restDeadlineNotificationMockMvc
            .perform(delete(ENTITY_API_URL_ID, deadlineNotification.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return deadlineNotificationRepository.count();
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

    protected DeadlineNotification getPersistedDeadlineNotification(DeadlineNotification deadlineNotification) {
        return deadlineNotificationRepository.findById(deadlineNotification.getId()).orElseThrow();
    }

    protected void assertPersistedDeadlineNotificationToMatchAllProperties(DeadlineNotification expectedDeadlineNotification) {
        assertDeadlineNotificationAllPropertiesEquals(
            expectedDeadlineNotification,
            getPersistedDeadlineNotification(expectedDeadlineNotification)
        );
    }

    protected void assertPersistedDeadlineNotificationToMatchUpdatableProperties(DeadlineNotification expectedDeadlineNotification) {
        assertDeadlineNotificationAllUpdatablePropertiesEquals(
            expectedDeadlineNotification,
            getPersistedDeadlineNotification(expectedDeadlineNotification)
        );
    }
}
