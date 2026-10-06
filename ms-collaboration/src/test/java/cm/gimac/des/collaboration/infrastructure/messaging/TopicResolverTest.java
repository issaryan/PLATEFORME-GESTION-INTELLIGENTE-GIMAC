package cm.gimac.des.collaboration.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TopicResolverTest {

    final TopicResolver topics = new TopicResolver();

    @Test
    @DisplayName("Sujet de l'agrégat ; l'accusé de lecture (D-88) suit les messages sur collaboration.message")
    void sujets() {
        assertThat(topics.topicFor("collaboration.message-sent.v1")).isEqualTo("collaboration.message");
        assertThat(topics.topicFor("collaboration.message-typing.v1")).isEqualTo("collaboration.message");
        assertThat(topics.topicFor("collaboration.conversation-read.v1")).isEqualTo("collaboration.message");
        assertThat(topics.topicFor("homologation.operation-closed.v1")).isEqualTo("homologation.operation");
        assertThatThrownBy(() -> topics.topicFor("mal.forme")).isInstanceOf(IllegalArgumentException.class);
    }
}
