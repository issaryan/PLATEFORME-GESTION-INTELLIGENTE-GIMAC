package cm.gimac.des.collaboration.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Traitement des echecs de consommation du journal d'audit.
 *
 * <p>
 * <b>Le defaut corrige.</b> Les neuf consommateurs enveloppaient le scellement
 * dans un {@code catch (Exception e) { log.error(...) }}. L'exception etant
 * absorbee, la methode rendait la main normalement et Kafka validait l'offset :
 * l'evenement etait <b>definitivement perdu</b>. Une indisponibilite passagere de
 * la base — un conflit de serialisation, une reconnexion — suffisait a effacer
 * une trace reglementaire. Le journal se presente comme une boite noire
 * exhaustive (COBAC R-2016/04) alors qu'il pouvait perdre des ecritures en
 * silence, et son controle d'integrite n'y voyait rien : une trace jamais ecrite
 * ne casse aucune chaine.
 *
 * <p>
 * <b>Le traitement retenu.</b> Les consommateurs laissent desormais l'exception
 * remonter. Le conteneur rejoue le message, puis, l'echec persistant, le publie
 * sur {@code <topic>.dlq} avant de valider l'offset. Rien n'est perdu : ce qui
 * echoue est conserve et rejouable. Le scellement etant idempotent
 * ({@code sealIfNotDuplicate}), un rejeu ne cree pas de doublon.
 *
 * <p>
 * <b>Pourquoi les files de rebut sont declarees ici.</b> Si le topic
 * {@code .dlq} n'existe pas et que la creation automatique est desactivee,
 * la publication echoue, l'offset n'est pas valide, et le consommateur boucle
 * indefiniment sur le meme message — une panne pire que celle corrigee. Ce piege
 * s'est deja produit sur ce depot avec le topic {@code demande.validation}. Les
 * topics sont donc crees au demarrage par {@code KafkaAdmin}.
 */
@ConditionalOnProperty(name = "gimac.kafka.rebut.actif", havingValue = "true")
@Configuration
@EnableConfigurationProperties(KafkaErrorHandlingConfiguration.RebutProperties.class)
public class KafkaErrorHandlingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfiguration.class);

    /** Suffixe des files de rebut, aligne sur la convention des services Python. */
    private static final String SUFFIXE_REBUT = ".dlq";

    /**
     * Reglage du rejeu et de la mise en rebut.
     *
     * <p>
     * Liaison par {@code @ConfigurationProperties} et non par {@code @Value} : le
     * profil de test charge {@code src/test/resources/config/application.yml}, qui
     * masque entierement le fichier principal. Avec {@code @Value}, une propriete
     * absente de ce fichier de test empeche la resolution du placeholder et fait
     * tomber tout le contexte Spring — donc l'ensemble des tests d'integration du
     * service, pour un reglage de rejeu. La liaison conserve simplement la valeur
     * du champ lorsque la propriete est absente.
     *
     * <p>
     * Les valeurs faisant foi, avec leur provenance, sont declarees dans
     * {@code src/main/resources/config/application.yml}. Les initialisations
     * ci-dessous les reprennent a l'identique et ne servent que de filet.
     */
    @ConfigurationProperties(prefix = "gimac.kafka.rebut")
    public static class RebutProperties {

        /** Remises du message, rejeux compris. */
        private int nombreTentatives = 3;

        /** Attente entre deux remises, en millisecondes. */
        private long intervalleMs = 1_000L;

        /** Partitions des files de rebut. */
        private int partitions = 1;

        /** Facteur de replication des files de rebut. */
        private short replication = 1;

        /**
         * Topics consommes, dont la file de rebut doit exister au demarrage.
         * Ajouter un consommateur sans ajouter son topic ici laisserait sa file
         * de rebut absente, donc son consommateur en boucle au premier echec.
         */
        private List<String> topicsSurveilles = List.of();

        public int getNombreTentatives() {
            return nombreTentatives;
        }

        public void setNombreTentatives(int nombreTentatives) {
            this.nombreTentatives = nombreTentatives;
        }

        public long getIntervalleMs() {
            return intervalleMs;
        }

        public void setIntervalleMs(long intervalleMs) {
            this.intervalleMs = intervalleMs;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        public short getReplication() {
            return replication;
        }

        public void setReplication(short replication) {
            this.replication = replication;
        }

        public List<String> getTopicsSurveilles() {
            return topicsSurveilles == null ? List.of() : topicsSurveilles;
        }

        public void setTopicsSurveilles(List<String> topicsSurveilles) {
            this.topicsSurveilles = topicsSurveilles;
        }
    }

    /**
     * Route un message en echec definitif vers {@code <topic>.dlq}.
     *
     * <p>
     * La partition de destination est laissee au choix du courtier
     * ({@code -1}) : la file de rebut n'a pas necessairement le meme nombre de
     * partitions que le topic d'origine, et forcer la partition d'origine
     * echouerait des que la file en compte moins.
     *
     * @param kafkaTemplate producteur deja utilise par le relais Outbox
     * @return le recuperateur de messages en echec
     */
    @Bean
    public DeadLetterPublishingRecoverer recuperateurRebut(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recuperateur = new DeadLetterPublishingRecoverer(kafkaTemplate, (enregistrement, exception) ->
            new TopicPartition(enregistrement.topic() + SUFFIXE_REBUT, -1)
        );
        // Explicite, et non laisse au defaut : un echec de publication en rebut
        // doit remonter, sinon le gestionnaire d'erreur validerait l'offset en
        // croyant le message sauve. Ce serait exactement la perte silencieuse que
        // ce dispositif corrige, deplacee d'un cran.
        recuperateur.setFailIfSendResultIsError(true);
        return recuperateur;
    }

    /**
     * Gestionnaire d'erreur applique a tous les {@code @KafkaListener} du service.
     *
     * <p>
     * Spring Boot injecte automatiquement ce bean dans la fabrique de conteneurs
     * de listeners : aucune annotation supplementaire n'est requise sur les
     * consommateurs.
     *
     * @param recuperateur publication vers la file de rebut
     * @param reglage      nombre de remises et attente entre deux remises
     * @return le gestionnaire d'erreur
     */
    @Bean
    public DefaultErrorHandler gestionnaireErreurKafka(DeadLetterPublishingRecoverer recuperateur, RebutProperties reglage) {
        // FixedBackOff compte les REJEUX, pas les remises : pour N remises au
        // total il faut N-1 rejeux. Une valeur nulle ou negative reviendrait a
        // supprimer le rejeu sans le dire.
        // PI-GIMAC-01 §0.7 : trois rejeux espacés de 1 s, 5 s puis 25 s avant la file de rebut.
        int rejeux = Math.max(0, reglage.getNombreTentatives());
        ExponentialBackOffWithMaxRetries attente = new ExponentialBackOffWithMaxRetries(rejeux);
        attente.setInitialInterval(reglage.getIntervalleMs());
        attente.setMultiplier(5.0);
        DefaultErrorHandler gestionnaire = new DefaultErrorHandler(recuperateur, attente);
        gestionnaire.setCommitRecovered(true);
        log.info(
            "Echecs de consommation : {} rejeux a partir de {} ms (x5), puis publication sur <topic>{}.",
            reglage.getNombreTentatives(),
            reglage.getIntervalleMs(),
            SUFFIXE_REBUT
        );
        return gestionnaire;
    }

    /**
     * Cree au demarrage la file de rebut de chaque topic consomme.
     *
     * <p>
     * Le type de retour est {@link KafkaAdmin.NewTopics} et non
     * {@code List<NewTopic>} : {@code KafkaAdmin.newTopics()} ne recense que les
     * beans de type {@link NewTopic} ou {@link KafkaAdmin.NewTopics}. Un bean de
     * type {@code java.util.List} est purement ignore — les onze files de rebut
     * n'auraient jamais ete creees, et la premiere mise en rebut aurait fait
     * boucler le consommateur sur le meme message. Defaut releve en revue
     * adversariale et verifie dans le bytecode de spring-kafka 4.0.6.
     *
     * @param reglage topics surveilles, partitions et replication
     * @return les files de rebut a creer
     */
    @Bean
    public KafkaAdmin.NewTopics filesDeRebut(RebutProperties reglage) {
        return new KafkaAdmin.NewTopics(construireFilesDeRebut(reglage));
    }

    /**
     * Construit les files de rebut a partir du reglage.
     *
     * <p>
     * Separee du bean parce que {@code KafkaAdmin.NewTopics.getNewTopics()} n'est
     * visible que dans son propre paquet : sans cette methode, le contenu du bean
     * ne serait pas verifiable par un test.
     *
     * @param reglage topics surveilles, partitions et replication
     * @return les files de rebut, dans l'ordre de declaration
     */
    public NewTopic[] construireFilesDeRebut(RebutProperties reglage) {
        List<String> topicsSurveilles = reglage.getTopicsSurveilles();
        if (topicsSurveilles.isEmpty()) {
            log.warn(
                "Aucun topic declare dans gimac.kafka.rebut.topics-surveilles : les files de rebut " +
                "ne seront pas creees au demarrage. Au premier echec de scellement, le consommateur " +
                "bouclera sur le meme message si la creation automatique de topics est desactivee."
            );
        }
        return topicsSurveilles
            .stream()
            .map(topic ->
                TopicBuilder.name(topic + SUFFIXE_REBUT)
                    .partitions(reglage.getPartitions())
                    .replicas(reglage.getReplication())
                    .build()
            )
            .toArray(NewTopic[]::new);
    }
}
