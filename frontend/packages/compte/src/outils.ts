import { useTranslation } from 'react-i18next'
import { estApiError, refusReauth } from '@gimac/socle'

// Outils communs des écrans de compte : messages d'erreur traduits par leur code, dates locales.

export type MessageErreur = { texte: string; correlationId?: string; code?: string }

/** Message d'une erreur : refus de réauthentification, code du service traduit, sinon message brut. */
export function useMessageErreur() {
  const { t } = useTranslation()
  return (e: unknown): MessageErreur => {
    const refus = refusReauth(e)
    if (refus) return { texte: t(`reauth.${refus}`), code: refus, correlationId: estApiError(e) ? e.correlationId || undefined : undefined }
    if (estApiError(e)) return { texte: t(`erreur.${e.code}`, { defaultValue: e.message || t('commun.erreur.titre') }), correlationId: e.correlationId || undefined, code: e.code }
    return { texte: e instanceof Error ? e.message : String(e) }
  }
}

/** Date et heure locales (fuseau de Douala), ou tiret si absente. */
export function dateHeure(iso: string | null | undefined, langue: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString(langue, { dateStyle: 'short', timeStyle: 'short', timeZone: 'Africa/Douala' })
}
