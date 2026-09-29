import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { LangueProvider, type LangueDS } from '@gimac/design-system'

/** Aligne les libellés internes des composants du catalogue sur la langue choisie (FR, EN, ES). */
export function LangueComposants({ children }: { children: ReactNode }) {
  const { i18n } = useTranslation()
  const l = i18n.language.slice(0, 2)
  const langue: LangueDS = l === 'en' || l === 'es' ? l : 'fr'
  return <LangueProvider langue={langue}>{children}</LangueProvider>
}
