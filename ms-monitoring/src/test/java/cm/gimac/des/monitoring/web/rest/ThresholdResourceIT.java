package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.ThresholdAsserts.*;
import static cm.gimac.des.monitoring.web.rest.TestUtil.createUpdateProxyForBean;
import static cm.gimac.des.monitoring.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.domain.Threshold;
import cm.gimac.des.monitoring.domain.enumeration.MeasureDirection;
import cm.gimac.des.monitoring.repository.ThresholdRepository;
import cm.gimac.des.monitoring.service.ThresholdService;
import cm.gimac.des.monitoring.service.dto.ThresholdDTO;
import cm.gimac.des.monitoring.service.mapper.ThresholdMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link ThresholdResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ThresholdResourceIT {

    private static final String DEFAULT_INDICATOR = "AAAAAAAAAA";
    private static final String UPDATED_INDICATOR = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_WARNING = new BigDecimal(1);
    private static final BigDecimal UPDATED_WARNING = new BigDecimal(2);

    private static final BigDecimal DEFAULT_CRITICAL = new BigDecimal(1);
    private static final BigDecimal UPDATED_CRITICAL = new BigDecimal(2);

    private static final BigDecimal DEFAULT_HYSTERESIS = new BigDecimal(0);
    private static final BigDecimal UPDATED_HYSTERESIS = new BigDecimal(1);

    private static final MeasureDirection DEFAULT_DIRECTION = MeasureDirection.HAUSSE_DEFAVORABLE;
    private static final MeasureDirection UPDATED_DIRECTION = MeasureDirection.BAISSE_DEFAVORABLE;

    private static final Integer DEFAULT_DEBOUNCE = 1;
    private static final Integer UPDATED_DEBOUNCE = 2;

    private static final String ENTITY_API_URL = "/api/thresholds";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ThresholdRepository thresholdRepository;

    @Mock
    private ThresholdRepository thresholdRepositoryMock;

    @Autowired
    private ThresholdMapper thresholdMapper;

    @Mock
    private ThresholdService thresholdServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restThresholdMockMvc;

    private Threshold threshold;

    private Threshold insertedThreshold;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Threshold createEntity(EntityManager em) {
        Threshold threshold = new Threshold()
            .indicator(DEFAULT_INDICATOR)
            .warning(DEFAULT_WARNING)
            .critical(DEFAULT_CRITICAL)
            .hysteresis(DEFAULT_HYSTERESIS)
            .direction(DEFAULT_DIRECTION)
            .debounce(DEFAULT_DEBOUNCE);
        // Add required entity
        SupervisedLink supervisedLink;
        if (TestUtil.findAll(em, SupervisedLink.class).isEmpty()) {
            supervisedLink = SupervisedLinkResourceIT.createEntity();
            em.persist(supervisedLink);
            em.flush();
        } else {
            supervisedLink = TestUtil.findAll(em, SupervisedLink.class).get(0);
        }
        threshold.setLink(supervisedLink);
        return threshold;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Threshold createUpdatedEntity(EntityManager em) {
        Threshold updatedThreshold = new Threshold()
            .indicator(UPDATED_INDICATOR)
            .warning(UPDATED_WARNING)
            .critical(UPDATED_CRITICAL)
            .hysteresis(UPDATED_HYSTERESIS)
            .direction(UPDATED_DIRECTION)
            .debounce(UPDATED_DEBOUNCE);
        // Add required entity
        SupervisedLink supervisedLink;
        if (TestUtil.findAll(em, SupervisedLink.class).isEmpty()) {
            supervisedLink = SupervisedLinkResourceIT.createUpdatedEntity();
            em.persist(supervisedLink);
            em.flush();
        } else {
            supervisedLink = TestUtil.findAll(em, SupervisedLink.class).get(0);
        }
        updatedThreshold.setLink(supervisedLink);
        return updatedThreshold;
    }

    @BeforeEach
    void initTest() {
        threshold = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedThreshold != null) {
            thresholdRepository.delete(insertedThreshold);
            insertedThreshold = null;
        }
    }

    @Test
    @Transactional
    void createThreshold() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Threshold
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);
        var returnedThresholdDTO = om.readValue(
            restThresholdMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(thresholdDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ThresholdDTO.class
        );

        // Validate the Threshold in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedThreshold = thresholdMapper.toEntity(returnedThresholdDTO);
        assertThresholdUpdatableFieldsEquals(returnedThreshold, getPersistedThreshold(returnedThreshold));

        insertedThreshold = returnedThreshold;
    }

    @Test
    @Transactional
    void createThresholdWithExistingId() throws Exception {
        // Create the Threshold with an existing ID
        insertedThreshold = thresholdRepository.saveAndFlush(threshold);
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restThresholdMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(thresholdDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Threshold in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkIndicatorIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        threshold.setIndicator(null);

        // Create the Threshold, which fails.
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        restThresholdMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(thresholdDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkWarningIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        threshold.setWarning(null);

        // Create the Threshold, which fails.
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        restThresholdMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(thresholdDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCriticalIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        threshold.setCritical(null);

        // Create the Threshold, which fails.
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        restThresholdMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(thresholdDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkHysteresisIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        threshold.setHysteresis(null);

        // Create the Threshold, which fails.
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        restThresholdMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(thresholdDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDirectionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        threshold.setDirection(null);

        // Create the Threshold, which fails.
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        restThresholdMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(thresholdDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDebounceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        threshold.setDebounce(null);

        // Create the Threshold, which fails.
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        restThresholdMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(thresholdDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllThresholds() throws Exception {
        // Initialize the database
        insertedThreshold = thresholdRepository.saveAndFlush(threshold);

        // Get all the thresholdList
        restThresholdMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(threshold.getId().toString())))
            .andExpect(jsonPath("$.[*].indicator").value(hasItem(DEFAULT_INDICATOR)))
            .andExpect(jsonPath("$.[*].warning").value(hasItem(sameNumber(DEFAULT_WARNING))))
            .andExpect(jsonPath("$.[*].critical").value(hasItem(sameNumber(DEFAULT_CRITICAL))))
            .andExpect(jsonPath("$.[*].hysteresis").value(hasItem(sameNumber(DEFAULT_HYSTERESIS))))
            .andExpect(jsonPath("$.[*].direction").value(hasItem(DEFAULT_DIRECTION.toString())))
            .andExpect(jsonPath("$.[*].debounce").value(hasItem(DEFAULT_DEBOUNCE)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllThresholdsWithEagerRelationshipsIsEnabled() throws Exception {
        when(thresholdServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restThresholdMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(thresholdServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllThresholdsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(thresholdServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restThresholdMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(thresholdRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getThreshold() throws Exception {
        // Initialize the database
        insertedThreshold = thresholdRepository.saveAndFlush(threshold);

        // Get the threshold
        restThresholdMockMvc
            .perform(get(ENTITY_API_URL_ID, threshold.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(threshold.getId().toString()))
            .andExpect(jsonPath("$.indicator").value(DEFAULT_INDICATOR))
            .andExpect(jsonPath("$.warning").value(sameNumber(DEFAULT_WARNING)))
            .andExpect(jsonPath("$.critical").value(sameNumber(DEFAULT_CRITICAL)))
            .andExpect(jsonPath("$.hysteresis").value(sameNumber(DEFAULT_HYSTERESIS)))
            .andExpect(jsonPath("$.direction").value(DEFAULT_DIRECTION.toString()))
            .andExpect(jsonPath("$.debounce").value(DEFAULT_DEBOUNCE));
    }

    @Test
    @Transactional
    void getNonExistingThreshold() throws Exception {
        // Get the threshold
        restThresholdMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingThreshold() throws Exception {
        // Initialize the database
        insertedThreshold = thresholdRepository.saveAndFlush(threshold);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the threshold
        Threshold updatedThreshold = thresholdRepository.findById(threshold.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedThreshold are not directly saved in db
        em.detach(updatedThreshold);
        updatedThreshold
            .indicator(UPDATED_INDICATOR)
            .warning(UPDATED_WARNING)
            .critical(UPDATED_CRITICAL)
            .hysteresis(UPDATED_HYSTERESIS)
            .direction(UPDATED_DIRECTION)
            .debounce(UPDATED_DEBOUNCE);
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(updatedThreshold);

        restThresholdMockMvc
            .perform(
                put(ENTITY_API_URL_ID, thresholdDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(thresholdDTO))
            )
            .andExpect(status().isOk());

        // Validate the Threshold in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedThresholdToMatchAllProperties(updatedThreshold);
    }

    @Test
    @Transactional
    void putNonExistingThreshold() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        threshold.setId(UUID.randomUUID());

        // Create the Threshold
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restThresholdMockMvc
            .perform(
                put(ENTITY_API_URL_ID, thresholdDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(thresholdDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Threshold in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchThreshold() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        threshold.setId(UUID.randomUUID());

        // Create the Threshold
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restThresholdMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(thresholdDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Threshold in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamThreshold() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        threshold.setId(UUID.randomUUID());

        // Create the Threshold
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restThresholdMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(thresholdDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Threshold in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateThresholdWithPatch() throws Exception {
        // Initialize the database
        insertedThreshold = thresholdRepository.saveAndFlush(threshold);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the threshold using partial update
        Threshold partialUpdatedThreshold = new Threshold();
        partialUpdatedThreshold.setId(threshold.getId());

        partialUpdatedThreshold.critical(UPDATED_CRITICAL).hysteresis(UPDATED_HYSTERESIS).debounce(UPDATED_DEBOUNCE);

        restThresholdMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedThreshold.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedThreshold))
            )
            .andExpect(status().isOk());

        // Validate the Threshold in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertThresholdUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedThreshold, threshold),
            getPersistedThreshold(threshold)
        );
    }

    @Test
    @Transactional
    void fullUpdateThresholdWithPatch() throws Exception {
        // Initialize the database
        insertedThreshold = thresholdRepository.saveAndFlush(threshold);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the threshold using partial update
        Threshold partialUpdatedThreshold = new Threshold();
        partialUpdatedThreshold.setId(threshold.getId());

        partialUpdatedThreshold
            .indicator(UPDATED_INDICATOR)
            .warning(UPDATED_WARNING)
            .critical(UPDATED_CRITICAL)
            .hysteresis(UPDATED_HYSTERESIS)
            .direction(UPDATED_DIRECTION)
            .debounce(UPDATED_DEBOUNCE);

        restThresholdMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedThreshold.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedThreshold))
            )
            .andExpect(status().isOk());

        // Validate the Threshold in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertThresholdUpdatableFieldsEquals(partialUpdatedThreshold, getPersistedThreshold(partialUpdatedThreshold));
    }

    @Test
    @Transactional
    void patchNonExistingThreshold() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        threshold.setId(UUID.randomUUID());

        // Create the Threshold
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restThresholdMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, thresholdDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(thresholdDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Threshold in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchThreshold() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        threshold.setId(UUID.randomUUID());

        // Create the Threshold
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restThresholdMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(thresholdDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Threshold in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamThreshold() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        threshold.setId(UUID.randomUUID());

        // Create the Threshold
        ThresholdDTO thresholdDTO = thresholdMapper.toDto(threshold);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restThresholdMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(thresholdDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Threshold in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteThreshold() throws Exception {
        // Initialize the database
        insertedThreshold = thresholdRepository.saveAndFlush(threshold);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the threshold
        restThresholdMockMvc
            .perform(delete(ENTITY_API_URL_ID, threshold.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return thresholdRepository.count();
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

    protected Threshold getPersistedThreshold(Threshold threshold) {
        return thresholdRepository.findById(threshold.getId()).orElseThrow();
    }

    protected void assertPersistedThresholdToMatchAllProperties(Threshold expectedThreshold) {
        assertThresholdAllPropertiesEquals(expectedThreshold, getPersistedThreshold(expectedThreshold));
    }

    protected void assertPersistedThresholdToMatchUpdatableProperties(Threshold expectedThreshold) {
        assertThresholdAllUpdatablePropertiesEquals(expectedThreshold, getPersistedThreshold(expectedThreshold));
    }
}
