package cm.gimac.des.pilotage.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.domain.ProcessedEvent;
import cm.gimac.des.pilotage.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lecture de iam.account-administered.v1 : DESACTIVATION et SUPPRESSION sont des départs ; PROFILS et
 * COMPTE_RATTACHEMENT_MODIFIE font revoir les rôles du compte (D-93) ; iam.configuration-changed.v1 d'une unité fait
 * revoir les rôles CHEF_UNITE.
 */
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

    @Test
    @DisplayName("D-93 : profils remplacés ou rattachement modifié : compte à revoir ; unité modifiée : code lu dans les champs")
    void changements() throws Exception {
        String type = ComptesIamConsumer.ADMINISTRE;
        assertThat(ComptesIamConsumer.changement(type, json.readTree("{\"action\":\"PROFILS\",\"compteId\":\"c1\"}"))).contains("c1");
        assertThat(ComptesIamConsumer.changement(type, json.readTree("{\"action\":\"COMPTE_RATTACHEMENT_MODIFIE\",\"compteId\":\"c1\"}"))).contains("c1");
        assertThat(ComptesIamConsumer.changement(type, json.readTree("{\"action\":\"FICHE\",\"compteId\":\"c1\"}"))).isEmpty();
        assertThat(ComptesIamConsumer.changement(type, json.readTree("{\"action\":\"PROFILS\"}"))).isEmpty();
        String conf = ComptesIamConsumer.CONFIGURATION;
        assertThat(ComptesIamConsumer.uniteModifiee(conf, json.readTree("{\"objet\":\"UNITE_ORGANISATIONNELLE\",\"champs\":[\"MODIFICATION\",\"code=POLE_RESEAU\",\"responsable=x\"]}"))).contains("POLE_RESEAU");
        assertThat(ComptesIamConsumer.uniteModifiee(conf, json.readTree("{\"objet\":\"MATRICE_POUVOIRS\",\"champs\":[\"code=X\"]}"))).isEmpty();
        assertThat(ComptesIamConsumer.uniteModifiee(conf, json.readTree("{\"objet\":\"UNITE_ORGANISATIONNELLE\",\"champs\":[\"code=\"]}"))).isEmpty();
        assertThat(ComptesIamConsumer.uniteModifiee(type, json.readTree("{\"objet\":\"UNITE_ORGANISATIONNELLE\",\"champs\":[\"code=X\"]}"))).isEmpty();
    }

    @Test
    @DisplayName("D-93 : aiguillage vers le cas d'utilisation, cause selon l'action, une seule fois par événement")
    void aiguillage() {
        InstancesProcessusUseCase processus = mock(InstancesProcessusUseCase.class);
        ProcessedEventRepository traites = mock(ProcessedEventRepository.class);
        ComptesIamConsumer c = new ComptesIamConsumer(processus, traites, json, Clock.fixed(Instant.parse("2026-10-08T08:00:00Z"), ZoneOffset.UTC));
        String id = UUID.randomUUID().toString();
        c.recevoir("{\"id\":\"" + id + "\",\"type\":\"iam.account-administered.v1\",\"data\":{\"action\":\"PROFILS\",\"compteId\":\"c1\",\"acteurId\":\"a1\"}}");
        verify(processus).traiterChangementCompte("c1", "PROFIL_RETIRE", "a1");
        verify(traites).save(ArgumentMatchers.any(ProcessedEvent.class));
        c.recevoir("{\"type\":\"iam.account-administered.v1\",\"data\":{\"action\":\"COMPTE_RATTACHEMENT_MODIFIE\",\"compteId\":\"c2\"}}");
        verify(processus).traiterChangementCompte("c2", "CHANGEMENT_UNITE", null);
        c.recevoirConfiguration("{\"type\":\"iam.configuration-changed.v1\",\"data\":{\"objet\":\"UNITE_ORGANISATIONNELLE\",\"acteurId\":\"a1\",\"champs\":[\"code=SNA\"]}}");
        verify(processus).traiterChangementChef("SNA", "a1");
        // Déjà traité : ignoré ; message illisible : ignoré.
        String deja = UUID.randomUUID().toString();
        when(traites.existsByEventId(UUID.fromString(deja))).thenReturn(true);
        c.recevoirConfiguration("{\"id\":\"" + deja + "\",\"type\":\"iam.configuration-changed.v1\",\"data\":{\"objet\":\"UNITE_ORGANISATIONNELLE\",\"champs\":[\"code=DDRM\"]}}");
        c.recevoirConfiguration("pas du json");
        verify(processus, never()).traiterChangementChef(ArgumentMatchers.eq("DDRM"), anyString());
    }
}
