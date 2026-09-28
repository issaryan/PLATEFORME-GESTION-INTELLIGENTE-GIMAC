package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.CertificationAttestationAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.CertificationAttestation;
import cm.gimac.des.certification.repository.CertificationAttestationRepository;
import cm.gimac.des.certification.service.mapper.CertificationAttestationMapper;
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
 * Integration tests for the {@link CertificationAttestationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class CertificationAttestationResourceIT {

    private static final String DEFAULT_ATTESTATION_NUMBER = "ATT-CERT-4526-5769942";
    private static final String UPDATED_ATTESTATION_NUMBER = "ATT-CERT-9281-81659";

    private static final UUID DEFAULT_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_ID = UUID.randomUUID();

    private static final String DEFAULT_PDF_DIGEST = "ddb230ecf783736ea8fe57de5f7117d45eef4788b422f8fac2d8a6331200669f";
    private static final String UPDATED_PDF_DIGEST = "2e514c5821f8de010afd5106fe60b38e8c06e13a176b9caded0de994dcb878f8";

    private static final String DEFAULT_SIGNATURE_LEVEL = "AAAAAAAAAA";
    private static final String UPDATED_SIGNATURE_LEVEL = "BBBBBBBBBB";

    private static final String DEFAULT_ATTACHMENTS = "AAAAAAAAAA";
    private static final String UPDATED_ATTACHMENTS = "BBBBBBBBBB";

    private static final Instant DEFAULT_ISSUED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ISSUED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_SEAL_CONFIRMED = false;
    private static final Boolean UPDATED_SEAL_CONFIRMED = true;

    private static final String ENTITY_API_URL = "/api/certification-attestations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CertificationAttestationRepository certificationAttestationRepository;

    @Autowired
    private CertificationAttestationMapper certificationAttestationMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCertificationAttestationMockMvc;

    private CertificationAttestation certificationAttestation;

    private CertificationAttestation insertedCertificationAttestation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CertificationAttestation createEntity() {
        return new CertificationAttestation()
            .attestationNumber(DEFAULT_ATTESTATION_NUMBER)
            .documentId(DEFAULT_DOCUMENT_ID)
            .pdfDigest(DEFAULT_PDF_DIGEST)
            .signatureLevel(DEFAULT_SIGNATURE_LEVEL)
            .attachments(DEFAULT_ATTACHMENTS)
            .issuedAt(DEFAULT_ISSUED_AT)
            .sealConfirmed(DEFAULT_SEAL_CONFIRMED);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CertificationAttestation createUpdatedEntity() {
        return new CertificationAttestation()
            .attestationNumber(UPDATED_ATTESTATION_NUMBER)
            .documentId(UPDATED_DOCUMENT_ID)
            .pdfDigest(UPDATED_PDF_DIGEST)
            .signatureLevel(UPDATED_SIGNATURE_LEVEL)
            .attachments(UPDATED_ATTACHMENTS)
            .issuedAt(UPDATED_ISSUED_AT)
            .sealConfirmed(UPDATED_SEAL_CONFIRMED);
    }

    @BeforeEach
    void initTest() {
        certificationAttestation = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCertificationAttestation != null) {
            certificationAttestationRepository.delete(insertedCertificationAttestation);
            insertedCertificationAttestation = null;
        }
    }

    @Test
    @Transactional
    void getAllCertificationAttestations() throws Exception {
        // Initialize the database
        insertedCertificationAttestation = certificationAttestationRepository.saveAndFlush(certificationAttestation);

        // Get all the certificationAttestationList
        restCertificationAttestationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(certificationAttestation.getId().toString())))
            .andExpect(jsonPath("$.[*].attestationNumber").value(hasItem(DEFAULT_ATTESTATION_NUMBER)))
            .andExpect(jsonPath("$.[*].documentId").value(hasItem(DEFAULT_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].pdfDigest").value(hasItem(DEFAULT_PDF_DIGEST)))
            .andExpect(jsonPath("$.[*].signatureLevel").value(hasItem(DEFAULT_SIGNATURE_LEVEL)))
            .andExpect(jsonPath("$.[*].attachments").value(hasItem(DEFAULT_ATTACHMENTS)))
            .andExpect(jsonPath("$.[*].issuedAt").value(hasItem(DEFAULT_ISSUED_AT.toString())))
            .andExpect(jsonPath("$.[*].sealConfirmed").value(hasItem(DEFAULT_SEAL_CONFIRMED)));
    }

    @Test
    @Transactional
    void getCertificationAttestation() throws Exception {
        // Initialize the database
        insertedCertificationAttestation = certificationAttestationRepository.saveAndFlush(certificationAttestation);

        // Get the certificationAttestation
        restCertificationAttestationMockMvc
            .perform(get(ENTITY_API_URL_ID, certificationAttestation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(certificationAttestation.getId().toString()))
            .andExpect(jsonPath("$.attestationNumber").value(DEFAULT_ATTESTATION_NUMBER))
            .andExpect(jsonPath("$.documentId").value(DEFAULT_DOCUMENT_ID.toString()))
            .andExpect(jsonPath("$.pdfDigest").value(DEFAULT_PDF_DIGEST))
            .andExpect(jsonPath("$.signatureLevel").value(DEFAULT_SIGNATURE_LEVEL))
            .andExpect(jsonPath("$.attachments").value(DEFAULT_ATTACHMENTS))
            .andExpect(jsonPath("$.issuedAt").value(DEFAULT_ISSUED_AT.toString()))
            .andExpect(jsonPath("$.sealConfirmed").value(DEFAULT_SEAL_CONFIRMED));
    }

    @Test
    @Transactional
    void getNonExistingCertificationAttestation() throws Exception {
        // Get the certificationAttestation
        restCertificationAttestationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return certificationAttestationRepository.count();
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

    protected CertificationAttestation getPersistedCertificationAttestation(CertificationAttestation certificationAttestation) {
        return certificationAttestationRepository.findById(certificationAttestation.getId()).orElseThrow();
    }

    protected void assertPersistedCertificationAttestationToMatchAllProperties(CertificationAttestation expectedCertificationAttestation) {
        assertCertificationAttestationAllPropertiesEquals(
            expectedCertificationAttestation,
            getPersistedCertificationAttestation(expectedCertificationAttestation)
        );
    }

    protected void assertPersistedCertificationAttestationToMatchUpdatableProperties(
        CertificationAttestation expectedCertificationAttestation
    ) {
        assertCertificationAttestationAllUpdatablePropertiesEquals(
            expectedCertificationAttestation,
            getPersistedCertificationAttestation(expectedCertificationAttestation)
        );
    }
}
