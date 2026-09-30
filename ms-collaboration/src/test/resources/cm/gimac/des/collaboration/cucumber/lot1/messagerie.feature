# language: fr
Fonctionnalité: Messagerie instantanée de travail entre acteurs du GIMAC (D-64)
  Les acteurs internes du GIMAC échangent des messages directs ou de groupe. Les destinataires sont
  choisis dans l'annuaire de la plateforme ; un message peut citer une liaison et porter des pièces
  jointes déposées dans MS-DOCUMENT. Seuls les membres lisent une conversation. Tout numéro de carte
  est masqué avant l'enregistrement (RG-52) et le contenu est chiffré au repos.

  Contexte:
    Étant donné les acteurs internes de l'annuaire, authentifiés à deux facteurs avec les permissions de la messagerie

  Scénario: Envoyer un message direct
    Quand "Paul Mbarga Atangana" écrit "Bonjour Aminata, le PV est prêt" à "Aminata Ngono"
    Alors une conversation directe réunit "Paul Mbarga Atangana" et "Aminata Ngono"
    Et "Aminata Ngono" lit le message "Bonjour Aminata, le PV est prêt" de "Paul Mbarga Atangana"
    Et l'événement "collaboration.message-sent.v1" est remis pour "Aminata Ngono" sans le contenu du message
    Quand "Aminata Ngono" écrit "Merci Paul" à "Paul Mbarga Atangana"
    Alors la même conversation directe est réutilisée

  Scénario: Étiqueter une liaison
    Quand "Paul Mbarga Atangana" écrit "Point d'avancement de la recette" à "Aminata Ngono" en citant la liaison "LIA-2026-027"
    Alors le message porte l'étiquette "LIA-2026-027"
    Et "Aminata Ngono" retrouve la conversation par la liaison citée

  Scénario: Joindre une pièce
    Quand "Paul Mbarga Atangana" envoie à "Aminata Ngono" la pièce "pv-recette.pdf"
    Alors "Aminata Ngono" télécharge la pièce "pv-recette.pdf" à l'identique, avec son empreinte SHA-256

  Scénario: Lire et compter les non-lus
    Étant donné "Paul Mbarga Atangana" a écrit 3 messages à "Aminata Ngono"
    Alors "Aminata Ngono" a 3 messages non lus
    Et "Paul Mbarga Atangana" a 0 message non lu
    Quand "Aminata Ngono" marque la conversation comme lue
    Alors "Aminata Ngono" a 0 message non lu

  Scénario: Refus pour un non-membre
    Étant donné "Paul Mbarga Atangana" a écrit 1 message à "Aminata Ngono"
    Quand "Serge Ndzana" tente de lire la conversation
    Alors la réponse est 404 avec le code "COL_CONVERSATION_NOT_FOUND"
    Quand "Serge Ndzana" tente d'écrire "Bonjour" dans la conversation
    Alors la réponse est 404 avec le code "COL_CONVERSATION_NOT_FOUND"

  Scénario: Masquage d'un numéro de carte (RG-52)
    Quand "Paul Mbarga Atangana" écrit "Carte 4111 1111 1111 1111 bloquée au DAB" à "Aminata Ngono"
    Alors "Aminata Ngono" lit le message "Carte 411111******1111 bloquée au DAB" de "Paul Mbarga Atangana"
    Et aucun numéro de carte en clair n'est enregistré
