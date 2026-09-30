import { useEffect, useRef, useState, type MouseEvent, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Banner, Footer, SideNav, TopBar, type IconName } from '@gimac/design-system'
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
  /** Pied de page GIMAC (§6.3) : version de la plateforme, documentation, dernière corrélation. */
  version?: string
  documentation?: string
  correlationId?: string
  /**
   * Espace personnel (D-62) : racine de la couche (/des, /portail, /console). Ajoute le groupe
   * « Mon espace » (Mon compte, Se déconnecter), toujours visible, en bas de la navigation.
   */
  espacePersonnel?: string
  children: ReactNode
}

function heureLocale() {
  return new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit', timeZone: 'Africa/Douala' })
}

// Enveloppe applicative commune (DS-GIMAC-01 §6) : barre supérieure, navigation latérale, bandeau de
// perte du canal poussé, préférences d'affichage, pied de page GIMAC. Composée uniquement de
// composants du catalogue.
export function Enveloppe(p: EnveloppeProps) {
  const { t } = useTranslation()
  const { theme } = usePreferences()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [heure, setHeure] = useState(heureLocale)
  // Hauteur du bloc supérieur fixe (barre, préférences, bandeau) : la navigation latérale se loge dessous.
  const haut = useRef<HTMLDivElement>(null)
  const [dessus, setDessus] = useState(56)
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
      const h = haut.current?.offsetHeight || 56
      setDessus(h)
      setHauteur(window.innerHeight - h)
      setEtroit(window.innerWidth < 1280)
    }
    redim()
    window.addEventListener('resize', redim)
    const obs = typeof ResizeObserver === 'undefined' || !haut.current ? null : new ResizeObserver(redim)
    if (obs && haut.current) obs.observe(haut.current)
    return () => {
      window.clearInterval(id)
      window.removeEventListener('resize', redim)
      obs?.disconnect()
    }
  }, [])

  const navigation: GroupeNavigation[] = p.espacePersonnel
    ? [
        ...p.navigation,
        {
          libelleKey: 'nav.groupe.espace',
          entrees: [
            { cle: 'monCompte', libelleKey: 'nav.monCompte', icone: 'user', chemin: `${p.espacePersonnel}/mon-compte` },
            { cle: 'deconnexion', libelleKey: 'nav.deconnexion', icone: 'lock', chemin: `${p.espacePersonnel}/deconnexion` },
          ],
        },
      ]
    : p.navigation
  const autorisee = (e: EntreeNavigation) => !e.permission || !p.permissions || p.permissions.includes(e.permission)
  const groupes = navigation
    .map((g) => ({ label: g.libelleKey ? t(g.libelleKey) : undefined, items: g.entrees.filter(autorisee).map((e) => ({ key: e.cle, label: t(e.libelleKey), icon: e.icone, href: e.chemin })) }))
    .filter((g) => g.items.length > 0)
  const courante = navigation.flatMap((g) => g.entrees).filter((e) => pathname.startsWith(e.chemin)).sort((a, b) => b.chemin.length - a.chemin.length)[0]

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
    <div className="gm-root" style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }} onClick={intercepter}>
      <div ref={haut} className="gm-shell-top">
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
      </div>
      <div style={{ display: 'flex', alignItems: 'stretch', flex: 1 }}>
        {etroit ? null : (
          <div className="gm-shell-nav" style={{ top: dessus, height: hauteur }}>
            <SideNav current={courante?.cle ?? ''} groups={groupes} height={hauteur} />
          </div>
        )}
        <main style={{ flex: 1, minWidth: 0, padding: etroit ? 'var(--gm-space-4)' : 'var(--gm-space-5) var(--gm-space-6)' }}>{p.children}</main>
      </div>
      <Footer version={p.version} documentation={p.documentation} correlationId={p.correlationId} />
      {etroit && navOuverte ? (
        // Tiroir de navigation : voile du catalogue (gm-overlay) et SideNav, fermeture par le voile ou Échap.
        <div className="gm-overlay" style={{ justifyContent: 'flex-start', alignItems: 'stretch', top: dessus, padding: 0 }} onClick={(e) => e.target === e.currentTarget && setNavOuverte(false)} onKeyDown={(e) => e.key === 'Escape' && setNavOuverte(false)}>
          <SideNav current={courante?.cle ?? ''} groups={groupes} height={hauteur} />
        </div>
      ) : null}
    </div>
  )
}
