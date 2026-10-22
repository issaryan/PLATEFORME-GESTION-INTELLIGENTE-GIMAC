package cm.gimac.des.document.infrastructure.sandbox;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Relais de l'outbox de la sandbox (sbx_evenement_sortant) vers le bus, en enveloppe CloudEvents 1.0 : sujet de la
 * plateforme (« document.document » pour document.document-sealed.v1), clé de partition de l'événement. Le lot
 * s'arrête au premier échec pour préserver l'ordre ; après {@link #TENTATIVES_MAX} échecs l'événement passe au rebut
 * (alerte d'exploitation). FOR UPDATE SKIP LOCKED : plusieurs réplicas sans doublon.
 */
public class RelaisEvenementsSandbox {

    static final int LOT = 100;
    static final int TENTATIVES_MAX = 10;
    static final String SOURCE = "/gimac/des/document";
    private static final Logger LOG = LoggerFactory.getLogger(RelaisEvenementsSandbox.class);

    record Ligne(UUID id, String type, String cle, String donnees, OffsetDateTime le, int tentatives) {}

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate transactions;
    private final Clock horloge;
    private final ObjectMapper json = new ObjectMapper();

    public RelaisEvenementsSandbox(JdbcClient jdbc, KafkaTemplate<String, String> kafka, TransactionTemplate transactions, Clock horloge) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.transactions = transactions;
        this.horloge = horloge;
    }

    /** Sujet de la plateforme : « domaine.agrégat » tiré du type (document.document-sealed.v1 → document.document). */
    static String sujet(String type) {
        String[] parties = type.split("\\.");
        return parties[0] + "." + parties[1].substring(0, parties[1].indexOf('-'));
    }

    /** Un passage du relais ; rend le nombre d'événements publiés. */
    public int relayer() {
        return java.util.Objects.requireNonNullElse(transactions.execute(s -> passage()), 0);
    }

    private int passage() {
        List<Ligne> lignes = jdbc
            .sql(
                "select id, type, cle, donnees, cree_le, tentatives from sbx_evenement_sortant where statut = 'EN_ATTENTE' " +
                "order by cree_le limit :lot for update skip locked"
            )
            .param("lot", LOT)
            .query((rs, i) ->
                new Ligne(
                    rs.getObject("id", UUID.class),
                    rs.getString("type"),
                    rs.getString("cle"),
                    rs.getString("donnees"),
                    rs.getObject("cree_le", OffsetDateTime.class),
                    rs.getInt("tentatives")
                )
            )
            .list();
        int publies = 0;
        for (Ligne l : lignes) {
            try {
                kafka.send(sujet(l.type()), l.cle(), enveloppe(l)).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                echec(l, e);
                break;
            } catch (Exception e) {
                echec(l, e);
                break;
            }
            jdbc
                .sql("update sbx_evenement_sortant set statut = 'ENVOYE', envoye_le = :le where id = :id")
                .param("le", OffsetDateTime.ofInstant(horloge.instant(), ZoneOffset.UTC))
                .param("id", l.id())
                .update();
            publies++;
        }
        return publies;
    }

    private void echec(Ligne l, Exception e) {
        int tentatives = l.tentatives() + 1;
        String statut = tentatives >= TENTATIVES_MAX ? "REBUT" : "EN_ATTENTE";
        jdbc.sql("update sbx_evenement_sortant set tentatives = :t, statut = :s where id = :id").param("t", tentatives).param("s", statut).param("id", l.id()).update();
        if ("REBUT".equals(statut)) {
            LOG.error("ALERTE_EXPLOITATION événement {} ({}) au rebut après {} essais : {}", l.id(), l.type(), tentatives, e.toString());
        } else {
            LOG.warn("Relais de l'événement {} ({}) en échec, essai {} : {}", l.id(), l.type(), tentatives, e.toString());
        }
    }

    String enveloppe(Ligne l) {
        com.fasterxml.jackson.databind.node.ObjectNode ce = json.createObjectNode();
        ce.put("specversion", "1.0");
        ce.put("id", l.id().toString());
        ce.put("source", SOURCE);
        ce.put("type", l.type());
        ce.put("subject", l.cle());
        ce.put("time", l.le().toInstant().toString());
        ce.put("datacontenttype", "application/json");
        // Données écrites par la plateforme elle-même (JSON valide) : reprises telles quelles.
        ce.putRawValue("data", new com.fasterxml.jackson.databind.util.RawValue(l.donnees()));
        return ce.toString();
    }
}
