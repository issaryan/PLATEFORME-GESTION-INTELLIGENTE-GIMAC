package cm.gimac.des.pilotage.domain.activite;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Identifiant UUID attribué par le service (un rapport est connu par son identifiant avant même son
 * enregistrement, pour servir de référence au dépôt de son document) : l'entité se déclare nouvelle
 * jusqu'à son enregistrement, sans lecture préalable.
 */
@MappedSuperclass
public abstract class EntitePilotage implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean nouvelle = true;

    protected EntitePilotage() {
        this(UUID.randomUUID());
    }

    protected EntitePilotage(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return nouvelle;
    }

    @PostLoad
    @PostPersist
    void marquerEnregistree() {
        nouvelle = false;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o != null && getClass() == o.getClass() && Objects.equals(id, ((EntitePilotage) o).id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
