package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.ScopeItemAsserts.*;
import static cm.gimac.des.homologation.domain.ScopeItemTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScopeItemMapperTest {

    private ScopeItemMapper scopeItemMapper;

    @BeforeEach
    void setUp() {
        scopeItemMapper = new ScopeItemMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getScopeItemSample1();
        var actual = scopeItemMapper.toEntity(scopeItemMapper.toDto(expected));
        assertScopeItemAllPropertiesEquals(expected, actual);
    }
}
