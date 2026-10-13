import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Banner, Button, Skeleton } from '@gimac/design-system'
import { http } from '../http/client'
import { estApiError } from '../http/errors'
import { SESSION } from '../http/prefixes'
import { adresseConnexion, navigation, retourCourant } from './deconnexion'
import { SurveillanceInactivite } from './inactivite'

// Session de l'utilisateur (D-58, D-61) : la matrice des rôles (permissions) du jeton obtenu à la
// connexion à deux facteurs. L'interface n'affiche que les tableaux de bord et les actions que ces
// permissions ouvrent ; la passerelle (OPA) et chaque service refont le contrôle à chaque appel.

export type Session = {
  sujet: string
  nom: string
  identifiant: string
  roles: string[]
  permissions: string[]
  institutionId: string
  acr: string
  doubleFacteur: boolean
  /** Fin d'inactivité prévue par la passerelle (ISO 8601, D-65 §6). */
  expireA?: string | null
  /** Session restreinte du premier accès (acr 1, D-65) : seul l'assistant de premier accès est servi. */
  restreinte?: boolean
}

const Contexte = createContext<Session | null>(null)

/**
 * Permissions qui en valent d'autres, miroir de `permissions_equivalentes` de la politique OPA (D-92, D-106 §12) :
 * REF_METIER_ADMIN vaut chacune des permissions du référentiel des opérations. Sans ce miroir, l'interface cachait à
 * l'administration des référentiels métier des gestes que la passerelle et le service lui accordent (migration…).
 */
export const PERMISSIONS_EQUIVALENTES: Readonly<Record<string, readonly string[]>> = {
  REF_METIER_ADMIN: ['HOM_REFERENTIEL_READ', 'HOM_REFERENTIEL_EDIT', 'HOM_REFERENTIEL_PUBLISH', 'HOM_REFERENTIEL_MIGRATE'],
}

/** Permissions du jeton complétées de celles qu'elles valent (sans doublon, ordre du jeton d'abord). */
export function permissionsEffectives(permissions: readonly string[]): string[] {
  const r = [...permissions]
  for (const p of permissions) for (const e of PERMISSIONS_EQUIVALENTES[p] ?? []) if (!r.includes(e)) r.push(e)
  return r
}

export function SessionProvider({ session, children }: { session: Session; children: ReactNode }) {
  const valeur = useMemo(() => ({ ...session, permissions: permissionsEffectives(session.permissions ?? []) }), [session])
  return <Contexte.Provider value={valeur}>{children}</Contexte.Provider>
}

/** Session courante, ou null hors de SessionProvider (tests d'un composant isolé). */
export function useSession(): Session | null {
  return useContext(Contexte)
}

/** Permissions effectives ; undefined hors session, ce qui laisse les composants du catalogue tout afficher. */
export function usePermissions(): string[] | undefined {
  return useContext(Contexte)?.permissions
}

/** Vrai si la session porte l'une des permissions (aucune exigée : vrai). */
export function aPermission(permissions: string[] | undefined, ...exigees: string[]): boolean {
  if (exigees.length === 0) return true
  return !!permissions && exigees.some((p) => permissions.includes(p))
}

/**
 * Session ouverte : fournit la session et surveille l'inactivité (fermeture à 30 minutes, avertissement
 * à 28, D-65 §6). `surveiller` à faux pour un composant isolé.
 */
export function SessionActive({ session, surveiller = true, prolonger, children }: {
  session: Session
  surveiller?: boolean
  /** Prolongation de la session serveur ; sans passerelle (services simulés), une fonction sans effet. */
  prolonger?: () => Promise<unknown>
  children: ReactNode
}) {
  return (
    <SessionProvider session={session}>
      {surveiller ? <SurveillanceInactivite expireA={session.expireA} prolonger={prolonger} /> : null}
      {children}
    </SessionProvider>
  )
}

export const chargerSession = (): Promise<Session> => http.get<Session>(SESSION)

type EtatSession = { etat: 'chargement' } | { etat: 'ok'; session: Session } | { etat: 'anonyme' } | { etat: 'erreur'; correlationId?: string }

/**
 * Garde de session : charge la session, renvoie vers l'écran de connexion de la plateforme sans session,
 * refuse l'accès tant que le jeton n'est pas issu d'une authentification à deux facteurs (hors session
 * restreinte du premier accès, que l'assistant de premier accès prend en charge), puis fournit la session.
 */
export function GardeSession({ charger = chargerSession, connexion, children }: {
  charger?: () => Promise<Session>
  /** Adresse de l'écran de connexion ; par défaut /connexion avec la page courante en retour. */
  connexion?: string
  children: (session: Session) => ReactNode
}) {
  const { t } = useTranslation()
  const [etat, setEtat] = useState<EtatSession>({ etat: 'chargement' })
  useEffect(() => {
    let actif = true
    charger()
      .then((session) => actif && setEtat({ etat: 'ok', session }))
      .catch((e: unknown) => {
        if (!actif) return
        if (estApiError(e) && e.status === 401) setEtat({ etat: 'anonyme' })
        else setEtat({ etat: 'erreur', correlationId: estApiError(e) ? e.correlationId : undefined })
      })
    return () => {
      actif = false
    }
  }, [charger])

  useEffect(() => {
    if (etat.etat === 'anonyme') navigation.vers(connexion ?? adresseConnexion({ retour: retourCourant() }))
  }, [etat.etat, connexion])

  if (etat.etat === 'chargement') return <Skeleton height={32} />
  const reconnecter = (
    <Button variant="primary" icon="lock" onClick={() => navigation.vers(connexion ?? adresseConnexion({ retour: retourCourant() }))}>
      {t('session.seConnecter')}
    </Button>
  )
  if (etat.etat === 'anonyme') {
    return (
      <div className="gm-col gm-pad">
        <Banner level="info">{t('session.expiree')}</Banner>
        <div>{reconnecter}</div>
      </div>
    )
  }
  if (etat.etat === 'erreur') {
    return (
      <div className="gm-col gm-pad">
        <Banner level="crit">{t('session.erreur') + (etat.correlationId ? ` (${etat.correlationId})` : '')}</Banner>
      </div>
    )
  }
  if (!etat.session.doubleFacteur && !etat.session.restreinte) {
    return (
      <div className="gm-col gm-pad">
        <Banner level="warn">{t('session.doubleFacteurRequis')}</Banner>
        <div>{reconnecter}</div>
      </div>
    )
  }
  return (
    <SessionActive session={etat.session}>
      <SessionUniqueParNavigateur sujet={etat.session.sujet} />
      {children(etat.session)}
    </SessionActive>
  )
}

/** Clé partagée par les onglets d'une même origine : compte de la session ouverte dans ce navigateur. */
export const CLE_COMPTE_ONGLETS = 'gm.session.compte'

/**
 * Une session par navigateur : le témoin de session de la passerelle est commun à tous les onglets d'une
 * origine. Quand un autre onglet ouvre la session d'un autre compte (ou ferme la session), cet onglet ne doit
 * plus agir ni afficher au nom de l'ancien compte : il se recharge et prend la session en cours.
 */
function SessionUniqueParNavigateur({ sujet }: { sujet: string }) {
  useEffect(() => {
    try {
      localStorage.setItem(CLE_COMPTE_ONGLETS, sujet)
    } catch {
      // Stockage indisponible (navigation privée stricte) : pas de surveillance entre onglets.
    }
    const changement = (e: StorageEvent) => {
      if (e.key === CLE_COMPTE_ONGLETS && e.newValue !== sujet) navigation.recharger()
    }
    window.addEventListener('storage', changement)
    return () => window.removeEventListener('storage', changement)
  }, [sujet])
  return null
}
