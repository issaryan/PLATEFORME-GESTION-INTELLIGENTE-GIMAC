import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError, EtatsRequete, estApiError } from '@gimac/socle'

// Compositions communes des écrans DES-08 et DES-09 : les quatre états d'une donnée, traduits.

type Requete<R> = { isPending: boolean; isError: boolean; error: ApiError | null; data: R | undefined; refetch: () => unknown }

/** Chargement, vide (avec sa cause), erreur (identifiant de corrélation et réessai), nominal. */
export function Donnees<R>({ requete, vide, estVide, children }: { requete: Requete<R>; vide?: string; estVide?: (r: R) => boolean; children: (r: R) => ReactNode }) {
  const { t } = useTranslation()
  return (
    <EtatsRequete etat={requete} isEmpty={estVide} emptyTitle={vide ?? t('commun.vide')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
      {children}
    </EtatsRequete>
  )
}

/** État d'une requête de liste pour ResourceList et DataTable. */
export function etatListe<R>(q: Requete<R>, lignes: unknown[] | undefined): { state: 'loading' | 'error' | 'empty' | 'ok'; error?: { message: string; correlationId?: string } } {
  if (q.isPending) return { state: 'loading' }
  if (q.isError) return { state: 'error', error: { message: q.error?.message ?? '', correlationId: q.error?.correlationId || undefined } }
  return { state: lignes && lignes.length ? 'ok' : 'empty' }
}

/** Erreur du service pour le formulaire : message traduit par son code, identifiant de corrélation conservé. */
export function useErreurService() {
  const { t } = useTranslation()
  return (e: unknown): ApiError | undefined => {
    if (!e) return undefined
    if (estApiError(e)) {
      const msg = t(`erreur.${e.code}`, { defaultValue: e.message || t('commun.erreur.titre') })
      return new ApiError(e.status, e.code, msg, e.correlationId, e.details)
    }
    return undefined
  }
}
