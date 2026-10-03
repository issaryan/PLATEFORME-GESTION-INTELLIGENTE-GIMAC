import type { Page, Route } from '@playwright/test'

// Passerelle simulée pour la console construite avec VITE_SERVICES=http : chaque appel /api et /bff est
// intercepté par Playwright et servi depuis un état en mémoire, aux formes de MS-IAM et de MS-GATEWAY.
// Le flux SSE /api/plateforme/flux renvoie une impulsion puis se ferme : le client se reconnecte.

const IAM = ['ACCOUNT_READ', 'ACCOUNT_CREATE', 'ACCOUNT_EDIT', 'ACCOUNT_ROLES', 'ACCOUNT_STATUS', 'ACCOUNT_DELETE', 'ACCOUNT_SECURITY', 'JOURNAL_READ', 'PROFILE_EDIT'].map((p) => `IAM_${p}`)
// Messagerie instantanée (D-64) : tout membre interne, dont le RSSI et le super administrateur.
const MESSAGERIE = ['COL_MESSAGE_READ', 'COL_MESSAGE_SEND']
export const PERMISSIONS_RSSI = [...IAM, ...MESSAGERIE, 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_MESSAGING_EDIT']
export const PERMISSIONS_SUPER_ADMIN = [...IAM, ...MESSAGERIE, 'IAM_ROLE_PRIVILEGED_GRANT', 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_CONFIG_EDIT',
  'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_SECURITY_POLICY_EDIT', 'PLATFORM_MESSAGING_EDIT']

const il = (h: number) => new Date(Date.now() - h * 3_600_000).toISOString()
const compte = (id: string, identifiant: string, prenom: string, nom: string, profils: string[], o: Record<string, unknown> = {}) => ({
  id, identifiant, prenom, nom, nomAffiche: `${prenom} ${nom}`, email: `${identifiant}@gimac-afr.org`, emailVerifie: true, actif: true, creeLe: il(900),
  telephone: '+237 699 00 11 22', fonction: 'Agent', langue: 'fr', institutionId: null, profilComplet: true, premierAccesEnAttente: false, profils, actionsRequises: [],
  secondFacteur: true, ...o,
})
const service = (nom: string, etat = 'UP') => ({
  nom, url: `http://${nom}:8080`, etat, latenceMs: etat === 'UP' ? 18 : null, composants: {}, info: { version: '1.0.0', artefact: nom, dateConstruction: null, commit: null, branche: null },
  metriques: etat === 'UP' ? { dureeFonctionnementSecondes: 3600, cpuProcessus: 0.12, cpuSysteme: 0.3, nombreCpu: 4, memoireTasUtiliseeOctets: 1, memoireTasMaxOctets: 2, memoireTasPourcent: 40,
    disqueLibreOctets: 50, disqueTotalOctets: 100, chargeMoyenne1m: 1.1, threadsActifs: 30 } : null,
  erreur: null, controleLe: new Date().toISOString(),
})
const instantane = (etat: 'OPERATIONNELLE' | 'DEGRADEE') => {
  const services = [service('gateway'), service('iam'), service('participant', etat === 'OPERATIONNELLE' ? 'UP' : 'DOWN')]
  const up = services.filter((s) => s.etat === 'UP').length
  return { horodatage: new Date().toISOString(), global: { etat, total: 3, up, down: 3 - up, injoignables: 0 }, services, keycloak: null }
}

const instantaneDegrade = () => {
  const i = instantane('DEGRADEE')
  return { ...i, services: [...i.services, service('audit', 'INJOIGNABLE'), service('document', 'INCONNU')], keycloak: service('keycloak', 'DOWN') }
}

export type OptionsPasserelle = {
  /** Session de la passerelle ; null : aucune session (401). */
  session?: { permissions: string[]; doubleFacteur?: boolean; sujet?: string } | null
  /** Défi de réauthentification refusé faute de clé enrôlée (409 IAM_NO_SECURITY_KEY). */
  sansCle?: boolean
  premierAcces?: boolean
  /** La passerelle répond 503 à /api/session. */
  sessionIndisponible?: boolean
  /** Infrastructure dégradée : service injoignable, Prometheus et Alertmanager en défaut, alertes. */
  degradee?: boolean
  /** La liste des comptes échoue (500). */
  comptesEnErreur?: boolean
  /** Compte courant sans second facteur ni courriel vérifié. */
  moiIncomplet?: boolean
  /** La passerelle a fermé la session (inactivité) : tout appel /bff répond 401 CNX_SESSION_EXPIREE. */
  sessionExpiree?: boolean
}

/** Connexion sur la plateforme (D-65) : mot de passe provisoire, puis mot de passe définitif et code accepté. */
export const CONNEXION_E2E = { provisoire: 'Provisoire-2026!', motDePasse: 'Definitif-Gimac-2026!', code: '123456' }

export class Passerelle {
  appels: { methode: string; chemin: string; recherche: string; entetes: Record<string, string>; corps: unknown }[] = []
  fluxOuverts = 0
  profilsRefuses = 0
  politiqueRefusee = false
  comptes = [
    compte('u-super-admin', 'super.admin', 'Serge', 'Mouelle Kombi', ['ROLE_SUPER_ADMIN']),
    compte('u-rssi', 'rssi', 'Carine', 'Ebongue Ndjock', ['ROLE_RSSI']),
    compte('u-sna', 'r.biyong', 'Rachel', 'Biyong Mengue', ['ROLE_SNA'], { secondFacteur: false }),
  ]
  cles = [{ id: 'k-1', nom: 'YubiKey (bureau)', creeeLe: il(100), derniereUtilisation: il(2) }]
  moi: Record<string, unknown>
  o: OptionsPasserelle

  motDePasse = CONNEXION_E2E.motDePasse
  /** Appareils connectés (MS-GATEWAY, docs/lot1/mon-compte.md §3). */
  appareils = [
    { id: 'app-courante', courante: true, appareil: { navigateur: 'Chrome 129', systeme: 'Windows 11', type: 'ORDINATEUR' }, adresseIp: '10.20.1.15', couche: 'console', ouverteLe: il(1), derniereActivite: il(0), expireA: null },
    { id: 'app-ancienne', courante: false, appareil: { navigateur: 'Firefox 131', systeme: 'Ubuntu 24.04', type: 'ORDINATEUR' }, adresseIp: '10.20.4.•', couche: 'console', ouverteLe: il(26), derniereActivite: il(20), expireA: null },
    { id: 'app-mobile', courante: false, appareil: { navigateur: 'Safari 18', systeme: 'iOS 18', type: 'MOBILE' }, adresseIp: '41.202.219.•', couche: 'des', ouverteLe: il(3), derniereActivite: il(1), expireA: null },
  ]
  /** Photo de profil déposée (multipart reçu), version servie par /moi. */
  photos = 0
  /** Actions exigées de la plateforme restant à faire au premier accès (D-65). */
  restantes: string[]

  constructor(o: OptionsPasserelle = {}) {
    this.o = o
    this.restantes = o.premierAcces ? ['MOT_DE_PASSE', 'SECOND_FACTEUR', 'CHARTE'] : []
    this.moi = o.premierAcces
      ? { ...compte('u-super-admin', 'super.admin', 'Serge', 'Mouelle Kombi', ['ROLE_SUPER_ADMIN'], { profilComplet: false, telephone: null, secondFacteur: false, actionsRequises: ['MOT_DE_PASSE', 'SECOND_FACTEUR'] }) }
      : { ...this.comptes[0], ...(o.moiIncomplet ? { secondFacteur: false, emailVerifie: false, telephone: null, fonction: null, langue: null, prenom: null } : {}) }
  }

  private retirer(etape: string) {
    this.restantes = this.restantes.filter((e) => e !== etape)
  }

  private etapes() {
    const m = this.moi as { profilComplet: boolean }
    return [...this.restantes, ...(m.profilComplet ? [] : ['FICHE'])]
  }

  async brancher(page: Page) {
    // Clé de sécurité simulée (WebAuthn) : assertion et attestation factices, sans authentificateur réel.
    await page.addInitScript(() => {
      const tampon = (...v: number[]) => new Uint8Array(v).buffer
      // window.__gmCleAnnulee = true : l'utilisateur annule la demande de sa clé (NotAllowedError).
      const annulee = () => (window as unknown as { __gmCleAnnulee?: boolean }).__gmCleAnnulee
      const refus = () => new DOMException('Demande annulée', 'NotAllowedError')
      Object.defineProperty(navigator, 'credentials', {
        configurable: true,
        value: {
          get: async () => {
            if (annulee()) throw refus()
            return { id: 'cle-e2e', type: 'public-key', response: { clientDataJSON: tampon(1, 2), authenticatorData: tampon(3), signature: tampon(4), userHandle: tampon(5) } }
          },
          create: async () => {
            if (annulee()) throw refus()
            return { id: 'cle-nouvelle', type: 'public-key', response: { clientDataJSON: tampon(6), attestationObject: tampon(7, 8) } }
          },
        },
      })
    })
    await page.route(/\/(api|bff)\//, (r) => this.servir(r))
  }

  private async servir(route: Route) {
    const req = route.request()
    const url = new URL(req.url())
    const chemin = url.pathname
    const methode = req.method()
    const brut = req.postDataBuffer()?.toString('utf8') ?? null
    const multipart = (req.headers()['content-type'] ?? '').startsWith('multipart/form-data')
    // Parties d'un dépôt fragmenté (D-74) : octets bruts, jamais lus comme du JSON.
    const octets = (req.headers()['content-type'] ?? '').startsWith('application/octet-stream')
    const corps = brut && !multipart && !octets ? JSON.parse(brut) : undefined
    this.appels.push({ methode, chemin, recherche: url.search, entetes: req.headers(), corps })
    const json = (b: unknown, status = 200) => route.fulfill({ status, json: b, headers: { 'X-Correlation-Id': `e2e-${this.appels.length}` } })
    const refus = (status: number, code: string, message: string, details: Record<string, unknown> = {}) => json({ code, message, details, correlationId: `e2e-${code}` }, status)
    const vide = () => route.fulfill({ status: 204 })

    // ------------------------------------------------ Connexion sur la plateforme (D-65, /api/connexion/**)
    if (chemin === '/api/connexion') {
      const premier = !!this.o.premierAcces && this.etapes().length > 0
      const attendu = this.o.premierAcces ? (this.etapes().includes('MOT_DE_PASSE') ? CONNEXION_E2E.provisoire : this.motDePasse) : CONNEXION_E2E.motDePasse
      if (corps.identifiant !== 'super.admin' || corps.motDePasse !== attendu) return refus(401, 'CNX_IDENTIFIANTS_INVALIDES', 'Identifiants invalides')
      if (corps.couche !== 'console') return refus(403, 'CNX_COUCHE_REFUSEE', 'Couche refusée')
      if (premier) return json({ etat: 'PREMIER_ACCES', etapes: this.etapes() })
      if (!corps.code) return json({ etat: 'CODE_REQUIS' })
      if (corps.code !== CONNEXION_E2E.code) return refus(401, 'CNX_IDENTIFIANTS_INVALIDES', 'Identifiants invalides')
      this.o.premierAcces = false
      this.o.session = { permissions: PERMISSIONS_SUPER_ADMIN }
      return json({ etat: 'CONNECTE', session: {} })
    }
    if (chemin === '/api/connexion/mot-de-passe-oublie') return route.fulfill({ status: 202 })
    if (chemin === '/api/connexion/nouveau-mot-de-passe') {
      if (corps.jeton !== 'jeton-valide-e2e-0123456789') return refus(400, 'CNX_JETON_INVALIDE', 'Jeton invalide')
      if (corps.motDePasse.length < 12) return refus(400, 'IAM_PASSWORD_POLICY', 'Politique', { violations: ['invalidPasswordMinLengthMessage', 'invalidPasswordMinDigitsMessage'] })
      return vide()
    }
    if (chemin === '/api/session/prolongation') return vide()
    // ------------------------------------------------ Appareils connectés et historique (docs/lot1/mon-compte.md §3)
    if (chemin === '/api/session/appareils' && methode === 'GET') return json(this.appareils)
    if (chemin === '/api/session/appareils' && methode === 'DELETE') {
      this.appareils = this.appareils.filter((a) => a.courante)
      return vide()
    }
    if (chemin.startsWith('/api/session/appareils/') && methode === 'DELETE') {
      const id = decodeURIComponent(chemin.split('/').pop()!)
      if (!this.appareils.some((a) => a.id === id)) return refus(404, 'CNX_APPAREIL_INCONNU', 'Session inconnue')
      if (this.appareils.find((a) => a.id === id)?.courante) return refus(409, 'CNX_SESSION_COURANTE', 'Session courante')
      this.appareils = this.appareils.filter((a) => a.id !== id)
      return vide()
    }
    if (chemin === '/api/session/historique') {
      const page = Number(url.searchParams.get('page') ?? 0)
      const taille = Number(url.searchParams.get('taille') ?? 20)
      const tout = Array.from({ length: 27 }, (_, i) => ({ le: il(i * 7 + 1), resultat: i === 3 ? 'ECHEC_CODE' : i === 9 ? 'BLOQUEE' : 'REUSSIE', appareil: { navigateur: 'Chrome 129', systeme: 'Windows 11', type: 'ORDINATEUR' }, adresseIp: '10.20.1.•', couche: 'console' }))
      return json({ contenu: tout.slice(page * taille, (page + 1) * taille), total: tout.length })
    }
    // ------------------------------------------------ Photos de profil (docs/lot1/mon-compte.md §1)
    if (chemin.startsWith('/bff/console/iam/avatars/')) {
      return route.fulfill({ status: 200, contentType: 'image/svg+xml', body: '<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"><rect width="256" height="256" fill="#3f8a83"/><circle cx="90" cy="80" r="30" fill="#f8f4e3"/></svg>' })
    }
    if (chemin === '/bff/console/iam/moi/avatar' && methode === 'PUT') {
      if (!multipart || !brut?.includes('name="image"')) return refus(400, 'IAM_AVATAR_UNREADABLE', 'Image illisible')
      this.moi.avatarVersion = `e2e${++this.photos}`
      return json({ version: this.moi.avatarVersion })
    }
    if (chemin === '/bff/console/iam/moi/avatar' && methode === 'DELETE') {
      this.moi.avatarVersion = null
      return vide()
    }
    if (this.o.sessionExpiree && chemin.startsWith('/bff/')) return refus(401, 'CNX_SESSION_EXPIREE', 'Session fermée après inactivité')
    if (chemin === '/api/session') {
      if (this.o.sessionIndisponible) return refus(503, 'HTTP_503', 'Passerelle indisponible')
      if (!this.o.session) return refus(401, 'HTTP_401', 'Non authentifié')
      const rssi = !this.o.session.permissions.includes('IAM_ROLE_PRIVILEGED_GRANT')
      return json({ sujet: this.o.session.sujet ?? 'u-super-admin', nom: rssi ? 'Carine Ebongue Ndjock' : 'Serge Mouelle Kombi', identifiant: rssi ? 'rssi' : 'super.admin', roles: [rssi ? 'ROLE_RSSI' : 'ROLE_SUPER_ADMIN', 'ROLE_USER'], permissions: this.o.session.permissions,
        institutionId: '', acr: this.o.session.doubleFacteur === false || this.o.premierAcces ? '1' : '2', doubleFacteur: this.o.session.doubleFacteur !== false && !this.o.premierAcces,
        ...(this.o.premierAcces ? { restreinte: true } : {}), expireA: new Date(Date.now() + 30 * 60_000).toISOString() })
    }
    if (chemin === '/api/logout') return json({ logoutUrl: '/connexion' })

    // ------------------------------------------------ Mon compte (/bff/console/iam/moi)
    const M = '/bff/console/iam/moi'
    if (chemin === M && methode === 'GET') return json({ compte: this.moi, clesSecurite: this.cles.length, sessions: 1, etapesRestantes: this.etapes() })
    if (chemin === M && methode === 'PUT') {
      Object.assign(this.moi, corps)
      return json({ compte: this.moi, clesSecurite: this.cles.length, sessions: 1, etapesRestantes: this.etapes() })
    }
    if (chemin === `${M}/premiere-configuration`) {
      Object.assign(this.moi, { telephone: corps.telephone, langue: corps.langue, profilComplet: true })
      this.retirer('CHARTE')
      return json({ compte: this.moi, clesSecurite: this.cles.length, sessions: 1, etapesRestantes: this.etapes() })
    }
    if (chemin === `${M}/mot-de-passe`) {
      const actuel = this.etapes().includes('MOT_DE_PASSE') ? CONNEXION_E2E.provisoire : this.motDePasse
      if (corps.actuel !== actuel) return refus(400, 'IAM_PASSWORD_INVALID', 'Mot de passe actuel incorrect')
      if (corps.nouveau.length < 12) return refus(400, 'IAM_PASSWORD_POLICY', 'Politique', { violations: ['invalidPasswordMinLengthMessage'] })
      this.motDePasse = corps.nouveau
      this.retirer('MOT_DE_PASSE')
      return vide()
    }
    if (chemin === `${M}/second-facteur/totp/enrolement`) {
      return json({ enrolementId: 'enr-totp-1', secret: 'JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP', uri: 'otpauth://totp/GIMAC%20DES%3Asuper.admin?secret=JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP&issuer=GIMAC%20DES&digits=6&period=30&algorithm=SHA1' })
    }
    if (chemin === `${M}/second-facteur/totp`) {
      if (corps.code === '000000') return refus(400, 'IAM_TOTP_CODE_INVALID', 'Code incorrect')
      this.moi.secondFacteur = true
      this.retirer('SECOND_FACTEUR')
      return vide()
    }
    if (chemin === `${M}/sessions` && methode === 'GET') return json([{ id: 's-1', adresseIp: '10.20.1.15', debut: il(1), dernierAcces: il(0), applications: ['gateway'] }])
    if (chemin.startsWith(`${M}/sessions`) && methode === 'DELETE') return vide()
    if (chemin === `${M}/cles-securite` && methode === 'GET') return json(this.cles)
    if (chemin === `${M}/cles-securite/defi`) {
      return json({ challengeId: 'enr-1', challenge: 'AQID', rp: { id: 'localhost', name: 'GIMAC DES' }, user: { id: 'dS0x', name: 'u-super-admin', displayName: 'Serge' },
        pubKeyCredParams: [{ type: 'public-key', alg: -7 }, { type: 'public-key', alg: -257 }], timeoutMs: 60000, excludeCredentials: this.cles.map(() => 'AQ') })
    }
    if (chemin === `${M}/cles-securite` && methode === 'POST') {
      const cle = { id: `k-${this.cles.length + 1}`, nom: corps.nom, creeeLe: new Date().toISOString(), derniereUtilisation: null }
      this.cles = [...this.cles, cle]
      return json(cle)
    }
    if (chemin.startsWith(`${M}/cles-securite/`) && methode === 'DELETE') {
      this.cles = this.cles.filter((c) => !chemin.endsWith(c.id))
      return vide()
    }
    if (chemin === '/bff/console/iam/reauthentification/defi') {
      if (this.o.sansCle) return refus(409, 'IAM_NO_SECURITY_KEY', 'Aucune clé de sécurité enrôlée')
      return json({ challengeId: 'defi-1', challenge: 'AQIDBA', rpId: 'localhost', allowCredentials: ['AQ'], timeoutMs: 60000 })
    }

    // ------------------------------------------------ Comptes (/bff/console/iam/comptes)
    const C = '/bff/console/iam/comptes'
    if (chemin === C && methode === 'GET' && this.o.comptesEnErreur) return refus(500, 'HTTP_500', 'Erreur interne du service')
    if (chemin === C && methode === 'GET') {
      const q = (url.searchParams.get('q') ?? '').toLowerCase()
      const l = this.comptes.filter((c) => !q || c.identifiant.includes(q))
      return json({ contenu: l, total: l.length })
    }
    if (chemin === `${C}/indicateurs`) {
      return json({ total: 3, actifs: 3, inactifs: 0, premierAccesEnAttente: 0, sansSecondFacteur: 1, privilegies: 2, creesDepuis30Jours: 0, actesDepuis24Heures: 4, parProfil: { ROLE_SUPER_ADMIN: 1, ROLE_RSSI: 1, ROLE_SNA: 1 } })
    }
    if (chemin === `${C}/journal`) {
      return json({ contenu: [{ id: 'j-1', horodatage: il(1), acteurId: 'u-rssi', acteurNom: 'Carine Ebongue Ndjock', action: 'PROFILS', cibleId: 'u-sna', cibleLibelle: 'r.biyong', motif: 'Revue annuelle', details: 'ROLE_SNA', correlationId: 'c-1' }], total: 1 })
    }
    if (chemin === C && methode === 'POST') {
      if (this.comptes.some((c) => c.identifiant === corps.identifiant)) return refus(409, 'IAM_ACCOUNT_CONFLICT', 'Identifiant ou courriel déjà utilisé')
      const n = compte(`u-${corps.identifiant}`, corps.identifiant, corps.prenom, corps.nom, corps.profils, { profilComplet: false, premierAccesEnAttente: true })
      this.comptes.push(n)
      return json({ compte: n, invitationEnvoyee: corps.envoyerInvitation, motDePasseProvisoire: corps.motDePasseProvisoire ? 'Hn5#Wd2!Qs8vKp4z' : null }, 201)
    }
    const m = chemin.match(/^\/bff\/console\/iam\/comptes\/([^/]+)(\/.*)?$/)
    if (m) {
      const c = this.comptes.find((x) => x.id === decodeURIComponent(m[1]))
      if (!c) return refus(404, 'IAM_ACCOUNT_NOT_FOUND', 'Ressource introuvable')
      const suite = m[2] ?? ''
      if (suite === '' && methode === 'GET') return json(c)
      if (suite === '/sessions') return json([])
      if (suite === '/profils') {
        const assertion = req.headers()['x-reauth-assertion']
        const a = assertion ? JSON.parse(Buffer.from(assertion, 'base64').toString('utf8')) : null
        if (!a || a.challengeId !== 'defi-1' || a.id !== 'cle-e2e') return refus(403, 'IAM_REAUTH_INVALID', 'Réauthentification forte invalide')
        if (this.profilsRefuses++ === 0) return refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification forte exigée')
        c.profils = corps.profils
        return json(c)
      }
      if (suite === '/desactivation' || suite === '/activation') {
        c.actif = suite === '/activation'
        return json(c)
      }
      if (suite === '' && methode === 'PUT') return json(Object.assign(c, corps))
      if (suite === '/actions-requises') return json(Object.assign(c, { actionsRequises: corps.actions, premierAccesEnAttente: true }))
      if (suite === '/second-facteur/reinitialisation') return json(Object.assign(c, { secondFacteur: false }))
      if (suite === '/mot-de-passe-provisoire') return json({ motDePasseProvisoire: 'Zq8#Lm3!Vt6pWx2r' })
      if (suite === '/sessions/fermeture') return vide()
      if (suite === '' && methode === 'DELETE') {
        if (!req.headers()['x-reauth-assertion']) return refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification forte exigée')
        this.comptes = this.comptes.filter((x) => x !== c)
        return vide()
      }
    }

    // ------------------------------------------------ Configuration (/bff/console/iam/configuration)
    const K = '/bff/console/iam/configuration'
    const politique = { longueurMinimale: 14, majusculesMin: 1, minusculesMin: 1, chiffresMin: 1, speciauxMin: 1, historique: 5, expirationJours: 90, interditIdentifiant: true,
      otpChiffres: 6, otpPeriodeSecondes: 30, otpAlgorithme: 'HmacSHA256', sessionInactiviteMinutes: 15, sessionDureeMaxHeures: 8, forceBruteActive: true, forceBruteEchecsMax: 5, forceBruteAttenteMinutes: 15 }
    if (chemin === `${K}/politique-securite` && methode === 'GET') return json(politique)
    if (chemin === `${K}/politique-securite` && methode === 'PUT') {
      if (!this.politiqueRefusee) {
        this.politiqueRefusee = true
        return refus(400, 'IAM_POLICY_INVALID', 'Politique de sécurité refusée', { champs: { sessionInactiviteMinutes: '5 à 120', classes: 'somme > longueur' } })
      }
      return json(corps.politique)
    }
    const messagerie = { hote: 'smtp.gimac-afr.org', port: 587, expediteur: 'des@gimac-afr.org', nomExpediteur: 'Plateforme DES', starttls: true, ssl: false, authentification: true,
      utilisateur: 'des', motDePasse: null, motDePasseDefini: false }
    if (chemin === `${K}/messagerie` && methode === 'GET') return json(messagerie)
    if (chemin === `${K}/messagerie` && methode === 'PUT') return json({ ...corps.messagerie, motDePasse: null, motDePasseDefini: !!corps.messagerie.motDePasse })
    if (chemin === `${K}/messagerie/test`) return vide()
    if (chemin.startsWith(`${K}/profils/`) && methode === 'PUT') {
      if (!req.headers()['x-reauth-assertion']) return refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification forte exigée')
      return json({ code: 'ROLE_SNA', description: 'Service Normes et Agréments', permissions: corps.permissions, privilegie: false, titulaires: 1 })
    }
    if (chemin === `${K}/profils`) return json([{ code: 'ROLE_SNA', description: 'Service Normes et Agréments', permissions: ['HOM_EVIDENCE_SUBMIT', 'INTERNAL_IAM_READ'], privilegie: false, titulaires: 1 }])
    if (chemin === `${K}/permissions`) return json(['HOM_EVIDENCE_SUBMIT', 'HOM_REPLAY_ADMIN', 'IAM_ACCOUNT_READ', 'INTERNAL_IAM_READ'])

    // ------------------------------------------------ Supervision (/api/plateforme)
    if (this.o.degradee && chemin === '/api/plateforme/services') return json(instantaneDegrade())
    if (this.o.degradee && chemin === '/api/plateforme/serveurs') {
      return json({ horodatage: il(0), prometheusJoignable: false, erreur: 'Prometheus injoignable : connexion refusée', ciblesUp: 0, ciblesDown: 0, cibles: [], nodeExporter: false, hotes: [], conteneurs: [] })
    }
    if (this.o.degradee && chemin === '/api/plateforme/alertes') {
      return json({ horodatage: il(0), joignable: true, erreur: null, nombre: 3, alertes: [
        { nom: 'ServiceInjoignable', severite: 'critical', resume: 'MS-AUDIT ne répond plus', description: null, depuis: il(0.1), cible: 'audit:8087', job: 'audit', etat: 'firing' },
        { nom: 'LatenceElevee', severite: 'warning', resume: 'Latence de la passerelle élevée', description: null, depuis: il(1), cible: null, job: 'gateway', etat: 'firing' },
        { nom: 'CertificatBientotExpire', severite: null, resume: null, description: null, depuis: null, cible: 'gateway:8080', job: 'gateway', etat: 'firing' },
      ] })
    }
    if (chemin === '/api/plateforme/services') return json(instantane('DEGRADEE'))
    if (chemin === '/api/plateforme/serveurs') return json({ horodatage: il(0), prometheusJoignable: true, erreur: null, ciblesUp: 3, ciblesDown: 0, cibles: [], nodeExporter: true,
      hotes: [{ instance: 'gimac-app-01', cpuPourcent: 12, memoirePourcent: 40, memoireTotaleOctets: 1, disquePourcent: 20, chargeMoyenne1m: 0.4 }], conteneurs: [] })
    if (chemin === '/api/plateforme/alertes') return json({ horodatage: il(0), joignable: true, erreur: null, nombre: 0, alertes: [] })
    if (chemin === '/api/plateforme/configuration') {
      return json({ horodatage: il(0), routes: [{ id: 'bff-console-iam', uri: 'http://iam:8081', predicats: 'Paths: [/bff/console/iam/**]', filtres: ['OpaAuthorize'], ordre: 0 }],
        services: [{ nom: 'iam', url: 'http://iam:8081', lue: true, erreur: null, profilsActifs: ['prod'], proprietes: { 'spring.datasource.password': '******' } }] })
    }
    if (chemin.startsWith('/api/plateforme/journalisation/') && methode === 'GET') {
      return json({ service: 'iam', niveauxDisponibles: ['OFF', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'], loggers: [{ nom: 'ROOT', niveauConfigure: 'INFO', niveauEffectif: 'INFO' }] })
    }
    if (chemin.startsWith('/api/plateforme/journalisation/') && methode === 'POST') {
      return json({ service: 'iam', logger: corps.logger, niveau: corps.niveau, motif: corps.motif, auteur: 'super.admin', horodatage: il(0) })
    }
    if (chemin === '/api/plateforme/flux') {
      this.fluxOuverts++
      const impulsion = { sequence: this.fluxOuverts, instantane: this.o.degradee ? instantaneDegrade() : instantane('OPERATIONNELLE'), nombreAlertes: this.o.degradee ? -1 : 0 }
      return route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' }, body: `id: ${this.fluxOuverts}\nevent: plateforme\ndata: ${JSON.stringify(impulsion)}\n\n` })
    }
    // ------------------------------------------------ Messagerie instantanée depuis la console (D-64)
    const CM = '/bff/console/collaboration/messagerie'
    if (chemin === '/bff/console/live') {
      return route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' }, body: ': battement\n\n' })
    }
    if (chemin === '/bff/console/presence') return json({ enLigne: ['u-dg'], vusLe: {} })
    if (chemin === `${CM}/conversations/c-dg/saisie`) return vide()
    // Dépôt fragmenté (D-74) : parties de 8 octets ; la deuxième échoue une fois (503), puis vérification en deux relectures.
    const D = '/bff/console/document/depots'
    if (chemin === D && methode === 'POST') {
      this.depot = { id: 'dep-1', etat: 'OUVERT', nomFichier: corps.nomFichier, typeMime: corps.typeMime, taille: corps.taille, taillePartie: 8, nombreParties: Math.ceil(corps.taille / 8), partiesRecues: [] as number[] }
      return json(this.depot, 201)
    }
    const partie = new RegExp(`^${D}/dep-1/parties/(\\d+)$`).exec(chemin)
    if (partie && methode === 'PUT') {
      const n = Number(partie[1])
      this.parties.push({ n, type: req.headers()['content-type'] ?? '', octets: req.postDataBuffer()?.length ?? 0 })
      if (n === 2 && !this.partieEchouee) {
        this.partieEchouee = true
        return refus(503, 'DOC_STORAGE_UNAVAILABLE', 'Dépôt objet indisponible')
      }
      this.depot!.partiesRecues.push(n)
      return vide()
    }
    if (chemin === `${D}/dep-1/fin` && methode === 'POST') {
      this.depot!.etat = 'EN_VERIFICATION'
      return json(this.depot, 202)
    }
    if (chemin === `${D}/dep-1` && methode === 'GET') {
      if (this.depot!.etat === 'EN_VERIFICATION' && ++this.relectures >= 2) Object.assign(this.depot!, { etat: 'DISPONIBLE', documentId: 'doc-1', sha256: 'b'.repeat(64), dedoublonne: false })
      return json(this.depot)
    }
    // Pièce volumineuse (4 Go) : téléchargée par lien direct, en flux.
    if (chemin === `${CM}/pieces/pj-gros`) return route.fulfill({ status: 200, headers: { 'Content-Type': 'video/mp4', 'Content-Disposition': 'attachment; filename="atelier-revue.mp4"' }, body: 'video' })
    if (chemin === `${CM}/non-lus`) return json({ total: this.messages.length === 1 ? 1 : 0 })
    if (chemin === `${CM}/conversations` && methode === 'GET') return json({ contenu: [this.conversation()], total: 1, page: 0, taille: 20 })
    if (chemin === `${CM}/conversations/c-dg` && methode === 'GET') return json(this.conversation())
    if (chemin === `${CM}/conversations/c-dg/messages` && methode === 'GET') return json({ contenu: this.messages.slice().reverse(), plusAnciens: false })
    if (chemin === `${CM}/conversations/c-dg/lu` && methode === 'POST') return vide()
    if (chemin === `${CM}/conversations/c-dg/messages` && methode === 'POST') {
      const contenu = /"contenu":"([^"]*)"/.exec(brut ?? '')?.[1] ?? ''
      const documents = JSON.parse(/"documents":(\[[^\]]*\])/.exec(brut ?? '')?.[1] ?? '[]') as string[]
      this.documentsRecus.push(...documents)
      const piecesJointes = documents.map((d) => ({ id: `pj-${d}`, nom: this.depot?.nomFichier ?? d, taille: this.depot?.taille ?? 0, typeMime: this.depot?.typeMime ?? 'application/octet-stream', empreinte: 'b'.repeat(64) }))
      const m = { id: `m-${this.messages.length + 1}`, conversationId: 'c-dg', auteur: { id: 'u-super-admin', nom: 'Serge Mouelle Kombi', fonction: null }, moi: true, contenu, etiquettes: [], piecesJointes, envoyeLe: il(0) }
      this.messages.push(m)
      return json(m, 201)
    }
    return refus(404, 'HTTP_404', `Route non simulée : ${methode} ${chemin}`)
  }

  /** Dépôt fragmenté en cours (D-74), parties reçues, tickets portés par les messages. */
  depot: { id: string; etat: string; nomFichier: string; typeMime: string; taille: number; taillePartie: number; nombreParties: number; partiesRecues: number[]; documentId?: string } | null = null
  parties: { n: number; type: string; octets: number }[] = []
  partieEchouee = false
  relectures = 0
  documentsRecus: string[] = []

  /** Échange direct avec la direction générale, servi par MS-COLLABORATION derrière /bff/console. */
  messages: Record<string, unknown>[] = [{
    id: 'm-1', conversationId: 'c-dg', auteur: { id: 'u-dg', nom: 'François Tchoumi Kamga', fonction: 'Directeur général adjoint' }, moi: false,
    contenu: 'Pouvez-vous me confirmer la revue des habilitations avant vendredi ?', etiquettes: [],
    piecesJointes: [{ id: 'pj-gros', nom: 'atelier-revue.mp4', taille: 4 * 1024 ** 3, typeMime: 'video/mp4', empreinte: 'c'.repeat(64) }], envoyeLe: il(1),
  }]

  private conversation() {
    const dernier = this.messages.at(-1) as { id: string; auteur: { nom: string }; contenu: string; envoyeLe: string }
    return {
      id: 'c-dg', type: 'DIRECTE', titre: null, liaison: null, nonLus: this.messages.length === 1 ? 1 : 0, misAJourLe: dernier.envoyeLe, creeeLe: il(2),
      membres: [{ id: 'u-super-admin', nom: 'Serge Mouelle Kombi', fonction: null }, { id: 'u-dg', nom: 'François Tchoumi Kamga', fonction: 'Directeur général adjoint' }],
      dernierMessage: { id: dernier.id, auteurNom: dernier.auteur.nom, extrait: dernier.contenu, envoyeLe: dernier.envoyeLe, piecesJointes: 0 },
    }
  }
}
