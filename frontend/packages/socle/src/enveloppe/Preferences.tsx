import { Select, Switch } from '@gimac/design-system'
import { useTranslation } from 'react-i18next'
import { LANGUES, type Langue } from '../i18n/i18n'
import { usePreferences } from './usePreferences'

const NOMS: Record<Langue, string> = { fr: 'Français', en: 'English', es: 'Español' }

// Préférences d'affichage (DS-GIMAC-01 TRA-08, D-66) : menu surgissant du globe de la barre supérieure,
// titré « Préférences », en deux rubriques séparées par un filet : la langue (FR, EN, ES : langues de la
// zone CEMAC) et le thème (interrupteur carré du catalogue). Composé du catalogue.
export function Preferences() {
  const { t } = useTranslation()
  const p = usePreferences()
  return (
    <div className="gm-prefs">
      <h2 className="gm-prefs-titre">{t('shell.preferences')}</h2>
      <section className="gm-prefs-rubrique" aria-label={t('shell.langue')}>
        <Select
          name="langue"
          label={t('shell.langue')}
          value={p.langue}
          options={LANGUES.map((l) => ({ value: l, label: NOMS[l] }))}
          onChange={(v: string) => p.changerLangue(v as Langue)}
        />
      </section>
      <section className="gm-prefs-rubrique" aria-label={t('shell.theme')}>
        <p className="gm-prefs-libelle">{t('shell.theme')}</p>
        <Switch label={t('shell.theme.sombre')} checked={p.theme === 'dark'} onChange={(v: boolean) => p.changerTheme(v ? 'dark' : 'light')} />
      </section>
    </div>
  )
}
