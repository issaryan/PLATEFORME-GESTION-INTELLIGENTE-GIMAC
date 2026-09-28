package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.RoomArchiveTestSamples.*;
import static cm.gimac.des.collaboration.domain.RoomTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RoomArchiveTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoomArchive.class);
        RoomArchive roomArchive1 = getRoomArchiveSample1();
        RoomArchive roomArchive2 = new RoomArchive();
        assertThat(roomArchive1).isNotEqualTo(roomArchive2);

        roomArchive2.setId(roomArchive1.getId());
        assertThat(roomArchive1).isEqualTo(roomArchive2);

        roomArchive2 = getRoomArchiveSample2();
        assertThat(roomArchive1).isNotEqualTo(roomArchive2);
    }

    @Test
    void roomTest() {
        RoomArchive roomArchive = getRoomArchiveRandomSampleGenerator();
        Room roomBack = getRoomRandomSampleGenerator();

        roomArchive.setRoom(roomBack);
        assertThat(roomArchive.getRoom()).isEqualTo(roomBack);

        roomArchive.room(null);
        assertThat(roomArchive.getRoom()).isNull();
    }
}
