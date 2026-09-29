import { useEffect, useState, type MouseEvent, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Banner, SideNav, TopBar, type IconName } from '@gimac/design-system'
import logoBleu from '@gimac/design-system/assets/logo/gimac-bleu.png'
import logoBlanc from '@gimac/design-system/assets/logo/gimac-blanc.png'
import type { Couche } from '../http/prefixes'
import type { EtatCanal } from '../live/live'
import { Preferences } from './Preferences'
import { usePreferences } from './usePreferences'

export type EntreeNavigation = { cle: string; libelleKey: string; icone: IconName; chemin: string; permission?: string }
export type GroupeNavigation = { libelleKey?: string; entrees: EntreeNavigation[] }

export type EnveloppeProps = {
  couche: Couche
  navigation: GroupeNavigation[]
  /** Permissions effectives du jeton : une entrée non autorisée est absente du DOM, pas grisée (§6.2). */
  permissions?: string[]
  utilisateur?: string
  institution?: string
  fil?: { label: string; href?: string }[]
  etatCanal?: EtatCanal
  children: ReactNode
}

function heureLocale() {
  return new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit', timeZone: 'Africa/Douala' })
}

// Enveloppe applicative commune (DS-GIMAC-01 §6) : barre supérieure, navigation latérale, bandeau de
// perte du canal poussé, préférences d'affichage. Composée uniquement de composants du catalogue.
export function Enveloppe(p: EnveloppeProps) {
  const { t } = useTranslation()
  const { theme } = usePreferences()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [heure, setHeure] = useState(heureLocale)
  const [hauteur, setHauteur] = useState(() => window.innerHeight - 56)
  const [prefs, setPrefs] = useState(false)
  // Sous 1 280 px (--gm-bp-lg), la navigation devient un tiroir ouvert par le bouton de menu (§6.2, §10.2).
  const [etroit, setEtroit] = useState(() => window.innerWidth < 1280)
  const [navOuverte, setNavOuverte] = useState(false)

  useEffect(() => {
    setNavOuverte(false)
  }, [pathname])

  useEffect(() => {
    const id = window.setInterval(() => setHeure(heureLocale()), 1000)
    const redim = () => {
      setHauteur(window.innerHeight - 56)
      setEtroit(window.innerWidth < 1280)
    }
    window.addEventListener('resize', redim)
    return () => {
      window.clearInterval(id)
      window.removeEventListener('resize', redim)
    }
  }, [])

  const autorisee = (e: EntreeNavigation) => !e.permission || !p.permissions || p.permissions.includes(e.permission)
  const groupes = p.navigation
    .map((g) => ({ label: g.libelleKey ? t(g.libelleKey) : undefined, items: g.entrees.filter(autorisee).map((e) => ({ key: e.cle, label: t(e.libelleKey), icon: e.icone, href: e.chemin })) }))
    .filter((g) => g.items.length > 0)
  const courante = p.navigation.flatMap((g) => g.entrees).filter((e) => pathname.startsWith(e.chemin)).sort((a, b) => b.chemin.length - a.chemin.length)[0]

  // Navigation sans rechargement : les liens du catalogue sont interceptés par le routeur.
  const intercepter = (e: MouseEvent<HTMLElement>) => {
    const a = (e.target as HTMLElement).closest('a')
    const href = a?.getAttribute('href')
    if (a && href && href.startsWith('/') && !e.ctrlKey && !e.metaKey) {
      e.preventDefault()
      void navigate(href)
    }
  }

  return (
    <div className="gm-root" style={{ minHeight: '100vh' }} onClick={intercepter}>
      <TopBar
        layer={p.couche}
        logo={theme === 'dark' ? logoBlanc : logoBleu}
        breadcrumb={p.fil}
        user={p.utilisateur}
        institution={p.institution}
        live={p.etatCanal !== 'lost'}
        clock={heure}
        onLangue={() => setPrefs((v) => !v)}
        onMenu={etroit ? () => setNavOuverte((v) => !v) : undefined}
      />
      {prefs ? (
        <div className="gm-row gm-pad" style={{ justifyContent: 'flex-end', background: 'var(--gm-color-surface)', borderBottom: 'var(--gm-border-1) solid var(--gm-color-border)' }}>
          <Preferences />
        </div>
      ) : null}
      {p.etatCanal === 'lost' ? <Banner level="info">{t('live.perdu')}</Banner> : null}
      <div style={{ display: 'flex', alignItems: 'stretch' }}>
        {etroit ? null : <SideNav current={courante?.cle ?? ''} groups={groupes} height={hauteur} />}
        <main style={{ flex: 1, minWidth: 0, padding: etroit ? 'var(--gm-space-4)' : 'var(--gm-space-5) var(--gm-space-6)' }}>{p.children}</main>
      </div>
      {etroit && navOuverte ? (
        // Tiroir de navigation : voile du catalogue (gm-overlay) et SideNav, fermeture par le voile ou Échap.
        <div className="gm-overlay" style={{ justifyContent: 'flex-start', alignItems: 'stretch', top: 'var(--gm-size-topbar)', padding: 0 }} onClick={(e) => e.target === e.currentTarget && setNavOuverte(false)} onKeyDown={(e) => e.key === 'Escape' && setNavOuverte(false)}>
          <SideNav current={courante?.cle ?? ''} groups={groupes} height={hauteur} />
        </div>
      ) : null}
    </div>
  )
}
