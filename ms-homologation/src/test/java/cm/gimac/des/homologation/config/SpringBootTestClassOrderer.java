package cm.gimac.des.homologation.config;

import cm.gimac.des.homologation.IntegrationTest;
import java.util.Comparator;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

public class SpringBootTestClassOrderer implements ClassOrderer {

    @Override
    public void orderClasses(ClassOrdererContext context) {
        context.getClassDescriptors().sort(Comparator.comparingInt(SpringBootTestClassOrderer::getOrder));
    }

    private static int getOrder(ClassDescriptor classDescriptor) {
        if (classDescriptor.findAnnotation(IntegrationTest.class).isPresent()) {
            return 2;
        }
        return 1;
    }
}
