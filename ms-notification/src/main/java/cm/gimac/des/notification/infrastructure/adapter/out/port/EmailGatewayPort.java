package cm.gimac.des.notification.infrastructure.adapter.out.port;

public interface EmailGatewayPort {
    void sendEmail(String to, String subject, String body);
}
