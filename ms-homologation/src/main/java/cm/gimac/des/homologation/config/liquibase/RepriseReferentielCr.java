package cm.gimac.des.homologation.config.liquibase;

import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ConversionAnnexeA;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Reprise des données du moteur unique (D-77, docs/lot1/modelisateur-processus.md §3), exécutée une fois par
 * Liquibase. Pour chaque type de projet : la version fidèle à l'annexe A du CR v1.4 est construite par
 * {@link ConversionAnnexeA} (la logique qui reproduit l'ancien chemin du CSV), contrôlée comme une publication
 * (complétude, graphe et modèle), puis publiée sous le numéro suivant ; la version publiée antérieure est retirée ;
 * toutes les liaisons du type y sont rattachées ; leurs parcours reçoivent la version et le modèle de chaque
 * opération (libellé, geste, jalon, déclencheur, particularité, livrables, délai, responsable). Les codes étant les
 * rangs, aucun état d'exécution ne change. Idempotente : un type qui a déjà sa version de ce libellé est ignoré.
 * <p>
 * Choix d'une tâche Java plutôt que d'un script SQL : la conversion du CSV en opérations (porteurs en qualités,
 * gestes, jalons, déclencheurs, raccord explicite du rang 27, livrables) est celle des jeux d'essai et des tests
 * de fidélité, une seule fois écrite ; la reprise s'exécute dans la transaction de Liquibase, avant que
 * l'application ne serve la moindre requête, et reste tracée dans le journal des changements.
 */
public class RepriseReferentielCr implements CustomTaskChange {

    private int versionsCreees;

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            Connection c = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
            versionsCreees = executer(c, Instant.now()).size();
        } catch (SQLException e) {
            throw new CustomChangeException("Reprise du référentiel de l'annexe A en échec", e);
        }
    }

    /** Exécute la reprise sur la connexion ; rend, par type repris, l'identifiant de la version publiée. */
    public static Map<TypeProjet, UUID> executer(Connection c, Instant maintenant) throws SQLException {
        Map<TypeProjet, UUID> reprises = new LinkedHashMap<>();
        for (TypeProjet type : TypeProjet.values()) {
            if (entier(c, "select count(*) from referential_version where type_projet = ? and libelle = ?", type.name(), ConversionAnnexeA.LIBELLE_VERSION) > 0) {
                continue;
            }
            reprises.put(type, reprendre(c, type, maintenant));
        }
        return reprises;
    }

    private static UUID reprendre(Connection c, TypeProjet type, Instant maintenant) throws SQLException {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(type, ConversionAnnexeA.LIBELLE_VERSION, maintenant);
        UUID id = UUID.randomUUID();
        v.setId(id);
        ConversionAnnexeA.version1(type).forEach(o -> v.upsertOperation(o.code(), o.data()));
        if (!new ReferentialCompletenessChecker().check(v.getOperations()).isEmpty()) {
            throw new IllegalStateException("Version 1 incomplète pour " + type);
        }
        List<String> ordre = new ReferentialGraphValidator().validate(v.noeuds());
        int numero = entier(c, "select coalesce(max(numero), 0) from referential_version where type_projet = ?", type.name()) + 1;
        executerMaj(c, "update referential_version set etat = 'RETIREE' where type_projet = ? and etat = 'PUBLIEE'", type.name());
        try (
            PreparedStatement ps = c.prepareStatement(
                "insert into referential_version (id, type_projet, numero, libelle, etat, date_creation, date_publication, ordre_topologique, version_source_id) " +
                "values (?, ?, ?, ?, 'PUBLIEE', ?, ?, ?::jsonb, null)"
            )
        ) {
            ps.setObject(1, id);
            ps.setString(2, type.name());
            ps.setInt(3, numero);
            ps.setString(4, ConversionAnnexeA.LIBELLE_VERSION);
            ps.setTimestamp(5, Timestamp.from(maintenant));
            ps.setTimestamp(6, Timestamp.from(maintenant));
            ps.setString(7, JsonTexte.ecrire(ordre));
            ps.executeUpdate();
        }
        for (ReferenceOperation o : v.getOperations()) {
            inserer(c, id, o);
        }
        executerMaj(c, "update liaison set version_id = ? where type_projet = ?", id, type.name());
        executerMaj(c, "update parcours_operation set version_id = ? where liaison_id in (select id from liaison where type_projet = ?)", id, type.name());
        executerMaj(
            c,
            "update parcours_liaison set version_id = ?, version_numero = ? where liaison_id in (select id from liaison where type_projet = ?)",
            id,
            numero,
            type.name()
        );
        try (
            PreparedStatement ps = c.prepareStatement(
                "update parcours_operation set libelle = ?, geste = ?, jalon = ?, declencheur = ?, particularite = ?, livrables = ?::jsonb, delai_jours = ?, " +
                "responsable = ? where rang = ? and liaison_id in (select id from liaison where type_projet = ?)"
            )
        ) {
            for (ReferenceOperation o : v.getOperations()) {
                ps.setString(1, o.getLibelle());
                ps.setString(2, o.gesteEffectif().name());
                ps.setString(3, o.getJalon() == null ? null : o.getJalon().name());
                ps.setString(4, o.getDeclencheur());
                ps.setString(5, o.getParticularite() == null ? null : o.getParticularite().name());
                ps.setString(6, JsonTexte.ecrire(o.getLivrables()));
                ps.setInt(7, o.delaiEffectif());
                ps.setString(8, o.responsableEffectif().name());
                ps.setString(9, o.getCode());
                ps.setString(10, type.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return id;
    }

    private static void inserer(Connection c, UUID versionId, ReferenceOperation o) throws SQLException {
        try (
            PreparedStatement ps = c.prepareStatement(
                "insert into reference_operation (id, code, libelle, phase, segment, partie, service_code, nature_couverture, preuve_attendue, id_scenario, " +
                "predecesseurs, fonction_responsable, grille, version_id, description, etape, ordre, responsable, validateur, consultes, informes, externes, " +
                "delai_jours, grain, plateforme, cle, secret, oubli_sans_symptome, pivot, valeur_reference, livrables, geste, jalon, declencheur, position, particularite) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb, ?)"
            )
        ) {
            int i = 1;
            ps.setObject(i++, UUID.randomUUID());
            ps.setString(i++, o.getCode());
            ps.setString(i++, o.getLibelle());
            ps.setString(i++, o.getPhase());
            ps.setString(i++, nom(o.getSegment()));
            ps.setString(i++, nom(o.getPartie()));
            ps.setString(i++, o.getServiceCode());
            ps.setString(i++, nom(o.getNatureCouverture()));
            ps.setString(i++, o.getPreuveAttendue());
            ps.setString(i++, o.getIdScenario());
            ps.setString(i++, o.getPredecesseurs());
            ps.setString(i++, o.getFonctionResponsable());
            ps.setString(i++, o.getGrille());
            ps.setObject(i++, versionId);
            ps.setString(i++, o.getDescription());
            ps.setString(i++, o.getEtape());
            if (o.getOrdre() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, o.getOrdre());
            }
            ps.setString(i++, nom(o.getResponsable()));
            ps.setString(i++, nom(o.getValidateur()));
            ps.setString(i++, JsonTexte.ecrire(o.getConsultes()));
            ps.setString(i++, JsonTexte.ecrire(o.getInformes()));
            ps.setString(i++, JsonTexte.ecrire(o.getExternes()));
            ps.setInt(i++, o.delaiEffectif());
            ps.setString(i++, nom(o.getGrain()));
            ps.setString(i++, nom(o.getPlateforme()));
            ps.setBoolean(i++, o.isCle());
            ps.setBoolean(i++, o.isSecret());
            ps.setBoolean(i++, o.isOubliSansSymptome());
            ps.setBoolean(i++, o.isPivot());
            ps.setString(i++, o.getValeurReference());
            ps.setString(i++, JsonTexte.ecrire(o.getLivrables()));
            ps.setString(i++, nom(o.getGeste()));
            ps.setString(i++, nom(o.getJalon()));
            ps.setString(i++, o.getDeclencheur());
            ps.setString(i++, o.getPosition() == null ? null : JsonTexte.ecrire(o.getPosition()));
            ps.setString(i, nom(o.getParticularite()));
            ps.executeUpdate();
        }
    }

    private static String nom(Enum<?> e) {
        return e == null ? null : e.name();
    }

    private static int entier(Connection c, String sql, Object... parametres) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parametres.length; i++) {
                ps.setObject(i + 1, parametres[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void executerMaj(Connection c, String sql, Object... parametres) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parametres.length; i++) {
                ps.setObject(i + 1, parametres[i]);
            }
            ps.executeUpdate();
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Référentiel de l'annexe A repris en version publiée : " + versionsCreees + " type(s) de projet";
    }

    @Override
    public void setUp() throws SetupException {
        // Aucun paramètre.
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // Les ressources sont lues sur le chemin de classes de l'application.
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
