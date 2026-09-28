package cm.gimac.des.collaboration.broker;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer implements Supplier<String> {

    @Override
    public String get() {
        return "kafka_producer";
    }
}
