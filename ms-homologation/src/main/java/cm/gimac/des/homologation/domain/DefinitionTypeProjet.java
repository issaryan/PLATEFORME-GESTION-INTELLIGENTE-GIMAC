package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.ModeleTypeProjet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Type de projet (D-91, docs/lot1/types-projet.md §1) : code (identifiant, gardé en texte dans les liaisons et les
 * versions du référentiel), libellé, description, modèle dont il hérite les comportements, état actif. Le code et le
 * modèle sont figés après la première publication d'une version de son référentiel ; un type ne se supprime pas.
 */
@Entity
@Table(name = "type_projet")
public class DefinitionTypeProjet implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "code", length = 40)
    private String code;

    @Column(name = "libelle", length = 120, nullable = false)
    private String libelle;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "modele", length = 30, nullable = false)
    private ModeleTypeProjet modele;

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

    protected DefinitionTypeProjet() {}

    /** Nouveau type, actif ; code au format {@link TypeProjet#CODE}, libellé 1..120, description 0..1000. */
    public static DefinitionTypeProjet creer(String code, String libelle, String description, ModeleTypeProjet modele, UUID acteur, Instant instant) {
        if (code == null || !TypeProjet.CODE.matcher(code).matches()) {
            throw HomologationException.unprocessable(
                "HOM_TYPE_PROJET_CODE_INVALIDE",
                "Le code d'un type de projet compte 3 à 40 majuscules, chiffres ou soulignés et commence par une lettre."
            );
        }
        if (modele == null) {
            throw HomologationException.unprocessable("HOM_TYPE_PROJET_MODELE_INVALIDE", "Le modèle du type de projet est exigé.");
        }
        DefinitionTypeProjet t = new DefinitionTypeProjet();
        t.code = code;
        t.libelle = libelle(libelle);
        t.description = description(description);
        t.modele = modele;
        t.actif = true;
        t.creeLe = instant;
        t.creePar = acteur;
        return t;
    }

    /**
     * Modifie le libellé, la description et l'état actif ; rend les champs changés. Une valeur absente est gardée.
     */
    public List<String> modifier(String nouveauLibelle, String nouvelleDescription, Boolean nouvelActif, UUID acteur, Instant instant) {
        List<String> changes = new ArrayList<>();
        if (nouveauLibelle != null && !libelle(nouveauLibelle).equals(libelle)) {
            libelle = libelle(nouveauLibelle);
            changes.add("libelle");
        }
        if (nouvelleDescription != null && !Objects.equals(description(nouvelleDescription), description)) {
            description = description(nouvelleDescription);
            changes.add("description");
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

    /** Nouveau modèle, tant qu'aucune version du référentiel n'est publiée (l'appelant le vérifie) ; vrai s'il change. */
    public boolean changerModele(ModeleTypeProjet nouveau, UUID acteur, Instant instant) {
        if (nouveau == null || nouveau == modele) {
            return false;
        }
        modele = nouveau;
        modifieLe = instant;
        modifiePar = acteur;
        return true;
    }

    private static String libelle(String l) {
        String t = l == null ? "" : l.trim();
        if (t.isEmpty() || t.length() > 120) {
            throw HomologationException.unprocessable("HOM_TYPE_PROJET_LIBELLE_INVALIDE", "Le libellé d'un type de projet compte 1 à 120 caractères.");
        }
        return t;
    }

    private static String description(String d) {
        String t = d == null ? "" : d.trim();
        if (t.length() > 1000) {
            throw HomologationException.unprocessable("HOM_TYPE_PROJET_DESCRIPTION_INVALIDE", "La description d'un type de projet compte au plus 1000 caractères.");
        }
        return t.isEmpty() ? null : t;
    }

    public TypeProjet type() {
        return TypeProjet.valueOf(code);
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getDescription() {
        return description;
    }

    public ModeleTypeProjet getModele() {
        return modele;
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
        return this == o || (o instanceof DefinitionTypeProjet t && code != null && code.equals(t.code));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "DefinitionTypeProjet{code='" + code + "', modele=" + modele + ", actif=" + actif + "}";
    }
}
