package cm.gimac.des.gateway.domain.supervision;

/** Service absent de gimac.services (et différent de la passerelle). */
public class ServiceInconnuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServiceInconnuException(String nom) {
        super("Service inconnu de la plateforme : " + nom);
    }
}
