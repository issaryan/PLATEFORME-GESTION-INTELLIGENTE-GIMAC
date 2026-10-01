import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Dialog } from '@gimac/design-system'
import { http } from '../http/client'
import { estApiError } from '../http/errors'
import { API_CONNEXION } from '../http/prefixes'
import { deconnecter } from './deconnexion'

// Fermeture de la session après 30 minutes d'inactivité (D-65 §6). La passerelle tient la session serveur
// et la ferme d'elle-même (401 CNX_SESSION_EXPIREE) ; l'interface la suit : minuterie remise à zéro par
// la souris, le clavier, le défilement et le toucher, partagée entre onglets (BroadcastChannel) ;
// avertissement deux minutes avant l'échéance, avec compte à rebours et « Rester connecté » ; à
// l'échéance, déconnexion et retour à la connexion avec le motif « inactivité ».

/** Plafond non négociable de l'inactivité (D-65 §6). */
export const INACTIVITE_MAX_MS = 30 * 60_000
/** Plancher retenu quand la fin d'inactivité (expireA) est très proche. */
export const INACTIVITE_MIN_MS = 60_000
/** Avertissement avant l'échéance (28 min sur 30). */
export const PREAVIS_MS = 2 * 60_000
/** Une activité locale prolonge la session serveur au plus toutes les 5 minutes. */
export const PROLONGATION_ACTIVITE_MS = 5 * 60_000
export const CANAL_SESSION = 'gimac-session'
const EVENEMENTS = ['mousemove', 'mousedown', 'pointerdown', 'keydown', 'scroll', 'wheel', 'touchstart'] as const

type Message = { type: 'activite' | 'prolongation' | 'expiration'; t?: number }

/** Durée d'inactivité : lue sur la fin prévue par la passerelle (expireA), bornée à 1 et 30 minutes. */
export function dureeInactivite(expireA?: string | null, maintenant = Date.now()): number {
  const fin = expireA ? Date.parse(expireA) : Number.NaN
  if (Number.isNaN(fin)) return INACTIVITE_MAX_MS
  return Math.min(INACTIVITE_MAX_MS, Math.max(INACTIVITE_MIN_MS, Math.ceil((fin - maintenant) / 60_000) * 60_000))
}

/** « Rester connecté » et activité locale : POST /api/session/prolongation (204). */
export const prolongerSession = (): Promise<void> => http.post<void>(API_CONNEXION.prolongation)

/** Compte à rebours mm:ss. */
export const minutesSecondes = (ms: number): string => {
  const s = Math.max(0, Math.ceil(ms / 1000))
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

export type SurveillanceInactiviteProps = {
  /** Fin d'inactivité prévue par la passerelle (GET /api/session) ; 30 minutes à défaut. */
  expireA?: string | null
  prolonger?: () => Promise<unknown>
  /** Échéance atteinte ; par défaut, déconnexion et retour à la connexion (motif « inactivité »). */
  expirer?: (minutes: number) => void
}

export function SurveillanceInactivite({ expireA, prolonger = prolongerSession, expirer }: SurveillanceInactiviteProps) {
  const { t } = useTranslation()
  const [restant, setRestant] = useState<number | null>(null)
  const [enCours, setEnCours] = useState(false)
  const reglages = useRef({ duree: INACTIVITE_MAX_MS, preavis: PREAVIS_MS, prolonger, expirer })
  const derniere = useRef(Date.now())
  const dernierAppel = useRef(Date.now())
  const avertissement = useRef(false)
  const fini = useRef(false)
  const canal = useRef<BroadcastChannel | null>(null)

  useEffect(() => {
    const duree = dureeInactivite(expireA)
    reglages.current = { duree, preavis: Math.min(PREAVIS_MS, Math.floor(duree / 2)), prolonger, expirer }
  }, [expireA, prolonger, expirer])

  const terminer = useCallback((diffuser: boolean) => {
    if (fini.current) return
    fini.current = true
    if (diffuser) canal.current?.postMessage({ type: 'expiration' } satisfies Message)
    const { duree, expirer: e } = reglages.current
    const minutes = Math.round(duree / 60_000)
    if (e) e(minutes)
    else void deconnecter('inactivite', minutes)
  }, [])

  const tic = useCallback(() => {
    if (fini.current) return
    const { duree, preavis } = reglages.current
    const ecoule = Date.now() - derniere.current
    if (ecoule >= duree) {
      terminer(true)
    } else if (ecoule >= duree - preavis) {
      avertissement.current = true
      setRestant(duree - ecoule)
    } else if (avertissement.current) {
      avertissement.current = false
      setRestant(null)
    }
  }, [terminer])

  useEffect(() => {
    const bc = typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel(CANAL_SESSION)
    canal.current = bc
    let diffuse = 0
    const activite = () => {
      // Pendant l'avertissement, seule une réponse explicite (« Rester connecté ») prolonge la session.
      if (avertissement.current || fini.current) return
      const n = Date.now()
      derniere.current = n
      if (n - diffuse >= 1000) {
        diffuse = n
        bc?.postMessage({ type: 'activite', t: n } satisfies Message)
      }
      if (n - dernierAppel.current >= PROLONGATION_ACTIVITE_MS) {
        dernierAppel.current = n
        reglages.current.prolonger().catch(() => undefined)
      }
    }
    if (bc) {
      bc.onmessage = (m: MessageEvent<Message>) => {
        const d = m.data
        if (d?.type === 'expiration') {
          terminer(false)
          return
        }
        const n = d?.t ?? Date.now()
        derniere.current = Math.max(derniere.current, n)
        dernierAppel.current = Math.max(dernierAppel.current, n)
        tic()
      }
    }
    EVENEMENTS.forEach((e) => window.addEventListener(e, activite, { passive: true, capture: true }))
    const id = window.setInterval(tic, 1000)
    return () => {
      window.clearInterval(id)
      EVENEMENTS.forEach((e) => window.removeEventListener(e, activite, { capture: true }))
      bc?.close()
      canal.current = null
    }
  }, [tic, terminer])

  const rester = async () => {
    setEnCours(true)
    try {
      await reglages.current.prolonger()
      const n = Date.now()
      derniere.current = n
      dernierAppel.current = n
      avertissement.current = false
      setRestant(null)
      canal.current?.postMessage({ type: 'prolongation', t: n } satisfies Message)
    } catch (e) {
      if (estApiError(e) && e.status === 401) terminer(true)
    } finally {
      setEnCours(false)
    }
  }

  if (restant === null) return null
  return (
    <Dialog
      id="gm-inactivite"
      role="alertdialog"
      title={t('session.inactivite.titre')}
      description={t('session.inactivite.texte')}
      onClose={() => void rester()}
      footer={[
        <Button key="sortir" variant="secondary" onClick={() => terminer(true)}>{t('nav.deconnexion')}</Button>,
        <Button key="rester" variant="primary" loading={enCours} onClick={() => void rester()}>{t('session.inactivite.rester')}</Button>,
      ]}
    >
      <p style={{ margin: 0 }}>
        {t('session.inactivite.restant')}{' '}
        <strong className="gm-mono" role="timer" aria-live="off" data-testid="compte-a-rebours">{minutesSecondes(restant)}</strong>
      </p>
    </Dialog>
  )
}
