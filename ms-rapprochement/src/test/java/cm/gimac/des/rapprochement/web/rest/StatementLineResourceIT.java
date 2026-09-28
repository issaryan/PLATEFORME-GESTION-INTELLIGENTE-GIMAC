package cm.gimac.des.rapprochement.web.rest;

import static cm.gimac.des.rapprochement.domain.StatementLineAsserts.*;
import static cm.gimac.des.rapprochement.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.rapprochement.IntegrationTest;
import cm.gimac.des.rapprochement.domain.ImportedStatement;
import cm.gimac.des.rapprochement.domain.StatementLine;
import cm.gimac.des.rapprochement.repository.StatementLineRepository;
import cm.gimac.des.rapprochement.service.StatementLineService;
import cm.gimac.des.rapprochement.service.mapper.StatementLineMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link StatementLineResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class StatementLineResourceIT {

    private static final Integer DEFAULT_LINE_NUMBER = 1;
    private static final Integer UPDATED_LINE_NUMBER = 2;
    private static final Integer SMALLER_LINE_NUMBER = 1 - 1;

    private static final String DEFAULT_TRANSACTION_REF = "AAAAAAAAAA";
    private static final String UPDATED_TRANSACTION_REF = "BBBBBBBBBB";

    private static final String DEFAULT_AUTHORIZATION_REF = "AAAAAAAAAA";
    private static final String UPDATED_AUTHORIZATION_REF = "BBBBBBBBBB";

    private static final Instant DEFAULT_OPERATION_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OPERATION_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal(1);
    private static final BigDecimal UPDATED_AMOUNT = new BigDecimal(2);
    private static final BigDecimal SMALLER_AMOUNT = new BigDecimal(1 - 1);

    private static final String DEFAULT_PARTICIPANT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_PARTICIPANT_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_LINE_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_LINE_STATUS = "BBBBBBBBBB";

    private static final String DEFAULT_CURRENCY = "AAA";
    private static final String UPDATED_CURRENCY = "BBB";

    private static final String ENTITY_API_URL = "/api/statement-lines";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private StatementLineRepository statementLineRepository;

    @Mock
    private StatementLineRepository statementLineRepositoryMock;

    @Autowired
    private StatementLineMapper statementLineMapper;

    @Mock
    private StatementLineService statementLineServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restStatementLineMockMvc;

    private StatementLine statementLine;

    private StatementLine insertedStatementLine;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StatementLine createEntity(EntityManager em) {
        StatementLine statementLine = new StatementLine()
            .lineNumber(DEFAULT_LINE_NUMBER)
            .transactionRef(DEFAULT_TRANSACTION_REF)
            .authorizationRef(DEFAULT_AUTHORIZATION_REF)
            .operationAt(DEFAULT_OPERATION_AT)
            .amount(DEFAULT_AMOUNT)
            .participantCode(DEFAULT_PARTICIPANT_CODE)
            .lineStatus(DEFAULT_LINE_STATUS)
            .currency(DEFAULT_CURRENCY);
        // Add required entity
        ImportedStatement importedStatement;
        if (TestUtil.findAll(em, ImportedStatement.class).isEmpty()) {
            importedStatement = ImportedStatementResourceIT.createEntity();
            em.persist(importedStatement);
            em.flush();
        } else {
            importedStatement = TestUtil.findAll(em, ImportedStatement.class).get(0);
        }
        statementLine.setStatement(importedStatement);
        return statementLine;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StatementLine createUpdatedEntity(EntityManager em) {
        StatementLine updatedStatementLine = new StatementLine()
            .lineNumber(UPDATED_LINE_NUMBER)
            .transactionRef(UPDATED_TRANSACTION_REF)
            .authorizationRef(UPDATED_AUTHORIZATION_REF)
            .operationAt(UPDATED_OPERATION_AT)
            .amount(UPDATED_AMOUNT)
            .participantCode(UPDATED_PARTICIPANT_CODE)
            .lineStatus(UPDATED_LINE_STATUS)
            .currency(UPDATED_CURRENCY);
        // Add required entity
        ImportedStatement importedStatement;
        if (TestUtil.findAll(em, ImportedStatement.class).isEmpty()) {
            importedStatement = ImportedStatementResourceIT.createUpdatedEntity();
            em.persist(importedStatement);
            em.flush();
        } else {
            importedStatement = TestUtil.findAll(em, ImportedStatement.class).get(0);
        }
        updatedStatementLine.setStatement(importedStatement);
        return updatedStatementLine;
    }

    @BeforeEach
    void initTest() {
        statementLine = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedStatementLine != null) {
            statementLineRepository.delete(insertedStatementLine);
            insertedStatementLine = null;
        }
    }

    @Test
    @Transactional
    void getAllStatementLines() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList
        restStatementLineMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(statementLine.getId().toString())))
            .andExpect(jsonPath("$.[*].lineNumber").value(hasItem(DEFAULT_LINE_NUMBER)))
            .andExpect(jsonPath("$.[*].transactionRef").value(hasItem(DEFAULT_TRANSACTION_REF)))
            .andExpect(jsonPath("$.[*].authorizationRef").value(hasItem(DEFAULT_AUTHORIZATION_REF)))
            .andExpect(jsonPath("$.[*].operationAt").value(hasItem(DEFAULT_OPERATION_AT.toString())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(sameNumber(DEFAULT_AMOUNT))))
            .andExpect(jsonPath("$.[*].participantCode").value(hasItem(DEFAULT_PARTICIPANT_CODE)))
            .andExpect(jsonPath("$.[*].lineStatus").value(hasItem(DEFAULT_LINE_STATUS)))
            .andExpect(jsonPath("$.[*].currency").value(hasItem(DEFAULT_CURRENCY)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllStatementLinesWithEagerRelationshipsIsEnabled() throws Exception {
        when(statementLineServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restStatementLineMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(statementLineServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllStatementLinesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(statementLineServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restStatementLineMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(statementLineRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getStatementLine() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get the statementLine
        restStatementLineMockMvc
            .perform(get(ENTITY_API_URL_ID, statementLine.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(statementLine.getId().toString()))
            .andExpect(jsonPath("$.lineNumber").value(DEFAULT_LINE_NUMBER))
            .andExpect(jsonPath("$.transactionRef").value(DEFAULT_TRANSACTION_REF))
            .andExpect(jsonPath("$.authorizationRef").value(DEFAULT_AUTHORIZATION_REF))
            .andExpect(jsonPath("$.operationAt").value(DEFAULT_OPERATION_AT.toString()))
            .andExpect(jsonPath("$.amount").value(sameNumber(DEFAULT_AMOUNT)))
            .andExpect(jsonPath("$.participantCode").value(DEFAULT_PARTICIPANT_CODE))
            .andExpect(jsonPath("$.lineStatus").value(DEFAULT_LINE_STATUS))
            .andExpect(jsonPath("$.currency").value(DEFAULT_CURRENCY));
    }

    @Test
    @Transactional
    void getStatementLinesByIdFiltering() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        UUID id = statementLine.getId();

        defaultStatementLineFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineNumberIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineNumber equals to
        defaultStatementLineFiltering("lineNumber.equals=" + DEFAULT_LINE_NUMBER, "lineNumber.equals=" + UPDATED_LINE_NUMBER);
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineNumberIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineNumber in
        defaultStatementLineFiltering(
            "lineNumber.in=" + DEFAULT_LINE_NUMBER + "," + UPDATED_LINE_NUMBER,
            "lineNumber.in=" + UPDATED_LINE_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineNumberIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineNumber is not null
        defaultStatementLineFiltering("lineNumber.specified=true", "lineNumber.specified=false");
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineNumberIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineNumber is greater than or equal to
        defaultStatementLineFiltering(
            "lineNumber.greaterThanOrEqual=" + DEFAULT_LINE_NUMBER,
            "lineNumber.greaterThanOrEqual=" + UPDATED_LINE_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineNumberIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineNumber is less than or equal to
        defaultStatementLineFiltering(
            "lineNumber.lessThanOrEqual=" + DEFAULT_LINE_NUMBER,
            "lineNumber.lessThanOrEqual=" + SMALLER_LINE_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineNumberIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineNumber is less than
        defaultStatementLineFiltering("lineNumber.lessThan=" + UPDATED_LINE_NUMBER, "lineNumber.lessThan=" + DEFAULT_LINE_NUMBER);
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineNumberIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineNumber is greater than
        defaultStatementLineFiltering("lineNumber.greaterThan=" + SMALLER_LINE_NUMBER, "lineNumber.greaterThan=" + DEFAULT_LINE_NUMBER);
    }

    @Test
    @Transactional
    void getAllStatementLinesByTransactionRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where transactionRef equals to
        defaultStatementLineFiltering(
            "transactionRef.equals=" + DEFAULT_TRANSACTION_REF,
            "transactionRef.equals=" + UPDATED_TRANSACTION_REF
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByTransactionRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where transactionRef in
        defaultStatementLineFiltering(
            "transactionRef.in=" + DEFAULT_TRANSACTION_REF + "," + UPDATED_TRANSACTION_REF,
            "transactionRef.in=" + UPDATED_TRANSACTION_REF
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByTransactionRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where transactionRef is not null
        defaultStatementLineFiltering("transactionRef.specified=true", "transactionRef.specified=false");
    }

    @Test
    @Transactional
    void getAllStatementLinesByTransactionRefContainsSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where transactionRef contains
        defaultStatementLineFiltering(
            "transactionRef.contains=" + DEFAULT_TRANSACTION_REF,
            "transactionRef.contains=" + UPDATED_TRANSACTION_REF
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByTransactionRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where transactionRef does not contain
        defaultStatementLineFiltering(
            "transactionRef.doesNotContain=" + UPDATED_TRANSACTION_REF,
            "transactionRef.doesNotContain=" + DEFAULT_TRANSACTION_REF
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByAuthorizationRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where authorizationRef equals to
        defaultStatementLineFiltering(
            "authorizationRef.equals=" + DEFAULT_AUTHORIZATION_REF,
            "authorizationRef.equals=" + UPDATED_AUTHORIZATION_REF
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByAuthorizationRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where authorizationRef in
        defaultStatementLineFiltering(
            "authorizationRef.in=" + DEFAULT_AUTHORIZATION_REF + "," + UPDATED_AUTHORIZATION_REF,
            "authorizationRef.in=" + UPDATED_AUTHORIZATION_REF
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByAuthorizationRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where authorizationRef is not null
        defaultStatementLineFiltering("authorizationRef.specified=true", "authorizationRef.specified=false");
    }

    @Test
    @Transactional
    void getAllStatementLinesByAuthorizationRefContainsSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where authorizationRef contains
        defaultStatementLineFiltering(
            "authorizationRef.contains=" + DEFAULT_AUTHORIZATION_REF,
            "authorizationRef.contains=" + UPDATED_AUTHORIZATION_REF
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByAuthorizationRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where authorizationRef does not contain
        defaultStatementLineFiltering(
            "authorizationRef.doesNotContain=" + UPDATED_AUTHORIZATION_REF,
            "authorizationRef.doesNotContain=" + DEFAULT_AUTHORIZATION_REF
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByOperationAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where operationAt equals to
        defaultStatementLineFiltering("operationAt.equals=" + DEFAULT_OPERATION_AT, "operationAt.equals=" + UPDATED_OPERATION_AT);
    }

    @Test
    @Transactional
    void getAllStatementLinesByOperationAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where operationAt in
        defaultStatementLineFiltering(
            "operationAt.in=" + DEFAULT_OPERATION_AT + "," + UPDATED_OPERATION_AT,
            "operationAt.in=" + UPDATED_OPERATION_AT
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByOperationAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where operationAt is not null
        defaultStatementLineFiltering("operationAt.specified=true", "operationAt.specified=false");
    }

    @Test
    @Transactional
    void getAllStatementLinesByAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where amount equals to
        defaultStatementLineFiltering("amount.equals=" + DEFAULT_AMOUNT, "amount.equals=" + UPDATED_AMOUNT);
    }

    @Test
    @Transactional
    void getAllStatementLinesByAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where amount in
        defaultStatementLineFiltering("amount.in=" + DEFAULT_AMOUNT + "," + UPDATED_AMOUNT, "amount.in=" + UPDATED_AMOUNT);
    }

    @Test
    @Transactional
    void getAllStatementLinesByAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where amount is not null
        defaultStatementLineFiltering("amount.specified=true", "amount.specified=false");
    }

    @Test
    @Transactional
    void getAllStatementLinesByAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where amount is greater than or equal to
        defaultStatementLineFiltering("amount.greaterThanOrEqual=" + DEFAULT_AMOUNT, "amount.greaterThanOrEqual=" + UPDATED_AMOUNT);
    }

    @Test
    @Transactional
    void getAllStatementLinesByAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where amount is less than or equal to
        defaultStatementLineFiltering("amount.lessThanOrEqual=" + DEFAULT_AMOUNT, "amount.lessThanOrEqual=" + SMALLER_AMOUNT);
    }

    @Test
    @Transactional
    void getAllStatementLinesByAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where amount is less than
        defaultStatementLineFiltering("amount.lessThan=" + UPDATED_AMOUNT, "amount.lessThan=" + DEFAULT_AMOUNT);
    }

    @Test
    @Transactional
    void getAllStatementLinesByAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where amount is greater than
        defaultStatementLineFiltering("amount.greaterThan=" + SMALLER_AMOUNT, "amount.greaterThan=" + DEFAULT_AMOUNT);
    }

    @Test
    @Transactional
    void getAllStatementLinesByParticipantCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where participantCode equals to
        defaultStatementLineFiltering(
            "participantCode.equals=" + DEFAULT_PARTICIPANT_CODE,
            "participantCode.equals=" + UPDATED_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByParticipantCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where participantCode in
        defaultStatementLineFiltering(
            "participantCode.in=" + DEFAULT_PARTICIPANT_CODE + "," + UPDATED_PARTICIPANT_CODE,
            "participantCode.in=" + UPDATED_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByParticipantCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where participantCode is not null
        defaultStatementLineFiltering("participantCode.specified=true", "participantCode.specified=false");
    }

    @Test
    @Transactional
    void getAllStatementLinesByParticipantCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where participantCode contains
        defaultStatementLineFiltering(
            "participantCode.contains=" + DEFAULT_PARTICIPANT_CODE,
            "participantCode.contains=" + UPDATED_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByParticipantCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where participantCode does not contain
        defaultStatementLineFiltering(
            "participantCode.doesNotContain=" + UPDATED_PARTICIPANT_CODE,
            "participantCode.doesNotContain=" + DEFAULT_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineStatus equals to
        defaultStatementLineFiltering("lineStatus.equals=" + DEFAULT_LINE_STATUS, "lineStatus.equals=" + UPDATED_LINE_STATUS);
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineStatus in
        defaultStatementLineFiltering(
            "lineStatus.in=" + DEFAULT_LINE_STATUS + "," + UPDATED_LINE_STATUS,
            "lineStatus.in=" + UPDATED_LINE_STATUS
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineStatus is not null
        defaultStatementLineFiltering("lineStatus.specified=true", "lineStatus.specified=false");
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineStatusContainsSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineStatus contains
        defaultStatementLineFiltering("lineStatus.contains=" + DEFAULT_LINE_STATUS, "lineStatus.contains=" + UPDATED_LINE_STATUS);
    }

    @Test
    @Transactional
    void getAllStatementLinesByLineStatusNotContainsSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where lineStatus does not contain
        defaultStatementLineFiltering(
            "lineStatus.doesNotContain=" + UPDATED_LINE_STATUS,
            "lineStatus.doesNotContain=" + DEFAULT_LINE_STATUS
        );
    }

    @Test
    @Transactional
    void getAllStatementLinesByCurrencyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where currency equals to
        defaultStatementLineFiltering("currency.equals=" + DEFAULT_CURRENCY, "currency.equals=" + UPDATED_CURRENCY);
    }

    @Test
    @Transactional
    void getAllStatementLinesByCurrencyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where currency in
        defaultStatementLineFiltering("currency.in=" + DEFAULT_CURRENCY + "," + UPDATED_CURRENCY, "currency.in=" + UPDATED_CURRENCY);
    }

    @Test
    @Transactional
    void getAllStatementLinesByCurrencyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where currency is not null
        defaultStatementLineFiltering("currency.specified=true", "currency.specified=false");
    }

    @Test
    @Transactional
    void getAllStatementLinesByCurrencyContainsSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where currency contains
        defaultStatementLineFiltering("currency.contains=" + DEFAULT_CURRENCY, "currency.contains=" + UPDATED_CURRENCY);
    }

    @Test
    @Transactional
    void getAllStatementLinesByCurrencyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedStatementLine = statementLineRepository.saveAndFlush(statementLine);

        // Get all the statementLineList where currency does not contain
        defaultStatementLineFiltering("currency.doesNotContain=" + UPDATED_CURRENCY, "currency.doesNotContain=" + DEFAULT_CURRENCY);
    }

    @Test
    @Transactional
    void getAllStatementLinesByStatementIsEqualToSomething() throws Exception {
        ImportedStatement statement;
        if (TestUtil.findAll(em, ImportedStatement.class).isEmpty()) {
            statementLineRepository.saveAndFlush(statementLine);
            statement = ImportedStatementResourceIT.createEntity();
        } else {
            statement = TestUtil.findAll(em, ImportedStatement.class).get(0);
        }
        em.persist(statement);
        em.flush();
        statementLine.setStatement(statement);
        statementLineRepository.saveAndFlush(statementLine);
        UUID statementId = statement.getId();
        // Get all the statementLineList where statement equals to statementId
        defaultStatementLineShouldBeFound("statementId.equals=" + statementId);

        // Get all the statementLineList where statement equals to UUID.randomUUID()
        defaultStatementLineShouldNotBeFound("statementId.equals=" + UUID.randomUUID());
    }

    private void defaultStatementLineFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultStatementLineShouldBeFound(shouldBeFound);
        defaultStatementLineShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultStatementLineShouldBeFound(String filter) throws Exception {
        restStatementLineMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(statementLine.getId().toString())))
            .andExpect(jsonPath("$.[*].lineNumber").value(hasItem(DEFAULT_LINE_NUMBER)))
            .andExpect(jsonPath("$.[*].transactionRef").value(hasItem(DEFAULT_TRANSACTION_REF)))
            .andExpect(jsonPath("$.[*].authorizationRef").value(hasItem(DEFAULT_AUTHORIZATION_REF)))
            .andExpect(jsonPath("$.[*].operationAt").value(hasItem(DEFAULT_OPERATION_AT.toString())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(sameNumber(DEFAULT_AMOUNT))))
            .andExpect(jsonPath("$.[*].participantCode").value(hasItem(DEFAULT_PARTICIPANT_CODE)))
            .andExpect(jsonPath("$.[*].lineStatus").value(hasItem(DEFAULT_LINE_STATUS)))
            .andExpect(jsonPath("$.[*].currency").value(hasItem(DEFAULT_CURRENCY)));

        // Check, that the count call also returns 1
        restStatementLineMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultStatementLineShouldNotBeFound(String filter) throws Exception {
        restStatementLineMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restStatementLineMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingStatementLine() throws Exception {
        // Get the statementLine
        restStatementLineMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return statementLineRepository.count();
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

    protected StatementLine getPersistedStatementLine(StatementLine statementLine) {
        return statementLineRepository.findById(statementLine.getId()).orElseThrow();
    }

    protected void assertPersistedStatementLineToMatchAllProperties(StatementLine expectedStatementLine) {
        assertStatementLineAllPropertiesEquals(expectedStatementLine, getPersistedStatementLine(expectedStatementLine));
    }

    protected void assertPersistedStatementLineToMatchUpdatableProperties(StatementLine expectedStatementLine) {
        assertStatementLineAllUpdatablePropertiesEquals(expectedStatementLine, getPersistedStatementLine(expectedStatementLine));
    }
}
