import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

export type Theme = 'light' | 'dark'
export type Density = 'normal' | 'compact'

export interface ThemeContextValue {
  theme: Theme
  density: Density
  setTheme: (t: Theme) => void
  setDensity: (d: Density) => void
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

export interface ThemeProviderProps {
  /** Préférence du profil (IAM-12) ; à défaut, la préférence système n'est qu'une valeur initiale. */
  theme?: Theme
  density?: Density
  /** Appelé quand l'utilisateur change de préférence, pour la mémoriser dans son profil. */
  onChange?: (prefs: { theme: Theme; density: Density }) => void
  /** Élément qui porte data-theme et data-density ; par défaut la racine du document. */
  target?: HTMLElement
  children: ReactNode
}

function themeSysteme(): Theme {
  return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

// Fournisseur de thème (DS-GIMAC-01 §3.2 et §12.2) : pose data-theme et data-density.
// Aucune préférence n'est stockée ici : la couche de présentation ne détient aucun état durable.
export function ThemeProvider({ theme: themeInitial, density: densiteInitiale = 'normal', onChange, target, children }: ThemeProviderProps) {
  const [theme, setThemeState] = useState<Theme>(() => themeInitial ?? themeSysteme())
  const [density, setDensityState] = useState<Density>(densiteInitiale)

  useEffect(() => {
    if (themeInitial) setThemeState(themeInitial)
  }, [themeInitial])
  useEffect(() => {
    setDensityState(densiteInitiale)
  }, [densiteInitiale])

  useEffect(() => {
    const el = target ?? document.documentElement
    el.setAttribute('data-theme', theme)
    el.setAttribute('data-density', density)
  }, [theme, density, target])

  const value = useMemo<ThemeContextValue>(
    () => ({
      theme,
      density,
      setTheme: (t) => {
        setThemeState(t)
        onChange?.({ theme: t, density })
      },
      setDensity: (d) => {
        setDensityState(d)
        onChange?.({ theme, density: d })
      },
    }),
    [theme, density, onChange],
  )
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme doit être employé sous ThemeProvider')
  return ctx
}
