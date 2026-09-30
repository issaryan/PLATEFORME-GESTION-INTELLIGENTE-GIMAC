package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.QuitusAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.domain.enumeration.EtatQuitus;
import cm.gimac.des.certification.domain.enumeration.NatureQuitus;
import cm.gimac.des.certification.repository.QuitusRepository;
import cm.gimac.des.certification.service.dto.QuitusDTO;
import cm.gimac.des.certification.service.mapper.QuitusMapper;
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
 * Integration tests for the {@link QuitusResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.certification.security.AuthoritiesConstants.ADMIN)
class QuitusResourceIT {

    private static final NatureQuitus DEFAULT_NATURE = NatureQuitus.TECHNIQUE;
    private static final NatureQuitus UPDATED_NATURE = NatureQuitus.SECURITE;

    private static final EtatQuitus DEFAULT_ETAT = EtatQuitus.ATTENDU;
    private static final EtatQuitus UPDATED_ETAT = EtatQuitus.SIGNE_FAVORABLE;

    private static final Instant DEFAULT_DATE_ATTENDU = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_ATTENDU = Instant.ofEpochMilli(1702411306964L);

    private static final String DEFAULT_MOTIF_CADUCITE = "AAAAAAAAAA";
    private static final String UPDATED_MOTIF_CADUCITE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/quituses";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private QuitusRepository quitusRepository;

    @Autowired
    private QuitusMapper quitusMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restQuitusMockMvc;

    private Quitus quitus;

    private Quitus insertedQuitus;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Quitus createEntity(EntityManager em) {
        Quitus quitus = new Quitus()
            .nature(DEFAULT_NATURE)
            .etat(DEFAULT_ETAT)
            .dateAttendu(DEFAULT_DATE_ATTENDU)
            .motifCaducite(DEFAULT_MOTIF_CADUCITE);
        // Add required entity
        CertificationCircuit certificationCircuit;
        if (TestUtil.findAll(em, CertificationCircuit.class).isEmpty()) {
            certificationCircuit = CertificationCircuitResourceIT.createEntity();
            em.persist(certificationCircuit);
            em.flush();
        } else {
            certificationCircuit = TestUtil.findAll(em, CertificationCircuit.class).getFirst();
        }
        quitus.setCircuit(certificationCircuit);
        return quitus;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Quitus createUpdatedEntity(EntityManager em) {
        Quitus updatedQuitus = new Quitus()
            .nature(UPDATED_NATURE)
            .etat(UPDATED_ETAT)
            .dateAttendu(UPDATED_DATE_ATTENDU)
            .motifCaducite(UPDATED_MOTIF_CADUCITE);
        // Add required entity
        CertificationCircuit certificationCircuit;
        if (TestUtil.findAll(em, CertificationCircuit.class).isEmpty()) {
            certificationCircuit = CertificationCircuitResourceIT.createUpdatedEntity();
            em.persist(certificationCircuit);
            em.flush();
        } else {
            certificationCircuit = TestUtil.findAll(em, CertificationCircuit.class).getFirst();
        }
        updatedQuitus.setCircuit(certificationCircuit);
        return updatedQuitus;
    }

    @BeforeEach
    void initTest() {
        quitus = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedQuitus != null) {
            quitusRepository.delete(insertedQuitus);
            insertedQuitus = null;
        }
    }

    @Test
    @Transactional
    void createQuitus() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Quitus
        QuitusDTO quitusDTO = quitusMapper.toDto(quitus);
        var returnedQuitusDTO = om.readValue(
            restQuitusMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(quitusDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            QuitusDTO.class
        );

        // Validate the Quitus in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedQuitus = quitusMapper.toEntity(returnedQuitusDTO);
        assertQuitusUpdatableFieldsEquals(returnedQuitus, getPersistedQuitus(returnedQuitus));

        insertedQuitus = returnedQuitus;
    }

    @Test
    @Transactional
    void createQuitusWithExistingId() throws Exception {
        // Create the Quitus with an existing ID
        insertedQuitus = quitusRepository.saveAndFlush(quitus);
        QuitusDTO quitusDTO = quitusMapper.toDto(quitus);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restQuitusMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(quitusDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Quitus in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNatureIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        quitus.setNature(null);

        // Create the Quitus, which fails.
        QuitusDTO quitusDTO = quitusMapper.toDto(quitus);

        restQuitusMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(quitusDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEtatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        quitus.setEtat(null);

        // Create the Quitus, which fails.
        QuitusDTO quitusDTO = quitusMapper.toDto(quitus);

        restQuitusMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(quitusDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllQuituses() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList
        restQuitusMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(quitus.getId().toString())))
            .andExpect(jsonPath("$.[*].nature").value(hasItem(DEFAULT_NATURE.toString())))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].dateAttendu").value(hasItem(DEFAULT_DATE_ATTENDU.toString())))
            .andExpect(jsonPath("$.[*].motifCaducite").value(hasItem(DEFAULT_MOTIF_CADUCITE)));
    }

    @Test
    @Transactional
    void getQuitus() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get the quitus
        restQuitusMockMvc
            .perform(get(ENTITY_API_URL_ID, quitus.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(quitus.getId().toString()))
            .andExpect(jsonPath("$.nature").value(DEFAULT_NATURE.toString()))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()))
            .andExpect(jsonPath("$.dateAttendu").value(DEFAULT_DATE_ATTENDU.toString()))
            .andExpect(jsonPath("$.motifCaducite").value(DEFAULT_MOTIF_CADUCITE));
    }

    @Test
    @Transactional
    void getNonExistingQuitus() throws Exception {
        // Get the quitus
        restQuitusMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingQuitus() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the quitus
        Quitus updatedQuitus = quitusRepository.findById(quitus.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedQuitus are not directly saved in db
        em.detach(updatedQuitus);
        updatedQuitus.nature(UPDATED_NATURE).etat(UPDATED_ETAT).dateAttendu(UPDATED_DATE_ATTENDU).motifCaducite(UPDATED_MOTIF_CADUCITE);
        QuitusDTO quitusDTO = quitusMapper.toDto(updatedQuitus);

        restQuitusMockMvc
            .perform(
                put(ENTITY_API_URL_ID, quitusDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(quitusDTO))
            )
            .andExpect(status().isOk());

        // Validate the Quitus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedQuitusToMatchAllProperties(updatedQuitus);
    }

    @Test
    @Transactional
    void putNonExistingQuitus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        quitus.setId(UUID.randomUUID());

        // Create the Quitus
        QuitusDTO quitusDTO = quitusMapper.toDto(quitus);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restQuitusMockMvc
            .perform(
                put(ENTITY_API_URL_ID, quitusDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(quitusDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Quitus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchQuitus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        quitus.setId(UUID.randomUUID());

        // Create the Quitus
        QuitusDTO quitusDTO = quitusMapper.toDto(quitus);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQuitusMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(quitusDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Quitus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamQuitus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        quitus.setId(UUID.randomUUID());

        // Create the Quitus
        QuitusDTO quitusDTO = quitusMapper.toDto(quitus);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQuitusMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(quitusDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Quitus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateQuitusWithPatch() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the quitus using partial update
        Quitus partialUpdatedQuitus = new Quitus();
        partialUpdatedQuitus.setId(quitus.getId());

        partialUpdatedQuitus.nature(UPDATED_NATURE).etat(UPDATED_ETAT).dateAttendu(UPDATED_DATE_ATTENDU);

        restQuitusMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedQuitus.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedQuitus))
            )
            .andExpect(status().isOk());

        // Validate the Quitus in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertQuitusUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedQuitus, quitus), getPersistedQuitus(quitus));
    }

    @Test
    @Transactional
    void fullUpdateQuitusWithPatch() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the quitus using partial update
        Quitus partialUpdatedQuitus = new Quitus();
        partialUpdatedQuitus.setId(quitus.getId());

        partialUpdatedQuitus
            .nature(UPDATED_NATURE)
            .etat(UPDATED_ETAT)
            .dateAttendu(UPDATED_DATE_ATTENDU)
            .motifCaducite(UPDATED_MOTIF_CADUCITE);

        restQuitusMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedQuitus.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedQuitus))
            )
            .andExpect(status().isOk());

        // Validate the Quitus in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertQuitusUpdatableFieldsEquals(partialUpdatedQuitus, getPersistedQuitus(partialUpdatedQuitus));
    }

    @Test
    @Transactional
    void patchNonExistingQuitus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        quitus.setId(UUID.randomUUID());

        // Create the Quitus
        QuitusDTO quitusDTO = quitusMapper.toDto(quitus);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restQuitusMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, quitusDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(quitusDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Quitus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchQuitus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        quitus.setId(UUID.randomUUID());

        // Create the Quitus
        QuitusDTO quitusDTO = quitusMapper.toDto(quitus);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQuitusMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(quitusDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Quitus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamQuitus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        quitus.setId(UUID.randomUUID());

        // Create the Quitus
        QuitusDTO quitusDTO = quitusMapper.toDto(quitus);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQuitusMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(quitusDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Quitus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteQuitus() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the quitus
        restQuitusMockMvc
            .perform(delete(ENTITY_API_URL_ID, quitus.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return quitusRepository.count();
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

    protected Quitus getPersistedQuitus(Quitus quitus) {
        return quitusRepository.findById(quitus.getId()).orElseThrow();
    }

    protected void assertPersistedQuitusToMatchAllProperties(Quitus expectedQuitus) {
        assertQuitusAllPropertiesEquals(expectedQuitus, getPersistedQuitus(expectedQuitus));
    }

    protected void assertPersistedQuitusToMatchUpdatableProperties(Quitus expectedQuitus) {
        assertQuitusAllUpdatablePropertiesEquals(expectedQuitus, getPersistedQuitus(expectedQuitus));
    }
}
