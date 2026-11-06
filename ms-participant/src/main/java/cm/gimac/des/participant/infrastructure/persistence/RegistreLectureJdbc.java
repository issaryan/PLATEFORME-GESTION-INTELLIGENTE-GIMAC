package cm.gimac.des.participant.infrastructure.persistence;

import cm.gimac.des.participant.application.port.out.RegistreLecture;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Lecture du registre par requêtes SQL en lecture seule (tables générées institution, correspondent,
 * subscribed_service, inchangées) : aucune donnée de contact (courriel, téléphone) n'en sort.
 */
@Component
public class RegistreLectureJdbc implements RegistreLecture {

    private final JdbcClient jdbc;

    public RegistreLectureJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Identite> parId(UUID id) {
        return jdbc
            .sql("select id, participant_code, name, status from institution where id = :id")
            .param("id", id)
            .query((rs, n) -> new Identite(rs.getObject("id", UUID.class), rs.getString("participant_code"), rs.getString("name"), rs.getString("status")))
            .optional();
    }

    @Override
    public Optional<Identite> parCode(String codeParticipant) {
        return jdbc
            .sql("select id, participant_code, name, status from institution where participant_code = :code")
            .param("code", codeParticipant)
            .query((rs, n) -> new Identite(rs.getObject("id", UUID.class), rs.getString("participant_code"), rs.getString("name"), rs.getString("status")))
            .optional();
    }

    @Override
    public List<Correspondant> correspondantsActifs(UUID institutionId) {
        return jdbc
            .sql("select id, user_id, full_name, duty from correspondent where institution_id = :id and active = true order by full_name, id")
            .param("id", institutionId)
            .query((rs, n) -> new Correspondant(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class), rs.getString("full_name"), rs.getString("duty")))
            .list();
    }

    @Override
    public List<String> servicesSouscrits(UUID institutionId) {
        return jdbc
            .sql("select distinct service_code from subscribed_service where institution_id = :institution and state = 'ACTIF' order by service_code")
            .param("institution", institutionId)
            .query(String.class)
            .list();
    }

    @Override
    public List<Membre> membres(java.util.Set<String> statuts, int page, int taille) {
        String filtre = statuts.isEmpty() ? "" : " and status in (:statuts)";
        var requete = jdbc
            .sql("select participant_code, status from institution where participant_code is not null" + filtre + " order by participant_code limit :limite offset :decalage")
            .param("limite", taille)
            .param("decalage", (long) page * taille);
        if (!statuts.isEmpty()) {
            requete = requete.param("statuts", statuts);
        }
        return requete.query((rs, n) -> new Membre(rs.getString("participant_code"), rs.getString("status"))).list();
    }

    @Override
    public long nombreMembres(java.util.Set<String> statuts) {
        String filtre = statuts.isEmpty() ? "" : " and status in (:statuts)";
        var requete = jdbc.sql("select count(*) from institution where participant_code is not null" + filtre);
        if (!statuts.isEmpty()) {
            requete = requete.param("statuts", statuts);
        }
        return requete.query(Long.class).single();
    }

    @Override
    public List<Service> services(int page, int taille) {
        return jdbc
            .sql(
                "select service_code, count(distinct institution_id) as n from subscribed_service where state = 'ACTIF' " +
                "group by service_code order by service_code limit :limite offset :decalage"
            )
            .param("limite", taille)
            .param("decalage", (long) page * taille)
            .query((rs, n) -> new Service(rs.getString("service_code"), rs.getLong("n")))
            .list();
    }

    @Override
    public long nombreServices() {
        return jdbc.sql("select count(distinct service_code) from subscribed_service where state = 'ACTIF'").query(Long.class).single();
    }
}
