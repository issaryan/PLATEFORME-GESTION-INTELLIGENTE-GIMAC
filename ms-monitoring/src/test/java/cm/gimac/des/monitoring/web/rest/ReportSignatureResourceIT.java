package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.ReportSignatureAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.ReportSignature;
import cm.gimac.des.monitoring.domain.ShiftReport;
import cm.gimac.des.monitoring.domain.enumeration.SignatureRole;
import cm.gimac.des.monitoring.repository.ReportSignatureRepository;
import cm.gimac.des.monitoring.service.ReportSignatureService;
import cm.gimac.des.monitoring.service.mapper.ReportSignatureMapper;
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
 * Integration tests for the {@link ReportSignatureResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ReportSignatureResourceIT {

    private static final SignatureRole DEFAULT_ROLE = SignatureRole.SORTANT;
    private static final SignatureRole UPDATED_ROLE = SignatureRole.ENTRANT;

    private static final UUID DEFAULT_SIGNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_SIGNER_ID = UUID.randomUUID();

    private static final String DEFAULT_CHALLENGE = "20886a90047903f4206274c3a2dfabba568a426d429cfcaff49cbdb6f0c2fdaa";
    private static final String UPDATED_CHALLENGE = "dfbb47b796728ad4be3ab2b451942772454573c78a387156def3079012647b53";

    private static final String DEFAULT_ASSERTION = "AAAAAAAAAA";
    private static final String UPDATED_ASSERTION = "BBBBBBBBBB";

    private static final Instant DEFAULT_SIGNED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SIGNED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_RESERVATIONS = "AAAAAAAAAA";
    private static final String UPDATED_RESERVATIONS = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/report-signatures";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReportSignatureRepository reportSignatureRepository;

    @Mock
    private ReportSignatureRepository reportSignatureRepositoryMock;

    @Autowired
    private ReportSignatureMapper reportSignatureMapper;

    @Mock
    private ReportSignatureService reportSignatureServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReportSignatureMockMvc;

    private ReportSignature reportSignature;

    private ReportSignature insertedReportSignature;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReportSignature createEntity(EntityManager em) {
        ReportSignature reportSignature = new ReportSignature()
            .role(DEFAULT_ROLE)
            .signerId(DEFAULT_SIGNER_ID)
            .challenge(DEFAULT_CHALLENGE)
            .assertion(DEFAULT_ASSERTION)
            .signedAt(DEFAULT_SIGNED_AT)
            .reservations(DEFAULT_RESERVATIONS);
        // Add required entity
        ShiftReport shiftReport;
        if (TestUtil.findAll(em, ShiftReport.class).isEmpty()) {
            shiftReport = ShiftReportResourceIT.createEntity(em);
            em.persist(shiftReport);
            em.flush();
        } else {
            shiftReport = TestUtil.findAll(em, ShiftReport.class).get(0);
        }
        reportSignature.setReport(shiftReport);
        return reportSignature;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReportSignature createUpdatedEntity(EntityManager em) {
        ReportSignature updatedReportSignature = new ReportSignature()
            .role(UPDATED_ROLE)
            .signerId(UPDATED_SIGNER_ID)
            .challenge(UPDATED_CHALLENGE)
            .assertion(UPDATED_ASSERTION)
            .signedAt(UPDATED_SIGNED_AT)
            .reservations(UPDATED_RESERVATIONS);
        // Add required entity
        ShiftReport shiftReport;
        if (TestUtil.findAll(em, ShiftReport.class).isEmpty()) {
            shiftReport = ShiftReportResourceIT.createUpdatedEntity(em);
            em.persist(shiftReport);
            em.flush();
        } else {
            shiftReport = TestUtil.findAll(em, ShiftReport.class).get(0);
        }
        updatedReportSignature.setReport(shiftReport);
        return updatedReportSignature;
    }

    @BeforeEach
    void initTest() {
        reportSignature = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedReportSignature != null) {
            reportSignatureRepository.delete(insertedReportSignature);
            insertedReportSignature = null;
        }
    }

    @Test
    @Transactional
    void getAllReportSignatures() throws Exception {
        // Initialize the database
        insertedReportSignature = reportSignatureRepository.saveAndFlush(reportSignature);

        // Get all the reportSignatureList
        restReportSignatureMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(reportSignature.getId().toString())))
            .andExpect(jsonPath("$.[*].role").value(hasItem(DEFAULT_ROLE.toString())))
            .andExpect(jsonPath("$.[*].signerId").value(hasItem(DEFAULT_SIGNER_ID.toString())))
            .andExpect(jsonPath("$.[*].challenge").value(hasItem(DEFAULT_CHALLENGE)))
            .andExpect(jsonPath("$.[*].assertion").value(hasItem(DEFAULT_ASSERTION)))
            .andExpect(jsonPath("$.[*].signedAt").value(hasItem(DEFAULT_SIGNED_AT.toString())))
            .andExpect(jsonPath("$.[*].reservations").value(hasItem(DEFAULT_RESERVATIONS)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllReportSignaturesWithEagerRelationshipsIsEnabled() throws Exception {
        when(reportSignatureServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restReportSignatureMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(reportSignatureServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllReportSignaturesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(reportSignatureServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restReportSignatureMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(reportSignatureRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getReportSignature() throws Exception {
        // Initialize the database
        insertedReportSignature = reportSignatureRepository.saveAndFlush(reportSignature);

        // Get the reportSignature
        restReportSignatureMockMvc
            .perform(get(ENTITY_API_URL_ID, reportSignature.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(reportSignature.getId().toString()))
            .andExpect(jsonPath("$.role").value(DEFAULT_ROLE.toString()))
            .andExpect(jsonPath("$.signerId").value(DEFAULT_SIGNER_ID.toString()))
            .andExpect(jsonPath("$.challenge").value(DEFAULT_CHALLENGE))
            .andExpect(jsonPath("$.assertion").value(DEFAULT_ASSERTION))
            .andExpect(jsonPath("$.signedAt").value(DEFAULT_SIGNED_AT.toString()))
            .andExpect(jsonPath("$.reservations").value(DEFAULT_RESERVATIONS));
    }

    @Test
    @Transactional
    void getNonExistingReportSignature() throws Exception {
        // Get the reportSignature
        restReportSignatureMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return reportSignatureRepository.count();
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

    protected ReportSignature getPersistedReportSignature(ReportSignature reportSignature) {
        return reportSignatureRepository.findById(reportSignature.getId()).orElseThrow();
    }

    protected void assertPersistedReportSignatureToMatchAllProperties(ReportSignature expectedReportSignature) {
        assertReportSignatureAllPropertiesEquals(expectedReportSignature, getPersistedReportSignature(expectedReportSignature));
    }

    protected void assertPersistedReportSignatureToMatchUpdatableProperties(ReportSignature expectedReportSignature) {
        assertReportSignatureAllUpdatablePropertiesEquals(expectedReportSignature, getPersistedReportSignature(expectedReportSignature));
    }
}
