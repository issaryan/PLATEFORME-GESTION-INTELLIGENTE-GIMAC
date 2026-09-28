package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.MeetingAttendance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MeetingAttendance entity.
 */
@Repository
public interface MeetingAttendanceRepository extends JpaRepository<MeetingAttendance, UUID> {
    default Optional<MeetingAttendance> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MeetingAttendance> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MeetingAttendance> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select meetingAttendance from MeetingAttendance meetingAttendance left join fetch meetingAttendance.meeting",
        countQuery = "select count(meetingAttendance) from MeetingAttendance meetingAttendance"
    )
    Page<MeetingAttendance> findAllWithToOneRelationships(Pageable pageable);

    @Query("select meetingAttendance from MeetingAttendance meetingAttendance left join fetch meetingAttendance.meeting")
    List<MeetingAttendance> findAllWithToOneRelationships();

    @Query(
        "select meetingAttendance from MeetingAttendance meetingAttendance left join fetch meetingAttendance.meeting where meetingAttendance.id =:id"
    )
    Optional<MeetingAttendance> findOneWithToOneRelationships(@Param("id") UUID id);
}
