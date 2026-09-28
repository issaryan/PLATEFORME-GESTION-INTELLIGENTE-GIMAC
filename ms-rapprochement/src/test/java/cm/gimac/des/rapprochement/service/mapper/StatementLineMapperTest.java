package cm.gimac.des.rapprochement.service.mapper;

import static cm.gimac.des.rapprochement.domain.StatementLineAsserts.*;
import static cm.gimac.des.rapprochement.domain.StatementLineTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatementLineMapperTest {

    private StatementLineMapper statementLineMapper;

    @BeforeEach
    void setUp() {
        statementLineMapper = new StatementLineMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getStatementLineSample1();
        var actual = statementLineMapper.toEntity(statementLineMapper.toDto(expected));
        assertStatementLineAllPropertiesEquals(expected, actual);
    }
}
