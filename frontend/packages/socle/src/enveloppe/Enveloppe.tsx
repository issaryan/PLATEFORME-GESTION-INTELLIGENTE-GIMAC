import { useEffect, useRef, useState, type MouseEvent, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Banner, Footer, SideNav, TopBar, type IconName } from '@gimac/design-system'
import logoCouleur from '@gimac/design-system/assets/logo/gimac-logo-couleur.png'
import logoBlanc from '@gimac/design-system/assets/logo/gimac-logo-blanc.png'
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
  /** Compteurs affichés en pastille sur les entrées de navigation (clé de l'entrée → nombre), par ex. les messages non lus. */
  badges?: Record<string, number>
  utilisateur?: string
  /** Photo de profil de l'utilisateur (adresse versionnée, urlAvatar) ; initiales à défaut. */
  avatar?: string | null
  /** Fonction ou profil de l'utilisateur, seconde ligne du menu utilisateur. */
  fonction?: string | null
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
// composants du catalogue. Coquille fixe : la page ne défile pas ; seule la zone de contenu défile
// (classes gm-shell-* du système de conception) et change au clic d'une entrée de la navigation,
// son défilement revenant alors en haut. Aucune hauteur n'est calculée ici. Menu utilisateur de la barre
// supérieure (docs/lot1/mon-compte.md §4) : photo de profil ou initiales, nom, fonction, Mon compte, déconnexion.
export function Enveloppe(p: EnveloppeProps) {
  const { t } = useTranslation()
  const { theme } = usePreferences()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [heure, setHeure] = useState(heureLocale)
  const contenu = useRef<HTMLDivElement>(null)
  // Sous 1 280 px (--gm-bp-lg), la navigation devient un tiroir ouvert par le bouton de menu (§6.2, §10.2).
  const [etroit, setEtroit] = useState(() => window.innerWidth < 1280)
  const [navOuverte, setNavOuverte] = useState(false)

  // Changement de page : le tiroir se referme et la zone de contenu revient en haut.
  useEffect(() => {
    setNavOuverte(false)
    const zone = contenu.current
    if (zone) zone.scrollTop = 0
  }, [pathname])

  useEffect(() => {
    const id = window.setInterval(() => setHeure(heureLocale()), 1000)
    const redim = () => setEtroit(window.innerWidth < 1280)
    window.addEventListener('resize', redim)
    return () => {
      window.clearInterval(id)
      window.removeEventListener('resize', redim)
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
    .map((g) => ({ label: g.libelleKey ? t(g.libelleKey) : undefined, items: g.entrees.filter(autorisee).map((e) => ({ key: e.cle, label: t(e.libelleKey), icon: e.icone, href: e.chemin, badge: p.badges?.[e.cle] })) }))
    .filter((g) => g.items.length > 0)
  const courante = navigation.flatMap((g) => g.entrees).filter((e) => pathname.startsWith(e.chemin)).sort((a, b) => b.chemin.length - a.chemin.length)[0]

  // Navigation sans rechargement : les liens internes du catalogue sont interceptés par le routeur.
  const intercepter = (e: MouseEvent<HTMLElement>) => {
    const a = (e.target as HTMLElement).closest('a')
    const href = a?.getAttribute('href')
    if (a && href && href.startsWith('/') && !e.ctrlKey && !e.metaKey) {
      e.preventDefault()
      void navigate(href)
    }
  }
  // Logo principal blanc, fondu dans le dégradé bleu de la navigation (dans les deux thèmes).
  const nav = <SideNav current={courante?.cle ?? ''} groups={groupes} height="100%" logo={logoBlanc} couche={p.couche} />

  return (
    <div className="gm-root gm-shell" onClick={intercepter}>
      <div className="gm-shell-top">
        <TopBar
          avatar={p.avatar}
          userDetail={p.fonction ?? undefined}
          userMenu={
            p.espacePersonnel
              ? [
                  { key: 'monCompte', label: t('nav.monCompte'), icon: 'user', onClick: () => void navigate(`${p.espacePersonnel}/mon-compte`) },
                  { key: 'deconnexion', label: t('nav.deconnexion'), icon: 'log-out', onClick: () => void navigate(`${p.espacePersonnel}/deconnexion`) },
                ]
              : undefined
          }
          layer={p.couche}
          logo={theme === 'dark' ? logoBlanc : logoCouleur}
          sansMarque={!etroit}
          breadcrumb={p.fil}
          user={p.utilisateur}
          institution={p.institution}
          live={p.etatCanal !== 'lost'}
          clock={heure}
          preferences={<Preferences />}
          onMenu={etroit ? () => setNavOuverte((v) => !v) : undefined}
        />
        {p.etatCanal === 'lost' ? <Banner level="info">{t('live.perdu')}</Banner> : null}
      </div>
      <div className="gm-shell-corps">
        {etroit ? null : <div className="gm-shell-nav">{nav}</div>}
        <div ref={contenu} className="gm-shell-contenu" data-testid="gm-shell-contenu">
          <main className="gm-shell-main">{p.children}</main>
          <Footer version={p.version} documentation={p.documentation} correlationId={p.correlationId} />
        </div>
        {etroit && navOuverte ? (
          // Tiroir de navigation : voile du catalogue confiné à la zone sous la barre supérieure, fermeture par le voile ou Échap.
          <div className="gm-overlay gm-overlay-inline gm-shell-tiroir" onClick={(e) => e.target === e.currentTarget && setNavOuverte(false)} onKeyDown={(e) => e.key === 'Escape' && setNavOuverte(false)}>
            {nav}
          </div>
        ) : null}
      </div>
    </div>
  )
}
