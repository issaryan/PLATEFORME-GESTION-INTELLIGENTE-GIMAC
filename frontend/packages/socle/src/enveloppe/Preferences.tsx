import { Select, Switch } from '@gimac/design-system'
import { useTranslation } from 'react-i18next'
import { LANGUES, type Langue } from '../i18n/i18n'
import { usePreferences } from './usePreferences'

const NOMS: Record<Langue, string> = { fr: 'Français', en: 'English', es: 'Español' }

// Choix de la langue (FR, EN, ES : langues de la zone CEMAC) et du thème, composé du catalogue.
export function Preferences() {
  const { t } = useTranslation()
  const p = usePreferences()
  return (
    <div className="gm-row" style={{ gap: 'var(--gm-space-4)', alignItems: 'flex-end' }}>
      <div style={{ width: 160 }}>
        <Select
          name="langue"
          label={t('shell.langue')}
          value={p.langue}
          options={LANGUES.map((l) => ({ value: l, label: NOMS[l] }))}
          onChange={(v: string) => p.changerLangue(v as Langue)}
        />
      </div>
      <div style={{ paddingBottom: 'var(--gm-space-2)' }}>
        <Switch label={t('shell.theme.sombre')} checked={p.theme === 'dark'} onChange={(v: boolean) => p.changerTheme(v ? 'dark' : 'light')} />
      </div>
    </div>
  )
}
