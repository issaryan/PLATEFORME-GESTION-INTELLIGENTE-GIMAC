package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.ChangeRecordSnapshotAsserts.*;
import static cm.gimac.des.certification.domain.ChangeRecordSnapshotTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeRecordSnapshotMapperTest {

    private ChangeRecordSnapshotMapper changeRecordSnapshotMapper;

    @BeforeEach
    void setUp() {
        changeRecordSnapshotMapper = new ChangeRecordSnapshotMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getChangeRecordSnapshotSample1();
        var actual = changeRecordSnapshotMapper.toEntity(changeRecordSnapshotMapper.toDto(expected));
        assertChangeRecordSnapshotAllPropertiesEquals(expected, actual);
    }
}
