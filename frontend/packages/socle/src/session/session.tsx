import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Banner, Button, Skeleton } from '@gimac/design-system'
import { http } from '../http/client'
import { estApiError } from '../http/errors'
import { CONNEXION, SESSION } from '../http/prefixes'

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
}

const Contexte = createContext<Session | null>(null)

export function SessionProvider({ session, children }: { session: Session; children: ReactNode }) {
  return <Contexte.Provider value={session}>{children}</Contexte.Provider>
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

export const chargerSession = (): Promise<Session> => http.get<Session>(SESSION)

type EtatSession = { etat: 'chargement' } | { etat: 'ok'; session: Session } | { etat: 'anonyme' } | { etat: 'erreur'; correlationId?: string }

/**
 * Garde de session : charge la session, renvoie vers la connexion sans session, refuse l'accès
 * tant que le jeton n'est pas issu d'une authentification à deux facteurs, puis fournit la session.
 */
export function GardeSession({ charger = chargerSession, connexion = CONNEXION, children }: {
  charger?: () => Promise<Session>
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

  if (etat.etat === 'chargement') return <Skeleton height={32} />
  const reconnecter = (
    <Button variant="primary" icon="lock" onClick={() => window.location.assign(connexion)}>
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
  if (!etat.session.doubleFacteur) {
    return (
      <div className="gm-col gm-pad">
        <Banner level="warn">{t('session.doubleFacteurRequis')}</Banner>
        <div>{reconnecter}</div>
      </div>
    )
  }
  return <SessionProvider session={etat.session}>{children(etat.session)}</SessionProvider>
}
