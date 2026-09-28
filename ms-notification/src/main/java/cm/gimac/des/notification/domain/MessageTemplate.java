package cm.gimac.des.notification.domain;

import cm.gimac.des.notification.domain.enumeration.Channel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * Gabarit par type de notification, canal et langue (BNF-18). Pour MESSAGERIE
 * et SMS, le gabarit est obligatoirement minimal (RG-64) : type, niveau,
 * identifiant de notification et invitation à se connecter.
 */
@Entity
@Table(name = "message_template")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 150)
    @Column(name = "notification_type", length = 150, nullable = false)
    private String notificationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel;

    @NotNull
    @Size(max = 5)
    @Pattern(regexp = "^(fr|en)$")
    @Column(name = "language", length = 5, nullable = false)
    private String language;

    @NotNull
    @Size(max = 255)
    @Column(name = "subject_pattern", length = 255, nullable = false)
    private String subjectPattern;

    @Lob
    @Column(name = "body_pattern", nullable = false)
    private String bodyPattern;

    /**
     * Variables nommées admises, séparées par des virgules.
     */
    @Size(max = 500)
    @Column(name = "variables", length = 500)
    private String variables;

    @NotNull
    @Column(name = "minimal", nullable = false)
    private Boolean minimal;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public MessageTemplate id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNotificationType() {
        return this.notificationType;
    }

    public MessageTemplate notificationType(String notificationType) {
        this.setNotificationType(notificationType);
        return this;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public MessageTemplate channel(Channel channel) {
        this.setChannel(channel);
        return this;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getLanguage() {
        return this.language;
    }

    public MessageTemplate language(String language) {
        this.setLanguage(language);
        return this;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSubjectPattern() {
        return this.subjectPattern;
    }

    public MessageTemplate subjectPattern(String subjectPattern) {
        this.setSubjectPattern(subjectPattern);
        return this;
    }

    public void setSubjectPattern(String subjectPattern) {
        this.subjectPattern = subjectPattern;
    }

    public String getBodyPattern() {
        return this.bodyPattern;
    }

    public MessageTemplate bodyPattern(String bodyPattern) {
        this.setBodyPattern(bodyPattern);
        return this;
    }

    public void setBodyPattern(String bodyPattern) {
        this.bodyPattern = bodyPattern;
    }

    public String getVariables() {
        return this.variables;
    }

    public MessageTemplate variables(String variables) {
        this.setVariables(variables);
        return this;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public Boolean getMinimal() {
        return this.minimal;
    }

    public MessageTemplate minimal(Boolean minimal) {
        this.setMinimal(minimal);
        return this;
    }

    public void setMinimal(Boolean minimal) {
        this.minimal = minimal;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageTemplate)) {
            return false;
        }
        return getId() != null && getId().equals(((MessageTemplate) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageTemplate{" +
            "id=" + getId() +
            ", notificationType='" + getNotificationType() + "'" +
            ", channel='" + getChannel() + "'" +
            ", language='" + getLanguage() + "'" +
            ", subjectPattern='" + getSubjectPattern() + "'" +
            ", bodyPattern='" + getBodyPattern() + "'" +
            ", variables='" + getVariables() + "'" +
            ", minimal='" + getMinimal() + "'" +
            "}";
    }
}
