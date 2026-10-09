import { afterEach, describe, expect, it, vi } from 'vitest'
import { ReauthCancelled } from '@gimac/socle'
import { servicesConsoleSimules } from './index'
import { politiqueInitiale } from './donnees'

// Serveur simulé de la console : mêmes formes et mêmes refus que MS-IAM et MS-GATEWAY.
const A = btoa('assertion-simulee')
const RSSI = { id: 'u-rssi', nom: 'Hervé Kamga Tchoupo', permissions: ['IAM_ACCOUNT_READ'] }
const s = (o = {}) => servicesConsoleSimules({ latenceMs: 0, ...o })

afterEach(() => vi.useRealTimers())

describe('comptes simulés', () => {
  it('liste filtrée par recherche, profil et état ; pagination ; indicateurs', async () => {
    const c = s().comptes
    expect((await c.lister({ page: 1, taille: 100 })).total).toBe(14)
    expect((await c.lister({ page: 1, taille: 100, q: 'super' })).contenu.map((x) => x.identifiant)).toEqual(['super.admin'])
    expect((await c.lister({ page: 1, taille: 100, profil: 'ROLE_RSSI' })).contenu.map((x) => x.identifiant)).toEqual(['h.kamga'])
    expect((await c.lister({ page: 1, taille: 100, etat: 'INACTIF' })).contenu.map((x) => x.identifiant)).toEqual(['k.ateba'])
    expect((await c.lister({ page: 1, taille: 100, etat: 'PREMIER_ACCES' })).contenu.map((x) => x.identifiant)).toEqual(['l.nguema'])
    expect((await c.lister({ page: 1, taille: 100, etat: 'SANS_2FA' })).contenu.map((x) => x.identifiant)).toEqual(['b.hamadou', 'l.nguema'])
    expect((await c.lister({ page: 1, taille: 100, etat: 'ACTIF' })).total).toBe(13)
    expect((await c.lister({ page: 2, taille: 5 })).contenu).toHaveLength(5)
    const i = await c.indicateurs()
    expect(i).toMatchObject({ total: 14, actifs: 13, inactifs: 1, premierAccesEnAttente: 1, sansSecondFacteur: 2, privilegies: 3, creesDepuis30Jours: 1 })
    expect(i.parProfil.ROLE_SUPER_ADMIN).toBe(1)
    expect((await c.journal({ page: 1, taille: 10, compte: 'u-inactif' })).contenu[0].action).toBe('DESACTIVATION')
    expect((await c.sessions('u-chef-ddrm'))).toHaveLength(1)
    expect((await c.sessions('u-premier-acces'))).toHaveLength(0)
    await expect(c.lire('inconnu')).rejects.toMatchObject({ status: 404, code: 'IAM_ACCOUNT_NOT_FOUND' })
  })

  it('création : règles d’identifiant, de téléphone, de courriel, d’accueil, de profils et d’unicité', async () => {
    const c = s().comptes
    const base = { identifiant: 'n.agent', prenom: 'Nadia', nom: 'Agent', email: 'n.agent@gimac-afr.org', profils: ['ROLE_SNA'], envoyerInvitation: true, motDePasseProvisoire: false }
    await expect(c.creer({ ...base, identifiant: '1x' })).rejects.toMatchObject({ code: 'IAM_USERNAME_INVALID' })
    await expect(c.creer({ ...base, telephone: 'abc' })).rejects.toMatchObject({ code: 'IAM_PHONE_INVALID' })
    await expect(c.creer({ ...base, email: ' ' })).rejects.toMatchObject({ code: 'IAM_EMAIL_REQUIRED' })
    await expect(c.creer({ ...base, envoyerInvitation: false })).rejects.toMatchObject({ code: 'IAM_ONBOARDING_REQUIRED' })
    await expect(c.creer({ ...base, profils: [] })).rejects.toMatchObject({ code: 'IAM_PROFILE_UNKNOWN' })
    await expect(c.creer({ ...base, profils: ['ROLE_INCONNU'] })).rejects.toMatchObject({ code: 'IAM_PROFILE_UNKNOWN' })
    await expect(c.creer({ ...base, identifiant: 'h.kamga' })).rejects.toMatchObject({ status: 409, code: 'IAM_ACCOUNT_CONFLICT' })
    const r = await c.creer({ ...base, identifiant: ' N.Agent ', motDePasseProvisoire: true })
    expect(r.compte.identifiant).toBe('n.agent')
    expect(r.compte.premierAccesEnAttente).toBe(true)
    expect(r.motDePasseProvisoire).toBeTruthy()
    expect((await c.journal({ page: 1, taille: 1 })).contenu[0].action).toBe('CREATION')
    await expect(s({ acteur: RSSI }).comptes.creer({ ...base, profils: ['ROLE_SUPER_ADMIN'] })).rejects.toMatchObject({ status: 403, code: 'IAM_PRIVILEGED_PROFILE' })
  })

  it('actes sensibles : 428 sans assertion, 403 si invalide ; séparation des pouvoirs ; dernier super administrateur', async () => {
    const c = s().comptes
    await expect(c.definirProfils('u-sna', ['ROLE_SNA'], 'Motif valable', '')).rejects.toMatchObject({ status: 428, code: 'IAM_REAUTH_REQUIRED' })
    await expect(c.definirProfils('u-sna', ['ROLE_SNA'], 'Motif valable', 'faux')).rejects.toMatchObject({ status: 403, code: 'IAM_REAUTH_INVALID' })
    await expect(c.definirProfils('u-super-admin', ['ROLE_SNA'], 'Motif', A)).rejects.toMatchObject({ code: 'IAM_SELF_ADMINISTRATION' })
    await expect(c.definirProfils('u-sna', ['ROLE_SNA'], ' ', A)).rejects.toMatchObject({ code: 'IAM_REASON_REQUIRED' })
    expect((await c.definirProfils('u-sna', ['ROLE_SNA', 'ROLE_CONFORMITE'], 'Intérim conformité', A)).profils).toEqual(['ROLE_CONFORMITE', 'ROLE_SNA'])
    const r = s({ acteur: RSSI }).comptes
    await expect(r.definirProfils('u-sna', ['ROLE_RSSI'], 'Motif', A)).rejects.toMatchObject({ code: 'IAM_PRIVILEGED_PROFILE' })
    await expect(r.desactiver('u-super-admin', 'Motif')).rejects.toMatchObject({ code: 'IAM_LAST_SUPER_ADMIN' })
    await expect(r.supprimer('u-super-admin', 'Motif', A)).rejects.toMatchObject({ code: 'IAM_LAST_SUPER_ADMIN' })
    await expect(r.definirProfils('u-super-admin', ['ROLE_SNA'], 'Motif', A)).rejects.toMatchObject({ code: 'IAM_PRIVILEGED_PROFILE' })
    await expect(c.supprimer('u-sna', 'Motif', '')).rejects.toMatchObject({ status: 428 })
    await c.supprimer('u-sna', 'Doublon de compte', A)
    await expect(c.lire('u-sna')).rejects.toMatchObject({ status: 404 })
  })

  it('statut, actions requises, second facteur, mot de passe provisoire, sessions, fiche', async () => {
    const c = s().comptes
    expect((await c.desactiver('u-dg', 'Départ en retraite')).actif).toBe(false)
    expect((await c.activer('u-dg', 'Retour')).actif).toBe(true)
    await expect(c.exigerActions('u-dg', [], false, 'Motif')).rejects.toMatchObject({ code: 'IAM_ACTIONS_REQUIRED' })
    const x = await c.exigerActions('u-dg', ['MOT_DE_PASSE', 'VERIFIER_COURRIEL'], true, 'Suspicion de fuite')
    expect(x.actionsRequises).toEqual(['MOT_DE_PASSE', 'VERIFIER_COURRIEL'])
    expect(x.premierAccesEnAttente).toBe(true)
    expect((await c.reinitialiserSecondFacteur('u-dg', 'Téléphone perdu')).secondFacteur).toBe(false)
    expect((await c.motDePasseProvisoire('u-dg', 'Oubli')).motDePasseProvisoire).toBeTruthy()
    await c.fermerSessions('u-chef-des', 'Poste volé')
    expect(await c.sessions('u-chef-des')).toEqual([])
    await expect(c.modifier('u-dg', { telephone: '12' })).rejects.toMatchObject({ code: 'IAM_PHONE_INVALID' })
    await expect(c.modifier('u-dg', { email: 'h.kamga@gimac-afr.org' })).rejects.toMatchObject({ code: 'IAM_ACCOUNT_CONFLICT' })
    expect((await c.modifier('u-dg', { prenom: 'Francis', fonction: 'DGA' })).nomAffiche).toBe('Francis Tchoumi Kamga')
  })
})

describe('configuration simulée', () => {
  it('politique : planchers, champs refusés, application signée', async () => {
    const c = s().configuration
    const p = politiqueInitiale()
    await expect(c.appliquerPolitique(p, 'Motif', '')).rejects.toMatchObject({ status: 428 })
    const faible = { ...p, longueurMinimale: 8, majusculesMin: 5, minusculesMin: 5, historique: 30, expirationJours: 400, otpChiffres: 7, otpPeriodeSecondes: 10, otpAlgorithme: 'MD5',
      sessionInactiviteMinutes: 1, sessionDureeMaxHeures: 30, forceBruteActive: false, forceBruteEchecsMax: 20, forceBruteAttenteMinutes: 0 }
    const e = (await c.appliquerPolitique(faible, 'Motif', A).catch((x: unknown) => x)) as unknown as { code: string; details: { champs: Record<string, string> } }
    expect(e.code).toBe('IAM_POLICY_INVALID')
    expect(Object.keys(e.details.champs).sort()).toEqual(['classes', 'expirationJours', 'forceBruteActive', 'forceBruteAttenteMinutes', 'forceBruteEchecsMax', 'historique', 'longueurMinimale',
      'otpAlgorithme', 'otpChiffres', 'otpPeriodeSecondes', 'sessionDureeMaxHeures', 'sessionInactiviteMinutes'])
    expect((await c.appliquerPolitique({ ...p, longueurMinimale: 16 }, 'Alignement PSSI', A)).longueurMinimale).toBe(16)
  })

  it('messagerie : secret jamais relu, conservé sans nouvelle valeur ; test ; refus', async () => {
    const c = s().configuration
    const m = await c.messagerie()
    expect(m.motDePasse).toBeNull()
    expect(m.motDePasseDefini).toBe(true)
    await expect(c.appliquerMessagerie({ ...m, hote: '' }, 'Motif')).rejects.toMatchObject({ code: 'IAM_SMTP_INVALID' })
    await expect(c.appliquerMessagerie({ ...m, utilisateur: '' }, 'Motif')).rejects.toMatchObject({ code: 'IAM_SMTP_INVALID' })
    await expect(c.appliquerMessagerie(m, '')).rejects.toMatchObject({ code: 'IAM_REASON_REQUIRED' })
    const r = await c.appliquerMessagerie({ ...m, port: 465, ssl: true, motDePasse: 'nouveau' }, 'Nouveau relais')
    expect(r).toMatchObject({ port: 465, motDePasse: null, motDePasseDefini: true })
    await expect(c.testerMessagerie('pas-une-adresse')).rejects.toMatchObject({ code: 'IAM_SMTP_INVALID' })
    await c.testerMessagerie('rssi@gimac-afr.org')
  })

  it('profils : titulaires, privilégiés réservés, permissions inconnues', async () => {
    const c = s().configuration
    const profils = await c.profils()
    expect(profils.find((p) => p.code === 'ROLE_RSSI')).toMatchObject({ privilegie: true, titulaires: 1 })
    expect((await c.permissions()).some((p) => p.startsWith('INTERNAL_'))).toBe(true)
    await expect(c.definirPermissions('ROLE_X', [], 'Motif', A)).rejects.toMatchObject({ code: 'IAM_PROFILE_NOT_FOUND' })
    await expect(c.definirPermissions('ROLE_SNA', ['NOPE'], 'Motif', A)).rejects.toMatchObject({ code: 'IAM_PERMISSION_UNKNOWN' })
    expect((await c.definirPermissions('ROLE_SNA', ['HOM_EVIDENCE_SUBMIT'], 'Revue', A)).permissions).toEqual(['HOM_EVIDENCE_SUBMIT'])
    await expect(s({ acteur: RSSI }).configuration.definirPermissions('ROLE_SUPER_ADMIN', [], 'Motif', A)).rejects.toMatchObject({ code: 'IAM_PRIVILEGED_PROFILE' })
  })
})

describe('supervision simulée et clé de sécurité', () => {
  it('services, serveurs, alertes, configuration, journalisation', async () => {
    const p = s().plateforme
    expect((await p.services()).global.etat).toBe('DEGRADEE')
    expect((await p.serveurs()).hotes).toHaveLength(3)
    expect((await p.alertes()).nombre).toBe(3)
    expect((await p.configuration()).services.map((x) => x.nom)).toContain('iam')
    expect((await p.journalisation('iam', 'org.')).loggers.every((l) => l.nom.startsWith('org.'))).toBe(true)
    await expect(p.modifierJournalisation('iam', { logger: 'ROOT', niveau: 'DEBUG', motif: 'x' })).rejects.toMatchObject({ status: 400 })
    expect((await p.modifierJournalisation('iam', { logger: 'ROOT', niveau: 'DEBUG', motif: 'Analyse incident' })).niveau).toBe('DEBUG')
  })

  it('flux : connexion puis impulsions périodiques, arrêt', () => {
    vi.useFakeTimers()
    const etats: string[] = []
    const sequences: number[] = []
    const fermer = servicesConsoleSimules({ latenceMs: 10, periodeFluxMs: 100 }).plateforme.flux({ onEtat: (e) => etats.push(e), onImpulsion: (i) => sequences.push(i.sequence) })
    vi.advanceTimersByTime(10)
    expect(etats).toEqual(['connexion', 'connecte'])
    vi.advanceTimersByTime(200)
    expect(sequences).toEqual([1, 2, 3])
    fermer()
    vi.advanceTimersByTime(500)
    expect(sequences).toHaveLength(3)
  })

  it('clé de sécurité simulée : confirmée, annulée, absente', async () => {
    await expect(s().reauth.assertion('ADMIN_SENSITIVE')).resolves.toBe(A)
    await expect(s({ reauth: 'annulee' }).reauth.assertion('ADMIN_SENSITIVE')).rejects.toBeInstanceOf(ReauthCancelled)
    await expect(s({ reauth: 'sansCle' }).reauth.assertion('ADMIN_SENSITIVE')).rejects.toMatchObject({ status: 409, code: 'IAM_NO_SECURITY_KEY' })
  })
})
