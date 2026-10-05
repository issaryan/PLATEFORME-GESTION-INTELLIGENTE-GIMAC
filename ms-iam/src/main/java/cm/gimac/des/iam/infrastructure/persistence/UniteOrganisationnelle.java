package cm.gimac.des.iam.infrastructure.persistence;

import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Unité organisationnelle du GIMAC (D-76, docs/lot1/mon-activite.md §1.1). */
@Entity
@Table(name = "unite_organisationnelle")
public class UniteOrganisationnelle {

    @Id
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "libelle", nullable = false, length = 120)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TypeUnite type;

    @Column(name = "parent", length = 32)
    private String parent;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected UniteOrganisationnelle() {}

    public UniteOrganisationnelle(String code) {
        this.code = code;
    }

    public final void appliquer(Unite u) {
        this.libelle = u.libelle();
        this.type = u.type();
        this.parent = u.parent();
        this.active = u.active();
    }

    public Unite versDomaine() {
        return new Unite(code, libelle, type, parent, active);
    }
}
