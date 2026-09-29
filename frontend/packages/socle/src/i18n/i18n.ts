import i18next, { type Resource } from 'i18next'
import { initReactI18next } from 'react-i18next'
import fr from './fr.json'
import en from './en.json'
import es from './es.json'

// Catalogues des trois langues de la zone CEMAC (français, anglais, espagnol) ; le socle porte les
// clés communes, chaque lot ajoute les siennes. Bascule sans rechargement ; les données métier non
// traduisibles restent telles quelles ; les documents générés restent en français.
export const LANGUES = ['fr', 'en', 'es'] as const
export type Langue = (typeof LANGUES)[number]
export type Catalogue = Record<Langue, Record<string, string>>

export function initI18n(catalogues: Catalogue[] = [], lng: Langue = 'fr') {
  const fusion = (l: Langue, base: Record<string, string>) => Object.assign({}, base, ...catalogues.map((c) => c[l]))
  const resources: Resource = {
    fr: { translation: fusion('fr', fr) },
    en: { translation: fusion('en', en) },
    es: { translation: fusion('es', es) },
  }
  void i18next.use(initReactI18next).init({
    resources,
    lng,
    fallbackLng: 'fr',
    keySeparator: false,
    interpolation: { escapeValue: false },
    returnNull: false,
  })
  return i18next
}
