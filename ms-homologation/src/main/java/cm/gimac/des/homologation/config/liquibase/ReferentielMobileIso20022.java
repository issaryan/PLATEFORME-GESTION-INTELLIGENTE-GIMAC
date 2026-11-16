package cm.gimac.des.homologation.config.liquibase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Référentiel du type de projet MOBILE_ISO20022 (D-148, demande de l'utilisateur du 16/11/2026), exécuté une fois par
 * Liquibase après la création du type : la version publiée en vigueur du paiement mobile est copiée (version, étapes,
 * opérations avec leurs rôles, livrables et signatures), comme le départ COPIE d'un type créé dans la plateforme
 * (D-91), mais publiée d'emblée en version 1 pour que le type soit proposable au cadrage. Toutes les colonnes sont
 * recopiées telles quelles, sauf l'identifiant, la version de rattachement, le type, le numéro, le libellé, l'état,
 * les dates, l'origine et le pilote. Idempotente : rien n'est fait si le type a déjà une version ; sans version publiée
 * du paiement mobile, rien n'est fait (le référentiel se crée alors dans le modélisateur).
 */
public class ReferentielMobileIso20022 implements CustomTaskChange {

    static final String TYPE = "MOBILE_ISO20022";
    static final String SOURCE = "PAIEMENT_MOBILE";
    static final String LIBELLE = "Référentiel Paiement mobile ISO 20022 (version 1)";
    private static final Logger LOG = LoggerFactory.getLogger(ReferentielMobileIso20022.class);

    private boolean copie;

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            Connection c = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
            copie = executer(c, Instant.now()).isPresent();
        } catch (SQLException e) {
            throw new CustomChangeException("Référentiel du type MOBILE_ISO20022 en échec", e);
        }
    }

    /** Copie publiée du référentiel du paiement mobile ; rend l'identifiant de la version créée, vide sinon. */
    public static Optional<UUID> executer(Connection c, Instant maintenant) throws SQLException {
        if (RepriseReferentielCr.entier(c, "select count(*) from referential_version where type_projet = ?", TYPE) > 0) {
            return Optional.empty();
        }
        Optional<UUID> source = source(c);
        if (source.isEmpty()) {
            LOG.warn("Aucune version publiée du type {} : référentiel du type {} non copié", SOURCE, TYPE);
            return Optional.empty();
        }
        UUID id = UUID.randomUUID();
        Timestamp t = Timestamp.from(maintenant);
        Map<String, Object> version = new LinkedHashMap<>();
        version.put("id", id);
        version.put("type_projet", TYPE);
        version.put("numero", 1);
        version.put("libelle", LIBELLE);
        version.put("etat", "PUBLIEE");
        version.put("date_creation", t);
        version.put("date_publication", t);
        version.put("version_source_id", null);
        version.put("pilote_id", null);
        copier(c, "referential_version", source.orElseThrow(), version);
        for (String table : List.of("referential_etape", "reference_operation")) {
            for (UUID ligne : lignes(c, table, source.orElseThrow())) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", UUID.randomUUID());
                r.put("version_id", id);
                copier(c, table, ligne, r);
            }
        }
        LOG.info("Référentiel du type {} publié en version 1 ({}), copié de la version {} du type {}", TYPE, id, source.orElseThrow(), SOURCE);
        return Optional.of(id);
    }

    private static Optional<UUID> source(Connection c) throws SQLException {
        try (
            PreparedStatement ps = c.prepareStatement(
                "select id from referential_version where type_projet = ? and etat = 'PUBLIEE' order by numero desc"
            )
        ) {
            ps.setString(1, SOURCE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getObject(1, UUID.class)) : Optional.empty();
            }
        }
    }

    private static List<UUID> lignes(Connection c, String table, UUID version) throws SQLException {
        List<UUID> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("select id from " + table + " where version_id = ? order by id")) {
            ps.setObject(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getObject(1, UUID.class));
                }
            }
        }
        return ids;
    }

    /** Recopie la ligne {@code id} de la table, les colonnes de {@code remplacees} prenant la valeur donnée. */
    static void copier(Connection c, String table, UUID id, Map<String, Object> remplacees) throws SQLException {
        List<String> colonnes = colonnes(c, table);
        String select = colonnes.stream().map(col -> remplacees.containsKey(col) ? "?" : col).collect(Collectors.joining(", "));
        String sql = "insert into " + table + " (" + String.join(", ", colonnes) + ") select " + select + " from " + table + " where id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (String col : colonnes) {
                if (remplacees.containsKey(col)) {
                    Object v = remplacees.get(col);
                    if (v == null) {
                        ps.setNull(i++, typeColonne(c, table, col));
                    } else {
                        ps.setObject(i++, v);
                    }
                }
            }
            ps.setObject(i, id);
            ps.executeUpdate();
        }
    }

    private static List<String> colonnes(Connection c, String table) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select * from " + table + " where 1 = 0"); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData m = rs.getMetaData();
            List<String> l = new ArrayList<>();
            for (int i = 1; i <= m.getColumnCount(); i++) {
                l.add(m.getColumnName(i));
            }
            return l;
        }
    }

    private static int typeColonne(Connection c, String table, String colonne) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select " + colonne + " from " + table + " where 1 = 0"); ResultSet rs = ps.executeQuery()) {
            return rs.getMetaData().getColumnType(1);
        }
    }

    @Override
    public String getConfirmationMessage() {
        return copie ? "Référentiel du type MOBILE_ISO20022 publié en version 1" : "Référentiel du type MOBILE_ISO20022 non copié";
    }

    @Override
    public void setUp() throws SetupException {
        // Aucun paramètre.
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // Aucune ressource.
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
