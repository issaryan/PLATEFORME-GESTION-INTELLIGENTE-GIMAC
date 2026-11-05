package cm.gimac.des.audit.application.usecase;

import cm.gimac.des.audit.domain.sandbox.EnveloppeSandbox;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recherche dans le registre d'audit des événements de la Sandbox GIMAC (US-EXI-10, critère 2) : filtre par source
 * (domaine {@code sandbox}) et par institution, pages de 1 à 100, plus récents d'abord ; export au format ouvert (CSV
 * RFC 4180, UTF-8) ; rebut des enveloppes non conformes avec son seuil d'alerte (AUD-07). Lecture seule.
 */
@Service
@Transactional(readOnly = true)
public class RegistreSandboxQueries {

    /** Export : au plus 10 000 lignes par fichier. */
    static final int EXPORT_MAX = 10_000;

    public record Entree(long index, String eventId, Instant survenueLe, Instant inscritLe, String acteur, String type, String institution, String empreinte) {}

    public record Page<T>(List<T> contenu, long total, int page, int taille) {}

    public record Rebut(long id, String eventId, String sujet, String motif, Instant recuLe) {}

    /** Rebut : page, total, seuil d'alerte et alerte levée (total au-dessus du seuil). */
    public record PageRebut(List<Rebut> contenu, long total, int page, int taille, int seuil, boolean alerte) {}

    private final JdbcTemplate jdbc;
    private final int seuilRebut;

    public RegistreSandboxQueries(JdbcTemplate jdbc, @Value("${gimac.audit.sandbox.seuil-rebut:10}") int seuilRebut) {
        this.jdbc = jdbc;
        this.seuilRebut = seuilRebut;
    }

    static int taille(int t) {
        return Math.clamp(t, 1, 100);
    }

    static String institution(String i) {
        return i == null || i.isBlank() ? null : i.strip();
    }

    /** Suffixe du nom de fichier d'export : code d'institution réduit aux caractères sûrs, vide sans filtre. */
    public static String institutionAffichee(String i) {
        String c = institution(i);
        return c == null ? "" : "-" + c.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public Page<Entree> rechercher(String institutionDemandee, int page, int taille) {
        String inst = institution(institutionDemandee);
        int t = taille(taille);
        int p = Math.max(page, 0);
        List<Entree> l = jdbc.query(
            "select log_index, event_id, occurred_at, recorded_at, actor_id, subject, institution_id, leaf_hash from audit_event " +
            "where domain = ? and (cast(? as varchar) is null or institution_id = ?) order by log_index desc limit ? offset ?",
            RegistreSandboxQueries::entree,
            EnveloppeSandbox.DOMAINE,
            inst,
            inst,
            t,
            (long) p * t
        );
        Long total = jdbc.queryForObject(
            "select count(*) from audit_event where domain = ? and (cast(? as varchar) is null or institution_id = ?)",
            Long.class,
            EnveloppeSandbox.DOMAINE,
            inst,
            inst
        );
        return new Page<>(l, Objects.requireNonNullElse(total, 0L), p, t);
    }

    /** Export CSV des entrées de la sandbox (filtre d'institution facultatif), dans l'ordre du registre. */
    public String exporter(String institutionDemandee) {
        String inst = institution(institutionDemandee);
        List<Entree> l = jdbc.query(
            "select log_index, event_id, occurred_at, recorded_at, actor_id, subject, institution_id, leaf_hash from audit_event " +
            "where domain = ? and (cast(? as varchar) is null or institution_id = ?) order by log_index limit ?",
            RegistreSandboxQueries::entree,
            EnveloppeSandbox.DOMAINE,
            inst,
            inst,
            EXPORT_MAX
        );
        StringBuilder b = new StringBuilder("index,identifiant,survenu_le,inscrit_le,acteur,type,institution,empreinte_feuille\r\n");
        for (Entree e : l) {
            List<String> c = new ArrayList<>();
            c.add(String.valueOf(e.index()));
            c.add(e.eventId());
            c.add(String.valueOf(e.survenueLe()));
            c.add(String.valueOf(e.inscritLe()));
            c.add(e.acteur());
            c.add(e.type());
            c.add(e.institution() == null ? "" : e.institution());
            c.add(e.empreinte());
            b.append(String.join(",", c.stream().map(RegistreSandboxQueries::csv).toList())).append("\r\n");
        }
        return b.toString();
    }

    public PageRebut rebut(int page, int taille) {
        int t = taille(taille);
        int p = Math.max(page, 0);
        List<Rebut> l = jdbc.query(
            "select id, event_id, sujet, motif, recu_le from rebut_sandbox order by recu_le desc, id desc limit ? offset ?",
            (rs, n) -> new Rebut(rs.getLong("id"), rs.getString("event_id"), rs.getString("sujet"), rs.getString("motif"), instant(rs.getTimestamp("recu_le"))),
            t,
            (long) p * t
        );
        long total = Objects.requireNonNullElse(jdbc.queryForObject("select count(*) from rebut_sandbox", Long.class), 0L);
        return new PageRebut(l, total, p, t, seuilRebut, total > seuilRebut);
    }

    /** Champ CSV (RFC 4180) : entre guillemets s'il porte une virgule, un guillemet ou un saut de ligne. */
    static String csv(String v) {
        if (v == null) {
            return "";
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static Entree entree(ResultSet rs, int n) throws SQLException {
        return new Entree(
            rs.getLong("log_index"),
            rs.getString("event_id"),
            instant(rs.getTimestamp("occurred_at")),
            instant(rs.getTimestamp("recorded_at")),
            rs.getString("actor_id"),
            rs.getString("subject"),
            rs.getString("institution_id"),
            rs.getString("leaf_hash")
        );
    }

    static Instant instant(Timestamp t) {
        return t == null ? null : t.toInstant();
    }
}
