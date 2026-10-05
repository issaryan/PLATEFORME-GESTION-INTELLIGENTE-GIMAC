import { useEffect, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Banner, Button } from '@gimac/design-system'
import type { Couche } from '../http/prefixes'
import { adresseApplication, applicationDe } from './aiguillage'
import { navigation } from './deconnexion'
import { lireSessionDemo } from './demo'
import type { Session } from './session'

/** Délai entre le message d'aiguillage et le changement d'application ; réglable dans les tests. */
export const reglageAiguillage = { delaiMs: 1500 }

/** Message bref puis départ vers l'application de la session (avec un bouton pour partir aussitôt). */
export function MessageAiguillage({ depuis, vers }: { depuis: Couche; vers: Couche }) {
  const { t } = useTranslation()
  const adresse = adresseApplication(vers, undefined, lireSessionDemo())
  useEffect(() => {
    const id = window.setTimeout(() => navigation.vers(adresse), reglageAiguillage.delaiMs)
    return () => window.clearTimeout(id)
  }, [adresse])
  return (
    <div className="gm-col gm-pad" role="status">
      <Banner level="info">{t(`aiguillage.${depuis}.${vers}`, { defaultValue: t(`aiguillage.vers.${vers}`) })}</Banner>
      <div>
        <Button variant="primary" icon="arrow-right" onClick={() => navigation.vers(adresse)}>{t(`aiguillage.ouvrir.${vers}`)}</Button>
      </div>
    </div>
  )
}

/**
 * Garde d'aiguillage (D-79) : à l'ouverture d'une application avec une session existante, un compte qui
 * n'y a pas sa place (compte d'institution hors du portail, compte interne sur le portail, compte interne
 * sans permission de console sur la console) est conduit vers son application, après un message bref.
 */
export function Aiguillage({ session, couche, children }: { session: Session; couche: Couche; children: ReactNode }) {
  const cible = applicationDe(session, couche)
  if (cible === couche) return <>{children}</>
  return <MessageAiguillage depuis={couche} vers={cible} />
}
