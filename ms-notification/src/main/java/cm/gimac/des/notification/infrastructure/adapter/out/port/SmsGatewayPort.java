package cm.gimac.des.notification.infrastructure.adapter.out.port;

public interface SmsGatewayPort {
    void sendSms(String phoneNumber, String message);
}
