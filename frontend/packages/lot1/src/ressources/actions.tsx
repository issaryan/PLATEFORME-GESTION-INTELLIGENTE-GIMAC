import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { useTranslation } from 'react-i18next'
import { ConfirmDialog, SignDialog, Toast } from '@gimac/design-system'
import { estApiError, ReauthCancelled, type ActionContext, type ResourceActionLot1 } from '@gimac/socle'
import { useServices } from '../services/ServicesProvider'
import type { Services } from '../services/ports'

// Exécuteur des actions des descripteurs : confirmation (ConfirmDialog) ou signature (SignDialog avec
// réauthentification FIDO2), motif, appel du service injecté, invalidation et retour (Toast).
// Toute la présentation vient du design system ; aucune règle n'est évaluée ici.

type Demande = { action: ResourceActionLot1<unknown, Services>; row: unknown; titre: string; empreinte?: string; sujet?: string }
type Pilotes = { openForm?: (mode: 'create' | 'edit', row: unknown) => void; openTab?: (key: string) => void }

type Executeur = { lancer: <T>(action: ResourceActionLot1<T, Services>, row: T, titre: string, sujet?: string, pilotes?: Pilotes) => void }
const Contexte = createContext<Executeur | null>(null)

export function useExecuteurActions(): Executeur {
  const e = useContext(Contexte)
  if (!e) throw new Error('useExecuteurActions doit être employé sous ExecuteurActions')
  return e
}

export function ExecuteurActions({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  const services = useServices()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [demande, setDemande] = useState<Demande | null>(null)
  const [etatSignature, setEtatSignature] = useState<'idle' | 'waiting' | 'done' | 'error'>('idle')
  const [erreur, setErreur] = useState<string | undefined>()
  const [toast, setToast] = useState<string | null>(null)
  const pilotes = useRef<Pilotes>({})
  useEffect(() => {
    if (!toast) return
    const id = window.setTimeout(() => setToast(null), 6000)
    return () => window.clearTimeout(id)
  }, [toast])

  const executer = useCallback(
    async (d: Demande, motif?: string, assertion?: string) => {
      const ctx: ActionContext<unknown, Services> = {
        row: d.row,
        selection: [d.row],
        motif,
        assertion,
        services,
        navigate: (to) => void navigate(to),
        openForm: (mode) => pilotes.current.openForm?.(mode, d.row),
        openTab: (key) => pilotes.current.openTab?.(key),
        invalidate: () => void qc.invalidateQueries({ queryKey: [d.sujet ?? 'homologation'] }),
      }
      await d.action.run?.(ctx)
      void qc.invalidateQueries({ queryKey: [d.sujet ?? 'homologation'] })
    },
    [services, navigate, qc],
  )

  const fermer = () => {
    setDemande(null)
    setErreur(undefined)
    setEtatSignature('idle')
  }
  const message = (e: unknown) => (estApiError(e) ? (t(`erreur.${e.code}`, { defaultValue: e.message }) + (e.correlationId ? ` (${e.correlationId})` : '')) : String(e))

  const executeur = useMemo<Executeur>(
    () => ({
      lancer: (action, row, titre, sujet, p) => {
        pilotes.current = p ?? {}
        const d: Demande = { action: action as ResourceActionLot1<unknown, Services>, row, titre, sujet, empreinte: action.sign?.empreinte(row) }
        if (action.confirm === 'confirm' || action.confirm === 'sign') {
          setDemande(d)
          return
        }
        executer(d).catch((e: unknown) => setToast(message(e)))
      },
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [executer, t],
  )

  const confirmer = async (motif?: string) => {
    if (!demande) return
    try {
      setErreur(undefined)
      if (demande.action.confirm === 'sign' && demande.action.sign) {
        setEtatSignature('waiting')
        const assertion = await services.reauth.assertion(demande.action.sign.purpose)
        await executer(demande, motif, assertion)
        setEtatSignature('done')
      } else {
        await executer(demande, motif)
        fermer()
      }
      setToast(t('action.effectuee', { action: t(demande.action.label) }))
    } catch (e) {
      setEtatSignature('error')
      setErreur(e instanceof ReauthCancelled ? t('sign.annule') : message(e))
    }
  }

  return (
    <Contexte.Provider value={executeur}>
      {children}
      {demande && demande.action.confirm === 'sign' ? (
        <SignDialog
          open
          title={t(demande.action.label)}
          action={t(demande.action.label)}
          hash={demande.empreinte ?? ''}
          state={etatSignature}
          object={[{ key: t('action.objet'), value: demande.titre }]}
          extra={demande.action.consequences?.(demande.row)}
          reason={demande.action.reason}
          error={erreur}
          onSign={(motif: string) => void confirmer(motif)}
          onClose={fermer}
          onDone={fermer}
        />
      ) : demande ? (
        <ConfirmDialog
          open
          title={t(demande.action.label)}
          confirmLabel={t(demande.action.label)}
          danger={demande.action.variant === 'danger'}
          reason={demande.action.reason}
          object={[{ key: t('action.objet'), value: demande.titre }]}
          description={erreur}
          extra={demande.action.consequences?.(demande.row)}
          onConfirm={(motif: string) => void confirmer(motif)}
          onClose={fermer}
        />
      ) : null}
      {toast ? (
        <div style={{ position: 'fixed', right: 'var(--gm-space-5)', bottom: 'var(--gm-space-5)', zIndex: 60 }} onClick={() => setToast(null)}>
          <Toast>{toast}</Toast>
        </div>
      ) : null}
    </Contexte.Provider>
  )
}
