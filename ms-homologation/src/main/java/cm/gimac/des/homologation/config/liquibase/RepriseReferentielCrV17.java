package cm.gimac.des.homologation.config.liquibase;

import cm.gimac.des.homologation.domain.EtapeReferentiel;
import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Raci;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import cm.gimac.des.homologation.domain.service.ConversionAnnexeA;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Reprise de l'annexe A du CR-CERT-GIMAC-01 v1.7, qui fait foi (docs/lot1/modelisateur-processus.md §3), exécutée une
 * fois par Liquibase après la reprise de la version 1 (D-77) et les changesets qui ont depuis enrichi le modèle (étapes
 * D-89, signatures D-91, vue et tâches des consultés D-93). Pour chaque type de projet :
 * <ol>
 *   <li>la version fidèle à l'annexe A v1.7 est construite par {@link ConversionAnnexeA} (édition V1_7), contrôlée
 *   comme une publication (complétude, graphe), puis publiée sous le numéro suivant, avec ses onze étapes canoniques ;
 *   la version publiée en cours (la version 1, ou une version publiée depuis par l'administration) est retirée ;</li>
 *   <li>les liaisons du type qui ne sont pas closes y sont rattachées, avec leur parcours ; une opération faite, sans
 *   objet ou en signature ne reçoit que la version ; une opération à faire reçoit le modèle de la version (libellé,
 *   geste, jalon, déclencheur, particularité, livrables, délai, responsable, signatures, étape, vue) ; quand le
 *   responsable change de fonction (M6, M11 à M14, M16 et M17 : du pôle réseau à la Production monétique), la qualité
 *   R passe à la personne affectée à la nouvelle fonction sur la liaison, sinon à la fonction à affecter.</li>
 * </ol>
 * Aucun état d'exécution ne change (état, preuves, dépôts, échéances, signatures recueillies) : les codes étant les
 * rangs, chaque opération du parcours retrouve son modèle. Idempotente : un type qui a déjà sa version de ce libellé
 * est ignoré. Les tâches déjà offertes dans MS-PILOTAGE à l'ancienne fonction n'y sont pas reprises.
 */
public class RepriseReferentielCrV17 implements CustomTaskChange {

    /** États d'une opération qui n'est pas commencée ou pas aboutie : elle reçoit le modèle de la nouvelle version. */
    static final String ETATS_A_FAIRE = "('VERROUILLEE', 'A_FAIRE', 'EN_COURS', 'EN_VALIDATION', 'BLOQUEE')";

    private int versionsCreees;

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            Connection c = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
            versionsCreees = executer(c, Instant.now()).size();
        } catch (SQLException e) {
            throw new CustomChangeException("Reprise du référentiel de l'annexe A v1.7 en échec", e);
        }
    }

    /** Exécute la reprise sur la connexion ; rend, par type repris, l'identifiant de la version publiée. */
    public static Map<TypeProjet, UUID> executer(Connection c, Instant maintenant) throws SQLException {
        return executer(c, maintenant, java.util.Set.of(TypeProjet.values()));
    }

    /** Même reprise, limitée à certains types de projet. */
    public static Map<TypeProjet, UUID> executer(Connection c, Instant maintenant, java.util.Set<TypeProjet> types) throws SQLException {
        Map<TypeProjet, UUID> reprises = new LinkedHashMap<>();
        for (TypeProjet type : TypeProjet.values()) {
            if (!types.contains(type)) {
                continue;
            }
            String libelle = ConversionAnnexeA.Edition.V1_7.libelle();
            if (RepriseReferentielCr.entier(c, "select count(*) from referential_version where type_projet = ? and libelle = ?", type.name(), libelle) > 0) {
                continue;
            }
            reprises.put(type, reprendre(c, type, maintenant));
        }
        return reprises;
    }

    private static UUID reprendre(Connection c, TypeProjet type, Instant maintenant) throws SQLException {
        String libelle = ConversionAnnexeA.Edition.V1_7.libelle();
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(type, libelle, maintenant);
        UUID id = UUID.randomUUID();
        v.setId(id);
        ConversionAnnexeA.version(type, ConversionAnnexeA.Edition.V1_7).forEach(o -> v.upsertOperation(o.code(), o.data()));
        if (!new ReferentialCompletenessChecker().check(v.getOperations()).isEmpty()) {
            throw new IllegalStateException("Version de l'annexe A v1.7 incomplète pour " + type);
        }
        List<String> ordre = new ReferentialGraphValidator().validate(v.noeuds());
        int numero = RepriseReferentielCr.entier(c, "select coalesce(max(numero), 0) from referential_version where type_projet = ?", type.name()) + 1;
        RepriseReferentielCr.executerMaj(c, "update referential_version set etat = 'RETIREE' where type_projet = ? and etat = 'PUBLIEE'", type.name());
        try (
            PreparedStatement ps = c.prepareStatement(
                "insert into referential_version (id, type_projet, numero, libelle, etat, date_creation, date_publication, ordre_topologique, version_source_id) " +
                "values (?, ?, ?, ?, 'PUBLIEE', ?, ?, ?::jsonb, null)"
            )
        ) {
            ps.setObject(1, id);
            ps.setString(2, type.name());
            ps.setInt(3, numero);
            ps.setString(4, libelle);
            ps.setTimestamp(5, Timestamp.from(maintenant));
            ps.setTimestamp(6, Timestamp.from(maintenant));
            ps.setString(7, JsonTexte.ecrire(ordre));
            ps.executeUpdate();
        }
        for (EtapeReferentiel e : v.etapes()) {
            RepriseReferentielCr.executerMaj(
                c,
                "insert into referential_etape (id, version_id, code, libelle, ordre, couleur, canonique) values (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                id,
                e.getCode(),
                e.getLibelle(),
                e.getOrdre(),
                e.getCouleur().name(),
                e.getCanonique() == null ? null : e.getCanonique().name()
            );
        }
        for (ReferenceOperation o : v.getOperations()) {
            RepriseReferentielCr.inserer(c, id, o);
            List<SignatureOperation> sig = o.getSignatures();
            RepriseReferentielCr.executerMaj(
                c,
                "update reference_operation set signatures = ?::jsonb, vue = ?, consultes_recoivent_tache = ? where version_id = ? and code = ?",
                sig.isEmpty() ? null : JsonTexte.ecrire(sig),
                o.vueEffective().name(),
                o.isConsultesRecoiventTache(),
                id,
                o.getCode()
            );
        }
        rattacher(c, type, id, numero, v.getOperations());
        return id;
    }

    /** Rattache les liaisons non closes du type et leur parcours à la version, sans toucher à l'exécution. */
    private static void rattacher(Connection c, TypeProjet type, UUID id, int numero, List<ReferenceOperation> operations) throws SQLException {
        String liaisons = "(select id from liaison where type_projet = ? and etat <> 'CLOSE')";
        Map<String, ReferenceOperation> parCode = operations.stream().collect(Collectors.toMap(ReferenceOperation::getCode, Function.identity()));
        reaffecterResponsables(c, type, liaisons, parCode);
        RepriseReferentielCr.executerMaj(c, "update liaison set version_id = ? where id in " + liaisons, id, type.name());
        RepriseReferentielCr.executerMaj(
            c,
            "update parcours_liaison set version_id = ?, version_numero = ? where liaison_id in " + liaisons,
            id,
            numero,
            type.name()
        );
        RepriseReferentielCr.executerMaj(c, "update parcours_operation set version_id = ? where liaison_id in " + liaisons, id, type.name());
        try (
            PreparedStatement ps = c.prepareStatement(
                "update parcours_operation set libelle = ?, geste = ?, jalon = ?, declencheur = ?, particularite = ?, livrables = ?::jsonb, delai_jours = ?, " +
                "responsable = ?, signatures = ?::jsonb, etape_code = ?, vue = ?, consultes_recoivent_tache = ? " +
                "where rang = ? and etat in " + ETATS_A_FAIRE + " and liaison_id in " + liaisons
            )
        ) {
            for (ReferenceOperation o : operations) {
                List<SignatureOperation> sig = o.getSignatures();
                int i = 1;
                ps.setString(i++, o.getLibelle());
                ps.setString(i++, o.gesteEffectif().name());
                ps.setString(i++, o.getJalon() == null ? null : o.getJalon().name());
                ps.setString(i++, o.getDeclencheur());
                ps.setString(i++, o.getParticularite() == null ? null : o.getParticularite().name());
                ps.setString(i++, JsonTexte.ecrire(o.getLivrables()));
                ps.setInt(i++, o.delaiEffectif());
                ps.setString(i++, o.responsableEffectif().name());
                ps.setString(i++, sig.isEmpty() ? null : JsonTexte.ecrire(sig));
                ps.setString(i++, o.getEtape() != null && !o.getEtape().isBlank() ? o.getEtape() : o.etapeEffective().name());
                ps.setString(i++, o.vueEffective().name());
                ps.setBoolean(i++, o.isConsultesRecoiventTache());
                ps.setString(i++, o.getCode());
                ps.setString(i, type.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Opérations à faire dont le responsable change de fonction : la qualité R, tenue par l'ancienne fonction, passe à
     * la personne affectée à la nouvelle sur la liaison (ressources du rang 6), sinon à la fonction à affecter.
     */
    private static void reaffecterResponsables(Connection c, TypeProjet type, String liaisons, Map<String, ReferenceOperation> parCode)
        throws SQLException {
        record Ligne(UUID id, String rang, String responsable, String raci, String ressources) {}
        List<Ligne> lignes = new ArrayList<>();
        try (
            PreparedStatement ps = c.prepareStatement(
                "select po.id, po.rang, po.responsable, po.raci::text, pl.ressources::text from parcours_operation po " +
                "left join parcours_liaison pl on pl.liaison_id = po.liaison_id where po.etat in " + ETATS_A_FAIRE + " and po.liaison_id in " + liaisons
            )
        ) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lignes.add(new Ligne(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
                }
            }
        }
        for (Ligne l : lignes) {
            ReferenceOperation ref = parCode.get(l.rang());
            if (ref == null || l.responsable() == null || l.raci() == null) {
                continue;
            }
            Fonction nouvelle = ref.responsableEffectif();
            Raci raci = JsonTexte.lire(l.raci(), Raci.class);
            if (nouvelle.name().equals(l.responsable()) || raci.r() == null || raci.r().fonction() == null || !raci.r().fonction().name().equals(l.responsable())) {
                continue;
            }
            Personne titulaire = (l.ressources() == null ? List.<Personne>of() : JsonTexte.liste(l.ressources(), Personne.class))
                .stream()
                .filter(p -> p.fonction() == nouvelle)
                .findFirst()
                .orElse(Personne.aAffecter(nouvelle));
            RepriseReferentielCr.executerMaj(c, "update parcours_operation set raci = ?::jsonb where id = ?", JsonTexte.ecrire(raci.avecR(titulaire)), l.id());
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Référentiel de l'annexe A v1.7 repris en version publiée : " + versionsCreees + " type(s) de projet";
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
