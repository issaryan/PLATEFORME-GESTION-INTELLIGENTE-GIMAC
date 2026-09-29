import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useTheme } from '@gimac/design-system'
import { LANGUES, type Langue } from '../i18n/i18n'

// Préférences d'affichage (DS-GIMAC-01 §6.1, TRA-08) : thème et langue, seules données admises dans le
// stockage local (clés gm.theme et i18nextLng), en attendant leur mémorisation dans le profil (IAM-12).
export const CLE_THEME = 'gm.theme'
export const CLE_LANGUE = 'i18nextLng'

export function lirePreference(cle: string): string | null {
  try {
    return localStorage.getItem(cle)
  } catch {
    return null
  }
}
function ecrirePreference(cle: string, valeur: string) {
  try {
    localStorage.setItem(cle, valeur)
  } catch {
    /* navigation privée : préférence non mémorisée, sans conséquence */
  }
}

export function langueInitiale(): Langue {
  const l = lirePreference(CLE_LANGUE) ?? navigator.language.slice(0, 2)
  return (LANGUES as readonly string[]).includes(l) ? (l as Langue) : 'fr'
}

/** Thème et langue courants, avec leurs bascules ; synchronise lang sur la racine du document. */
export function usePreferences() {
  const { theme, setTheme } = useTheme()
  const { i18n } = useTranslation()
  const langue = (LANGUES as readonly string[]).includes(i18n.language) ? (i18n.language as Langue) : 'fr'
  useEffect(() => {
    document.documentElement.lang = langue
  }, [langue])
  return {
    theme,
    langue,
    changerTheme: (t: 'light' | 'dark') => {
      setTheme(t)
      ecrirePreference(CLE_THEME, t)
    },
    changerLangue: (l: Langue) => {
      void i18n.changeLanguage(l)
      ecrirePreference(CLE_LANGUE, l)
    },
  }
}
