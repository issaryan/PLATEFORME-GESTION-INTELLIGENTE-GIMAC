package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.SignedDecisionAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.domain.SignedDecision;
import cm.gimac.des.certification.repository.SignedDecisionRepository;
import cm.gimac.des.certification.service.dto.SignedDecisionDTO;
import cm.gimac.des.certification.service.mapper.SignedDecisionMapper;
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
 * Integration tests for the {@link SignedDecisionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class SignedDecisionResourceIT {

    private static final Boolean DEFAULT_DECISION_FAVORABLE = false;
    private static final Boolean UPDATED_DECISION_FAVORABLE = true;

    private static final String DEFAULT_MOTIF = "AAAAAAAAAA";
    private static final String UPDATED_MOTIF = "BBBBBBBBBB";

    private static final UUID DEFAULT_SIGNATAIRE_ID = UUID.randomUUID();
    private static final UUID UPDATED_SIGNATAIRE_ID = UUID.randomUUID();

    private static final String DEFAULT_SIGNATAIRE_ROLE = "AAAAAAAAAA";
    private static final String UPDATED_SIGNATAIRE_ROLE = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_SIGNATURE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_SIGNATURE = Instant.ofEpochMilli(1702411306964L);

    private static final String DEFAULT_SOCLE_EMPREINTE = "AAAAAAAAAA";
    private static final String UPDATED_SOCLE_EMPREINTE = "BBBBBBBBBB";

    private static final String DEFAULT_EMPREINTE_DECISION = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE_DECISION = "BBBBBBBBBB";

    private static final String DEFAULT_SIGNATURE = "AAAAAAAAAA";
    private static final String UPDATED_SIGNATURE = "BBBBBBBBBB";

    private static final String DEFAULT_OPERATIONS_DESIGNEES = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_OPERATIONS_DESIGNEES = "[\"B\"]";

    private static final String DEFAULT_SERVICES_DESIGNES = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_SERVICES_DESIGNES = "[\"B\"]";

    private static final String DEFAULT_REFERENCE_AUDIT = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE_AUDIT = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/signed-decisions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SignedDecisionRepository signedDecisionRepository;

    @Autowired
    private SignedDecisionMapper signedDecisionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSignedDecisionMockMvc;

    private SignedDecision signedDecision;

    private SignedDecision insertedSignedDecision;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SignedDecision createEntity(EntityManager em) {
        SignedDecision signedDecision = new SignedDecision()
            .decisionFavorable(DEFAULT_DECISION_FAVORABLE)
            .motif(DEFAULT_MOTIF)
            .signataireId(DEFAULT_SIGNATAIRE_ID)
            .signataireRole(DEFAULT_SIGNATAIRE_ROLE)
            .dateSignature(DEFAULT_DATE_SIGNATURE)
            .socleEmpreinte(DEFAULT_SOCLE_EMPREINTE)
            .empreinteDecision(DEFAULT_EMPREINTE_DECISION)
            .signature(DEFAULT_SIGNATURE)
            .operationsDesignees(DEFAULT_OPERATIONS_DESIGNEES)
            .servicesDesignes(DEFAULT_SERVICES_DESIGNES)
            .referenceAudit(DEFAULT_REFERENCE_AUDIT);
        // Add required entity
        Quitus quitus;
        if (TestUtil.findAll(em, Quitus.class).isEmpty()) {
            quitus = QuitusResourceIT.createEntity(em);
            em.persist(quitus);
            em.flush();
        } else {
            quitus = TestUtil.findAll(em, Quitus.class).getFirst();
        }
        signedDecision.setQuitus(quitus);
        return signedDecision;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SignedDecision createUpdatedEntity(EntityManager em) {
        SignedDecision updatedSignedDecision = new SignedDecision()
            .decisionFavorable(UPDATED_DECISION_FAVORABLE)
            .motif(UPDATED_MOTIF)
            .signataireId(UPDATED_SIGNATAIRE_ID)
            .signataireRole(UPDATED_SIGNATAIRE_ROLE)
            .dateSignature(UPDATED_DATE_SIGNATURE)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .empreinteDecision(UPDATED_EMPREINTE_DECISION)
            .signature(UPDATED_SIGNATURE)
            .operationsDesignees(UPDATED_OPERATIONS_DESIGNEES)
            .servicesDesignes(UPDATED_SERVICES_DESIGNES)
            .referenceAudit(UPDATED_REFERENCE_AUDIT);
        // Add required entity
        Quitus quitus;
        if (TestUtil.findAll(em, Quitus.class).isEmpty()) {
            quitus = QuitusResourceIT.createUpdatedEntity(em);
            em.persist(quitus);
            em.flush();
        } else {
            quitus = TestUtil.findAll(em, Quitus.class).getFirst();
        }
        updatedSignedDecision.setQuitus(quitus);
        return updatedSignedDecision;
    }

    @BeforeEach
    void initTest() {
        signedDecision = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSignedDecision != null) {
            signedDecisionRepository.delete(insertedSignedDecision);
            insertedSignedDecision = null;
        }
    }

    @Test
    @Transactional
    void createSignedDecision() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SignedDecision
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);
        var returnedSignedDecisionDTO = om.readValue(
            restSignedDecisionMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(signedDecisionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SignedDecisionDTO.class
        );

        // Validate the SignedDecision in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSignedDecision = signedDecisionMapper.toEntity(returnedSignedDecisionDTO);
        assertSignedDecisionUpdatableFieldsEquals(returnedSignedDecision, getPersistedSignedDecision(returnedSignedDecision));

        insertedSignedDecision = returnedSignedDecision;
    }

    @Test
    @Transactional
    void createSignedDecisionWithExistingId() throws Exception {
        // Create the SignedDecision with an existing ID
        insertedSignedDecision = signedDecisionRepository.saveAndFlush(signedDecision);
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSignedDecisionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SignedDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDecisionFavorableIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        signedDecision.setDecisionFavorable(null);

        // Create the SignedDecision, which fails.
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        restSignedDecisionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMotifIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        signedDecision.setMotif(null);

        // Create the SignedDecision, which fails.
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        restSignedDecisionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSignataireIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        signedDecision.setSignataireId(null);

        // Create the SignedDecision, which fails.
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        restSignedDecisionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateSignatureIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        signedDecision.setDateSignature(null);

        // Create the SignedDecision, which fails.
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        restSignedDecisionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSocleEmpreinteIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        signedDecision.setSocleEmpreinte(null);

        // Create the SignedDecision, which fails.
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        restSignedDecisionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEmpreinteDecisionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        signedDecision.setEmpreinteDecision(null);

        // Create the SignedDecision, which fails.
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        restSignedDecisionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSignedDecisions() throws Exception {
        // Initialize the database
        insertedSignedDecision = signedDecisionRepository.saveAndFlush(signedDecision);

        // Get all the signedDecisionList
        restSignedDecisionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(signedDecision.getId().toString())))
            .andExpect(jsonPath("$.[*].decisionFavorable").value(hasItem(DEFAULT_DECISION_FAVORABLE)))
            .andExpect(jsonPath("$.[*].motif").value(hasItem(DEFAULT_MOTIF)))
            .andExpect(jsonPath("$.[*].signataireId").value(hasItem(DEFAULT_SIGNATAIRE_ID.toString())))
            .andExpect(jsonPath("$.[*].signataireRole").value(hasItem(DEFAULT_SIGNATAIRE_ROLE)))
            .andExpect(jsonPath("$.[*].dateSignature").value(hasItem(DEFAULT_DATE_SIGNATURE.toString())))
            .andExpect(jsonPath("$.[*].socleEmpreinte").value(hasItem(DEFAULT_SOCLE_EMPREINTE)))
            .andExpect(jsonPath("$.[*].empreinteDecision").value(hasItem(DEFAULT_EMPREINTE_DECISION)))
            .andExpect(jsonPath("$.[*].signature").value(hasItem(DEFAULT_SIGNATURE)))
            .andExpect(jsonPath("$.[*].operationsDesignees").value(hasItem(DEFAULT_OPERATIONS_DESIGNEES)))
            .andExpect(jsonPath("$.[*].servicesDesignes").value(hasItem(DEFAULT_SERVICES_DESIGNES)))
            .andExpect(jsonPath("$.[*].referenceAudit").value(hasItem(DEFAULT_REFERENCE_AUDIT)));
    }

    @Test
    @Transactional
    void getSignedDecision() throws Exception {
        // Initialize the database
        insertedSignedDecision = signedDecisionRepository.saveAndFlush(signedDecision);

        // Get the signedDecision
        restSignedDecisionMockMvc
            .perform(get(ENTITY_API_URL_ID, signedDecision.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(signedDecision.getId().toString()))
            .andExpect(jsonPath("$.decisionFavorable").value(DEFAULT_DECISION_FAVORABLE))
            .andExpect(jsonPath("$.motif").value(DEFAULT_MOTIF))
            .andExpect(jsonPath("$.signataireId").value(DEFAULT_SIGNATAIRE_ID.toString()))
            .andExpect(jsonPath("$.signataireRole").value(DEFAULT_SIGNATAIRE_ROLE))
            .andExpect(jsonPath("$.dateSignature").value(DEFAULT_DATE_SIGNATURE.toString()))
            .andExpect(jsonPath("$.socleEmpreinte").value(DEFAULT_SOCLE_EMPREINTE))
            .andExpect(jsonPath("$.empreinteDecision").value(DEFAULT_EMPREINTE_DECISION))
            .andExpect(jsonPath("$.signature").value(DEFAULT_SIGNATURE))
            .andExpect(jsonPath("$.operationsDesignees").value(DEFAULT_OPERATIONS_DESIGNEES))
            .andExpect(jsonPath("$.servicesDesignes").value(DEFAULT_SERVICES_DESIGNES))
            .andExpect(jsonPath("$.referenceAudit").value(DEFAULT_REFERENCE_AUDIT));
    }

    @Test
    @Transactional
    void getNonExistingSignedDecision() throws Exception {
        // Get the signedDecision
        restSignedDecisionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSignedDecision() throws Exception {
        // Initialize the database
        insertedSignedDecision = signedDecisionRepository.saveAndFlush(signedDecision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the signedDecision
        SignedDecision updatedSignedDecision = signedDecisionRepository.findById(signedDecision.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSignedDecision are not directly saved in db
        em.detach(updatedSignedDecision);
        updatedSignedDecision
            .decisionFavorable(UPDATED_DECISION_FAVORABLE)
            .motif(UPDATED_MOTIF)
            .signataireId(UPDATED_SIGNATAIRE_ID)
            .signataireRole(UPDATED_SIGNATAIRE_ROLE)
            .dateSignature(UPDATED_DATE_SIGNATURE)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .empreinteDecision(UPDATED_EMPREINTE_DECISION)
            .signature(UPDATED_SIGNATURE)
            .operationsDesignees(UPDATED_OPERATIONS_DESIGNEES)
            .servicesDesignes(UPDATED_SERVICES_DESIGNES)
            .referenceAudit(UPDATED_REFERENCE_AUDIT);
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(updatedSignedDecision);

        restSignedDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, signedDecisionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isConflict());

        // Décision signée immuable (PI §0.3, D-35) : la modification est refusée, la base est inchangée.
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertThat(getPersistedSignedDecision(signedDecision).getMotif()).isEqualTo(DEFAULT_MOTIF);
    }

    @Test
    @Transactional
    void putNonExistingSignedDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        signedDecision.setId(UUID.randomUUID());

        // Create the SignedDecision
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSignedDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, signedDecisionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SignedDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSignedDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        signedDecision.setId(UUID.randomUUID());

        // Create the SignedDecision
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSignedDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SignedDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSignedDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        signedDecision.setId(UUID.randomUUID());

        // Create the SignedDecision
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSignedDecisionMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SignedDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSignedDecisionWithPatch() throws Exception {
        // Initialize the database
        insertedSignedDecision = signedDecisionRepository.saveAndFlush(signedDecision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the signedDecision using partial update
        SignedDecision partialUpdatedSignedDecision = new SignedDecision();
        partialUpdatedSignedDecision.setId(signedDecision.getId());

        partialUpdatedSignedDecision
            .motif(UPDATED_MOTIF)
            .signataireId(UPDATED_SIGNATAIRE_ID)
            .signataireRole(UPDATED_SIGNATAIRE_ROLE)
            .signature(UPDATED_SIGNATURE)
            .operationsDesignees(UPDATED_OPERATIONS_DESIGNEES)
            .servicesDesignes(UPDATED_SERVICES_DESIGNES)
            .referenceAudit(UPDATED_REFERENCE_AUDIT);

        restSignedDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSignedDecision.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSignedDecision))
            )
            .andExpect(status().isConflict());

        // Décision signée immuable (PI §0.3, D-35) : la modification est refusée, la base est inchangée.
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertThat(getPersistedSignedDecision(signedDecision).getMotif()).isEqualTo(DEFAULT_MOTIF);
    }

    @Test
    @Transactional
    void fullUpdateSignedDecisionWithPatch() throws Exception {
        // Initialize the database
        insertedSignedDecision = signedDecisionRepository.saveAndFlush(signedDecision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the signedDecision using partial update
        SignedDecision partialUpdatedSignedDecision = new SignedDecision();
        partialUpdatedSignedDecision.setId(signedDecision.getId());

        partialUpdatedSignedDecision
            .decisionFavorable(UPDATED_DECISION_FAVORABLE)
            .motif(UPDATED_MOTIF)
            .signataireId(UPDATED_SIGNATAIRE_ID)
            .signataireRole(UPDATED_SIGNATAIRE_ROLE)
            .dateSignature(UPDATED_DATE_SIGNATURE)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .empreinteDecision(UPDATED_EMPREINTE_DECISION)
            .signature(UPDATED_SIGNATURE)
            .operationsDesignees(UPDATED_OPERATIONS_DESIGNEES)
            .servicesDesignes(UPDATED_SERVICES_DESIGNES)
            .referenceAudit(UPDATED_REFERENCE_AUDIT);

        restSignedDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSignedDecision.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSignedDecision))
            )
            .andExpect(status().isConflict());

        // Décision signée immuable (PI §0.3, D-35) : la modification est refusée, la base est inchangée.
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertThat(getPersistedSignedDecision(signedDecision).getMotif()).isEqualTo(DEFAULT_MOTIF);
    }

    @Test
    @Transactional
    void patchNonExistingSignedDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        signedDecision.setId(UUID.randomUUID());

        // Create the SignedDecision
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSignedDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, signedDecisionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SignedDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSignedDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        signedDecision.setId(UUID.randomUUID());

        // Create the SignedDecision
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSignedDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SignedDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSignedDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        signedDecision.setId(UUID.randomUUID());

        // Create the SignedDecision
        SignedDecisionDTO signedDecisionDTO = signedDecisionMapper.toDto(signedDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSignedDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(signedDecisionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SignedDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSignedDecision() throws Exception {
        // Initialize the database
        insertedSignedDecision = signedDecisionRepository.saveAndFlush(signedDecision);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the signedDecision
        restSignedDecisionMockMvc
            .perform(delete(ENTITY_API_URL_ID, signedDecision.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return signedDecisionRepository.count();
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

    protected SignedDecision getPersistedSignedDecision(SignedDecision signedDecision) {
        return signedDecisionRepository.findById(signedDecision.getId()).orElseThrow();
    }

    protected void assertPersistedSignedDecisionToMatchAllProperties(SignedDecision expectedSignedDecision) {
        assertSignedDecisionAllPropertiesEquals(expectedSignedDecision, getPersistedSignedDecision(expectedSignedDecision));
    }

    protected void assertPersistedSignedDecisionToMatchUpdatableProperties(SignedDecision expectedSignedDecision) {
        assertSignedDecisionAllUpdatablePropertiesEquals(expectedSignedDecision, getPersistedSignedDecision(expectedSignedDecision));
    }
}
