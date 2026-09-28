package cm.gimac.des.reporting.domain;

import static cm.gimac.des.reporting.domain.ImportedStatementTestSamples.*;
import static cm.gimac.des.reporting.domain.IndicatorDefinitionTestSamples.*;
import static cm.gimac.des.reporting.domain.IndicatorValueTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class IndicatorValueTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(IndicatorValue.class);
        IndicatorValue indicatorValue1 = getIndicatorValueSample1();
        IndicatorValue indicatorValue2 = new IndicatorValue();
        assertThat(indicatorValue1).isNotEqualTo(indicatorValue2);

        indicatorValue2.setId(indicatorValue1.getId());
        assertThat(indicatorValue1).isEqualTo(indicatorValue2);

        indicatorValue2 = getIndicatorValueSample2();
        assertThat(indicatorValue1).isNotEqualTo(indicatorValue2);
    }

    @Test
    void definitionTest() {
        IndicatorValue indicatorValue = getIndicatorValueRandomSampleGenerator();
        IndicatorDefinition indicatorDefinitionBack = getIndicatorDefinitionRandomSampleGenerator();

        indicatorValue.setDefinition(indicatorDefinitionBack);
        assertThat(indicatorValue.getDefinition()).isEqualTo(indicatorDefinitionBack);

        indicatorValue.definition(null);
        assertThat(indicatorValue.getDefinition()).isNull();
    }

    @Test
    void statementTest() {
        IndicatorValue indicatorValue = getIndicatorValueRandomSampleGenerator();
        ImportedStatement importedStatementBack = getImportedStatementRandomSampleGenerator();

        indicatorValue.setStatement(importedStatementBack);
        assertThat(indicatorValue.getStatement()).isEqualTo(importedStatementBack);

        indicatorValue.statement(null);
        assertThat(indicatorValue.getStatement()).isNull();
    }
}
