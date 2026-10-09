package cm.gimac.des.notification.web.rest.boite;

import static cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter.CHEF_DES;
import static cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter.CONDUCTEUR;
import static cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter.INGENIEUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.notification.AbstractNotificationIT;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.EvenementProcessus;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.MessageEnvoye;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.RapportSoumis;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheAffectee;
import cm.gimac.des.notification.domain.service.ReglesNotifications.FaitsProcessus;
import cm.gimac.des.notification.domain.service.ReglesNotifications.NatureProcessus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Boîte de notifications (docs/lot1/notifications.md §2 et §3) de bout en bout sur PostgreSQL. */
class BoiteNotificationsIT extends AbstractNotificationIT {

    static final Instant T = Instant.parse("2026-10-05T08:00:00Z");

    @Autowired
    RecevoirEvenementsUseCase evenements;

    String message(String conversation, String auteur, Instant le, String... destinataires) {
        evenements.messageEnvoye(new MessageEnvoye(conversation, UUID.randomUUID().toString(), auteur, List.of(destinataires), le, null));
        return jdbc.queryForObject(
            "select id::text from notification_utilisateur where destinataire_id = ? and reference_id = ? order by mis_a_jour_le desc limit 1",
            String.class,
            destinataires[0],
            conversation
        );
    }

    @Test
    @DisplayName("Liste, forme exacte, consolidation par conversation, compteur, lecture (204), 404 sur la notification d'un autre")
    void boite() throws Exception {
        conversations.groupe("c-groupe", "Groupe Cadrage L-12", List.of(CHEF_DES, CONDUCTEUR, INGENIEUR));
        String id = message("c-groupe", CHEF_DES, T, CONDUCTEUR, INGENIEUR);
        // Deuxième message de la même conversation, d'un autre auteur : regroupé, remonté, titre du dernier expéditeur.
        message("c-groupe", INGENIEUR, T.plusSeconds(60), CONDUCTEUR, CHEF_DES);
        message("c-directe", CHEF_DES, T.plusSeconds(30), CONDUCTEUR);
        evenements.tacheAffectee(new TacheAffectee(UUID.randomUUID().toString(), CONDUCTEUR, CHEF_DES, T.minusSeconds(60), null, null));

        mvc
            .perform(get("/api/v1/notifications").with(interne(CONDUCTEUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.taille").value(20))
            .andExpect(jsonPath("$.contenu.length()").value(3))
            // Récentes d'abord : le groupe (T+60), la directe (T+30), la tâche (T-60).
            .andExpect(jsonPath("$.contenu[0].id").value(id))
            .andExpect(jsonPath("$.contenu[0].type").value("MESSAGE"))
            .andExpect(jsonPath("$.contenu[0].titre").value("Serge Ndzana"))
            .andExpect(jsonPath("$.contenu[0].sousTitre").value("Groupe Cadrage L-12"))
            .andExpect(jsonPath("$.contenu[0].lien").value("/des/messagerie?conversation=c-groupe"))
            .andExpect(jsonPath("$.contenu[0].reference.service").value("collaboration"))
            .andExpect(jsonPath("$.contenu[0].reference.type").value("CONVERSATION"))
            .andExpect(jsonPath("$.contenu[0].reference.id").value("c-groupe"))
            .andExpect(jsonPath("$.contenu[0].acteur.id").value(INGENIEUR))
            .andExpect(jsonPath("$.contenu[0].acteur.nomAffiche").value("Serge Ndzana"))
            .andExpect(jsonPath("$.contenu[0].nombre").value(2))
            .andExpect(jsonPath("$.contenu[0].creeLe").value("2026-10-05T08:00:00Z"))
            .andExpect(jsonPath("$.contenu[0].misAJourLe").value("2026-10-05T08:01:00Z"))
            .andExpect(jsonPath("$.contenu[0].lueLe").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.contenu[1].titre").value("Paul Mbarga Atangana"))
            .andExpect(jsonPath("$.contenu[1].sousTitre").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.contenu[1].nombre").value(1))
            .andExpect(jsonPath("$.contenu[2].type").value("TACHE"))
            .andExpect(jsonPath("$.contenu[2].titre").value("Tâche confiée par Paul Mbarga Atangana"))
            .andExpect(jsonPath("$.contenu[2].reference.service").value("pilotage"));
        // L'auteur n'est jamais notifié de son propre message ; Paul l'est du message de Serge.
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ? and acteur_id = ?", CHEF_DES, CHEF_DES)).isZero();
        mvc.perform(get("/api/v1/notifications/compteur").with(interne(CONDUCTEUR))).andExpect(status().isOk()).andExpect(jsonPath("$.nonLues").value(3));

        // Notification d'un autre : 404, sans effet.
        mvc
            .perform(post("/api/v1/notifications/" + id + "/lecture").with(interne(CHEF_DES)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_NOTIFICATION_INTROUVABLE"));
        mvc.perform(post("/api/v1/notifications/" + UUID.randomUUID() + "/lecture").with(interne(CONDUCTEUR))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/notifications/" + id + "/lecture").with(interne(CONDUCTEUR))).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/notifications/" + id + "/lecture").with(interne(CONDUCTEUR))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notifications/compteur").with(interne(CONDUCTEUR))).andExpect(jsonPath("$.nonLues").value(2));
        mvc
            .perform(get("/api/v1/notifications").param("etat", "non-lues").with(interne(CONDUCTEUR)))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.contenu[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(id))));
        mvc
            .perform(get("/api/v1/notifications").param("etat", "toutes").param("taille", "1").param("page", "0").with(interne(CONDUCTEUR)))
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.contenu[0].lueLe").isNotEmpty());

        // Message suivant dans une conversation lue : nouvelle notification, l'ancienne reste lue.
        message("c-groupe", CHEF_DES, T.plusSeconds(120), CONDUCTEUR);
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ? and reference_id = 'c-groupe'", CONDUCTEUR)).isEqualTo(2);

        // Lecture d'une conversation (ouverture dans l'interface), puis de toutes.
        mvc
            .perform(
                post("/api/v1/notifications/lecture")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"MESSAGE\",\"reference\":{\"service\":\"collaboration\",\"type\":\"CONVERSATION\",\"id\":\"c-directe\"}}")
                    .with(interne(CONDUCTEUR))
            )
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notifications/compteur").with(interne(CONDUCTEUR))).andExpect(jsonPath("$.nonLues").value(2));
        mvc.perform(post("/api/v1/notifications/lecture").with(interne(CONDUCTEUR))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notifications/compteur").with(interne(CONDUCTEUR))).andExpect(jsonPath("$.nonLues").value(0));
        // La boîte de Paul n'a pas été touchée.
        mvc.perform(get("/api/v1/notifications/compteur").with(interne(CHEF_DES))).andExpect(jsonPath("$.nonLues").value(1));

        // Requêtes invalides : 400 avec code.
        mvc.perform(get("/api/v1/notifications").param("etat", "lues").with(interne(CONDUCTEUR))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_REQUETE_INVALIDE"));
        mvc.perform(get("/api/v1/notifications").param("taille", "500").with(interne(CONDUCTEUR))).andExpect(status().isBadRequest());
        mvc
            .perform(post("/api/v1/notifications/lecture").contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"AUTRE\"}").with(interne(CONDUCTEUR)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Lecteur d'une institution (portail) : ni acteur ni conversation, aucun identifiant d'un membre du GIMAC ; compte interne inchangé")
    void projectionInstitution() throws Exception {
        String participant = "compte-participant-1";
        conversations.groupe("c-portail", "Groupe", List.of(CHEF_DES, participant));
        message("c-portail", CHEF_DES, T, participant);
        evenements.tacheAffectee(new TacheAffectee(UUID.randomUUID().toString(), participant, CHEF_DES, T.minusSeconds(60), null, null));
        var institution = jwt()
            .jwt(j -> j.subject(participant).claim("acr", "2").claim("institution_id", "INST-1"))
            .authorities(new SimpleGrantedAuthority(BOITE));
        String corps = mvc
            .perform(get("/api/v1/notifications").with(institution))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.contenu[0].type").value("MESSAGE"))
            .andExpect(jsonPath("$.contenu[0].acteur").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.contenu[0].conversationId").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.contenu[0].titre").isNotEmpty())
            .andExpect(jsonPath("$.contenu[1].type").value("TACHE"))
            .andExpect(jsonPath("$.contenu[1].acteur").value(org.hamcrest.Matchers.nullValue()))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(corps).doesNotContain(CHEF_DES);
        // Même boîte lue sans institution (compte interne) : l'acteur reste remis.
        mvc
            .perform(get("/api/v1/notifications").with(interne(participant)))
            .andExpect(jsonPath("$.contenu[0].acteur.id").value(CHEF_DES))
            .andExpect(jsonPath("$.contenu[1].acteur.id").value(CHEF_DES));
        // Institution vide : traitée comme un compte interne.
        mvc
            .perform(get("/api/v1/notifications").with(jwt().jwt(j -> j.subject(participant).claim("acr", "2").claim("institution_id", " ")).authorities(new SimpleGrantedAuthority(BOITE))))
            .andExpect(jsonPath("$.contenu[0].acteur.id").value(CHEF_DES));
    }

    @Test
    @DisplayName("CER-13 : un compte d'institution ne règle que les types du portail (homologation) ; un type interne est refusé")
    void preferencesPortail() throws Exception {
        var institution = jwt()
            .jwt(j -> j.subject("compte-participant-2").claim("acr", "2").claim("institution_id", "INST-1"))
            .authorities(new SimpleGrantedAuthority(BOITE));
        mvc
            .perform(get("/api/v1/preferences").with(institution))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.types.HOMOLOGATION").value(true))
            .andExpect(jsonPath("$.types.MESSAGE").doesNotExist())
            .andExpect(jsonPath("$.types.PROCESSUS").doesNotExist())
            .andExpect(jsonPath("$.types.DEMANDE").doesNotExist());
        mvc
            .perform(put("/api/v1/preferences").contentType(MediaType.APPLICATION_JSON).content("{\"types\":{\"RAPPORT\":false}}").with(institution))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("NOT_REQUETE_INVALIDE"));
        mvc
            .perform(put("/api/v1/preferences").contentType(MediaType.APPLICATION_JSON).content("{\"types\":{\"HOMOLOGATION\":false}}").with(institution))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.types.HOMOLOGATION").value(false))
            .andExpect(jsonPath("$.types.TACHE").doesNotExist());
        // Compte interne : tous les types.
        mvc.perform(get("/api/v1/preferences").with(interne(CONDUCTEUR))).andExpect(jsonPath("$.types.MESSAGE").exists()).andExpect(jsonPath("$.types.HOMOLOGATION").exists());
    }

    @Test
    @DisplayName("Préférences : défauts, modification (200), type désactivé sans notification, autre compte inchangé")
    void preferences() throws Exception {
        mvc
            .perform(get("/api/v1/preferences").with(interne(CONDUCTEUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.systeme").value(true))
            .andExpect(jsonPath("$.son").value(false))
            .andExpect(jsonPath("$.types.MESSAGE").value(true))
            .andExpect(jsonPath("$.types.TACHE").value(true))
            .andExpect(jsonPath("$.types.RAPPORT").value(true));
        mvc
            .perform(
                put("/api/v1/preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"systeme\":false,\"son\":true,\"types\":{\"MESSAGE\":false,\"TACHE\":true,\"RAPPORT\":true}}")
                    .with(interne(CONDUCTEUR))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.systeme").value(false))
            .andExpect(jsonPath("$.son").value(true))
            .andExpect(jsonPath("$.types.MESSAGE").value(false));
        mvc.perform(get("/api/v1/preferences").with(interne(CONDUCTEUR))).andExpect(jsonPath("$.types.MESSAGE").value(false)).andExpect(jsonPath("$.son").value(true));
        mvc.perform(get("/api/v1/preferences").with(interne(INGENIEUR))).andExpect(jsonPath("$.types.MESSAGE").value(true));

        message("c-1", CHEF_DES, T, INGENIEUR, CONDUCTEUR);
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ?", CONDUCTEUR)).isZero();
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ?", INGENIEUR)).isEqualTo(1);
        evenements.rapportSoumis(new RapportSoumis(UUID.randomUUID().toString(), "QUOTIDIEN", "2026-10-05", INGENIEUR, CONDUCTEUR, INGENIEUR, T));
        mvc
            .perform(get("/api/v1/notifications").with(interne(CONDUCTEUR)))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].type").value("RAPPORT"))
            .andExpect(jsonPath("$.contenu[0].titre").value("Rapport de Serge Ndzana"))
            .andExpect(jsonPath("$.contenu[0].sousTitre").value("Rapport quotidien 2026-10-05"));

        mvc
            .perform(put("/api/v1/preferences").contentType(MediaType.APPLICATION_JSON).content("{\"types\":{\"COURRIEL\":true}}").with(interne(CONDUCTEUR)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("NOT_REQUETE_INVALIDE"));
    }

    @Test
    @DisplayName("Mentions et réponses (D-82) : forme exacte, en plus du message, non regroupées, préférences, lues à l'ouverture de la conversation")
    void mentionsEtReponses() throws Exception {
        conversations.groupe("c-d82", "Groupe Cadrage L-12", List.of(CHEF_DES, CONDUCTEUR, INGENIEUR));
        // Paul répond à Aminata et mentionne Serge ; puis Serge mentionne Aminata en répondant à son message (une seule MENTION).
        evenements.messageEnvoye(new MessageEnvoye("c-d82", "m-1", CHEF_DES, List.of(CONDUCTEUR, INGENIEUR), T, null, CONDUCTEUR, List.of(INGENIEUR)));
        evenements.messageEnvoye(new MessageEnvoye("c-d82", "m-2", INGENIEUR, List.of(CONDUCTEUR, CHEF_DES), T.plusSeconds(60), null, CONDUCTEUR, List.of(CONDUCTEUR)));
        // Événement de groupe : rien.
        evenements.messageEnvoye(new MessageEnvoye("c-d82", "m-3", CHEF_DES, List.of(CONDUCTEUR), T.plusSeconds(90), "MEMBRES_AJOUTES", CONDUCTEUR, List.of(CONDUCTEUR)));

        mvc
            .perform(get("/api/v1/notifications").with(interne(CONDUCTEUR)))
            .andExpect(status().isOk())
            // Message regroupé (2), mention de m-2, réponse de m-1.
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MESSAGE')].nombre").value(org.hamcrest.Matchers.contains(2)))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MESSAGE')].conversationId").value(org.hamcrest.Matchers.contains("c-d82")))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MENTION')].titre").value(org.hamcrest.Matchers.contains("Serge Ndzana vous a mentionné")))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MENTION')].sousTitre").value(org.hamcrest.Matchers.contains("Groupe Cadrage L-12")))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MENTION')].lien").value(org.hamcrest.Matchers.contains("/des/messagerie?conversation=c-d82&message=m-2")))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MENTION')].reference.service").value(org.hamcrest.Matchers.contains("collaboration")))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MENTION')].reference.type").value(org.hamcrest.Matchers.contains("MESSAGE")))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MENTION')].reference.id").value(org.hamcrest.Matchers.contains("m-2")))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MENTION')].conversationId").value(org.hamcrest.Matchers.contains("c-d82")))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MENTION')].acteur.id").value(org.hamcrest.Matchers.contains(INGENIEUR)))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MENTION')].nombre").value(org.hamcrest.Matchers.contains(1)))
            .andExpect(jsonPath("$.contenu[?(@.type == 'REPONSE')].titre").value(org.hamcrest.Matchers.contains("Paul Mbarga Atangana a répondu à votre message")))
            .andExpect(jsonPath("$.contenu[?(@.type == 'REPONSE')].lien").value(org.hamcrest.Matchers.contains("/des/messagerie?conversation=c-d82&message=m-1")));
        mvc
            .perform(get("/api/v1/notifications").with(interne(INGENIEUR)))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.contenu[?(@.type == 'MENTION')].titre").value(org.hamcrest.Matchers.contains("Paul Mbarga Atangana vous a mentionné")));
        // Jamais pour l'auteur : Paul n'a que le message de Serge.
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ? and type in ('MENTION', 'REPONSE')", CHEF_DES)).isZero();

        // Ouvrir la conversation (référence de la conversation, sans type) lit message, mention et réponse.
        mvc
            .perform(
                post("/api/v1/notifications/lecture")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reference\":{\"service\":\"collaboration\",\"type\":\"CONVERSATION\",\"id\":\"c-d82\"}}")
                    .with(interne(CONDUCTEUR))
            )
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notifications/compteur").with(interne(CONDUCTEUR))).andExpect(jsonPath("$.nonLues").value(0));
        mvc.perform(get("/api/v1/notifications/compteur").with(interne(INGENIEUR))).andExpect(jsonPath("$.nonLues").value(2));

        // Préférences : MENTION et REPONSE actives par défaut, désactivables ; un type désactivé ne crée rien.
        mvc
            .perform(get("/api/v1/preferences").with(interne(INGENIEUR)))
            .andExpect(jsonPath("$.types.MENTION").value(true))
            .andExpect(jsonPath("$.types.REPONSE").value(true));
        mvc
            .perform(put("/api/v1/preferences").contentType(MediaType.APPLICATION_JSON).content("{\"types\":{\"MENTION\":false}}").with(interne(INGENIEUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.types.MENTION").value(false))
            .andExpect(jsonPath("$.types.REPONSE").value(true))
            .andExpect(jsonPath("$.types.MESSAGE").value(true));
        evenements.messageEnvoye(new MessageEnvoye("c-d82", "m-4", CHEF_DES, List.of(CONDUCTEUR, INGENIEUR), T.plusSeconds(120), null, INGENIEUR, List.of(INGENIEUR)));
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ? and reference_id = 'm-4'", INGENIEUR)).isZero();
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ? and type = 'MESSAGE' and lue_le is null", INGENIEUR)).isEqualTo(1);
    }

    @Test
    @DisplayName("D-87 : PROCESSUS en base (contrainte CHECK, préférence type_processus) : liste, lecture par référence de l'instance, préférence active par défaut puis désactivée")
    void processus() throws Exception {
        String instance = UUID.randomUUID().toString();
        String lien = "/des/processus/instances/" + instance + "?etape=ANALYSE";
        FaitsProcessus faits = new FaitsProcessus(NatureProcessus.ETAPE_SOUMISE, "OUVERTURE_FLUX_RESEAU", "Flux vers la banque X", "ANALYSE", "Analyse de la demande", "VALIDATION", false, false, null);
        mvc.perform(get("/api/v1/preferences").with(interne(CONDUCTEUR))).andExpect(status().isOk()).andExpect(jsonPath("$.types.PROCESSUS").value(true));
        assertThat(evenements.evenementProcessus(new EvenementProcessus(instance, faits, INGENIEUR, null, List.of(CONDUCTEUR, INGENIEUR), lien, T))).isEqualTo(1);
        assertThat(evenements.evenementProcessus(new EvenementProcessus(instance, faits, INGENIEUR, null, List.of(CONDUCTEUR), lien, T.plusSeconds(1)))).isEqualTo(1);
        mvc
            .perform(get("/api/v1/notifications").with(interne(CONDUCTEUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.contenu[0].type").value("PROCESSUS"))
            .andExpect(jsonPath("$.contenu[0].titre").value("Validation attendue : Analyse de la demande"))
            .andExpect(jsonPath("$.contenu[0].sousTitre").value("Flux vers la banque X, soumise par Serge Ndzana"))
            .andExpect(jsonPath("$.contenu[0].lien").value(lien))
            .andExpect(jsonPath("$.contenu[0].nombre").value(1));
        mvc
            .perform(
                post("/api/v1/notifications/lecture")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reference\":{\"service\":\"pilotage\",\"type\":\"PROCESSUS\",\"id\":\"" + instance + "\"}}")
                    .with(interne(CONDUCTEUR))
            )
            .andExpect(status().is2xxSuccessful());
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ? and type = 'PROCESSUS' and lue_le is null", CONDUCTEUR)).isZero();
        mvc
            .perform(put("/api/v1/preferences").contentType(MediaType.APPLICATION_JSON).content("{\"types\":{\"PROCESSUS\":false}}").with(interne(CONDUCTEUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.types.PROCESSUS").value(false))
            .andExpect(jsonPath("$.types.MESSAGE").value(true));
        assertThat(evenements.evenementProcessus(new EvenementProcessus(instance, faits, INGENIEUR, null, List.of(CONDUCTEUR), lien, T.plusSeconds(2)))).isZero();
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ? and type = 'PROCESSUS'", CONDUCTEUR)).isEqualTo(2);
    }

    @Test
    @DisplayName("Sécurité : 401 sans jeton, 403 sans second facteur ou sans NOT_INBOX_READ, routes internes refusées")
    void securite() throws Exception {
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/notifications/compteur").with(utilisateur(CONDUCTEUR, "COL_MESSAGE_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(
                get("/api/v1/preferences").with(
                    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(j -> j.subject(CONDUCTEUR).claim("acr", "1"))
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(BOITE))
                )
            )
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/preferences").contentType(MediaType.APPLICATION_JSON).content("{}").with(utilisateur(CONDUCTEUR))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/interne/notifications").with(interne(CONDUCTEUR))).andExpect(status().isForbidden());
    }
}
