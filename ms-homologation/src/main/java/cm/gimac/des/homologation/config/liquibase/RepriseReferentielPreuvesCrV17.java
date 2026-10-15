package cm.gimac.des.homologation.config.liquibase;

import cm.gimac.des.homologation.domain.EtapeReferentiel;
import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.DepotLivrable;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Preuve;
import cm.gimac.des.homologation.domain.parcours.Raci;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import cm.gimac.des.homologation.domain.service.PreuvesSignaturesCrV17;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Reprise D-109 (docs/lot1/referentiel-preuves-signatures.md) : pour chaque type de projet, la version de l'annexe A du
 * CR v1.7 complétée des preuves, livrables, signatures, RACI et délais ({@link PreuvesSignaturesCrV17}) est contrôlée
 * comme une publication, puis publiée sous le numéro suivant (la version publiée en cours est retirée), avec ses
 * étapes ; les rangs, prédécesseurs, gestes, jalons et déclencheurs sont ceux de l'annexe A v1.7, inchangés. Les
 * liaisons non closes du type y sont rattachées sans changer leur état d'exécution (état, preuve, dépôts, échéances,
 * date de clôture, signatures recueillies) :
 * <ul>
 *   <li>opération ouverte (verrouillée, à faire, en cours, en validation, bloquée) : modèle de la version (libellé,
 *   livrables, signatures, délais, responsable, étape, vue) ; R et V passés, quand leur fonction change, à la personne
 *   affectée sur la liaison, sinon à la fonction à affecter ; une opération en validation dont la preuve est déposée
 *   reçoit cette pièce, marquée reprise, comme dépôt de ses livrables obligatoires encore absents (rien ne lui est
 *   redemandé) ;</li>
 *   <li>opération faite, sans objet ou en signature : version et liste des livrables attendus (affichage), sans
 *   signature exigée ; rien ne lui est jamais demandé, une opération reprise (D-107) s'affiche « reprise, pièce non
 *   numérisée ».</li>
 * </ul>
 * Idempotente : un type qui a déjà sa version de ce libellé est ignoré.
 */
public class RepriseReferentielPreuvesCrV17 implements CustomTaskChange {

    /** États d'une opération pas aboutie : elle reçoit le modèle complet de la version. */
    static final String ETATS_OUVERTS = RepriseReferentielCrV17.ETATS_A_FAIRE;

    /** Anomalies tolérées à la reprise : le catalogue des fonctions n'est pas chargé pendant la migration. */
    static final Set<ReferentialGraphValidator.TypeAnomalie> TOLEREES = EnumSet.of(
        ReferentialGraphValidator.TypeAnomalie.FONCTION_INCONNUE,
        ReferentialGraphValidator.TypeAnomalie.SIGNATAIRE_INCONNU
    );

    private int versionsCreees;

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            Connection c = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
            versionsCreees = executer(c, Instant.now()).size();
        } catch (SQLException e) {
            throw new CustomChangeException("Reprise des preuves et signatures du CR v1.7 en échec", e);
        }
    }

    /** Exécute la reprise ; rend, par type repris, l'identifiant de la version publiée. */
    public static Map<TypeProjet, UUID> executer(Connection c, Instant maintenant) throws SQLException {
        return executer(c, maintenant, Set.of(TypeProjet.values()));
    }

    /** Même reprise, limitée à certains types de projet. */
    public static Map<TypeProjet, UUID> executer(Connection c, Instant maintenant, Set<TypeProjet> types) throws SQLException {
        Map<TypeProjet, UUID> reprises = new LinkedHashMap<>();
        String libelle = PreuvesSignaturesCrV17.libelleVersion();
        for (TypeProjet type : TypeProjet.values()) {
            if (!types.contains(type)) {
                continue;
            }
            if (RepriseReferentielCr.entier(c, "select count(*) from referential_version where type_projet = ? and libelle = ?", type.name(), libelle) > 0) {
                continue;
            }
            reprises.put(type, reprendre(c, type, libelle, maintenant));
        }
        return reprises;
    }

    /** Version complétée d'un type, contrôlée comme une publication (anomalies hors catalogue : refus). */
    static ReferentialVersion version(TypeProjet type, String libelle, Instant maintenant) {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(type, libelle, maintenant);
        PreuvesSignaturesCrV17.version(type).forEach(o -> v.upsertOperation(o.code(), o.data()));
        if (!new ReferentialCompletenessChecker().check(v.getOperations()).isEmpty()) {
            throw new IllegalStateException("Version des preuves et signatures incomplète pour " + type);
        }
        List<ReferentialGraphValidator.Anomalie> anomalies = new ReferentialGraphValidator()
            .controler(v.noeuds())
            .anomalies()
            .stream()
            .filter(a -> !TOLEREES.contains(a.type()))
            .toList();
        if (!anomalies.isEmpty()) {
            throw new IllegalStateException("Version des preuves et signatures non publiable pour " + type + " : " + anomalies);
        }
        return v;
    }

    private static UUID reprendre(Connection c, TypeProjet type, String libelle, Instant maintenant) throws SQLException {
        ReferentialVersion v = version(type, libelle, maintenant);
        UUID id = UUID.randomUUID();
        v.setId(id);
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
                "update reference_operation set signatures = ?::jsonb, vue = ?, consultes_recoivent_tache = ?, delai_max_jours = ? where version_id = ? and code = ?",
                sig.isEmpty() ? null : JsonTexte.ecrire(sig),
                o.vueEffective().name(),
                o.isConsultesRecoiventTache(),
                o.getDelaiMaxJours(),
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
        reaffecter(c, type, liaisons, parCode);
        RepriseReferentielCr.executerMaj(c, "update liaison set version_id = ? where id in " + liaisons, id, type.name());
        RepriseReferentielCr.executerMaj(c, "update parcours_liaison set version_id = ?, version_numero = ? where liaison_id in " + liaisons, id, numero, type.name());
        RepriseReferentielCr.executerMaj(c, "update parcours_operation set version_id = ? where liaison_id in " + liaisons, id, type.name());
        try (
            PreparedStatement ouvertes = c.prepareStatement(
                "update parcours_operation set libelle = ?, geste = ?, jalon = ?, declencheur = ?, particularite = ?, livrables = ?::jsonb, delai_jours = ?, " +
                "delai_max_jours = ?, responsable = ?, signatures = ?::jsonb, etape_code = ?, vue = ?, consultes_recoivent_tache = ? " +
                "where rang = ? and etat in " + ETATS_OUVERTS + " and liaison_id in " + liaisons
            );
            PreparedStatement closes = c.prepareStatement(
                "update parcours_operation set livrables = ?::jsonb where rang = ? and etat not in " + ETATS_OUVERTS + " and liaison_id in " + liaisons
            )
        ) {
            for (ReferenceOperation o : operations) {
                List<SignatureOperation> sig = o.getSignatures();
                int i = 1;
                ouvertes.setString(i++, o.getLibelle());
                ouvertes.setString(i++, o.gesteEffectif().name());
                ouvertes.setString(i++, o.getJalon() == null ? null : o.getJalon().name());
                ouvertes.setString(i++, o.getDeclencheur());
                ouvertes.setString(i++, o.getParticularite() == null ? null : o.getParticularite().name());
                ouvertes.setString(i++, JsonTexte.ecrire(o.getLivrables()));
                ouvertes.setInt(i++, o.delaiEffectif());
                ouvertes.setInt(i++, o.delaiMaxEffectif());
                ouvertes.setString(i++, o.responsableEffectif().name());
                ouvertes.setString(i++, sig.isEmpty() ? null : JsonTexte.ecrire(sig));
                ouvertes.setString(i++, o.getEtape() != null && !o.getEtape().isBlank() ? o.getEtape() : o.etapeEffective().name());
                ouvertes.setString(i++, o.vueEffective().name());
                ouvertes.setBoolean(i++, o.isConsultesRecoiventTache());
                ouvertes.setString(i++, o.getCode());
                ouvertes.setString(i, type.name());
                ouvertes.addBatch();
                closes.setString(1, JsonTexte.ecrire(o.getLivrables()));
                closes.setString(2, o.getCode());
                closes.setString(3, type.name());
                closes.addBatch();
            }
            ouvertes.executeBatch();
            closes.executeBatch();
        }
        reprendreDepotsEnValidation(c, type, liaisons, parCode);
    }

    /**
     * R et V des opérations ouvertes dont la fonction change : la personne affectée à la nouvelle fonction sur la
     * liaison (ressources du rang 6), sinon la fonction à affecter ; une qualité tenue hors de l'ancienne fonction
     * (attribution explicite) est conservée.
     */
    private static void reaffecter(Connection c, TypeProjet type, String liaisons, Map<String, ReferenceOperation> parCode) throws SQLException {
        record Ligne(UUID id, String rang, String raci, String ressources) {}
        List<Ligne> lignes = new ArrayList<>();
        try (
            PreparedStatement ps = c.prepareStatement(
                "select po.id, po.rang, po.raci::text, pl.ressources::text from parcours_operation po " +
                "left join parcours_liaison pl on pl.liaison_id = po.liaison_id where po.etat in " + ETATS_OUVERTS + " and po.liaison_id in " + liaisons
            )
        ) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lignes.add(new Ligne(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4)));
                }
            }
        }
        for (Ligne l : lignes) {
            ReferenceOperation ref = parCode.get(l.rang());
            if (ref == null || l.raci() == null) {
                continue;
            }
            List<Personne> ressources = l.ressources() == null ? List.of() : JsonTexte.liste(l.ressources(), Personne.class);
            Raci avant = JsonTexte.lire(l.raci(), Raci.class);
            Raci apres = avant;
            Fonction r = ref.responsableEffectif();
            if (apres.r() != null && apres.r().fonction() != null && !r.equals(apres.r().fonction()) && !r.externe() && !apres.r().fonction().externe()) {
                apres = apres.avecR(titulaire(ressources, r));
            }
            Fonction v = ref.getValidateur();
            if (v != null && apres.v() != null && apres.v().fonction() != null && !v.equals(apres.v().fonction()) && !v.externe()) {
                apres = apres.avecV(titulaire(ressources, v));
            }
            if (!apres.equals(avant)) {
                RepriseReferentielCr.executerMaj(c, "update parcours_operation set raci = ?::jsonb where id = ?", JsonTexte.ecrire(apres), l.id());
            }
        }
    }

    private static Personne titulaire(List<Personne> ressources, Fonction f) {
        return ressources.stream().filter(p -> f.equals(p.fonction())).findFirst().orElse(Personne.aAffecter(f));
    }

    /**
     * Opérations en validation : la preuve déjà déposée (hors reprise D-107) vaut dépôt, marqué repris, de chaque
     * livrable obligatoire encore absent, pour que la validation en cours ne soit pas bloquée par la nouvelle exigence.
     */
    private static void reprendreDepotsEnValidation(Connection c, TypeProjet type, String liaisons, Map<String, ReferenceOperation> parCode) throws SQLException {
        record Ligne(UUID id, String rang, String preuve, String depots) {}
        List<Ligne> lignes = new ArrayList<>();
        try (
            PreparedStatement ps = c.prepareStatement(
                "select id, rang, preuve::text, depots::text from parcours_operation where etat = 'EN_VALIDATION' and preuve is not null and liaison_id in " + liaisons
            )
        ) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lignes.add(new Ligne(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4)));
                }
            }
        }
        for (Ligne l : lignes) {
            ReferenceOperation ref = parCode.get(l.rang());
            Preuve p = JsonTexte.lire(l.preuve(), Preuve.class);
            if (ref == null || p == null || p.estReprise() || p.documentRef() == null || p.documentRef().isBlank()) {
                continue;
            }
            List<DepotLivrable> depots = new ArrayList<>(l.depots() == null ? List.of() : JsonTexte.liste(l.depots(), DepotLivrable.class));
            boolean ajout = false;
            for (Livrable x : ref.getLivrables()) {
                if (x.obligatoire() && depots.stream().noneMatch(d -> d.livrable().equals(x.code()))) {
                    depots.add(new DepotLivrable(x.code(), p.documentRef(), p.nom(), p.empreinte(), null, p.deposeLe(), p.deposePar(), null, null, Boolean.TRUE));
                    ajout = true;
                }
            }
            if (ajout) {
                RepriseReferentielCr.executerMaj(c, "update parcours_operation set depots = ?::jsonb where id = ?", JsonTexte.ecrire(depots), l.id());
            }
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Preuves, livrables et signatures du CR v1.7 repris en version publiée : " + versionsCreees + " type(s) de projet";
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
