package cm.gimac.des.notification.infrastructure.adapter.out;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import cm.gimac.des.notification.infrastructure.adapter.out.port.SmsGatewayPort;
import java.security.SecureRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "gimac.notification.sms-simule.actif", havingValue = "true")
@Component
@Profile("stub")
public class StubSmsAdapter implements SmsGatewayPort {

    private final SecureRandom random = new SecureRandom();

    @Override
    public void sendSms(String phoneNumber, String message) {
        int chance = random.nextInt(100);
        if (chance < 15) {
            throw new RuntimeException("Timeout sending SMS to " + phoneNumber);
        } else if (chance < 20) {
            throw new RuntimeException("Permanent error sending SMS to " + phoneNumber);
        }
        // 80% success
    }
}
