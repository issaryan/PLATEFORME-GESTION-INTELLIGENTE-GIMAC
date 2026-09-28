package cm.gimac.des.reporting.web.rest;

import static cm.gimac.des.reporting.domain.DimensionAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.reporting.IntegrationTest;
import cm.gimac.des.reporting.domain.Dimension;
import cm.gimac.des.reporting.domain.enumeration.DimensionKind;
import cm.gimac.des.reporting.repository.DimensionRepository;
import cm.gimac.des.reporting.service.mapper.DimensionMapper;
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
 * Integration tests for the {@link DimensionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DimensionResourceIT {

    private static final DimensionKind DEFAULT_KIND = DimensionKind.TEMPS;
    private static final DimensionKind UPDATED_KIND = DimensionKind.INSTITUTION;

    private static final String DEFAULT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final String DEFAULT_PARENT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_PARENT_KEY = "BBBBBBBBBB";

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/dimensions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DimensionRepository dimensionRepository;

    @Autowired
    private DimensionMapper dimensionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDimensionMockMvc;

    private Dimension dimension;

    private Dimension insertedDimension;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Dimension createEntity() {
        return new Dimension()
            .kind(DEFAULT_KIND)
            .key(DEFAULT_KEY)
            .label(DEFAULT_LABEL)
            .parentKey(DEFAULT_PARENT_KEY)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Dimension createUpdatedEntity() {
        return new Dimension()
            .kind(UPDATED_KIND)
            .key(UPDATED_KEY)
            .label(UPDATED_LABEL)
            .parentKey(UPDATED_PARENT_KEY)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        dimension = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDimension != null) {
            dimensionRepository.delete(insertedDimension);
            insertedDimension = null;
        }
    }

    @Test
    @Transactional
    void getAllDimensions() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList
        restDimensionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(dimension.getId().toString())))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].parentKey").value(hasItem(DEFAULT_PARENT_KEY)))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getDimension() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get the dimension
        restDimensionMockMvc
            .perform(get(ENTITY_API_URL_ID, dimension.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(dimension.getId().toString()))
            .andExpect(jsonPath("$.kind").value(DEFAULT_KIND.toString()))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.parentKey").value(DEFAULT_PARENT_KEY))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getDimensionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        UUID id = dimension.getId();

        defaultDimensionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllDimensionsByKindIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where kind equals to
        defaultDimensionFiltering("kind.equals=" + DEFAULT_KIND, "kind.equals=" + UPDATED_KIND);
    }

    @Test
    @Transactional
    void getAllDimensionsByKindIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where kind in
        defaultDimensionFiltering("kind.in=" + DEFAULT_KIND + "," + UPDATED_KIND, "kind.in=" + UPDATED_KIND);
    }

    @Test
    @Transactional
    void getAllDimensionsByKindIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where kind is not null
        defaultDimensionFiltering("kind.specified=true", "kind.specified=false");
    }

    @Test
    @Transactional
    void getAllDimensionsByKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where key equals to
        defaultDimensionFiltering("key.equals=" + DEFAULT_KEY, "key.equals=" + UPDATED_KEY);
    }

    @Test
    @Transactional
    void getAllDimensionsByKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where key in
        defaultDimensionFiltering("key.in=" + DEFAULT_KEY + "," + UPDATED_KEY, "key.in=" + UPDATED_KEY);
    }

    @Test
    @Transactional
    void getAllDimensionsByKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where key is not null
        defaultDimensionFiltering("key.specified=true", "key.specified=false");
    }

    @Test
    @Transactional
    void getAllDimensionsByKeyContainsSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where key contains
        defaultDimensionFiltering("key.contains=" + DEFAULT_KEY, "key.contains=" + UPDATED_KEY);
    }

    @Test
    @Transactional
    void getAllDimensionsByKeyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where key does not contain
        defaultDimensionFiltering("key.doesNotContain=" + UPDATED_KEY, "key.doesNotContain=" + DEFAULT_KEY);
    }

    @Test
    @Transactional
    void getAllDimensionsByLabelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where label equals to
        defaultDimensionFiltering("label.equals=" + DEFAULT_LABEL, "label.equals=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllDimensionsByLabelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where label in
        defaultDimensionFiltering("label.in=" + DEFAULT_LABEL + "," + UPDATED_LABEL, "label.in=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllDimensionsByLabelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where label is not null
        defaultDimensionFiltering("label.specified=true", "label.specified=false");
    }

    @Test
    @Transactional
    void getAllDimensionsByLabelContainsSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where label contains
        defaultDimensionFiltering("label.contains=" + DEFAULT_LABEL, "label.contains=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllDimensionsByLabelNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where label does not contain
        defaultDimensionFiltering("label.doesNotContain=" + UPDATED_LABEL, "label.doesNotContain=" + DEFAULT_LABEL);
    }

    @Test
    @Transactional
    void getAllDimensionsByParentKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where parentKey equals to
        defaultDimensionFiltering("parentKey.equals=" + DEFAULT_PARENT_KEY, "parentKey.equals=" + UPDATED_PARENT_KEY);
    }

    @Test
    @Transactional
    void getAllDimensionsByParentKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where parentKey in
        defaultDimensionFiltering("parentKey.in=" + DEFAULT_PARENT_KEY + "," + UPDATED_PARENT_KEY, "parentKey.in=" + UPDATED_PARENT_KEY);
    }

    @Test
    @Transactional
    void getAllDimensionsByParentKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where parentKey is not null
        defaultDimensionFiltering("parentKey.specified=true", "parentKey.specified=false");
    }

    @Test
    @Transactional
    void getAllDimensionsByParentKeyContainsSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where parentKey contains
        defaultDimensionFiltering("parentKey.contains=" + DEFAULT_PARENT_KEY, "parentKey.contains=" + UPDATED_PARENT_KEY);
    }

    @Test
    @Transactional
    void getAllDimensionsByParentKeyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where parentKey does not contain
        defaultDimensionFiltering("parentKey.doesNotContain=" + UPDATED_PARENT_KEY, "parentKey.doesNotContain=" + DEFAULT_PARENT_KEY);
    }

    @Test
    @Transactional
    void getAllDimensionsByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where updatedAt equals to
        defaultDimensionFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllDimensionsByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where updatedAt in
        defaultDimensionFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllDimensionsByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDimension = dimensionRepository.saveAndFlush(dimension);

        // Get all the dimensionList where updatedAt is not null
        defaultDimensionFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    private void defaultDimensionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultDimensionShouldBeFound(shouldBeFound);
        defaultDimensionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDimensionShouldBeFound(String filter) throws Exception {
        restDimensionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(dimension.getId().toString())))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].parentKey").value(hasItem(DEFAULT_PARENT_KEY)))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restDimensionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDimensionShouldNotBeFound(String filter) throws Exception {
        restDimensionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDimensionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDimension() throws Exception {
        // Get the dimension
        restDimensionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return dimensionRepository.count();
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

    protected Dimension getPersistedDimension(Dimension dimension) {
        return dimensionRepository.findById(dimension.getId()).orElseThrow();
    }

    protected void assertPersistedDimensionToMatchAllProperties(Dimension expectedDimension) {
        assertDimensionAllPropertiesEquals(expectedDimension, getPersistedDimension(expectedDimension));
    }

    protected void assertPersistedDimensionToMatchUpdatableProperties(Dimension expectedDimension) {
        assertDimensionAllUpdatablePropertiesEquals(expectedDimension, getPersistedDimension(expectedDimension));
    }
}
