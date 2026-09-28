package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.ClosedOperationSnapshotAsserts.*;
import static cm.gimac.des.certification.domain.ClosedOperationSnapshotTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClosedOperationSnapshotMapperTest {

    private ClosedOperationSnapshotMapper closedOperationSnapshotMapper;

    @BeforeEach
    void setUp() {
        closedOperationSnapshotMapper = new ClosedOperationSnapshotMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getClosedOperationSnapshotSample1();
        var actual = closedOperationSnapshotMapper.toEntity(closedOperationSnapshotMapper.toDto(expected));
        assertClosedOperationSnapshotAllPropertiesEquals(expected, actual);
    }
}
