package cm.gimac.des.iam.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Photos de profil ; les versions se lisent sans charger les images. */
public interface AvatarCompteRepository extends JpaRepository<AvatarCompte, String> {
    /** Version de la photo d'un compte et compte qu'elle illustre. */
    interface VersionAvatar {
        String getCompteId();

        String getVersion();
    }

    @Query("select a.version from AvatarCompte a where a.compteId = :compteId")
    Optional<String> version(@Param("compteId") String compteId);

    @Query("select a.compteId as compteId, a.version as version from AvatarCompte a where a.compteId in :ids")
    List<VersionAvatar> versions(@Param("ids") Collection<String> ids);
}
