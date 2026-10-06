package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.parcours.CouleurEtape;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Étape d'une version du référentiel (D-89, docs/lot1/referentiel-droits-etapes.md §2) : code, libellé, ordre,
 * couleur (jeton du design system) et étape canonique ({@link EtapeCode}) par laquelle passe la progression des
 * liaisons ; {@code canonique} vide : rattachée à la canonique de l'étape qui la précède.
 */
@Entity
@Table(name = "referential_etape")
@EntityListeners(EtapeReferentiel.Garde.class)
public class EtapeReferentiel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Code d'une étape : une majuscule puis 1 à 39 majuscules, chiffres ou soulignés. */
    public static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,39}$");

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private ReferentialVersion version;

    @Column(name = "code", length = 40, nullable = false)
    private String code;

    @Column(name = "libelle", length = 120, nullable = false)
    private String libelle;

    @Column(name = "ordre", nullable = false)
    private int ordre;

    @Enumerated(EnumType.STRING)
    @Column(name = "couleur", length = 20, nullable = false)
    private CouleurEtape couleur;

    @Enumerated(EnumType.STRING)
    @Column(name = "canonique", length = 40)
    private EtapeCode canonique;

    protected EtapeReferentiel() {}

    EtapeReferentiel(ReferentialVersion version, String code, String libelle, int ordre, CouleurEtape couleur, EtapeCode canonique) {
        this.version = version;
        this.code = code;
        this.libelle = libelle;
        this.ordre = ordre;
        this.couleur = couleur;
        this.canonique = canonique;
    }

    /** Étape canonique telle qu'en version 1. */
    static EtapeReferentiel canonique(ReferentialVersion version, EtapeCode e) {
        return new EtapeReferentiel(version, e.name(), e.libelle(), e.ordinal() + 1, e.couleur(), e);
    }

    EtapeReferentiel copierVers(ReferentialVersion cible) {
        return new EtapeReferentiel(cible, code, libelle, ordre, couleur, canonique);
    }

    void modifier(String libelle, CouleurEtape couleur, EtapeCode canonique) {
        this.libelle = libelle;
        this.couleur = couleur;
        this.canonique = canonique;
    }

    void ordonner(int ordre) {
        this.ordre = ordre;
    }

    public UUID getId() {
        return id;
    }

    public ReferentialVersion getVersion() {
        return version;
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public int getOrdre() {
        return ordre;
    }

    public CouleurEtape getCouleur() {
        return couleur;
    }

    public EtapeCode getCanonique() {
        return canonique;
    }

    /** Étape liée à une canonique porteuse d'un jalon ou du verrou 2 : ni supprimée, ni déliée. */
    public boolean protegee() {
        return canonique != null && canonique.protegee();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof EtapeReferentiel e && id != null && id.equals(e.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "EtapeReferentiel{code='" + code + "', ordre=" + ordre + ", canonique=" + canonique + "}";
    }

    /** Défense en profondeur : aucune écriture sur une étape d'une version publiée ou retirée. */
    public static class Garde {

        @PrePersist
        @PreUpdate
        @PreRemove
        void verifier(EtapeReferentiel etape) {
            if (etape.version != null) {
                etape.version.assertModifiable();
            }
        }
    }
}
