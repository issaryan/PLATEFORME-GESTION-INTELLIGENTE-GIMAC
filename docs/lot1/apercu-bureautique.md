# Aperçu des documents bureautiques : Word, PowerPoint, Excel (D-83)

Demande de KOAGNE ISSA RYAN : voir l'aperçu des fichiers docx et pptx. Jusqu'ici, la fenêtre d'aperçu (D-68,
D-73) ne lisait que le PDF, les images et le texte ; les autres formats invitaient au téléchargement.

## 1. Principe

Le document bureautique est **converti en PDF côté serveur**, puis affiché par l'aperçu PDF existant (pdf.js,
canevas, aucune exécution du document). Raisons : rendu fidèle (polices, tableaux, diapositives, en-têtes),
aucune feuille de style injectée dans la page (la CSP `style-src 'self'` reste inchangée), une seule
visionneuse à maintenir, et les mêmes formats couverts partout (messagerie, rapports, documents).

- **Moteur** : Gotenberg 8 (LibreOffice sans interface) dans le conteneur `conversion-documents`, réseau
  interne seulement, aucun port publié, aucun accès sortant, Chromium et webhooks désactivés, macros jamais
  exécutées, mémoire bornée (1 Go), délai de conversion 60 s.
- **Service** : MS-DOCUMENT, adaptateur `ConvertisseurApercu` (port hexagonal), seul client de Gotenberg.
- **Formats** : `docx`, `doc`, `odt`, `rtf`, `pptx`, `ppt`, `odp`, `xlsx`, `xls`, `ods`. Source de 20 Mo au plus.
- **Cache** : PDF converti gardé par empreinte SHA-256 de la source (24 h, 500 Mo au plus) ; un même fichier
  n'est converti qu'une fois. Aucune copie durable de la source.

## 2. Route

Passerelle `POST /bff/{des,console}/document/apercus` → MS-DOCUMENT `POST /api/v1/apercus`. Compte interne
seulement (OPA refuse un jeton portant une institution), permission `PIL_ACTIVITY_OWN` (tous les profils
internes, qui ont déjà le contenu à afficher).

- Corps : les octets du fichier (`Content-Type` du fichier, ou `application/octet-stream`), en-tête
  `X-Nom-Fichier` (nom encodé en URI, pour l'extension). 20 Mo au plus (passerelle et service).
- Réponse 200 : `application/pdf`, `Content-Disposition: inline`, `Cache-Control: private, max-age=3600`.
- Erreurs (`application/problem+json`, champ `code`) : 413 `DOC_APERCU_TROP_VOLUMINEUX` ; 415
  `DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE` (extension hors liste, ou signature du fichier incohérente :
  conteneur ZIP pour les formats Open XML et OpenDocument, OLE2 pour doc, ppt, xls, `{\rtf` pour rtf) ; 422
  `DOC_APERCU_ILLISIBLE` (conversion refusée : fichier corrompu ou protégé par mot de passe) ; 429
  `DOC_APERCU_LIMITE` (30 conversions par minute et par compte) ; 503 `DOC_APERCU_INDISPONIBLE` (moteur
  injoignable ou délai dépassé).
- Journal : compte, taille, extension, durée, résultat ; jamais le nom complet ni le contenu.

## 3. Interface

`formatApercu` gagne le format `bureautique`. La fenêtre d'aperçu affiche « Préparation de l'aperçu… »,
envoie le contenu déjà téléchargé à la route ci-dessus, puis passe le PDF obtenu à l'aperçu PDF ; une icône
du type (Word, PowerPoint, Excel) précède le titre. En cas d'erreur : message de la cause et bouton
« Télécharger », comme aujourd'hui. Une présentation s'affiche une diapositive par page.

## 4. Mise en œuvre

### 4.1 Moteur de conversion

- Image `gotenberg/gotenberg:8.37.0` (version épinglée), service `conversion-documents` déclaré dans
  `infra/docker-compose/docker-compose.gimac.yml` (développement et serveur ; `restart: unless-stopped` ajouté par
  `docker-compose.serveur.yml`). Il est démarré et attendu sain par `gimac-dev.sh demarrer` comme tout service du
  socle, donc par `infra/ci/deployer-serveur.sh` sans changement ; `infra/ci/diagnostiquer-serveur.sh` affiche son
  état, sa santé vue du conteneur et vue de MS-DOCUMENT, et le décompte des résultats d'aperçu sur 24 h.
- Réseau Compose `conversion` (`internal: true`) partagé avec `document` seulement : aucun port publié, aucun accès
  sortant (résolution de noms extérieurs impossible, vérifiée). Kubernetes : `infra/kubernetes-gimac/conversion-documents.yaml`
  (Deployment et Service `ClusterIP`, segment `conversion`), politiques `conversion-documents` (entrée depuis les
  pods `app: document` sur 3000, aucune sortie hors DNS) et `sortie-document-conversion`.
- Options : `--chromium-disable-routes`, `--pdfengines-disable-routes`, `--webhook-disable`,
  `--api-disable-download-from` (aucune lecture d'URL), `--prometheus-disable-collect`, `--api-timeout=60s`,
  `--api-body-limit=21MB`, `--libreoffice-auto-start` (premier aperçu sans attente du démarrage),
  `--libreoffice-max-queue-size=20` (au-delà, 429 du moteur, rendu en 503). Les macros ne sont jamais exécutées
  (réglage de LibreOffice dans l'image). Mémoire bornée à 1 Go (`mem_limit`, limite Kubernetes 1 Gio), toutes les
  capacités retirées, `no-new-privileges`, racine en lecture seule (`/tmp` et le profil de LibreOffice en tmpfs ou
  `emptyDir`). Santé : `GET /health` (curl présent dans l'image).

### 4.2 MS-DOCUMENT

- Port `ConvertisseurApercu` (exceptions `ConversionRefusee`, `MoteurIndisponible`), adaptateur
  `ConvertisseurApercuGotenberg` (RestClient sur le client HTTP du JDK, comme le reste du service) :
  `POST /forms/libreoffice/convert`, multipart, partie `files` nommée `document.<extension>` : le nom d'origine ne
  quitte pas MS-DOCUMENT.
- Réponses du moteur constatées sur l'image 8.37.0 : fichier protégé par mot de passe, 400 ; fichier corrompu, 500
  (« LibreOffice failed to convert the document ») ; délai dépassé, 503. Traduction : 400, 422 et 500 donnent
  422 `DOC_APERCU_ILLISIBLE` ; tout autre statut, le délai de réponse (`gimac.apercu.delai`, 65 s, au-delà des 60 s
  du moteur) ou une connexion impossible donnent 503 `DOC_APERCU_INDISPONIBLE`.
- Réglages `gimac.apercu.*` : `url` (`GIMAC_APERCU_URL`, `http://conversion-documents:3000` dans le Compose,
  `http://conversion-documents.gimac-des.svc.cluster.local:3000` en Kubernetes), `delai` 65 s, `delai-connexion` 5 s,
  `taille-max` 20 Mo, `cache-duree` 24 h, `cache-taille-max` 500 Mo, `limite-par-minute` 30.
- Signature : ZIP (`PK\3\4`) pour docx, pptx, xlsx, odt, odp, ods ; OLE2 (`D0CF11E0A1B11AE1`) pour doc, ppt, xls ;
  `{\rtf` pour rtf. **Écart assumé** : un fichier Open XML protégé par mot de passe est un conteneur OLE2 chiffré ;
  il est admis pour docx, pptx et xlsx afin que l'utilisateur reçoive 422 `DOC_APERCU_ILLISIBLE` (« protégé par mot
  de passe ») plutôt que 415. Une extension hors liste (dont docm, dotx) ou un nom absent donne 415.
- Cache : Caffeine en mémoire de l'instance, clé « empreinte SHA-256 de la source et extension » (l'extension décide
  du filtre d'import), poids = taille du PDF, expiration 24 h après l'écriture. Aucune copie de la source n'est
  gardée, ni en mémoire après la réponse ni sur disque.
- Limite : 30 **conversions** par minute et par compte (sujet du jeton), fenêtre glissante ; un aperçu servi par
  le cache ne compte pas. Au-delà : 429 `DOC_APERCU_LIMITE`, `details.reessayerDans` (secondes) et
  `details.limiteParMinute`.
- Accès : jeton à deux facteurs (toute route `/api/v1/**`), `@PreAuthorize("hasAuthority('PIL_ACTIVITY_OWN')")`,
  puis compte interne : un jeton portant `institution_id` reçoit 403 `DOC_APERCU_RESERVE_INTERNE` (OPA le refuse
  déjà à la passerelle).
- Taille : `Content-Length` annoncé au-delà de 20 Mo, refus immédiat ; sinon le corps est lu au plus jusqu'à
  20 Mo + 1 octet, et le dépassement donne 413 `DOC_APERCU_TROP_VOLUMINEUX` (`details.taille`, `details.tailleMax`).
- Journal (`ApercuBureautiqueService`) : `compte`, `taille`, `extension` (admise, sinon `?`), `duree`, `resultat`
  (`SUCCES` ou le code d'erreur) ; jamais le nom du fichier ni son contenu.

### 4.3 Passerelle, OPA et relais

- Route `document-apercus` : `Path=/bff/des/document/apercus,/bff/console/document/apercus`, `OpaAuthorize`,
  `RequestSize` 20 Mo (413 de la passerelle au-delà, sans corps problem+json), réécriture vers `/api/v1/apercus`,
  délai de réponse de la route 90 s (`metadata.response-timeout`).
- OPA (`lot1_authz.rego`) : `POST ^/bff/(des|console)/document/apercus$` exige `PIL_ACTIVITY_OWN` ; tout chemin
  `…/document/apercus` est réservé aux comptes internes (`reserve_interne_externe`) et exclu du repli sur
  `@PreAuthorize` : autre méthode, sous-chemin ou portail refusés.
- nginx des couches (`frontend/nginx/gimac.conf.template`) et relais HTTPS (`installer-relais-https.sh`) : corps de
  110 Mo et délai de lecture d'une heure, déjà suffisants. Entrées Kubernetes des couches : `proxy-body-size: 110m`
  ajouté (le contrôleur NGINX refuse au-delà de 1 Mo par défaut).

### 4.4 Consignes pour l'interface

- Requête : `POST /bff/{des|console}/document/apercus`, corps = octets du fichier déjà téléchargé (`Blob`),
  `Content-Type` du fichier ou `application/octet-stream`, `X-Nom-Fichier: encodeURIComponent(nom)`,
  `Accept: application/pdf, application/problem+json`. Réponse 200 : `blob()` passé à l'aperçu PDF.
- Erreurs à lire dans `code` (problem+json) : 413 (aussi sans corps JSON si la passerelle refuse la taille
  annoncée), 415, 422, 429 (`details.reessayerDans`), 503 ; 403 si le compte n'y a pas droit. Dans tous les cas :
  message de la cause et bouton « Télécharger ». Ne pas envoyer plus de 20 Mo (contrôle avant l'envoi).

### 4.5 Interface (mise en œuvre, 06/10/2026)

- `formatApercu` (design system) rend `bureautique` d'après l'extension, avant le texte (un `.rtf` servi en
  `text/rtf` est converti) ; `familleBureautique` choisit le pictogramme du titre (Word, PowerPoint, Excel).
- `AttachmentViewer` reçoit `convertir(blob, nom) => Promise<Blob>` de l'application : le système de conception ne
  fait aucun appel réseau. Sans `convertir` (portail), ces formats invitent au téléchargement comme avant. Au-delà de
  20 Mo, la cause s'affiche sans aucun envoi. Pendant la conversion : « Préparation de l'aperçu… » (`role="status"`) ;
  le PDF reçu passe à l'aperçu PDF (pdf.js, sans cadre). Erreurs lues sur le statut (la passerelle peut répondre
  413 sans corps JSON) : 413, 415, 422, 429 (« réessayez dans n s » d'après `details.reessayerDans`), 503, 403, autre ;
  le bouton « Télécharger » reste dans le pied de la fenêtre.
- `@gimac/lot1` : port `ServiceApercu` (`services.apercu`), HTTP `apercuHttp(couche)` (couche de l'application :
  `des` dans l'application interne, `console` dans la console ; absent au portail) par `http.post` avec un corps
  `Blob`, `X-Nom-Fichier`, `Accept` et une réponse binaire (`binaire: true` du client du socle) ; service simulé qui
  rend un petit PDF de démonstration (refus imités : nom contenant « protege », 422 ; « indisponible », 503).
  Branché sur la messagerie (fil) et sur la consultation d'un rapport (`dialoguesRapport.tsx`).
