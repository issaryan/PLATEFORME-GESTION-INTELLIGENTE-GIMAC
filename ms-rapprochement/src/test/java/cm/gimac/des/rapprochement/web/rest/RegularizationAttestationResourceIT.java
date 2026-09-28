package cm.gimac.des.rapprochement.web.rest;

import static cm.gimac.des.rapprochement.domain.RegularizationAttestationAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.rapprochement.IntegrationTest;
import cm.gimac.des.rapprochement.domain.RegularizationAttestation;
import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.domain.enumeration.AttestationKind;
import cm.gimac.des.rapprochement.repository.RegularizationAttestationRepository;
import cm.gimac.des.rapprochement.service.RegularizationAttestationService;
import cm.gimac.des.rapprochement.service.mapper.RegularizationAttestationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link RegularizationAttestationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RegularizationAttestationResourceIT {

    private static final AttestationKind DEFAULT_KIND = AttestationKind.REGULARISATION;
    private static final AttestationKind UPDATED_KIND = AttestationKind.NON_ECART;

    private static final UUID DEFAULT_DOCUMENT_REF = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_REF = UUID.randomUUID();

    private static final String DEFAULT_DOCUMENT_DIGEST = "26fac088624b8553977e667cf9a39463ea342679c5eaef92545b78d14af436f6";
    private static final String UPDATED_DOCUMENT_DIGEST = "295a7f160a2f77c3670f194327c711067e807c032f63e5a253bcad09cf26834e";

    private static final UUID DEFAULT_DEPOSITOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_DEPOSITOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_DEPOSITED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DEPOSITED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_VALIDATOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_VALIDATOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_VALIDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_VALIDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/regularization-attestations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RegularizationAttestationRepository regularizationAttestationRepository;

    @Mock
    private RegularizationAttestationRepository regularizationAttestationRepositoryMock;

    @Autowired
    private RegularizationAttestationMapper regularizationAttestationMapper;

    @Mock
    private RegularizationAttestationService regularizationAttestationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRegularizationAttestationMockMvc;

    private RegularizationAttestation regularizationAttestation;

    private RegularizationAttestation insertedRegularizationAttestation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RegularizationAttestation createEntity(EntityManager em) {
        RegularizationAttestation regularizationAttestation = new RegularizationAttestation()
            .kind(DEFAULT_KIND)
            .documentRef(DEFAULT_DOCUMENT_REF)
            .documentDigest(DEFAULT_DOCUMENT_DIGEST)
            .depositorId(DEFAULT_DEPOSITOR_ID)
            .depositedAt(DEFAULT_DEPOSITED_AT)
            .validatorId(DEFAULT_VALIDATOR_ID)
            .validatedAt(DEFAULT_VALIDATED_AT);
        // Add required entity
        Suspens suspens;
        if (TestUtil.findAll(em, Suspens.class).isEmpty()) {
            suspens = SuspensResourceIT.createEntity(em);
            em.persist(suspens);
            em.flush();
        } else {
            suspens = TestUtil.findAll(em, Suspens.class).get(0);
        }
        regularizationAttestation.setSuspens(suspens);
        return regularizationAttestation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RegularizationAttestation createUpdatedEntity(EntityManager em) {
        RegularizationAttestation updatedRegularizationAttestation = new RegularizationAttestation()
            .kind(UPDATED_KIND)
            .documentRef(UPDATED_DOCUMENT_REF)
            .documentDigest(UPDATED_DOCUMENT_DIGEST)
            .depositorId(UPDATED_DEPOSITOR_ID)
            .depositedAt(UPDATED_DEPOSITED_AT)
            .validatorId(UPDATED_VALIDATOR_ID)
            .validatedAt(UPDATED_VALIDATED_AT);
        // Add required entity
        Suspens suspens;
        if (TestUtil.findAll(em, Suspens.class).isEmpty()) {
            suspens = SuspensResourceIT.createUpdatedEntity(em);
            em.persist(suspens);
            em.flush();
        } else {
            suspens = TestUtil.findAll(em, Suspens.class).get(0);
        }
        updatedRegularizationAttestation.setSuspens(suspens);
        return updatedRegularizationAttestation;
    }

    @BeforeEach
    void initTest() {
        regularizationAttestation = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedRegularizationAttestation != null) {
            regularizationAttestationRepository.delete(insertedRegularizationAttestation);
            insertedRegularizationAttestation = null;
        }
    }

    @Test
    @Transactional
    void getAllRegularizationAttestations() throws Exception {
        // Initialize the database
        insertedRegularizationAttestation = regularizationAttestationRepository.saveAndFlush(regularizationAttestation);

        // Get all the regularizationAttestationList
        restRegularizationAttestationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(regularizationAttestation.getId().toString())))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].documentDigest").value(hasItem(DEFAULT_DOCUMENT_DIGEST)))
            .andExpect(jsonPath("$.[*].depositorId").value(hasItem(DEFAULT_DEPOSITOR_ID.toString())))
            .andExpect(jsonPath("$.[*].depositedAt").value(hasItem(DEFAULT_DEPOSITED_AT.toString())))
            .andExpect(jsonPath("$.[*].validatorId").value(hasItem(DEFAULT_VALIDATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].validatedAt").value(hasItem(DEFAULT_VALIDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRegularizationAttestationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(regularizationAttestationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRegularizationAttestationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(regularizationAttestationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRegularizationAttestationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(regularizationAttestationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRegularizationAttestationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(regularizationAttestationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRegularizationAttestation() throws Exception {
        // Initialize the database
        insertedRegularizationAttestation = regularizationAttestationRepository.saveAndFlush(regularizationAttestation);

        // Get the regularizationAttestation
        restRegularizationAttestationMockMvc
            .perform(get(ENTITY_API_URL_ID, regularizationAttestation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(regularizationAttestation.getId().toString()))
            .andExpect(jsonPath("$.kind").value(DEFAULT_KIND.toString()))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF.toString()))
            .andExpect(jsonPath("$.documentDigest").value(DEFAULT_DOCUMENT_DIGEST))
            .andExpect(jsonPath("$.depositorId").value(DEFAULT_DEPOSITOR_ID.toString()))
            .andExpect(jsonPath("$.depositedAt").value(DEFAULT_DEPOSITED_AT.toString()))
            .andExpect(jsonPath("$.validatorId").value(DEFAULT_VALIDATOR_ID.toString()))
            .andExpect(jsonPath("$.validatedAt").value(DEFAULT_VALIDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingRegularizationAttestation() throws Exception {
        // Get the regularizationAttestation
        restRegularizationAttestationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return regularizationAttestationRepository.count();
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

    protected RegularizationAttestation getPersistedRegularizationAttestation(RegularizationAttestation regularizationAttestation) {
        return regularizationAttestationRepository.findById(regularizationAttestation.getId()).orElseThrow();
    }

    protected void assertPersistedRegularizationAttestationToMatchAllProperties(
        RegularizationAttestation expectedRegularizationAttestation
    ) {
        assertRegularizationAttestationAllPropertiesEquals(
            expectedRegularizationAttestation,
            getPersistedRegularizationAttestation(expectedRegularizationAttestation)
        );
    }

    protected void assertPersistedRegularizationAttestationToMatchUpdatableProperties(
        RegularizationAttestation expectedRegularizationAttestation
    ) {
        assertRegularizationAttestationAllUpdatablePropertiesEquals(
            expectedRegularizationAttestation,
            getPersistedRegularizationAttestation(expectedRegularizationAttestation)
        );
    }
}
