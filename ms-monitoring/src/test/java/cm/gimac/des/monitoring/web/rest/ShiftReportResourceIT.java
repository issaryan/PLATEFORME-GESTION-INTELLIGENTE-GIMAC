package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.ShiftReportAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.ShiftReport;
import cm.gimac.des.monitoring.domain.ShiftReport;
import cm.gimac.des.monitoring.domain.enumeration.ReportState;
import cm.gimac.des.monitoring.repository.ShiftReportRepository;
import cm.gimac.des.monitoring.service.ShiftReportService;
import cm.gimac.des.monitoring.service.mapper.ShiftReportMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
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
 * Integration tests for the {@link ShiftReportResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ShiftReportResourceIT {

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT = "BBBBBBBBBB";

    private static final String DEFAULT_INSTRUCTIONS = "AAAAAAAAAA";
    private static final String UPDATED_INSTRUCTIONS = "BBBBBBBBBB";

    private static final ReportState DEFAULT_STATE = ReportState.BROUILLON;
    private static final ReportState UPDATED_STATE = ReportState.SIGNE_SORTANT;

    private static final String DEFAULT_LOG_HEAD_HASH = "ad8c91892d5ae6d4a7ce87ab90a7e892c77e447e0900ca26cc33bf5632eb5da9";
    private static final String UPDATED_LOG_HEAD_HASH = "36c4ad5595c8cb186add2317d03164cf22ca4927c7564394e8901208ee69cf38";

    private static final String DEFAULT_CONTENT_HASH = "8dc17642d1f9ef891c4b8b3c7281d5d1a6d89da01e23a3f9662f16c66f342066";
    private static final String UPDATED_CONTENT_HASH = "5698766d3cf9a201f8013d2883c9fd5d78e520514372f264032fe140814680e1";

    private static final String DEFAULT_DIGEST = "1a3919a9f566c303740e7707ef73dd478e2f8e6c6b8e5fea47fb851a580ecb36";
    private static final String UPDATED_DIGEST = "f31bf33be82a7e6c590e5d4483888c13466a61d70645dbdae4769dd51c8d1b6a";

    private static final String DEFAULT_DOCUMENT_DIGEST = "e39d7f0981b20d78d55af052253472f2cd2bb0cd0b52176966479197f6c6bff0";
    private static final String UPDATED_DOCUMENT_DIGEST = "e78bee72854a504225f58d94a71b40d3615c24676bc1e291b118ee2f0e2a5f1a";

    private static final byte[] DEFAULT_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/png";

    private static final String DEFAULT_TIMESTAMP_SERIAL_NUMBER = "AAAAAAAAAA";
    private static final String UPDATED_TIMESTAMP_SERIAL_NUMBER = "BBBBBBBBBB";

    private static final String DEFAULT_DOCUMENT_REF = "AAAAAAAAAA";
    private static final String UPDATED_DOCUMENT_REF = "BBBBBBBBBB";

    private static final Instant DEFAULT_RESPONSIBILITY_TRANSFERRED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RESPONSIBILITY_TRANSFERRED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_SEALED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SEALED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/shift-reports";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ShiftReportRepository shiftReportRepository;

    @Mock
    private ShiftReportRepository shiftReportRepositoryMock;

    @Autowired
    private ShiftReportMapper shiftReportMapper;

    @Mock
    private ShiftReportService shiftReportServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restShiftReportMockMvc;

    private ShiftReport shiftReport;

    private ShiftReport insertedShiftReport;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ShiftReport createEntity(EntityManager em) {
        ShiftReport shiftReport = new ShiftReport()
            .reference(DEFAULT_REFERENCE)
            .content(DEFAULT_CONTENT)
            .instructions(DEFAULT_INSTRUCTIONS)
            .state(DEFAULT_STATE)
            .logHeadHash(DEFAULT_LOG_HEAD_HASH)
            .contentHash(DEFAULT_CONTENT_HASH)
            .digest(DEFAULT_DIGEST)
            .documentDigest(DEFAULT_DOCUMENT_DIGEST)
            .timestampToken(DEFAULT_TIMESTAMP_TOKEN)
            .timestampTokenContentType(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .timestampSerialNumber(DEFAULT_TIMESTAMP_SERIAL_NUMBER)
            .documentRef(DEFAULT_DOCUMENT_REF)
            .responsibilityTransferredAt(DEFAULT_RESPONSIBILITY_TRANSFERRED_AT)
            .sealedAt(DEFAULT_SEALED_AT);
        // Add required entity
        Shift shift;
        if (TestUtil.findAll(em, Shift.class).isEmpty()) {
            shift = ShiftResourceIT.createEntity(em);
            em.persist(shift);
            em.flush();
        } else {
            shift = TestUtil.findAll(em, Shift.class).get(0);
        }
        shiftReport.setShift(shift);
        return shiftReport;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ShiftReport createUpdatedEntity(EntityManager em) {
        ShiftReport updatedShiftReport = new ShiftReport()
            .reference(UPDATED_REFERENCE)
            .content(UPDATED_CONTENT)
            .instructions(UPDATED_INSTRUCTIONS)
            .state(UPDATED_STATE)
            .logHeadHash(UPDATED_LOG_HEAD_HASH)
            .contentHash(UPDATED_CONTENT_HASH)
            .digest(UPDATED_DIGEST)
            .documentDigest(UPDATED_DOCUMENT_DIGEST)
            .timestampToken(UPDATED_TIMESTAMP_TOKEN)
            .timestampTokenContentType(UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .timestampSerialNumber(UPDATED_TIMESTAMP_SERIAL_NUMBER)
            .documentRef(UPDATED_DOCUMENT_REF)
            .responsibilityTransferredAt(UPDATED_RESPONSIBILITY_TRANSFERRED_AT)
            .sealedAt(UPDATED_SEALED_AT);
        // Add required entity
        Shift shift;
        if (TestUtil.findAll(em, Shift.class).isEmpty()) {
            shift = ShiftResourceIT.createUpdatedEntity(em);
            em.persist(shift);
            em.flush();
        } else {
            shift = TestUtil.findAll(em, Shift.class).get(0);
        }
        updatedShiftReport.setShift(shift);
        return updatedShiftReport;
    }

    @BeforeEach
    void initTest() {
        shiftReport = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedShiftReport != null) {
            shiftReportRepository.delete(insertedShiftReport);
            insertedShiftReport = null;
        }
    }

    @Test
    @Transactional
    void getAllShiftReports() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList
        restShiftReportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(shiftReport.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].instructions").value(hasItem(DEFAULT_INSTRUCTIONS)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].logHeadHash").value(hasItem(DEFAULT_LOG_HEAD_HASH)))
            .andExpect(jsonPath("$.[*].contentHash").value(hasItem(DEFAULT_CONTENT_HASH)))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)))
            .andExpect(jsonPath("$.[*].documentDigest").value(hasItem(DEFAULT_DOCUMENT_DIGEST)))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].timestampSerialNumber").value(hasItem(DEFAULT_TIMESTAMP_SERIAL_NUMBER)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF)))
            .andExpect(jsonPath("$.[*].responsibilityTransferredAt").value(hasItem(DEFAULT_RESPONSIBILITY_TRANSFERRED_AT.toString())))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllShiftReportsWithEagerRelationshipsIsEnabled() throws Exception {
        when(shiftReportServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restShiftReportMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(shiftReportServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllShiftReportsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(shiftReportServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restShiftReportMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(shiftReportRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getShiftReport() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get the shiftReport
        restShiftReportMockMvc
            .perform(get(ENTITY_API_URL_ID, shiftReport.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(shiftReport.getId().toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT))
            .andExpect(jsonPath("$.instructions").value(DEFAULT_INSTRUCTIONS))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.logHeadHash").value(DEFAULT_LOG_HEAD_HASH))
            .andExpect(jsonPath("$.contentHash").value(DEFAULT_CONTENT_HASH))
            .andExpect(jsonPath("$.digest").value(DEFAULT_DIGEST))
            .andExpect(jsonPath("$.documentDigest").value(DEFAULT_DOCUMENT_DIGEST))
            .andExpect(jsonPath("$.timestampTokenContentType").value(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE))
            .andExpect(jsonPath("$.timestampToken").value(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN)))
            .andExpect(jsonPath("$.timestampSerialNumber").value(DEFAULT_TIMESTAMP_SERIAL_NUMBER))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF))
            .andExpect(jsonPath("$.responsibilityTransferredAt").value(DEFAULT_RESPONSIBILITY_TRANSFERRED_AT.toString()))
            .andExpect(jsonPath("$.sealedAt").value(DEFAULT_SEALED_AT.toString()));
    }

    @Test
    @Transactional
    void getShiftReportsByIdFiltering() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        UUID id = shiftReport.getId();

        defaultShiftReportFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllShiftReportsByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where reference equals to
        defaultShiftReportFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftReportsByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where reference in
        defaultShiftReportFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftReportsByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where reference is not null
        defaultShiftReportFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftReportsByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where reference contains
        defaultShiftReportFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftReportsByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where reference does not contain
        defaultShiftReportFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftReportsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where state equals to
        defaultShiftReportFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllShiftReportsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where state in
        defaultShiftReportFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllShiftReportsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where state is not null
        defaultShiftReportFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftReportsByLogHeadHashIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where logHeadHash equals to
        defaultShiftReportFiltering("logHeadHash.equals=" + DEFAULT_LOG_HEAD_HASH, "logHeadHash.equals=" + UPDATED_LOG_HEAD_HASH);
    }

    @Test
    @Transactional
    void getAllShiftReportsByLogHeadHashIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where logHeadHash in
        defaultShiftReportFiltering(
            "logHeadHash.in=" + DEFAULT_LOG_HEAD_HASH + "," + UPDATED_LOG_HEAD_HASH,
            "logHeadHash.in=" + UPDATED_LOG_HEAD_HASH
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByLogHeadHashIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where logHeadHash is not null
        defaultShiftReportFiltering("logHeadHash.specified=true", "logHeadHash.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftReportsByLogHeadHashContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where logHeadHash contains
        defaultShiftReportFiltering("logHeadHash.contains=" + DEFAULT_LOG_HEAD_HASH, "logHeadHash.contains=" + UPDATED_LOG_HEAD_HASH);
    }

    @Test
    @Transactional
    void getAllShiftReportsByLogHeadHashNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where logHeadHash does not contain
        defaultShiftReportFiltering(
            "logHeadHash.doesNotContain=" + UPDATED_LOG_HEAD_HASH,
            "logHeadHash.doesNotContain=" + DEFAULT_LOG_HEAD_HASH
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByContentHashIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where contentHash equals to
        defaultShiftReportFiltering("contentHash.equals=" + DEFAULT_CONTENT_HASH, "contentHash.equals=" + UPDATED_CONTENT_HASH);
    }

    @Test
    @Transactional
    void getAllShiftReportsByContentHashIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where contentHash in
        defaultShiftReportFiltering(
            "contentHash.in=" + DEFAULT_CONTENT_HASH + "," + UPDATED_CONTENT_HASH,
            "contentHash.in=" + UPDATED_CONTENT_HASH
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByContentHashIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where contentHash is not null
        defaultShiftReportFiltering("contentHash.specified=true", "contentHash.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftReportsByContentHashContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where contentHash contains
        defaultShiftReportFiltering("contentHash.contains=" + DEFAULT_CONTENT_HASH, "contentHash.contains=" + UPDATED_CONTENT_HASH);
    }

    @Test
    @Transactional
    void getAllShiftReportsByContentHashNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where contentHash does not contain
        defaultShiftReportFiltering(
            "contentHash.doesNotContain=" + UPDATED_CONTENT_HASH,
            "contentHash.doesNotContain=" + DEFAULT_CONTENT_HASH
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where digest equals to
        defaultShiftReportFiltering("digest.equals=" + DEFAULT_DIGEST, "digest.equals=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllShiftReportsByDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where digest in
        defaultShiftReportFiltering("digest.in=" + DEFAULT_DIGEST + "," + UPDATED_DIGEST, "digest.in=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllShiftReportsByDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where digest is not null
        defaultShiftReportFiltering("digest.specified=true", "digest.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftReportsByDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where digest contains
        defaultShiftReportFiltering("digest.contains=" + DEFAULT_DIGEST, "digest.contains=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllShiftReportsByDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where digest does not contain
        defaultShiftReportFiltering("digest.doesNotContain=" + UPDATED_DIGEST, "digest.doesNotContain=" + DEFAULT_DIGEST);
    }

    @Test
    @Transactional
    void getAllShiftReportsByDocumentDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where documentDigest equals to
        defaultShiftReportFiltering("documentDigest.equals=" + DEFAULT_DOCUMENT_DIGEST, "documentDigest.equals=" + UPDATED_DOCUMENT_DIGEST);
    }

    @Test
    @Transactional
    void getAllShiftReportsByDocumentDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where documentDigest in
        defaultShiftReportFiltering(
            "documentDigest.in=" + DEFAULT_DOCUMENT_DIGEST + "," + UPDATED_DOCUMENT_DIGEST,
            "documentDigest.in=" + UPDATED_DOCUMENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByDocumentDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where documentDigest is not null
        defaultShiftReportFiltering("documentDigest.specified=true", "documentDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftReportsByDocumentDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where documentDigest contains
        defaultShiftReportFiltering(
            "documentDigest.contains=" + DEFAULT_DOCUMENT_DIGEST,
            "documentDigest.contains=" + UPDATED_DOCUMENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByDocumentDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where documentDigest does not contain
        defaultShiftReportFiltering(
            "documentDigest.doesNotContain=" + UPDATED_DOCUMENT_DIGEST,
            "documentDigest.doesNotContain=" + DEFAULT_DOCUMENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByTimestampSerialNumberIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where timestampSerialNumber equals to
        defaultShiftReportFiltering(
            "timestampSerialNumber.equals=" + DEFAULT_TIMESTAMP_SERIAL_NUMBER,
            "timestampSerialNumber.equals=" + UPDATED_TIMESTAMP_SERIAL_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByTimestampSerialNumberIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where timestampSerialNumber in
        defaultShiftReportFiltering(
            "timestampSerialNumber.in=" + DEFAULT_TIMESTAMP_SERIAL_NUMBER + "," + UPDATED_TIMESTAMP_SERIAL_NUMBER,
            "timestampSerialNumber.in=" + UPDATED_TIMESTAMP_SERIAL_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByTimestampSerialNumberIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where timestampSerialNumber is not null
        defaultShiftReportFiltering("timestampSerialNumber.specified=true", "timestampSerialNumber.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftReportsByTimestampSerialNumberContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where timestampSerialNumber contains
        defaultShiftReportFiltering(
            "timestampSerialNumber.contains=" + DEFAULT_TIMESTAMP_SERIAL_NUMBER,
            "timestampSerialNumber.contains=" + UPDATED_TIMESTAMP_SERIAL_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByTimestampSerialNumberNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where timestampSerialNumber does not contain
        defaultShiftReportFiltering(
            "timestampSerialNumber.doesNotContain=" + UPDATED_TIMESTAMP_SERIAL_NUMBER,
            "timestampSerialNumber.doesNotContain=" + DEFAULT_TIMESTAMP_SERIAL_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByDocumentRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where documentRef equals to
        defaultShiftReportFiltering("documentRef.equals=" + DEFAULT_DOCUMENT_REF, "documentRef.equals=" + UPDATED_DOCUMENT_REF);
    }

    @Test
    @Transactional
    void getAllShiftReportsByDocumentRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where documentRef in
        defaultShiftReportFiltering(
            "documentRef.in=" + DEFAULT_DOCUMENT_REF + "," + UPDATED_DOCUMENT_REF,
            "documentRef.in=" + UPDATED_DOCUMENT_REF
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByDocumentRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where documentRef is not null
        defaultShiftReportFiltering("documentRef.specified=true", "documentRef.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftReportsByDocumentRefContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where documentRef contains
        defaultShiftReportFiltering("documentRef.contains=" + DEFAULT_DOCUMENT_REF, "documentRef.contains=" + UPDATED_DOCUMENT_REF);
    }

    @Test
    @Transactional
    void getAllShiftReportsByDocumentRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where documentRef does not contain
        defaultShiftReportFiltering(
            "documentRef.doesNotContain=" + UPDATED_DOCUMENT_REF,
            "documentRef.doesNotContain=" + DEFAULT_DOCUMENT_REF
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByResponsibilityTransferredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where responsibilityTransferredAt equals to
        defaultShiftReportFiltering(
            "responsibilityTransferredAt.equals=" + DEFAULT_RESPONSIBILITY_TRANSFERRED_AT,
            "responsibilityTransferredAt.equals=" + UPDATED_RESPONSIBILITY_TRANSFERRED_AT
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByResponsibilityTransferredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where responsibilityTransferredAt in
        defaultShiftReportFiltering(
            "responsibilityTransferredAt.in=" + DEFAULT_RESPONSIBILITY_TRANSFERRED_AT + "," + UPDATED_RESPONSIBILITY_TRANSFERRED_AT,
            "responsibilityTransferredAt.in=" + UPDATED_RESPONSIBILITY_TRANSFERRED_AT
        );
    }

    @Test
    @Transactional
    void getAllShiftReportsByResponsibilityTransferredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where responsibilityTransferredAt is not null
        defaultShiftReportFiltering("responsibilityTransferredAt.specified=true", "responsibilityTransferredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftReportsBySealedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where sealedAt equals to
        defaultShiftReportFiltering("sealedAt.equals=" + DEFAULT_SEALED_AT, "sealedAt.equals=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllShiftReportsBySealedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where sealedAt in
        defaultShiftReportFiltering("sealedAt.in=" + DEFAULT_SEALED_AT + "," + UPDATED_SEALED_AT, "sealedAt.in=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllShiftReportsBySealedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftReport = shiftReportRepository.saveAndFlush(shiftReport);

        // Get all the shiftReportList where sealedAt is not null
        defaultShiftReportFiltering("sealedAt.specified=true", "sealedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftReportsByShiftIsEqualToSomething() throws Exception {
        // Get already existing entity
        Shift shift = shiftReport.getShift();
        shiftReportRepository.saveAndFlush(shiftReport);
        UUID shiftId = shift.getId();
        // Get all the shiftReportList where shift equals to shiftId
        defaultShiftReportShouldBeFound("shiftId.equals=" + shiftId);

        // Get all the shiftReportList where shift equals to UUID.randomUUID()
        defaultShiftReportShouldNotBeFound("shiftId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllShiftReportsBySupersedesIsEqualToSomething() throws Exception {
        ShiftReport supersedes;
        if (TestUtil.findAll(em, ShiftReport.class).isEmpty()) {
            shiftReportRepository.saveAndFlush(shiftReport);
            supersedes = ShiftReportResourceIT.createUpdatedEntity(em); // champs uniques distincts de l'entité testée
            // Un rapport par quart (clé unique) : le rapport remplacé porte son propre quart.
            Shift autreQuart = ShiftResourceIT.createUpdatedEntity(em);
            em.persist(autreQuart);
            supersedes.setShift(autreQuart);
        } else {
            supersedes = TestUtil.findAll(em, ShiftReport.class).get(0);
        }
        em.persist(supersedes);
        em.flush();
        shiftReport.setSupersedes(supersedes);
        shiftReportRepository.saveAndFlush(shiftReport);
        UUID supersedesId = supersedes.getId();
        // Get all the shiftReportList where supersedes equals to supersedesId
        defaultShiftReportShouldBeFound("supersedesId.equals=" + supersedesId);

        // Get all the shiftReportList where supersedes equals to UUID.randomUUID()
        defaultShiftReportShouldNotBeFound("supersedesId.equals=" + UUID.randomUUID());
    }

    private void defaultShiftReportFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultShiftReportShouldBeFound(shouldBeFound);
        defaultShiftReportShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultShiftReportShouldBeFound(String filter) throws Exception {
        restShiftReportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(shiftReport.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].instructions").value(hasItem(DEFAULT_INSTRUCTIONS)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].logHeadHash").value(hasItem(DEFAULT_LOG_HEAD_HASH)))
            .andExpect(jsonPath("$.[*].contentHash").value(hasItem(DEFAULT_CONTENT_HASH)))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)))
            .andExpect(jsonPath("$.[*].documentDigest").value(hasItem(DEFAULT_DOCUMENT_DIGEST)))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].timestampSerialNumber").value(hasItem(DEFAULT_TIMESTAMP_SERIAL_NUMBER)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF)))
            .andExpect(jsonPath("$.[*].responsibilityTransferredAt").value(hasItem(DEFAULT_RESPONSIBILITY_TRANSFERRED_AT.toString())))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())));

        // Check, that the count call also returns 1
        restShiftReportMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultShiftReportShouldNotBeFound(String filter) throws Exception {
        restShiftReportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restShiftReportMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingShiftReport() throws Exception {
        // Get the shiftReport
        restShiftReportMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return shiftReportRepository.count();
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

    protected ShiftReport getPersistedShiftReport(ShiftReport shiftReport) {
        return shiftReportRepository.findById(shiftReport.getId()).orElseThrow();
    }

    protected void assertPersistedShiftReportToMatchAllProperties(ShiftReport expectedShiftReport) {
        assertShiftReportAllPropertiesEquals(expectedShiftReport, getPersistedShiftReport(expectedShiftReport));
    }

    protected void assertPersistedShiftReportToMatchUpdatableProperties(ShiftReport expectedShiftReport) {
        assertShiftReportAllUpdatablePropertiesEquals(expectedShiftReport, getPersistedShiftReport(expectedShiftReport));
    }
}
