package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.UnmatchedScenarioAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.UnmatchedScenario;
import cm.gimac.des.homologation.domain.enumeration.QualificationScenario;
import cm.gimac.des.homologation.repository.UnmatchedScenarioRepository;
import cm.gimac.des.homologation.service.dto.UnmatchedScenarioDTO;
import cm.gimac.des.homologation.service.mapper.UnmatchedScenarioMapper;
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
 * Integration tests for the {@link UnmatchedScenarioResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class UnmatchedScenarioResourceIT {

    private static final String DEFAULT_ID_SCENARIO = "AAAAAAAAAA";
    private static final String UPDATED_ID_SCENARIO = "BBBBBBBBBB";

    private static final String DEFAULT_CAMPAGNE_ID = "AAAAAAAAAA";
    private static final String UPDATED_CAMPAGNE_ID = "BBBBBBBBBB";

    private static final QualificationScenario DEFAULT_QUALIFICATION = QualificationScenario.A_QUALIFIER;
    private static final QualificationScenario UPDATED_QUALIFICATION = QualificationScenario.A_DECLARER_AU_REFERENTIEL;

    private static final String DEFAULT_MOTIF = "AAAAAAAAAA";
    private static final String UPDATED_MOTIF = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_DETECTION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_DETECTION = Instant.ofEpochMilli(1701958935371L);

    private static final String ENTITY_API_URL = "/api/unmatched-scenarios";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UnmatchedScenarioRepository unmatchedScenarioRepository;

    @Autowired
    private UnmatchedScenarioMapper unmatchedScenarioMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restUnmatchedScenarioMockMvc;

    private UnmatchedScenario unmatchedScenario;

    private UnmatchedScenario insertedUnmatchedScenario;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UnmatchedScenario createEntity(EntityManager em) {
        UnmatchedScenario unmatchedScenario = new UnmatchedScenario()
            .idScenario(DEFAULT_ID_SCENARIO)
            .campagneId(DEFAULT_CAMPAGNE_ID)
            .qualification(DEFAULT_QUALIFICATION)
            .motif(DEFAULT_MOTIF)
            .dateDetection(DEFAULT_DATE_DETECTION);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        unmatchedScenario.setLiaison(liaison);
        return unmatchedScenario;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UnmatchedScenario createUpdatedEntity(EntityManager em) {
        UnmatchedScenario updatedUnmatchedScenario = new UnmatchedScenario()
            .idScenario(UPDATED_ID_SCENARIO)
            .campagneId(UPDATED_CAMPAGNE_ID)
            .qualification(UPDATED_QUALIFICATION)
            .motif(UPDATED_MOTIF)
            .dateDetection(UPDATED_DATE_DETECTION);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        updatedUnmatchedScenario.setLiaison(liaison);
        return updatedUnmatchedScenario;
    }

    @BeforeEach
    void initTest() {
        unmatchedScenario = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedUnmatchedScenario != null) {
            unmatchedScenarioRepository.delete(insertedUnmatchedScenario);
            insertedUnmatchedScenario = null;
        }
    }

    @Test
    @Transactional
    void createUnmatchedScenario() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the UnmatchedScenario
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);
        var returnedUnmatchedScenarioDTO = om.readValue(
            restUnmatchedScenarioMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(unmatchedScenarioDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            UnmatchedScenarioDTO.class
        );

        // Validate the UnmatchedScenario in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedUnmatchedScenario = unmatchedScenarioMapper.toEntity(returnedUnmatchedScenarioDTO);
        assertUnmatchedScenarioUpdatableFieldsEquals(returnedUnmatchedScenario, getPersistedUnmatchedScenario(returnedUnmatchedScenario));

        insertedUnmatchedScenario = returnedUnmatchedScenario;
    }

    @Test
    @Transactional
    void createUnmatchedScenarioWithExistingId() throws Exception {
        // Create the UnmatchedScenario with an existing ID
        insertedUnmatchedScenario = unmatchedScenarioRepository.saveAndFlush(unmatchedScenario);
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restUnmatchedScenarioMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UnmatchedScenario in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkIdScenarioIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        unmatchedScenario.setIdScenario(null);

        // Create the UnmatchedScenario, which fails.
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);

        restUnmatchedScenarioMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkQualificationIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        unmatchedScenario.setQualification(null);

        // Create the UnmatchedScenario, which fails.
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);

        restUnmatchedScenarioMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateDetectionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        unmatchedScenario.setDateDetection(null);

        // Create the UnmatchedScenario, which fails.
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);

        restUnmatchedScenarioMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllUnmatchedScenarios() throws Exception {
        // Initialize the database
        insertedUnmatchedScenario = unmatchedScenarioRepository.saveAndFlush(unmatchedScenario);

        // Get all the unmatchedScenarioList
        restUnmatchedScenarioMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(unmatchedScenario.getId().toString())))
            .andExpect(jsonPath("$.[*].idScenario").value(hasItem(DEFAULT_ID_SCENARIO)))
            .andExpect(jsonPath("$.[*].campagneId").value(hasItem(DEFAULT_CAMPAGNE_ID)))
            .andExpect(jsonPath("$.[*].qualification").value(hasItem(DEFAULT_QUALIFICATION.toString())))
            .andExpect(jsonPath("$.[*].motif").value(hasItem(DEFAULT_MOTIF)))
            .andExpect(jsonPath("$.[*].dateDetection").value(hasItem(DEFAULT_DATE_DETECTION.toString())));
    }

    @Test
    @Transactional
    void getUnmatchedScenario() throws Exception {
        // Initialize the database
        insertedUnmatchedScenario = unmatchedScenarioRepository.saveAndFlush(unmatchedScenario);

        // Get the unmatchedScenario
        restUnmatchedScenarioMockMvc
            .perform(get(ENTITY_API_URL_ID, unmatchedScenario.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(unmatchedScenario.getId().toString()))
            .andExpect(jsonPath("$.idScenario").value(DEFAULT_ID_SCENARIO))
            .andExpect(jsonPath("$.campagneId").value(DEFAULT_CAMPAGNE_ID))
            .andExpect(jsonPath("$.qualification").value(DEFAULT_QUALIFICATION.toString()))
            .andExpect(jsonPath("$.motif").value(DEFAULT_MOTIF))
            .andExpect(jsonPath("$.dateDetection").value(DEFAULT_DATE_DETECTION.toString()));
    }

    @Test
    @Transactional
    void getNonExistingUnmatchedScenario() throws Exception {
        // Get the unmatchedScenario
        restUnmatchedScenarioMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingUnmatchedScenario() throws Exception {
        // Initialize the database
        insertedUnmatchedScenario = unmatchedScenarioRepository.saveAndFlush(unmatchedScenario);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the unmatchedScenario
        UnmatchedScenario updatedUnmatchedScenario = unmatchedScenarioRepository.findById(unmatchedScenario.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedUnmatchedScenario are not directly saved in db
        em.detach(updatedUnmatchedScenario);
        updatedUnmatchedScenario
            .idScenario(UPDATED_ID_SCENARIO)
            .campagneId(UPDATED_CAMPAGNE_ID)
            .qualification(UPDATED_QUALIFICATION)
            .motif(UPDATED_MOTIF)
            .dateDetection(UPDATED_DATE_DETECTION);
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(updatedUnmatchedScenario);

        restUnmatchedScenarioMockMvc
            .perform(
                put(ENTITY_API_URL_ID, unmatchedScenarioDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isOk());

        // Validate the UnmatchedScenario in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedUnmatchedScenarioToMatchAllProperties(updatedUnmatchedScenario);
    }

    @Test
    @Transactional
    void putNonExistingUnmatchedScenario() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        unmatchedScenario.setId(UUID.randomUUID());

        // Create the UnmatchedScenario
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUnmatchedScenarioMockMvc
            .perform(
                put(ENTITY_API_URL_ID, unmatchedScenarioDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UnmatchedScenario in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchUnmatchedScenario() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        unmatchedScenario.setId(UUID.randomUUID());

        // Create the UnmatchedScenario
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUnmatchedScenarioMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UnmatchedScenario in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamUnmatchedScenario() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        unmatchedScenario.setId(UUID.randomUUID());

        // Create the UnmatchedScenario
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUnmatchedScenarioMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the UnmatchedScenario in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateUnmatchedScenarioWithPatch() throws Exception {
        // Initialize the database
        insertedUnmatchedScenario = unmatchedScenarioRepository.saveAndFlush(unmatchedScenario);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the unmatchedScenario using partial update
        UnmatchedScenario partialUpdatedUnmatchedScenario = new UnmatchedScenario();
        partialUpdatedUnmatchedScenario.setId(unmatchedScenario.getId());

        partialUpdatedUnmatchedScenario
            .idScenario(UPDATED_ID_SCENARIO)
            .qualification(UPDATED_QUALIFICATION)
            .dateDetection(UPDATED_DATE_DETECTION);

        restUnmatchedScenarioMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedUnmatchedScenario.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedUnmatchedScenario))
            )
            .andExpect(status().isOk());

        // Validate the UnmatchedScenario in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertUnmatchedScenarioUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedUnmatchedScenario, unmatchedScenario),
            getPersistedUnmatchedScenario(unmatchedScenario)
        );
    }

    @Test
    @Transactional
    void fullUpdateUnmatchedScenarioWithPatch() throws Exception {
        // Initialize the database
        insertedUnmatchedScenario = unmatchedScenarioRepository.saveAndFlush(unmatchedScenario);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the unmatchedScenario using partial update
        UnmatchedScenario partialUpdatedUnmatchedScenario = new UnmatchedScenario();
        partialUpdatedUnmatchedScenario.setId(unmatchedScenario.getId());

        partialUpdatedUnmatchedScenario
            .idScenario(UPDATED_ID_SCENARIO)
            .campagneId(UPDATED_CAMPAGNE_ID)
            .qualification(UPDATED_QUALIFICATION)
            .motif(UPDATED_MOTIF)
            .dateDetection(UPDATED_DATE_DETECTION);

        restUnmatchedScenarioMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedUnmatchedScenario.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedUnmatchedScenario))
            )
            .andExpect(status().isOk());

        // Validate the UnmatchedScenario in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertUnmatchedScenarioUpdatableFieldsEquals(
            partialUpdatedUnmatchedScenario,
            getPersistedUnmatchedScenario(partialUpdatedUnmatchedScenario)
        );
    }

    @Test
    @Transactional
    void patchNonExistingUnmatchedScenario() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        unmatchedScenario.setId(UUID.randomUUID());

        // Create the UnmatchedScenario
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUnmatchedScenarioMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, unmatchedScenarioDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UnmatchedScenario in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchUnmatchedScenario() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        unmatchedScenario.setId(UUID.randomUUID());

        // Create the UnmatchedScenario
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUnmatchedScenarioMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UnmatchedScenario in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamUnmatchedScenario() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        unmatchedScenario.setId(UUID.randomUUID());

        // Create the UnmatchedScenario
        UnmatchedScenarioDTO unmatchedScenarioDTO = unmatchedScenarioMapper.toDto(unmatchedScenario);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUnmatchedScenarioMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(unmatchedScenarioDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the UnmatchedScenario in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteUnmatchedScenario() throws Exception {
        // Initialize the database
        insertedUnmatchedScenario = unmatchedScenarioRepository.saveAndFlush(unmatchedScenario);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the unmatchedScenario
        restUnmatchedScenarioMockMvc
            .perform(delete(ENTITY_API_URL_ID, unmatchedScenario.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return unmatchedScenarioRepository.count();
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

    protected UnmatchedScenario getPersistedUnmatchedScenario(UnmatchedScenario unmatchedScenario) {
        return unmatchedScenarioRepository.findById(unmatchedScenario.getId()).orElseThrow();
    }

    protected void assertPersistedUnmatchedScenarioToMatchAllProperties(UnmatchedScenario expectedUnmatchedScenario) {
        assertUnmatchedScenarioAllPropertiesEquals(expectedUnmatchedScenario, getPersistedUnmatchedScenario(expectedUnmatchedScenario));
    }

    protected void assertPersistedUnmatchedScenarioToMatchUpdatableProperties(UnmatchedScenario expectedUnmatchedScenario) {
        assertUnmatchedScenarioAllUpdatablePropertiesEquals(
            expectedUnmatchedScenario,
            getPersistedUnmatchedScenario(expectedUnmatchedScenario)
        );
    }
}
