package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.parcours.CatalogueFonctions;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Fonction du catalogue (D-91, docs/lot1/types-projet.md §2) : code (figé), libellé, profils IAM qui la confèrent
 * (le premier sert à chercher les intervenants dans l'annuaire), tiers ou non, active ou non, permissions qui la
 * caractérisent (D-94 : candidats d'une fonction à affecter). Une fonction ne se supprime pas : elle se désactive.
 */
@Entity
@Table(name = "referentiel_fonction")
public class DefinitionFonction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Profil IAM : ROLE_ suivi de majuscules, chiffres ou soulignés. */
    public static final Pattern PROFIL = Pattern.compile("^ROLE_[A-Z0-9_]{1,60}$");

    /** Permission IAM : majuscule, puis majuscules, chiffres ou soulignés (80 caractères au plus). */
    public static final Pattern PERMISSION = Pattern.compile("^[A-Z][A-Z0-9_]{1,79}$");

    @Id
    @Column(name = "code", length = 40)
    private String code;

    @Column(name = "libelle", length = 120, nullable = false)
    private String libelle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profils", columnDefinition = "jsonb", nullable = false)
    private String profils;

    @Column(name = "externe", nullable = false)
    private boolean externe;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "jsonb", nullable = false)
    private String permissions = "[]";

    @Column(name = "actif", nullable = false)
    private boolean actif;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "cree_par")
    private UUID creePar;

    @Column(name = "modifie_le")
    private Instant modifieLe;

    @Column(name = "modifie_par")
    private UUID modifiePar;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected DefinitionFonction() {}

    public static DefinitionFonction creer(String code, String libelle, List<String> profils, boolean externe, UUID acteur, Instant instant) {
        return creer(code, libelle, profils, externe, null, acteur, instant);
    }

    public static DefinitionFonction creer(String code, String libelle, List<String> profils, boolean externe, List<String> permissions, UUID acteur, Instant instant) {
        if (code == null || !Fonction.CODE.matcher(code).matches()) {
            throw HomologationException.unprocessable(
                "HOM_FONCTION_CODE_INVALIDE",
                "Le code d'une fonction compte 2 à 40 majuscules, chiffres ou soulignés et commence par une lettre."
            );
        }
        DefinitionFonction f = new DefinitionFonction();
        f.code = code;
        f.libelle = libelle(libelle);
        f.profils = JsonTexte.ecrire(profils(profils, externe));
        f.externe = externe;
        f.permissions = JsonTexte.ecrire(permissions(permissions));
        f.actif = true;
        f.creeLe = instant;
        f.creePar = acteur;
        return f;
    }

    /** Modifie libellé, profils, caractère externe et état actif ; une valeur absente est gardée. Rend les champs changés. */
    public List<String> modifier(String nouveauLibelle, List<String> nouveauxProfils, Boolean nouvelExterne, Boolean nouvelActif, UUID acteur, Instant instant) {
        return modifier(nouveauLibelle, nouveauxProfils, nouvelExterne, nouvelActif, null, acteur, instant);
    }

    /** Modifie aussi les permissions qui caractérisent la fonction ; une valeur absente est gardée. Rend les champs changés. */
    public List<String> modifier(
        String nouveauLibelle,
        List<String> nouveauxProfils,
        Boolean nouvelExterne,
        Boolean nouvelActif,
        List<String> nouvellesPermissions,
        UUID acteur,
        Instant instant
    ) {
        List<String> changes = new ArrayList<>();
        if (nouveauLibelle != null && !libelle(nouveauLibelle).equals(libelle)) {
            libelle = libelle(nouveauLibelle);
            changes.add("libelle");
        }
        boolean ext = nouvelExterne == null ? externe : nouvelExterne;
        List<String> p = profils(nouveauxProfils == null ? getProfils() : nouveauxProfils, ext);
        if (!p.equals(getProfils())) {
            profils = JsonTexte.ecrire(p);
            changes.add("profils");
        }
        if (ext != externe) {
            externe = ext;
            changes.add("externe");
        }
        if (nouvellesPermissions != null && !permissions(nouvellesPermissions).equals(getPermissions())) {
            permissions = JsonTexte.ecrire(permissions(nouvellesPermissions));
            changes.add("permissions");
        }
        if (nouvelActif != null && nouvelActif != actif) {
            actif = nouvelActif;
            changes.add("actif");
        }
        if (!changes.isEmpty()) {
            modifieLe = instant;
            modifiePar = acteur;
        }
        return List.copyOf(changes);
    }

    private static String libelle(String l) {
        String t = l == null ? "" : l.trim();
        if (t.isEmpty() || t.length() > 120) {
            throw HomologationException.unprocessable("HOM_FONCTION_LIBELLE_INVALIDE", "Le libellé d'une fonction compte 1 à 120 caractères.");
        }
        return t;
    }

    /** Profils distincts et bien formés ; une fonction interne en exige au moins un (recherche des intervenants). */
    private static List<String> profils(List<String> p, boolean externe) {
        List<String> l = p == null ? List.of() : p.stream().filter(Objects::nonNull).map(String::trim).distinct().toList();
        if (l.size() > 20 || l.stream().anyMatch(x -> !PROFIL.matcher(x).matches())) {
            throw HomologationException.unprocessable("HOM_FONCTION_PROFIL_INVALIDE", "Un profil est un code IAM de la forme ROLE_… (20 au plus).");
        }
        if (l.isEmpty() && !externe) {
            throw HomologationException.unprocessable("HOM_FONCTION_PROFIL_REQUIS", "Une fonction interne est reliée à au moins un profil.");
        }
        return l;
    }

    /** Permissions distinctes et bien formées, 20 au plus (D-94). */
    private static List<String> permissions(List<String> p) {
        List<String> l = p == null ? List.of() : p.stream().filter(Objects::nonNull).map(String::trim).distinct().toList();
        if (l.size() > 20 || l.stream().anyMatch(x -> !PERMISSION.matcher(x).matches())) {
            throw HomologationException.unprocessable("HOM_FONCTION_PERMISSION_INVALIDE", "Une permission est un code IAM en majuscules (20 au plus).");
        }
        return l;
    }

    public CatalogueFonctions.Definition definition() {
        return new CatalogueFonctions.Definition(code, libelle, getProfils(), externe, actif, getPermissions());
    }

    public List<String> getPermissions() {
        return permissions == null ? List.of() : JsonTexte.listeDeTextes(permissions);
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public List<String> getProfils() {
        return JsonTexte.listeDeTextes(profils);
    }

    public boolean isExterne() {
        return externe;
    }

    public boolean isActif() {
        return actif;
    }

    public Instant getCreeLe() {
        return creeLe;
    }

    public UUID getCreePar() {
        return creePar;
    }

    public Instant getModifieLe() {
        return modifieLe;
    }

    public UUID getModifiePar() {
        return modifiePar;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof DefinitionFonction f && code != null && code.equals(f.code));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "DefinitionFonction{code='" + code + "', actif=" + actif + "}";
    }
}
