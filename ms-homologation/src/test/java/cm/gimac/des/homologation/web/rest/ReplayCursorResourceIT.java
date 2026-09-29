package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.ReplayCursorAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.ReplayCursor;
import cm.gimac.des.homologation.domain.enumeration.EtatDisjoncteur;
import cm.gimac.des.homologation.repository.ReplayCursorRepository;
import cm.gimac.des.homologation.service.dto.ReplayCursorDTO;
import cm.gimac.des.homologation.service.mapper.ReplayCursorMapper;
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
 * Integration tests for the {@link ReplayCursorResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.homologation.security.AuthoritiesConstants.ADMIN)
class ReplayCursorResourceIT {

    private static final String DEFAULT_CAMPAGNE_ID = "AAAAAAAAAA";
    private static final String UPDATED_CAMPAGNE_ID = "BBBBBBBBBB";

    private static final UUID DEFAULT_LIAISON_ID = UUID.randomUUID();
    private static final UUID UPDATED_LIAISON_ID = UUID.randomUUID();

    private static final String DEFAULT_POSITION = "AAAAAAAAAA";
    private static final String UPDATED_POSITION = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_DERNIER_SUCCES = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_DERNIER_SUCCES = Instant.ofEpochMilli(1701958935371L);

    private static final Integer DEFAULT_ECHECS_CONSECUTIFS = 1;
    private static final Integer UPDATED_ECHECS_CONSECUTIFS = 2;

    private static final EtatDisjoncteur DEFAULT_ETAT_DISJONCTEUR = EtatDisjoncteur.FERME;
    private static final EtatDisjoncteur UPDATED_ETAT_DISJONCTEUR = EtatDisjoncteur.OUVERT;

    private static final String ENTITY_API_URL = "/api/replay-cursors";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReplayCursorRepository replayCursorRepository;

    @Autowired
    private ReplayCursorMapper replayCursorMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReplayCursorMockMvc;

    private ReplayCursor replayCursor;

    private ReplayCursor insertedReplayCursor;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReplayCursor createEntity() {
        return new ReplayCursor()
            .campagneId(DEFAULT_CAMPAGNE_ID)
            .liaisonId(DEFAULT_LIAISON_ID)
            .position(DEFAULT_POSITION)
            .dateDernierSucces(DEFAULT_DATE_DERNIER_SUCCES)
            .echecsConsecutifs(DEFAULT_ECHECS_CONSECUTIFS)
            .etatDisjoncteur(DEFAULT_ETAT_DISJONCTEUR);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReplayCursor createUpdatedEntity() {
        return new ReplayCursor()
            .campagneId(UPDATED_CAMPAGNE_ID)
            .liaisonId(UPDATED_LIAISON_ID)
            .position(UPDATED_POSITION)
            .dateDernierSucces(UPDATED_DATE_DERNIER_SUCCES)
            .echecsConsecutifs(UPDATED_ECHECS_CONSECUTIFS)
            .etatDisjoncteur(UPDATED_ETAT_DISJONCTEUR);
    }

    @BeforeEach
    void initTest() {
        replayCursor = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedReplayCursor != null) {
            replayCursorRepository.delete(insertedReplayCursor);
            insertedReplayCursor = null;
        }
    }

    @Test
    @Transactional
    void createReplayCursor() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ReplayCursor
        ReplayCursorDTO replayCursorDTO = replayCursorMapper.toDto(replayCursor);
        var returnedReplayCursorDTO = om.readValue(
            restReplayCursorMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(replayCursorDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ReplayCursorDTO.class
        );

        // Validate the ReplayCursor in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedReplayCursor = replayCursorMapper.toEntity(returnedReplayCursorDTO);
        assertReplayCursorUpdatableFieldsEquals(returnedReplayCursor, getPersistedReplayCursor(returnedReplayCursor));

        insertedReplayCursor = returnedReplayCursor;
    }

    @Test
    @Transactional
    void createReplayCursorWithExistingId() throws Exception {
        // Create the ReplayCursor with an existing ID
        insertedReplayCursor = replayCursorRepository.saveAndFlush(replayCursor);
        ReplayCursorDTO replayCursorDTO = replayCursorMapper.toDto(replayCursor);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReplayCursorMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(replayCursorDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReplayCursor in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllReplayCursors() throws Exception {
        // Initialize the database
        insertedReplayCursor = replayCursorRepository.saveAndFlush(replayCursor);

        // Get all the replayCursorList
        restReplayCursorMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(replayCursor.getId().toString())))
            .andExpect(jsonPath("$.[*].campagneId").value(hasItem(DEFAULT_CAMPAGNE_ID)))
            .andExpect(jsonPath("$.[*].liaisonId").value(hasItem(DEFAULT_LIAISON_ID.toString())))
            .andExpect(jsonPath("$.[*].position").value(hasItem(DEFAULT_POSITION)))
            .andExpect(jsonPath("$.[*].dateDernierSucces").value(hasItem(DEFAULT_DATE_DERNIER_SUCCES.toString())))
            .andExpect(jsonPath("$.[*].echecsConsecutifs").value(hasItem(DEFAULT_ECHECS_CONSECUTIFS)))
            .andExpect(jsonPath("$.[*].etatDisjoncteur").value(hasItem(DEFAULT_ETAT_DISJONCTEUR.toString())));
    }

    @Test
    @Transactional
    void getReplayCursor() throws Exception {
        // Initialize the database
        insertedReplayCursor = replayCursorRepository.saveAndFlush(replayCursor);

        // Get the replayCursor
        restReplayCursorMockMvc
            .perform(get(ENTITY_API_URL_ID, replayCursor.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(replayCursor.getId().toString()))
            .andExpect(jsonPath("$.campagneId").value(DEFAULT_CAMPAGNE_ID))
            .andExpect(jsonPath("$.liaisonId").value(DEFAULT_LIAISON_ID.toString()))
            .andExpect(jsonPath("$.position").value(DEFAULT_POSITION))
            .andExpect(jsonPath("$.dateDernierSucces").value(DEFAULT_DATE_DERNIER_SUCCES.toString()))
            .andExpect(jsonPath("$.echecsConsecutifs").value(DEFAULT_ECHECS_CONSECUTIFS))
            .andExpect(jsonPath("$.etatDisjoncteur").value(DEFAULT_ETAT_DISJONCTEUR.toString()));
    }

    @Test
    @Transactional
    void getNonExistingReplayCursor() throws Exception {
        // Get the replayCursor
        restReplayCursorMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReplayCursor() throws Exception {
        // Initialize the database
        insertedReplayCursor = replayCursorRepository.saveAndFlush(replayCursor);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the replayCursor
        ReplayCursor updatedReplayCursor = replayCursorRepository.findById(replayCursor.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReplayCursor are not directly saved in db
        em.detach(updatedReplayCursor);
        updatedReplayCursor
            .campagneId(UPDATED_CAMPAGNE_ID)
            .liaisonId(UPDATED_LIAISON_ID)
            .position(UPDATED_POSITION)
            .dateDernierSucces(UPDATED_DATE_DERNIER_SUCCES)
            .echecsConsecutifs(UPDATED_ECHECS_CONSECUTIFS)
            .etatDisjoncteur(UPDATED_ETAT_DISJONCTEUR);
        ReplayCursorDTO replayCursorDTO = replayCursorMapper.toDto(updatedReplayCursor);

        restReplayCursorMockMvc
            .perform(
                put(ENTITY_API_URL_ID, replayCursorDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(replayCursorDTO))
            )
            .andExpect(status().isOk());

        // Validate the ReplayCursor in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReplayCursorToMatchAllProperties(updatedReplayCursor);
    }

    @Test
    @Transactional
    void putNonExistingReplayCursor() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        replayCursor.setId(UUID.randomUUID());

        // Create the ReplayCursor
        ReplayCursorDTO replayCursorDTO = replayCursorMapper.toDto(replayCursor);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReplayCursorMockMvc
            .perform(
                put(ENTITY_API_URL_ID, replayCursorDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(replayCursorDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReplayCursor in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReplayCursor() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        replayCursor.setId(UUID.randomUUID());

        // Create the ReplayCursor
        ReplayCursorDTO replayCursorDTO = replayCursorMapper.toDto(replayCursor);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReplayCursorMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(replayCursorDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReplayCursor in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReplayCursor() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        replayCursor.setId(UUID.randomUUID());

        // Create the ReplayCursor
        ReplayCursorDTO replayCursorDTO = replayCursorMapper.toDto(replayCursor);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReplayCursorMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(replayCursorDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReplayCursor in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReplayCursorWithPatch() throws Exception {
        // Initialize the database
        insertedReplayCursor = replayCursorRepository.saveAndFlush(replayCursor);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the replayCursor using partial update
        ReplayCursor partialUpdatedReplayCursor = new ReplayCursor();
        partialUpdatedReplayCursor.setId(replayCursor.getId());

        partialUpdatedReplayCursor.liaisonId(UPDATED_LIAISON_ID).echecsConsecutifs(UPDATED_ECHECS_CONSECUTIFS);

        restReplayCursorMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReplayCursor.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReplayCursor))
            )
            .andExpect(status().isOk());

        // Validate the ReplayCursor in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReplayCursorUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedReplayCursor, replayCursor),
            getPersistedReplayCursor(replayCursor)
        );
    }

    @Test
    @Transactional
    void fullUpdateReplayCursorWithPatch() throws Exception {
        // Initialize the database
        insertedReplayCursor = replayCursorRepository.saveAndFlush(replayCursor);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the replayCursor using partial update
        ReplayCursor partialUpdatedReplayCursor = new ReplayCursor();
        partialUpdatedReplayCursor.setId(replayCursor.getId());

        partialUpdatedReplayCursor
            .campagneId(UPDATED_CAMPAGNE_ID)
            .liaisonId(UPDATED_LIAISON_ID)
            .position(UPDATED_POSITION)
            .dateDernierSucces(UPDATED_DATE_DERNIER_SUCCES)
            .echecsConsecutifs(UPDATED_ECHECS_CONSECUTIFS)
            .etatDisjoncteur(UPDATED_ETAT_DISJONCTEUR);

        restReplayCursorMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReplayCursor.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReplayCursor))
            )
            .andExpect(status().isOk());

        // Validate the ReplayCursor in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReplayCursorUpdatableFieldsEquals(partialUpdatedReplayCursor, getPersistedReplayCursor(partialUpdatedReplayCursor));
    }

    @Test
    @Transactional
    void patchNonExistingReplayCursor() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        replayCursor.setId(UUID.randomUUID());

        // Create the ReplayCursor
        ReplayCursorDTO replayCursorDTO = replayCursorMapper.toDto(replayCursor);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReplayCursorMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, replayCursorDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(replayCursorDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReplayCursor in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReplayCursor() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        replayCursor.setId(UUID.randomUUID());

        // Create the ReplayCursor
        ReplayCursorDTO replayCursorDTO = replayCursorMapper.toDto(replayCursor);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReplayCursorMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(replayCursorDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReplayCursor in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReplayCursor() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        replayCursor.setId(UUID.randomUUID());

        // Create the ReplayCursor
        ReplayCursorDTO replayCursorDTO = replayCursorMapper.toDto(replayCursor);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReplayCursorMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(replayCursorDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReplayCursor in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReplayCursor() throws Exception {
        // Initialize the database
        insertedReplayCursor = replayCursorRepository.saveAndFlush(replayCursor);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the replayCursor
        restReplayCursorMockMvc
            .perform(delete(ENTITY_API_URL_ID, replayCursor.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return replayCursorRepository.count();
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

    protected ReplayCursor getPersistedReplayCursor(ReplayCursor replayCursor) {
        return replayCursorRepository.findById(replayCursor.getId()).orElseThrow();
    }

    protected void assertPersistedReplayCursorToMatchAllProperties(ReplayCursor expectedReplayCursor) {
        assertReplayCursorAllPropertiesEquals(expectedReplayCursor, getPersistedReplayCursor(expectedReplayCursor));
    }

    protected void assertPersistedReplayCursorToMatchUpdatableProperties(ReplayCursor expectedReplayCursor) {
        assertReplayCursorAllUpdatablePropertiesEquals(expectedReplayCursor, getPersistedReplayCursor(expectedReplayCursor));
    }
}
