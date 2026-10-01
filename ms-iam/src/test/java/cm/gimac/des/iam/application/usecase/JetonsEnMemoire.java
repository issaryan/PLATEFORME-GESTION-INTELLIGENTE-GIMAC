package cm.gimac.des.iam.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.infrastructure.persistence.JetonLien;
import cm.gimac.des.iam.infrastructure.persistence.JetonLienRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Dépôt des jetons de lien simulé en mémoire, pour les tests unitaires des cas d'utilisation (D-65). */
final class JetonsEnMemoire {

    private JetonsEnMemoire() {}

    static JetonLienRepository creer(List<JetonLien> stock) {
        JetonLienRepository r = mock(JetonLienRepository.class, org.mockito.Mockito.withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
        when(r.save(any(JetonLien.class))).thenAnswer(i -> {
            JetonLien j = i.getArgument(0);
            stock.add(j);
            return j;
        });
        when(r.findByEmpreinteForUpdate(anyString())).thenAnswer(i -> stock.stream().filter(j -> j.getEmpreinte().equals(i.getArgument(0))).findFirst());
        when(r.ouverts(anyString())).thenAnswer(i -> new ArrayList<>(stock.stream().filter(j -> j.getCompteId().equals(i.getArgument(0)) && j.getUtiliseLe() == null).toList()));
        return r;
    }

    /** Jeton en clair extrait du lien d'un courriel (paramètre jeton=). */
    static Optional<String> jeton(String texte) {
        int i = texte.indexOf("jeton=");
        return i < 0 ? Optional.empty() : Optional.of(texte.substring(i + 6, i + 6 + 43));
    }
}
