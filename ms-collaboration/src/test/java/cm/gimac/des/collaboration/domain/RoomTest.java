package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.RoomArchiveTestSamples.*;
import static cm.gimac.des.collaboration.domain.RoomTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RoomTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Room.class);
        Room room1 = getRoomSample1();
        Room room2 = new Room();
        assertThat(room1).isNotEqualTo(room2);

        room2.setId(room1.getId());
        assertThat(room1).isEqualTo(room2);

        room2 = getRoomSample2();
        assertThat(room1).isNotEqualTo(room2);
    }

    @Test
    void roomArchiveTest() {
        Room room = getRoomRandomSampleGenerator();
        RoomArchive roomArchiveBack = getRoomArchiveRandomSampleGenerator();

        room.setRoomArchive(roomArchiveBack);
        assertThat(room.getRoomArchive()).isEqualTo(roomArchiveBack);
        assertThat(roomArchiveBack.getRoom()).isEqualTo(room);

        room.roomArchive(null);
        assertThat(room.getRoomArchive()).isNull();
        assertThat(roomArchiveBack.getRoom()).isNull();
    }
}
