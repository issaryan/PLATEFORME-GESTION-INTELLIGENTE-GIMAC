import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, InlineAlert } from '@gimac/design-system'
import logoCouleur from '@gimac/design-system/assets/logo/gimac-couleur.png'
import logoBlanc from '@gimac/design-system/assets/logo/gimac-blanc.png'
import type { Couche } from '../http/prefixes'
import { LANGUES } from '../i18n/i18n'
import { usePreferences } from '../enveloppe/usePreferences'
import './connexion.css'

export type PageConnexionProps = {
  couche: Couche | 'console'
  /** Délégation d'authentification de la passerelle (modèle BFF). */
  adresseConnexion?: string
  /** Erreur de délégation renvoyée par la passerelle, avec son identifiant de corrélation. */
  erreur?: { message: string; correlationId?: string }
}

/** Motif de trame (§5.6) : losanges et chevrons sur la grille de 8 px, période de 32 px, en tuile répétée. */
function Trame({ id }: { id: string }) {
  return (
    <svg className="gm-tuile-trame" aria-hidden="true">
      <defs>
        <pattern id={id} width="32" height="32" patternUnits="userSpaceOnUse">
          <path d="M16 0 L32 16 L16 32 L0 16 Z" />
          <path d="M0 8 L8 0 M24 32 L32 24 M0 24 L8 32 M24 0 L32 8" />
        </pattern>
      </defs>
      <rect width="100%" height="100%" fill={`url(#${id})`} />
    </svg>
  )
}

const format = (d: Date, fuseau: string) =>
  d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit', timeZone: fuseau })

// Connexion (TRA-01) : une mosaïque tissée qui prolonge la couverture ; la connexion est l'un de ses
// blocs. Aucune saisie d'identifiant ni de mot de passe : la passerelle délègue au fournisseur d'identité
// et pose le témoin de session HttpOnly. Langue et thème sont des tuiles de la mosaïque.
export function PageConnexion({ couche, adresseConnexion = '/oauth2/authorization/oidc', erreur }: PageConnexionProps) {
  const { t, i18n } = useTranslation()
  const p = usePreferences()
  const [maintenant, setMaintenant] = useState(() => new Date())
  useEffect(() => {
    const id = window.setInterval(() => setMaintenant(new Date()), 1000)
    return () => window.clearInterval(id)
  }, [])
  const date = maintenant.toLocaleDateString(i18n.language, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'Africa/Douala' })

  return (
    <div className="gm-mosaique gm-root">
      <div className="gm-tuile gm-tuile-action" aria-hidden="true" />
      <div className="gm-tuile gm-tuile-logo" aria-hidden="true" />
      <div className="gm-tuile gm-tuile-coche" aria-hidden="true" />
      <div className="gm-tuile gm-tuile-terre" aria-hidden="true"><Trame id="trame-terre" /></div>
      <div className="gm-tuile gm-tuile-relief" aria-hidden="true"><Trame id="trame-relief" /></div>

      <div className="gm-tuile gm-tuile-bande">
        <span className="gm-type-micro">{t(`connexion.couche.${couche}`)}</span>
        <span className="gm-type-micro">{t('connexion.classification')}</span>
      </div>

      <main className="gm-tuile gm-tuile-acces" aria-labelledby="gm-acces-titre">
        <img src={p.theme === 'dark' ? logoBlanc : logoCouleur} alt="GIMAC" />
        <h1 id="gm-acces-titre" className="gm-acces-nom">
          GIMAC <span>DES</span>
        </h1>
        <p className="gm-acces-texte">{t('connexion.explication')}</p>
        {erreur ? (
          <InlineAlert level="crit" title={t('connexion.erreur')} correlationId={erreur.correlationId}>
            {erreur.message}
          </InlineAlert>
        ) : null}
        <Button variant="primary" size="lg" icon="key-round" onClick={() => window.location.assign(adresseConnexion)}>
          {t('connexion.action')}
        </Button>
        <p className="gm-field-help" style={{ margin: 0 }}>{t('connexion.cle')}</p>
      </main>

      <section className="gm-tuile gm-tuile-instrument" aria-label={t('connexion.heure')}>
        <span className="gm-type-micro">{date}</span>
        <time className="gm-instrument-heure" dateTime={maintenant.toISOString()}>{format(maintenant, 'Africa/Douala')}</time>
        <span className="gm-instrument-utc">UTC {format(maintenant, 'UTC')}</span>
      </section>

      <section className="gm-tuile gm-tuile-choix gm-tuile-langue" aria-label={t('shell.langue')}>
        <span className="gm-type-micro">{t('shell.langue')}</span>
        <div className="gm-choix" role="group" aria-label={t('shell.langue')}>
          {LANGUES.map((l) => (
            <button key={l} type="button" lang={l} aria-pressed={p.langue === l} onClick={() => p.changerLangue(l)}>
              {l.toUpperCase()}
            </button>
          ))}
        </div>
      </section>

      <section className="gm-tuile gm-tuile-choix gm-tuile-theme" aria-label={t('shell.theme')}>
        <span className="gm-type-micro">{t('shell.theme')}</span>
        <div className="gm-choix" role="group" aria-label={t('shell.theme')}>
          <button type="button" aria-pressed={p.theme === 'light'} onClick={() => p.changerTheme('light')}>{t('shell.theme.clair')}</button>
          <button type="button" aria-pressed={p.theme === 'dark'} onClick={() => p.changerTheme('dark')}>{t('shell.theme.sombre.court')}</button>
        </div>
      </section>
    </div>
  )
}
