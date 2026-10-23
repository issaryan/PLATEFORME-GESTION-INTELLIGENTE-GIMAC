package cm.gimac.des.iam.infrastructure.persistence.sandbox;

import cm.gimac.des.iam.application.port.out.sandbox.DelegationsSandbox;
import cm.gimac.des.iam.domain.sandbox.Delegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adaptateur JDBC des délégations temporaires de la sandbox (D-119, table delegation_sandbox). */
@Component
public class DelegationsSandboxJdbc implements DelegationsSandbox {

    private final JdbcTemplate jdbc;

    public DelegationsSandboxJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static String liste(Collection<String> l) {
        return l.stream().sorted().collect(Collectors.joining(","));
    }

    static Set<String> ensemble(String s) {
        return s.isBlank() ? Set.of() : Set.copyOf(Arrays.asList(s.split(",")));
    }

    static Timestamp ts(Instant i) {
        return i == null ? null : Timestamp.from(i);
    }

    static Instant instant(ResultSet rs, String c) throws SQLException {
        Timestamp t = rs.getTimestamp(c);
        return t == null ? null : t.toInstant();
    }

    private static Delegation ligne(ResultSet rs, int i) throws SQLException {
        return new Delegation(
            rs.getString("id"),
            TypeDelegation.valueOf(rs.getString("type")),
            rs.getString("compte_id"),
            rs.getString("institution_id"),
            ensemble(rs.getString("permissions")),
            ensemble(rs.getString("composants")),
            instant(rs, "debut"),
            instant(rs, "fin"),
            EtatDelegation.valueOf(rs.getString("etat")),
            rs.getString("demandeur_id"),
            rs.getString("approbateur_id"),
            rs.getString("motif"),
            instant(rs, "cree_le"),
            instant(rs, "close_le")
        );
    }

    @Override
    public void enregistrer(Delegation d) {
        jdbc.update(
            "insert into delegation_sandbox (id, type, compte_id, institution_id, permissions, composants, debut, fin, etat, demandeur_id, approbateur_id, motif, cree_le, close_le) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            UUID.fromString(d.id()),
            d.type().name(),
            d.compteId(),
            d.institutionId(),
            liste(d.permissions()),
            liste(d.composants()),
            ts(d.debut()),
            ts(d.fin()),
            d.etat().name(),
            d.demandeurId(),
            d.approbateurId(),
            d.motif(),
            ts(d.creeLe()),
            ts(d.closeLe())
        );
    }

    @Override
    public void mettreAJour(Delegation d) {
        jdbc.update(
            "update delegation_sandbox set etat = ?, approbateur_id = ?, motif = ?, fin = ?, close_le = ? where id = ?",
            d.etat().name(),
            d.approbateurId(),
            d.motif(),
            ts(d.fin()),
            ts(d.closeLe()),
            UUID.fromString(d.id())
        );
    }

    @Override
    public Optional<Delegation> lire(String id) {
        UUID u;
        try {
            u = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return jdbc.query("select * from delegation_sandbox where id = ?", DelegationsSandboxJdbc::ligne, u).stream().findFirst();
    }

    @Override
    public List<Delegation> enCours(String compteId, TypeDelegation type, Instant t) {
        return jdbc.query(
            "select * from delegation_sandbox where compte_id = ? and type = ? and etat = 'ACCORDEE' and fin > ? order by debut",
            DelegationsSandboxJdbc::ligne,
            compteId,
            type.name(),
            ts(t)
        );
    }

    @Override
    public List<String> titulaires(String institutionId, Instant t) {
        return jdbc.queryForList(
            "select distinct compte_id from delegation_sandbox where institution_id = ? and type = 'MANDAT' and etat = 'ACCORDEE' and fin > ? order by compte_id",
            String.class,
            institutionId,
            ts(t)
        );
    }

    @Override
    public List<Delegation> lister(TypeDelegation type, int page, int taille) {
        return jdbc.query(
            "select * from delegation_sandbox where (cast(? as varchar) is null or type = ?) order by cree_le desc, id limit ? offset ?",
            DelegationsSandboxJdbc::ligne,
            type == null ? null : type.name(),
            type == null ? null : type.name(),
            taille,
            (long) page * taille
        );
    }

    @Override
    public long compter(TypeDelegation type) {
        return java.util.Objects.requireNonNullElse(
            jdbc.queryForObject("select count(*) from delegation_sandbox where (cast(? as varchar) is null or type = ?)", Long.class, type == null ? null : type.name(), type == null ? null : type.name()),
            0L
        );
    }
}
