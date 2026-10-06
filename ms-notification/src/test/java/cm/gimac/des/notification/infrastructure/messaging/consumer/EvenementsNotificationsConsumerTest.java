package cm.gimac.des.notification.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.DemandeDecidee;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.DemandeSoumise;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.EvenementProcessus;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.MessageEnvoye;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.RapportSoumis;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheAffectee;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheOfferte;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheRelancee;
import cm.gimac.des.notification.domain.service.ReglesNotifications.FaitsProcessus;
import cm.gimac.des.notification.domain.service.ReglesNotifications.NatureProcessus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Lecture des enveloppes CloudEvents et idempotence du consommateur (table processed_event). */
class EvenementsNotificationsConsumerTest {

    ProcessedEventRepository traites;
    RecevoirEvenementsUseCase evenements;
    EvenementsNotificationsConsumer consommateur;

    @BeforeEach
    void init() {
        traites = mock(ProcessedEventRepository.class);
        evenements = mock(RecevoirEvenementsUseCase.class);
        consommateur = new EvenementsNotificationsConsumer(new IdempotentConsumer(traites), evenements, new ObjectMapper());
    }

    @Test
    @DisplayName("collaboration.message-sent.v1 : charge lue (destinataires, auteur, instant, événement de groupe)")
    void messageEnvoye() throws Exception {
        consommateur.recevoir(
            "{\"specversion\":\"1.0\",\"id\":\"e-1\",\"type\":\"collaboration.message-sent.v1\",\"data\":{\"conversationId\":\"c-1\",\"messageId\":\"m-1\"," +
            "\"auteurId\":\"paul\",\"destinataires\":[\"aminata\",\"serge\"],\"envoyeLe\":\"2026-10-05T08:00:00Z\",\"evenement\":null}}"
        );
        ArgumentCaptor<MessageEnvoye> m = ArgumentCaptor.forClass(MessageEnvoye.class);
        verify(evenements).messageEnvoye(m.capture());
        assertThat(m.getValue()).isEqualTo(new MessageEnvoye("c-1", "m-1", "paul", List.of("aminata", "serge"), Instant.parse("2026-10-05T08:00:00Z"), null));
        // Ancien événement (avant D-82) : ni réponse ni mention.
        assertThat(m.getValue().reponseAAuteurId()).isNull();
        assertThat(m.getValue().mentionnes()).isEmpty();
        verify(traites).save(any());
    }

    @Test
    @DisplayName("collaboration.message-sent.v1 (D-82) : réponse et mentions lues ; absentes des anciens événements : null et liste vide")
    void messageAvecMentionsEtReponse() throws Exception {
        consommateur.recevoir(
            "{\"id\":\"e-3\",\"type\":\"collaboration.message-sent.v1\",\"data\":{\"conversationId\":\"c-1\",\"messageId\":\"m-2\"," +
            "\"auteurId\":\"paul\",\"destinataires\":[\"aminata\",\"serge\"],\"envoyeLe\":\"2026-10-05T08:00:00Z\",\"evenement\":null," +
            "\"reponseAAuteurId\":\"aminata\",\"mentionnes\":[\"serge\"]}}"
        );
        consommateur.recevoir(
            "{\"id\":\"e-4\",\"type\":\"collaboration.message-sent.v1\",\"data\":{\"conversationId\":\"c-1\",\"messageId\":\"m-3\"," +
            "\"auteurId\":\"paul\",\"destinataires\":[\"aminata\"],\"reponseAAuteurId\":null,\"mentionnes\":[]}}"
        );
        ArgumentCaptor<MessageEnvoye> m = ArgumentCaptor.forClass(MessageEnvoye.class);
        verify(evenements, times(2)).messageEnvoye(m.capture());
        assertThat(m.getAllValues().get(0).reponseAAuteurId()).isEqualTo("aminata");
        assertThat(m.getAllValues().get(0).mentionnes()).containsExactly("serge");
        assertThat(m.getAllValues().get(1).reponseAAuteurId()).isNull();
        assertThat(m.getAllValues().get(1).mentionnes()).isEmpty();
    }

    @Test
    @DisplayName("Idempotence : un événement déjà traité par le groupe est acquitté sans effet")
    void idempotence() throws Exception {
        String e = "{\"id\":\"e-2\",\"type\":\"pilotage.task-assigned.v1\",\"data\":{\"tacheId\":\"t-1\",\"responsableId\":\"aminata\",\"acteurId\":\"paul\",\"le\":1759651200.5}}";
        AtomicBoolean deja = new AtomicBoolean(false);
        when(traites.existsByEventIdAndConsumerGroup("e-2", EvenementsNotificationsConsumer.GROUPE)).thenAnswer(i -> deja.get());
        when(traites.save(any())).thenAnswer(i -> {
            deja.set(true);
            return i.getArgument(0);
        });
        consommateur.recevoir(e);
        consommateur.recevoir(e);
        ArgumentCaptor<TacheAffectee> t = ArgumentCaptor.forClass(TacheAffectee.class);
        verify(evenements, times(1)).tacheAffectee(t.capture());
        assertThat(t.getValue().responsableId()).isEqualTo("aminata");
        assertThat(t.getValue().le()).isEqualTo(Instant.ofEpochSecond(1759651200L, 500_000_000L));
        verify(traites, times(1)).save(any());
    }

    @Test
    @DisplayName("pilotage.report-submitted.v1 lu ; autres types (task-created, report-visaed) et enveloppes sans identifiant ignorés")
    void rapportEtTypesIgnores() throws Exception {
        consommateur.recevoir(
            "{\"id\":\"e-3\",\"type\":\"pilotage.report-submitted.v1\",\"data\":{\"rapportId\":\"r-1\",\"type\":\"QUOTIDIEN\",\"periode\":\"2026-10-05\"," +
            "\"etat\":\"SOUMIS\",\"auteurId\":\"serge\",\"destinataireId\":\"paul\",\"acteurId\":\"serge\",\"le\":\"2026-10-05T09:00:00Z\"}}"
        );
        verify(evenements).rapportSoumis(new RapportSoumis("r-1", "QUOTIDIEN", "2026-10-05", "serge", "paul", "serge", Instant.parse("2026-10-05T09:00:00Z")));
        consommateur.recevoir("{\"id\":\"e-4\",\"type\":\"pilotage.task-created.v1\",\"data\":{}}");
        consommateur.recevoir("{\"id\":\"e-5\",\"type\":\"pilotage.report-visaed.v1\",\"data\":{}}");
        consommateur.recevoir("{\"type\":\"collaboration.message-sent.v1\",\"data\":{}}");
        verify(evenements, never()).tacheAffectee(any());
        verify(evenements, never()).messageEnvoye(any());
        verify(traites, never()).existsByEventIdAndConsumerGroup(eq("e-4"), anyString());
    }

    @Test
    @DisplayName("Donnée transmise en chaîne JSON : lue comme un objet")
    void donneeEnChaine() throws Exception {
        RecevoirEvenementsUseCase autre = mock(RecevoirEvenementsUseCase.class);
        new EvenementsNotificationsConsumer(new IdempotentConsumer(traites), autre, new ObjectMapper()).recevoir(
            "{\"id\":\"e-6\",\"type\":\"collaboration.message-sent.v1\",\"data\":\"{\\\"conversationId\\\":\\\"c-9\\\",\\\"auteurId\\\":\\\"paul\\\",\\\"destinataires\\\":[\\\"serge\\\"]}\"}"
        );
        ArgumentCaptor<MessageEnvoye> m = ArgumentCaptor.forClass(MessageEnvoye.class);
        verify(autre).messageEnvoye(m.capture());
        assertThat(m.getValue().conversationId()).isEqualTo("c-9");
        assertThat(m.getValue().destinataires()).containsExactly("serge");
        verifyNoInteractions(evenements);
    }

    @Test
    @DisplayName("pilotage.task-offered.v1, pilotage.task-reminded.v1, iam.request-submitted.v1 et iam.request-decided.v1 : charges lues")
    void tachesAPrendreRelancesEtDemandes() throws Exception {
        consommateur.recevoir(
            "{\"id\":\"e-7\",\"type\":\"pilotage.task-offered.v1\",\"data\":{\"tacheId\":\"t-7\",\"titre\":\"Sauvegardes\",\"uniteCible\":\"POLE_RESEAU\"," +
            "\"destinataires\":[\"aminata\",\"serge\"],\"acteurId\":\"paul\",\"le\":\"2026-10-06T08:00:00Z\"}}"
        );
        verify(evenements).tacheOfferte(new TacheOfferte("t-7", "Sauvegardes", "POLE_RESEAU", List.of("aminata", "serge"), "paul", Instant.parse("2026-10-06T08:00:00Z")));
        consommateur.recevoir(
            "{\"id\":\"e-8\",\"type\":\"pilotage.task-reminded.v1\",\"data\":{\"tacheId\":\"t-7\",\"titre\":\"Sauvegardes\",\"responsableId\":\"serge\",\"acteurId\":\"paul\",\"le\":\"2026-10-06T09:00:00Z\"}}"
        );
        verify(evenements).tacheRelancee(new TacheRelancee("t-7", "Sauvegardes", "serge", "paul", Instant.parse("2026-10-06T09:00:00Z")));
        consommateur.recevoir(
            "{\"id\":\"e-9\",\"type\":\"iam.request-submitted.v1\",\"data\":{\"demandeId\":\"d-1\",\"type\":\"CREATION_COMPTE\",\"etat\":\"EN_ATTENTE\"," +
            "\"demandeurId\":\"paul\",\"cibleId\":null,\"destinataires\":[\"admin\",\"rssi\"],\"le\":\"2026-10-06T10:00:00Z\"}}"
        );
        verify(evenements).demandeSoumise(new DemandeSoumise("d-1", "CREATION_COMPTE", "paul", List.of("admin", "rssi"), Instant.parse("2026-10-06T10:00:00Z")));
        consommateur.recevoir(
            "{\"id\":\"e-10\",\"type\":\"iam.request-decided.v1\",\"data\":{\"demandeId\":\"d-1\",\"type\":\"CREATION_COMPTE\",\"etat\":\"REFUSEE\"," +
            "\"demandeurId\":\"paul\",\"acteurId\":\"admin\",\"destinataires\":[\"paul\"],\"le\":\"2026-10-06T11:00:00Z\"}}"
        );
        verify(evenements).demandeDecidee(new DemandeDecidee("d-1", "CREATION_COMPTE", "REFUSEE", "admin", List.of("paul"), Instant.parse("2026-10-06T11:00:00Z")));
        verify(traites, times(4)).save(any());
    }

    // ------------------------------------------------------------------ processus d'unité (D-87)

    static final String I = "5b3c7a10-0000-4000-8000-0000000000aa";
    static final String P = "5b3c7a10-0000-4000-8000-0000000000bb";
    static final String L = "/des/processus/instances/" + I;
    static final Instant LE = Instant.parse("2026-10-07T08:00:00Z");

    static String enveloppe(String id, String type, String data) {
        return "{\"specversion\":\"1.0\",\"id\":\"" + id + "\",\"type\":\"" + type + "\",\"source\":\"/ms-pilotage\",\"data\":" + data + "}";
    }

    EvenementProcessus recu(int fois) {
        ArgumentCaptor<EvenementProcessus> c = ArgumentCaptor.forClass(EvenementProcessus.class);
        verify(evenements, times(fois)).evenementProcessus(c.capture());
        return c.getValue();
    }

    @Test
    @DisplayName("D-87 : les six événements à destinataires de pilotage.process lus (acteur, précision, lien, destinataires) ; process-published ignoré")
    void processus() throws Exception {
        consommateur.recevoir(
            enveloppe(
                "p-1",
                "pilotage.process-instance-started.v1",
                "{\"instanceId\":\"" + I + "\",\"processusId\":\"" + P + "\",\"code\":\"OUVERTURE_FLUX_RESEAU\",\"version\":1,\"unite\":\"POLE_RESEAU\"," +
                "\"intitule\":\"Flux X\",\"lanceurId\":\"paul\",\"destinataires\":[\"chef\"],\"lien\":\"" + L + "\",\"le\":\"2026-10-07T08:00:00Z\"}"
            )
        );
        assertThat(recu(1)).isEqualTo(
            new EvenementProcessus(I, new FaitsProcessus(NatureProcessus.INSTANCE_LANCEE, "OUVERTURE_FLUX_RESEAU", "Flux X", null, null, null, false, false, null), "paul", null, List.of("chef"), L, LE)
        );

        consommateur.recevoir(
            enveloppe(
                "p-2",
                "pilotage.process-step-activated.v1",
                "{\"instanceId\":\"" + I + "\",\"processusId\":\"" + P + "\",\"code\":\"OUVERTURE_FLUX_RESEAU\",\"intitule\":\"Flux X\",\"etape\":\"ANALYSE\"," +
                "\"libelleEtape\":\"Analyse\",\"tacheId\":\"t-1\",\"executantId\":null,\"profil\":\"ROLE_POLE_RESEAU\",\"unite\":\"POLE_RESEAU\",\"echeance\":\"2026-10-09\"," +
                "\"renvoi\":false,\"motif\":null,\"destinataires\":[\"bob\",\"awa\"],\"lien\":\"" + L + "?etape=ANALYSE\",\"le\":\"2026-10-07T08:00:00Z\"}"
            )
        );
        assertThat(recu(2)).isEqualTo(
            new EvenementProcessus(I, new FaitsProcessus(NatureProcessus.ETAPE_ACTIVEE, "OUVERTURE_FLUX_RESEAU", "Flux X", "ANALYSE", "Analyse", null, false, true, "2026-10-09"), null, null, List.of("bob", "awa"), L + "?etape=ANALYSE", LE)
        );

        consommateur.recevoir(
            enveloppe(
                "p-3",
                "pilotage.process-step-activated.v1",
                "{\"instanceId\":\"" + I + "\",\"code\":\"C\",\"intitule\":\"Flux X\",\"etape\":\"ANALYSE\",\"libelleEtape\":\"Analyse\",\"executantId\":\"bob\"," +
                "\"renvoi\":true,\"motif\":\"Incomplet\",\"destinataires\":[\"bob\"],\"lien\":\"" + L + "\",\"le\":\"2026-10-07T08:00:00Z\"}"
            )
        );
        assertThat(recu(3).faits()).isEqualTo(new FaitsProcessus(NatureProcessus.ETAPE_ACTIVEE, "C", "Flux X", "ANALYSE", "Analyse", null, true, false, null));

        consommateur.recevoir(
            enveloppe(
                "p-4",
                "pilotage.process-step-submitted.v1",
                "{\"instanceId\":\"" + I + "\",\"code\":\"C\",\"intitule\":\"Flux X\",\"etape\":\"RECETTE\",\"libelleEtape\":\"Recette\",\"attente\":\"SIGNATURE\"," +
                "\"signatures\":[\"PV\"],\"acteurId\":\"bob\",\"destinataires\":[\"chef\"],\"lien\":\"" + L + "\",\"le\":\"2026-10-07T08:00:00Z\"}"
            )
        );
        EvenementProcessus soumise = recu(4);
        assertThat(soumise.faits().nature()).isEqualTo(NatureProcessus.ETAPE_SOUMISE);
        assertThat(soumise.faits().precision()).isEqualTo("SIGNATURE");
        assertThat(soumise.acteurId()).isEqualTo("bob");

        consommateur.recevoir(
            enveloppe(
                "p-5",
                "pilotage.process-step-signed.v1",
                "{\"instanceId\":\"" + I + "\",\"code\":\"C\",\"intitule\":\"Flux X\",\"etape\":\"RECETTE\",\"signature\":\"PV\",\"signataireId\":\"chef\"," +
                "\"empreinte\":\"ab12\",\"documents\":[{\"livrable\":\"PV_RECETTE\",\"documentId\":\"d-1\",\"sha256\":\"cd34\"}],\"etatEtape\":\"TERMINEE\"," +
                "\"destinataires\":[\"bob\"],\"lien\":\"" + L + "\",\"le\":\"2026-10-07T08:00:00Z\"}"
            )
        );
        EvenementProcessus signee = recu(5);
        assertThat(signee.faits()).isEqualTo(new FaitsProcessus(NatureProcessus.ETAPE_SIGNEE, "C", "Flux X", "RECETTE", null, "TERMINEE", false, false, null));
        assertThat(signee.acteurId()).isEqualTo("chef");

        consommateur.recevoir(
            enveloppe(
                "p-6",
                "pilotage.process-step-reassigned.v1",
                "{\"instanceId\":\"" + I + "\",\"code\":\"C\",\"intitule\":\"Flux X\",\"etape\":\"MISE_EN_OEUVRE\",\"cause\":\"DEPART\",\"roles\":[\"EXECUTANT\"]," +
                "\"ancienId\":\"parti\",\"executantId\":null,\"profil\":\"ROLE_POLE_RESEAU\",\"unite\":\"POLE_RESEAU\",\"chefId\":\"chef\",\"acteurId\":null,\"motif\":null," +
                "\"destinataires\":[\"awa\",\"chef\"],\"lien\":\"" + L + "\",\"le\":\"2026-10-07T08:00:00Z\"}"
            )
        );
        EvenementProcessus depart = recu(6);
        assertThat(depart.faits().precision()).isEqualTo("DEPART");
        assertThat(depart.acteurId()).isNull();
        assertThat(depart.ancienId()).isEqualTo("parti");
        assertThat(depart.destinataires()).containsExactly("awa", "chef");

        consommateur.recevoir(
            enveloppe(
                "p-7",
                "pilotage.process-step-reassigned.v1",
                "{\"instanceId\":\"" + I + "\",\"etape\":\"ANALYSE\",\"cause\":\"REAFFECTATION\",\"ancienId\":\"bob\",\"executantId\":\"awa\",\"acteurId\":\"chef\"," +
                "\"motif\":\"Congé\",\"destinataires\":[\"awa\"],\"lien\":\"" + L + "\",\"le\":\"2026-10-07T08:00:00Z\"}"
            )
        );
        EvenementProcessus reaffectee = recu(7);
        assertThat(reaffectee.acteurId()).isEqualTo("chef");
        assertThat(reaffectee.ancienId()).isNull();

        consommateur.recevoir(
            enveloppe(
                "p-8",
                "pilotage.process-instance-closed.v1",
                "{\"instanceId\":\"" + I + "\",\"processusId\":\"" + P + "\",\"code\":\"C\",\"intitule\":\"Flux X\",\"etat\":\"CLOSE\",\"lanceurId\":\"paul\"," +
                "\"acteurId\":\"chef\",\"motif\":null,\"destinataires\":[\"paul\"],\"lien\":\"" + L + "\",\"le\":\"2026-10-07T08:00:00Z\"}"
            )
        );
        assertThat(recu(8)).isEqualTo(
            new EvenementProcessus(I, new FaitsProcessus(NatureProcessus.INSTANCE_CLOSE, "C", "Flux X", null, null, "CLOSE", false, false, null), "chef", null, List.of("paul"), L, LE)
        );

        // Publication d'une version : aucun destinataire, aucune notification, rien d'inscrit.
        consommateur.recevoir(
            enveloppe("p-9", "pilotage.process-published.v1", "{\"processusId\":\"" + P + "\",\"code\":\"C\",\"version\":2,\"acteurId\":\"chef\",\"le\":\"2026-10-07T08:00:00Z\"}")
        );
        verify(evenements, times(8)).evenementProcessus(any());
        verify(traites, times(8)).save(any());
    }

    @Test
    @DisplayName("D-87 : un événement de processus rejoué n'est traité qu'une fois (processed_event)")
    void processusIdempotent() throws Exception {
        String e = enveloppe(
            "p-10",
            "pilotage.process-instance-closed.v1",
            "{\"instanceId\":\"" + I + "\",\"etat\":\"ANNULEE\",\"lanceurId\":\"paul\",\"acteurId\":\"chef\",\"destinataires\":[\"paul\"],\"lien\":\"" + L + "\"}"
        );
        AtomicBoolean deja = new AtomicBoolean(false);
        when(traites.existsByEventIdAndConsumerGroup("p-10", EvenementsNotificationsConsumer.GROUPE)).thenAnswer(i -> deja.get());
        when(traites.save(any())).thenAnswer(i -> {
            deja.set(true);
            return i.getArgument(0);
        });
        consommateur.recevoir(e);
        consommateur.recevoir(e);
        assertThat(recu(1).faits().precision()).isEqualTo("ANNULEE");
        verify(traites, times(1)).save(any());
    }
}
