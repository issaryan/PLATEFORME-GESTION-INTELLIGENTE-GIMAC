package cm.gimac.des.document.infrastructure.sandbox;

import cm.gimac.des.document.application.port.out.PiecesSandbox;
import cm.gimac.des.document.domain.sandbox.PieceSandbox;
import cm.gimac.des.document.domain.sandbox.TypePieceSandbox;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Tables sbx_type_piece, sbx_piece et sbx_evenement_sortant (Liquibase, D-120), lues et écrites en SQL. */
public class PiecesSandboxJdbc implements PiecesSandbox {

    private static final String COLONNES_PIECE =
        "id, type_piece, code_participant, reference, sha256, taille, etat, deposee_le, conservee_jusqu_au, service_deposant, occurrences_pan";

    private final JdbcClient jdbc;

    public PiecesSandboxJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<TypePieceSandbox> types() {
        return jdbc.sql("select * from sbx_type_piece order by code").query(PiecesSandboxJdbc::type).list();
    }

    @Override
    public Optional<TypePieceSandbox> type(String code) {
        return jdbc.sql("select * from sbx_type_piece where code = :code").param("code", code).query(PiecesSandboxJdbc::type).optional();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean modifierRetention(TypePieceSandbox t, int versionLue, String auteur, String motif, Instant le) {
        return (
            jdbc
                .sql(
                    "update sbx_type_piece set retention = :retention, version = :version, modifie_le = :le, modifie_par = :auteur, motif = :motif " +
                    "where code = :code and version = :lue"
                )
                .param("retention", t.retention().toString())
                .param("version", t.version())
                .param("le", OffsetDateTime.ofInstant(le, ZoneOffset.UTC))
                .param("auteur", auteur)
                .param("motif", motif)
                .param("code", t.code())
                .param("lue", versionLue)
                .update() == 1
        );
    }

    @Override
    public Optional<PieceSandbox> scellee(String codeParticipant, String type, String sha256) {
        return jdbc
            .sql("select " + COLONNES_PIECE + " from sbx_piece where code_participant = :code and type_piece = :type and sha256 = :sha and etat = 'SCELLEE'")
            .params(Map.of("code", codeParticipant, "type", type, "sha", sha256))
            .query(PiecesSandboxJdbc::piece)
            .optional();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void inscrire(PieceSandbox p) {
        jdbc
            .sql(
                "insert into sbx_piece (" + COLONNES_PIECE + ") values (:id, :type, :code, :reference, :sha, :taille, :etat, :le, :fin, :service, :pan)"
            )
            .param("id", p.id())
            .param("type", p.type())
            .param("code", p.codeParticipant())
            .param("reference", p.reference())
            .param("sha", p.sha256())
            .param("taille", p.taille())
            .param("etat", p.etat().name())
            .param("le", OffsetDateTime.ofInstant(p.deposeeLe(), ZoneOffset.UTC))
            .param("fin", p.conserveeJusquAu() == null ? null : OffsetDateTime.ofInstant(p.conserveeJusquAu(), ZoneOffset.UTC))
            .param("service", p.serviceDeposant())
            .param("pan", p.occurrencesPan())
            .update();
    }

    @Override
    public List<PieceSandbox> quarantaines(int page, int taille) {
        return jdbc
            .sql("select " + COLONNES_PIECE + " from sbx_piece where etat = 'QUARANTAINE' order by deposee_le desc, id limit :limite offset :decalage")
            .param("limite", taille)
            .param("decalage", (long) page * taille)
            .query(PiecesSandboxJdbc::piece)
            .list();
    }

    @Override
    public long nombreQuarantaines() {
        return jdbc.sql("select count(*) from sbx_piece where etat = 'QUARANTAINE'").query(Long.class).single();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publier(String type, String cle, String donneesJson, Instant le) {
        jdbc
            .sql("insert into sbx_evenement_sortant (id, type, cle, donnees, statut, tentatives, cree_le) values (:id, :type, :cle, :donnees, 'EN_ATTENTE', 0, :le)")
            .param("id", UUID.randomUUID())
            .param("type", type)
            .param("cle", cle)
            .param("donnees", donneesJson)
            .param("le", OffsetDateTime.ofInstant(le, ZoneOffset.UTC))
            .update();
    }

    static TypePieceSandbox type(ResultSet rs, int n) throws SQLException {
        return new TypePieceSandbox(
            rs.getString("code"),
            Map.of("fr", rs.getString("libelle_fr"), "en", rs.getString("libelle_en"), "es", rs.getString("libelle_es")),
            Period.parse(rs.getString("retention")),
            rs.getBoolean("probante"),
            new LinkedHashSet<>(Arrays.asList(rs.getString("services_autorises").split(","))),
            TypePieceSandbox.ReglePan.valueOf(rs.getString("regle_pan")),
            rs.getLong("taille_max"),
            rs.getBoolean("actif"),
            rs.getInt("version")
        );
    }

    static PieceSandbox piece(ResultSet rs, int n) throws SQLException {
        Timestamp fin = rs.getTimestamp("conservee_jusqu_au");
        return new PieceSandbox(
            rs.getObject("id", UUID.class),
            rs.getString("type_piece"),
            rs.getString("code_participant"),
            rs.getString("reference"),
            rs.getString("sha256"),
            rs.getLong("taille"),
            PieceSandbox.Etat.valueOf(rs.getString("etat")),
            rs.getTimestamp("deposee_le").toInstant(),
            fin == null ? null : fin.toInstant(),
            rs.getString("service_deposant"),
            rs.getInt("occurrences_pan")
        );
    }
}
