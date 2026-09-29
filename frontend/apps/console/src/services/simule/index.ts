import { ReauthCancelled } from '@gimac/socle'
import { creerSimulateur, type Compte, type SessionCompte } from '@gimac/compte'
import type { EntreeJournal } from '../../modele/comptes'
import { ALGORITHMES_OTP, type PolitiqueSecurite, type Profil } from '../../modele/configuration'
import type { ServiceComptes, ServiceConfiguration, ServicePlateforme, ServicesConsole } from '../ports'
import { PERMISSIONS_REALM, PRIVILEGIES, PROFILS_REALM } from './catalogue'
import * as D from './donnees'

// Serveur de substitution de MS-IAM et de la supervision de MS-GATEWAY pour la console : mêmes formes
// de réponse et mêmes codes de refus que les services ; ses « règles » ne sont qu'une imitation.

export type OptionsConsoleSimulee = {
  latenceMs?: number
  /** Acteur connecté (séparation des pouvoirs, octroi privilégié). */
  acteur?: { id: string; nom: string; permissions: string[] }
  /** Clé de sécurité simulée : annulation ou absence de clé. */
  reauth?: 'ok' | 'annulee' | 'sansCle'
  /** Période du flux temps réel simulé, en millisecondes. */
  periodeFluxMs?: number
}

const IDENTIFIANT = /^[a-z][a-z0-9._-]{2,49}$/
const TELEPHONE = /^\+?[0-9 ]{8,20}$/
const COURRIEL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/
const ASSERTION_SIMULEE = btoa('assertion-simulee')

export function servicesConsoleSimules(o: OptionsConsoleSimulee = {}): ServicesConsole {
  const { attendre, refus } = creerSimulateur({ latenceMs: o.latenceMs ?? 300 })
  const acteur = o.acteur ?? { id: 'u-super-admin', nom: 'Serge Mouelle Kombi', permissions: ['IAM_ROLE_PRIVILEGED_GRANT'] }
  const peutPrivilegie = acteur.permissions.includes('IAM_ROLE_PRIVILEGED_GRANT')
  let comptes: Compte[] = D.comptesInitiaux()
  const journal: EntreeJournal[] = D.journalInitial()
  let politique = D.politiqueInitiale()
  let messagerie = D.messagerieInitiale()
  const profils = PROFILS_REALM.map((p) => ({ ...p, permissions: [...p.permissions] }))
  const sessions = new Map<string, SessionCompte[]>()
  let n = 0

  const tracer = (action: string, c: { id: string | null; libelle: string }, motif: string | null, details: string[] = []) =>
    journal.unshift({ id: `j-sim-${++n}`, horodatage: new Date().toISOString(), acteurId: acteur.id, acteurNom: acteur.nom, action, cibleId: c.id, cibleLibelle: c.libelle, motif, details: details.join(', '), correlationId: `sim-${n}` })
  const trouver = (id: string) => {
    const c = comptes.find((x) => x.id === id)
    if (!c) throw refus(404, 'IAM_ACCOUNT_NOT_FOUND', 'Ressource introuvable', { id })
    return c
  }
  const recalculer = (c: Compte) => {
    c.nomAffiche = `${c.prenom ?? ''} ${c.nom ?? ''}`.trim() || c.identifiant
    c.premierAccesEnAttente = !c.profilComplet || c.actionsRequises.length > 0
    return c
  }
  const motif = (m: string) => {
    if (!m?.trim()) throw refus(400, 'IAM_REASON_REQUIRED', 'Motif obligatoire')
  }
  const pasSoiMeme = (id: string) => {
    if (id === acteur.id) throw refus(403, 'IAM_SELF_ADMINISTRATION', "Un administrateur n'agit pas sur son propre compte")
  }
  const reauth = (assertion: string | undefined) => {
    if (!assertion) throw refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification forte exigée')
    if (assertion !== ASSERTION_SIMULEE) throw refus(403, 'IAM_REAUTH_INVALID', 'Réauthentification forte invalide')
  }
  const telephone = (t?: string | null) => {
    if (t && !TELEPHONE.test(t)) throw refus(400, 'IAM_PHONE_INVALID', 'Numéro de téléphone invalide', { telephone: t })
  }
  const verifierProfils = (actuels: string[], demandes: string[]) => {
    const inconnus = demandes.filter((p) => !profils.some((x) => x.code === p))
    if (inconnus.length) throw refus(400, 'IAM_PROFILE_UNKNOWN', 'Profil inconnu du catalogue', { profils: inconnus })
    const changes = [...demandes.filter((p) => !actuels.includes(p)), ...actuels.filter((p) => !demandes.includes(p))]
    if (changes.some((p) => PRIVILEGIES.includes(p)) && !peutPrivilegie) {
      throw refus(403, 'IAM_PRIVILEGED_PROFILE', "L'attribution d'un profil privilégié est réservée au super administrateur")
    }
  }
  const dernierSuperAdmin = (c: Compte) => {
    if (c.actif && c.profils.includes('ROLE_SUPER_ADMIN') && comptes.filter((x) => x.actif && x.profils.includes('ROLE_SUPER_ADMIN')).length <= 1) {
      throw refus(409, 'IAM_LAST_SUPER_ADMIN', 'La plateforme doit garder au moins un super administrateur actif')
    }
  }
  const filtreEtat = (c: Compte, etat?: string) =>
    !etat || (etat === 'ACTIF' ? c.actif : etat === 'INACTIF' ? !c.actif : etat === 'PREMIER_ACCES' ? c.actif && c.premierAccesEnAttente : c.actif && !c.secondFacteur)
  const vueProfil = (p: (typeof profils)[number]): Profil => ({
    code: p.code, description: p.description, permissions: [...p.permissions].sort(), privilegie: PRIVILEGIES.includes(p.code), titulaires: comptes.filter((c) => c.profils.includes(p.code)).length,
  })
  const sessionsDe = (id: string) => {
    if (!sessions.has(id)) {
      const c = trouver(id)
      sessions.set(id, c.actif && !c.premierAccesEnAttente ? [{ id: `s-${id}-1`, adresseIp: '10.20.3.41', debut: new Date(Date.now() - 5_400_000).toISOString(), dernierAcces: new Date(Date.now() - 300_000).toISOString(), applications: ['gateway'] }] : [])
    }
    return sessions.get(id)!
  }

  const comptesSimules: ServiceComptes = {
    lister: (f) =>
      attendre(() => {
        const r = (f.q ?? '').trim().toLowerCase()
        const l = comptes
          .filter((c) => !r || [c.identifiant, c.nomAffiche, c.email].some((v) => (v ?? '').toLowerCase().includes(r)))
          .filter((c) => !f.profil || c.profils.includes(f.profil))
          .filter((c) => filtreEtat(c, f.etat))
          .sort((a, b) => a.identifiant.localeCompare(b.identifiant))
        const debut = (f.page - 1) * f.taille
        return { contenu: l.slice(debut, debut + f.taille), total: l.length }
      }),
    indicateurs: () =>
      attendre(() => {
        const parProfil: Record<string, number> = {}
        comptes.forEach((c) => c.profils.forEach((p) => (parProfil[p] = (parProfil[p] ?? 0) + 1)))
        const il30 = Date.now() - 30 * 86_400_000
        return {
          total: comptes.length, actifs: comptes.filter((c) => c.actif).length, inactifs: comptes.filter((c) => !c.actif).length,
          premierAccesEnAttente: comptes.filter((c) => c.actif && c.premierAccesEnAttente).length,
          sansSecondFacteur: comptes.filter((c) => c.actif && !c.secondFacteur).length,
          privilegies: comptes.filter((c) => c.profils.some((p) => PRIVILEGIES.includes(p))).length,
          creesDepuis30Jours: comptes.filter((c) => c.creeLe && Date.parse(c.creeLe) > il30).length,
          actesDepuis24Heures: journal.filter((j) => Date.parse(j.horodatage) > Date.now() - 86_400_000).length,
          parProfil,
        }
      }),
    journal: (f) =>
      attendre(() => {
        const l = journal.filter((j) => !f.compte || j.cibleId === f.compte)
        const debut = (f.page - 1) * f.taille
        return { contenu: l.slice(debut, debut + f.taille), total: l.length }
      }),
    lire: (id) => attendre(() => trouver(id)),
    sessions: (id) => attendre(() => sessionsDe(id)),
    creer: (x) =>
      attendre(() => {
        const identifiant = x.identifiant.trim().toLowerCase()
        if (!IDENTIFIANT.test(identifiant)) throw refus(400, 'IAM_USERNAME_INVALID', 'Identifiant invalide', { identifiant })
        telephone(x.telephone)
        if (!x.email?.trim()) throw refus(400, 'IAM_EMAIL_REQUIRED', 'Courriel obligatoire')
        if (!x.envoyerInvitation && !x.motDePasseProvisoire) throw refus(400, 'IAM_ONBOARDING_REQUIRED', "Choisir l'invitation par courriel ou un mot de passe provisoire")
        if (!x.profils.length) throw refus(400, 'IAM_PROFILE_UNKNOWN', 'Au moins un profil', { profils: [] })
        verifierProfils([], x.profils)
        if (comptes.some((c) => c.identifiant === identifiant || c.email?.toLowerCase() === x.email.trim().toLowerCase())) {
          throw refus(409, 'IAM_ACCOUNT_CONFLICT', 'Identifiant ou courriel déjà utilisé')
        }
        const c = recalculer({
          id: `u-sim-${++n}`, identifiant, prenom: x.prenom.trim(), nom: x.nom.trim(), nomAffiche: '', email: x.email.trim(), emailVerifie: false, actif: true,
          creeLe: new Date().toISOString(), telephone: x.telephone || null, fonction: x.fonction || null, langue: 'fr', institutionId: x.institutionId || null,
          profilComplet: false, premierAccesEnAttente: true, profils: [...x.profils].sort(), actionsRequises: ['UPDATE_PASSWORD', 'CONFIGURE_TOTP', 'UPDATE_PROFILE', 'VERIFY_EMAIL'], secondFacteur: false,
        })
        comptes = [...comptes, c]
        tracer('CREATION', { id: c.id, libelle: identifiant }, null, c.profils)
        return { compte: c, invitationEnvoyee: x.envoyerInvitation, motDePasseProvisoire: x.motDePasseProvisoire ? 'Kp7#vQ2m!Lx9Tz4w' : null }
      }),
    modifier: (id, f) =>
      attendre(() => {
        const c = trouver(id)
        telephone(f.telephone)
        if (f.email && f.email.toLowerCase() !== c.email?.toLowerCase() && comptes.some((x) => x.id !== id && x.email?.toLowerCase() === f.email!.toLowerCase())) {
          throw refus(409, 'IAM_ACCOUNT_CONFLICT', 'Identifiant ou courriel déjà utilisé')
        }
        Object.entries(f).forEach(([k, v]) => {
          if (v !== undefined && v !== null) (c as Record<string, unknown>)[k] = v
        })
        tracer('FICHE', { id, libelle: c.identifiant }, null)
        return recalculer(c)
      }),
    definirProfils: (id, demandes, m, assertion) =>
      attendre(() => {
        reauth(assertion)
        pasSoiMeme(id)
        motif(m)
        const c = trouver(id)
        verifierProfils(c.profils, demandes)
        if (!demandes.includes('ROLE_SUPER_ADMIN')) dernierSuperAdmin(c)
        c.profils = [...demandes].sort()
        tracer('PROFILS', { id, libelle: c.identifiant }, m, c.profils)
        return c
      }),
    activer: (id, m) =>
      attendre(() => {
        pasSoiMeme(id)
        motif(m)
        const c = trouver(id)
        c.actif = true
        tracer('ACTIVATION', { id, libelle: c.identifiant }, m)
        return c
      }),
    desactiver: (id, m) =>
      attendre(() => {
        pasSoiMeme(id)
        motif(m)
        const c = trouver(id)
        dernierSuperAdmin(c)
        c.actif = false
        tracer('DESACTIVATION', { id, libelle: c.identifiant }, m)
        return c
      }),
    supprimer: (id, m, assertion) =>
      attendre(() => {
        reauth(assertion)
        pasSoiMeme(id)
        motif(m)
        const c = trouver(id)
        dernierSuperAdmin(c)
        comptes = comptes.filter((x) => x.id !== id)
        tracer('SUPPRESSION', { id, libelle: c.identifiant }, m)
      }),
    exigerActions: (id, actions, parCourriel, m) =>
      attendre(() => {
        motif(m)
        if (!actions.length) throw refus(400, 'IAM_ACTIONS_REQUIRED', 'Au moins une action est requise')
        const c = trouver(id)
        c.actionsRequises = [...new Set([...c.actionsRequises, ...actions])]
        tracer('ACTIONS_REQUISES', { id, libelle: c.identifiant }, m, [...actions.map(String)].sort().concat(parCourriel ? ['par courriel'] : []))
        return recalculer(c)
      }),
    reinitialiserSecondFacteur: (id, m) =>
      attendre(() => {
        pasSoiMeme(id)
        motif(m)
        const c = trouver(id)
        c.secondFacteur = false
        sessions.set(id, [])
        tracer('REINITIALISATION_2FA', { id, libelle: c.identifiant }, m)
        return c
      }),
    motDePasseProvisoire: (id, m) =>
      attendre(() => {
        pasSoiMeme(id)
        motif(m)
        const c = trouver(id)
        sessions.set(id, [])
        tracer('MOT_DE_PASSE_PROVISOIRE', { id, libelle: c.identifiant }, m)
        return { motDePasseProvisoire: 'Rt4!zW8q#Nc2Hv6e' }
      }),
    fermerSessions: (id, m) =>
      attendre(() => {
        motif(m)
        const c = trouver(id)
        sessions.set(id, [])
        tracer('FERMETURE_SESSIONS', { id, libelle: c.identifiant }, m)
      }),
  }

  // Planchers de PolitiqueMotDePasse.verifier, en table : [champ, minimum, maximum, règle affichée].
  const BORNES: [keyof PolitiqueSecurite, number, number, string][] = [
    ['longueurMinimale', 12, 128, '12 à 128'], ['historique', 0, 24, '0 à 24'], ['expirationJours', 0, 365, '0 à 365'], ['otpPeriodeSecondes', 30, 60, '30 à 60'],
    ['sessionInactiviteMinutes', 5, 120, '5 à 120'], ['sessionDureeMaxHeures', 1, 24, '1 à 24'], ['forceBruteEchecsMax', 3, 10, '3 à 10'], ['forceBruteAttenteMinutes', 1, 1440, '1 à 1440'],
  ]
  const verifierPolitique = (p: PolitiqueSecurite) => {
    const e: Record<string, string> = Object.fromEntries(
      BORNES.filter(([k, min, max]) => (p[k] as number) < min || (p[k] as number) > max).map(([k, , , regle]) => [k, regle]),
    )
    if (p.majusculesMin + p.minusculesMin + p.chiffresMin + p.speciauxMin > p.longueurMinimale) e.classes = 'somme > longueur'
    if (![6, 8].includes(p.otpChiffres)) e.otpChiffres = '6 ou 8'
    if (!(ALGORITHMES_OTP as readonly string[]).includes(p.otpAlgorithme)) e.otpAlgorithme = 'HmacSHA1, HmacSHA256 ou HmacSHA512'
    if (!p.forceBruteActive) e.forceBruteActive = 'obligatoire'
    if (Object.keys(e).length) throw refus(400, 'IAM_POLICY_INVALID', 'Politique de sécurité refusée', { champs: e })
  }

  const configuration: ServiceConfiguration = {
    politique: () => attendre(() => politique),
    appliquerPolitique: (p, m, assertion) =>
      attendre(() => {
        reauth(assertion)
        motif(m)
        verifierPolitique(p)
        const champs = (Object.keys(p) as (keyof PolitiqueSecurite)[]).filter((k) => p[k] !== politique[k])
        politique = { ...p }
        tracer('CONFIGURATION_POLITIQUE_SECURITE', { id: null, libelle: 'POLITIQUE_SECURITE' }, m, champs)
        return politique
      }),
    messagerie: () => attendre(() => ({ ...messagerie, motDePasse: null })),
    appliquerMessagerie: (x, m) =>
      attendre(() => {
        motif(m)
        if (!x.hote?.trim() || x.port < 1 || x.port > 65535 || !COURRIEL.test(x.expediteur ?? '')) throw refus(400, 'IAM_SMTP_INVALID', "Hôte, port et adresse d'expédition obligatoires")
        if (x.authentification && !x.utilisateur?.trim()) throw refus(400, 'IAM_SMTP_INVALID', "Utilisateur obligatoire avec l'authentification")
        messagerie = { ...x, motDePasse: null, motDePasseDefini: x.motDePasse ? true : messagerie.motDePasseDefini }
        tracer('CONFIGURATION_MESSAGERIE', { id: null, libelle: 'MESSAGERIE' }, m, ['hote', 'port', 'expediteur'])
        return { ...messagerie }
      }),
    testerMessagerie: (d) =>
      attendre(() => {
        if (!COURRIEL.test(d ?? '')) throw refus(400, 'IAM_SMTP_INVALID', 'Adresse de destination invalide')
      }),
    profils: () => attendre(() => profils.map(vueProfil)),
    permissions: () => attendre(() => PERMISSIONS_REALM),
    definirPermissions: (code, permissions, m, assertion) =>
      attendre(() => {
        reauth(assertion)
        motif(m)
        const p = profils.find((x) => x.code === code)
        if (!p) throw refus(404, 'IAM_PROFILE_NOT_FOUND', 'Ressource introuvable', { id: code })
        if (PRIVILEGIES.includes(code) && !peutPrivilegie) throw refus(403, 'IAM_PRIVILEGED_PROFILE', 'La modification d’un profil privilégié est réservée au super administrateur')
        const inconnues = permissions.filter((x) => !PERMISSIONS_REALM.includes(x))
        if (inconnues.length) throw refus(400, 'IAM_PERMISSION_UNKNOWN', 'Permission inconnue', { permissions: inconnues })
        p.permissions = [...permissions]
        tracer(`CONFIGURATION_PROFIL_${code}`, { id: null, libelle: `PROFIL_${code}` }, m, [...permissions].sort())
        return vueProfil(p)
      }),
  }

  let sequence = 0
  const plateforme: ServicePlateforme = {
    services: () => attendre(() => D.instantane()),
    serveurs: () => attendre(D.serveurs),
    alertes: () => attendre(D.alertes),
    configuration: () => attendre(D.configurationPlateforme),
    journalisation: (service, filtre) =>
      attendre(() => {
        const j = D.journalisation(service)
        return { ...j, loggers: j.loggers.filter((l) => !filtre || l.nom.startsWith(filtre)) }
      }),
    modifierJournalisation: (service, d) =>
      attendre(() => {
        if (!d.motif || d.motif.trim().length < 5) throw refus(400, 'HTTP_400', 'Motif obligatoire (5 à 500 caractères)')
        return { service, logger: d.logger, niveau: d.niveau, motif: d.motif, auteur: acteur.nom, horodatage: new Date().toISOString() }
      }),
    // Flux simulé : une impulsion à l'ouverture puis à chaque période, charge légèrement variable.
    flux: (e) => {
      e.onEtat('connexion')
      const emettre = () => e.onImpulsion({ sequence: ++sequence, instantane: D.instantane((sequence % 7) / 7), nombreAlertes: 3 })
      const debut = window.setTimeout(() => {
        e.onEtat('connecte')
        emettre()
      }, o.latenceMs ?? 300)
      const id = window.setInterval(emettre, o.periodeFluxMs ?? 5000)
      return () => {
        window.clearTimeout(debut)
        window.clearInterval(id)
      }
    },
  }

  return {
    comptes: comptesSimules,
    configuration,
    plateforme,
    // Clé de sécurité simulée : confirmation après un délai ; annulation ou absence de clé à la demande.
    reauth: {
      assertion: () =>
        new Promise((res, rej) =>
          window.setTimeout(() => {
            if (o.reauth === 'annulee') rej(new ReauthCancelled())
            else if (o.reauth === 'sansCle') rej(refus(409, 'IAM_NO_SECURITY_KEY', 'Aucune clé de sécurité enrôlée : enrôlez-en une dans « Mon compte »'))
            else res(ASSERTION_SIMULEE)
          }, o.latenceMs ?? 600),
        ),
    },
  }
}
