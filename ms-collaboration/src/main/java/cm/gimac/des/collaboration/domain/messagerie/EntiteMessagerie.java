package cm.gimac.des.collaboration.domain.messagerie;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Identifiant UUID attribué par le service (le message est chiffré avec son identifiant comme donnée
 * associée) : l'entité se déclare nouvelle jusqu'à son enregistrement, sans lecture préalable.
 */
@MappedSuperclass
public abstract class EntiteMessagerie implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean nouvelle = true;

    protected EntiteMessagerie() {
        this(UUID.randomUUID());
    }

    protected EntiteMessagerie(UUID id) {
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
        return this == o || (o != null && getClass() == o.getClass() && Objects.equals(id, ((EntiteMessagerie) o).id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
