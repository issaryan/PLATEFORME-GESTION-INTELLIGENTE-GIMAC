package cm.gimac.des.notification.service.dto;

import cm.gimac.des.notification.domain.enumeration.Channel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.notification.domain.MessageTemplate} entity.
 */
@Schema(
    description = "Gabarit par type de notification, canal et langue (BNF-18). Pour MESSAGERIE\net SMS, le gabarit est obligatoirement minimal (RG-64) : type, niveau,\nidentifiant de notification et invitation à se connecter."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageTemplateDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 150)
    private String notificationType;

    @NotNull
    private Channel channel;

    @NotNull
    @Size(max = 5)
    @Pattern(regexp = "^(fr|en)$")
    private String language;

    @NotNull
    @Size(max = 255)
    private String subjectPattern;

    @Lob
    private String bodyPattern;

    @Size(max = 500)
    @Schema(description = "Variables nommées admises, séparées par des virgules.")
    private String variables;

    @NotNull
    private Boolean minimal;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSubjectPattern() {
        return subjectPattern;
    }

    public void setSubjectPattern(String subjectPattern) {
        this.subjectPattern = subjectPattern;
    }

    public String getBodyPattern() {
        return bodyPattern;
    }

    public void setBodyPattern(String bodyPattern) {
        this.bodyPattern = bodyPattern;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public Boolean getMinimal() {
        return minimal;
    }

    public void setMinimal(Boolean minimal) {
        this.minimal = minimal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageTemplateDTO)) {
            return false;
        }

        MessageTemplateDTO messageTemplateDTO = (MessageTemplateDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, messageTemplateDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageTemplateDTO{" +
            "id='" + getId() + "'" +
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
