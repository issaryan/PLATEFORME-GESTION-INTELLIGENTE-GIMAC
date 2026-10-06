package cm.gimac.des.pilotage.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lecture de iam.account-administered.v1 : seuls DESACTIVATION et SUPPRESSION sont des départs. */
class ComptesIamConsumerTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("Départ : désactivation ou suppression d'un compte ; tout autre acte est ignoré")
    void depart() throws Exception {
        String type = ComptesIamConsumer.ADMINISTRE;
        assertThat(ComptesIamConsumer.depart(type, json.readTree("{\"action\":\"DESACTIVATION\",\"compteId\":\"c1\"}"))).contains("c1");
        assertThat(ComptesIamConsumer.depart(type, json.readTree("{\"action\":\"SUPPRESSION\",\"compteId\":\"c1\"}"))).contains("c1");
        assertThat(ComptesIamConsumer.depart(type, json.readTree("{\"action\":\"ACTIVATION\",\"compteId\":\"c1\"}"))).isEmpty();
        assertThat(ComptesIamConsumer.depart(type, json.readTree("{\"action\":\"DESACTIVATION\"}"))).isEmpty();
        assertThat(ComptesIamConsumer.depart("iam.avatar-changed.v1", json.readTree("{\"action\":\"DESACTIVATION\",\"compteId\":\"c1\"}"))).isEmpty();
    }
}
