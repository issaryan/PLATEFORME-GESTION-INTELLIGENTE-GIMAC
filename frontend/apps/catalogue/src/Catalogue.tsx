import { Component, Fragment, Suspense, lazy, useEffect, useMemo, useRef, useState, type ComponentType, type ReactNode } from 'react'
import { Icon } from '@gimac/design-system'
import { GROUPES, PLAN } from './plan'
import { FONDATIONS } from './fondations'

// Chaque aperçu est un fichier src/apercus/<Composant>.tsx : l'ajouter suffit à l'afficher.
// Chargement paresseux : un aperçu en erreur n'empêche pas les autres de s'afficher.
const modules = import.meta.glob<{ default: ComponentType }>('./apercus/*.tsx')
const APERCUS: Record<string, ComponentType> = Object.fromEntries(
  Object.entries(modules).map(([chemin, charger]) => [chemin.replace(/^.*\/(.*)\.tsx$/, '$1'), lazy(charger)]),
)

class Garde extends Component<{ children: ReactNode }, { erreur: string | null }> {
  state = { erreur: null as string | null }
  static getDerivedStateFromError(e: unknown) {
    return { erreur: e instanceof Error ? e.message : String(e) }
  }
  render() {
    if (this.state.erreur) return <div className="gm-inline gm-banner-crit" role="alert">L'aperçu n'a pas pu être rendu : {this.state.erreur}</div>
    return this.props.children
  }
}

type Theme = 'light' | 'dark'

interface Entree {
  id: string
  nom: string
  groupe: string
  description: string
  largeur: number | null
  hauteur: number
  Apercu?: ComponentType
}

const ENTREES: Entree[] = [
  ...FONDATIONS.map((f) => ({ ...f, groupe: 'Fondations' })),
  ...PLAN.map((p) => ({ ...p, id: p.nom, Apercu: APERCUS[p.nom] })),
]

function lirePreference(): Theme {
  try {
    return localStorage.getItem('gm-catalogue-theme') === 'dark' ? 'dark' : 'light'
  } catch {
    return 'light'
  }
}

function idDepuisAdresse(): string {
  const id = decodeURIComponent(window.location.hash.replace(/^#\/?/, ''))
  return ENTREES.some((e) => e.id === id) ? id : ENTREES[0].id
}

// Rendu du sous-ensemble Markdown des fiches : paragraphes, **gras** et `code`.
function Fiche({ texte }: { texte: string }) {
  return (
    <>
      {texte.split(/\n\s*\n/).map((para, i) => (
        <p key={i} className="cat-fiche-para">
          {para.split(/(\*\*[^*]+\*\*|`[^`]+`)/).map((morceau, j): ReactNode => {
            if (morceau.startsWith('**')) return <strong key={j}>{morceau.slice(2, -2)}</strong>
            if (morceau.startsWith('`')) return <code key={j} className="gm-mono">{morceau.slice(1, -1)}</code>
            return <Fragment key={j}>{morceau}</Fragment>
          })}
        </p>
      ))}
    </>
  )
}

export function Catalogue() {
  const [id, setId] = useState(idDepuisAdresse)
  const [theme, setTheme] = useState<Theme>(lirePreference)

  useEffect(() => {
    const surHash = () => setId(idDepuisAdresse())
    window.addEventListener('hashchange', surHash)
    return () => window.removeEventListener('hashchange', surHash)
  }, [])

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    try {
      localStorage.setItem('gm-catalogue-theme', theme)
    } catch {
      /* préférence non mémorisée : sans conséquence */
    }
  }, [theme])

  const index = ENTREES.findIndex((e) => e.id === id)
  const courante = ENTREES[index]
  const aller = (cible: Entree) => {
    window.location.hash = `/${cible.id}`
  }

  const portes = PLAN.filter((p) => APERCUS[p.nom]).length
  const parGroupe = useMemo(
    () => ['Fondations', ...GROUPES].map((g) => ({ groupe: g, entrees: ENTREES.filter((e) => e.groupe === g) })),
    [],
  )

  useEffect(() => {
    const surTouche = (e: KeyboardEvent) => {
      if ((e.target as HTMLElement).closest('input, textarea, select')) return
      if (e.key === 'ArrowDown' && e.altKey) aller(ENTREES[Math.min(index + 1, ENTREES.length - 1)])
      if (e.key === 'ArrowUp' && e.altKey) aller(ENTREES[Math.max(index - 1, 0)])
    }
    window.addEventListener('keydown', surTouche)
    return () => window.removeEventListener('keydown', surTouche)
  }, [index])

  const Apercu = courante.Apercu

  // Largeur disponible : sous la largeur prévue de la carte, l'aperçu est réduit à l'échelle
  // (zoom) plutôt que comprimé, afin que sa mise en page reste celle de l'original.
  const contenu = useRef<HTMLElement>(null)
  const [dispo, setDispo] = useState(Infinity)
  useEffect(() => {
    const el = contenu.current
    if (!el) return
    const mesurer = () => {
      const cs = getComputedStyle(el)
      setDispo(el.clientWidth - parseFloat(cs.paddingLeft) - parseFloat(cs.paddingRight))
    }
    mesurer()
    const obs = new ResizeObserver(mesurer)
    obs.observe(el)
    return () => obs.disconnect()
  }, [])
  const largeurScene = (courante.largeur ?? 960) + 2
  const echelle = Math.min(1, dispo / largeurScene)

  return (
    <div className="cat">
      <aside className="cat-nav" aria-label="Composants du catalogue">
        <div className="cat-nav-tete">
          <div className="cat-marque">GIMAC DES</div>
          <div className="gm-type-micro cat-sous-titre">Système de conception</div>
          <div className="cat-progression" aria-live="polite">
            <span className="gm-num">{portes} / {PLAN.length}</span> composants portés en React
            <div className="cat-jauge" aria-hidden="true">
              <span style={{ width: `${(portes / PLAN.length) * 100}%` }} />
            </div>
          </div>
        </div>
        <nav className="cat-nav-liste">
          {parGroupe.map(({ groupe, entrees }) => (
            <div key={groupe}>
              <div className="gm-type-micro cat-groupe">{groupe}</div>
              <ul>
                {entrees.map((e) => (
                  <li key={e.id}>
                    <a
                      href={`#/${e.id}`}
                      aria-current={e.id === id ? 'page' : undefined}
                      className={e.Apercu ? undefined : 'cat-a-venir'}
                    >
                      <span>{e.nom}</span>
                      {e.Apercu ? null : <span className="cat-etiquette">à venir</span>}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>
      </aside>

      <main className="cat-contenu" ref={contenu}>
        <header className="cat-entete">
          <div>
            <div className="gm-type-micro cat-groupe-courant">{courante.groupe}</div>
            <h1 className="gm-type-display cat-titre">{courante.nom}</h1>
          </div>
          <div className="cat-outils">
            <div className="cat-bascule" role="group" aria-label="Thème de l'aperçu">
              <button type="button" aria-pressed={theme === 'light'} onClick={() => setTheme('light')}>
                Clair
              </button>
              <button type="button" aria-pressed={theme === 'dark'} onClick={() => setTheme('dark')}>
                Sombre
              </button>
            </div>
            <button type="button" className="gm-btn gm-btn-secondary gm-btn-sm" disabled={index <= 0} onClick={() => aller(ENTREES[index - 1])}>
              Précédent
            </button>
            <button type="button" className="gm-btn gm-btn-primary gm-btn-sm" disabled={index >= ENTREES.length - 1} onClick={() => aller(ENTREES[index + 1])}>
              Suivant
            </button>
          </div>
        </header>

        {courante.description ? (
          <section className="cat-fiche">
            <Fiche texte={courante.description} />
          </section>
        ) : null}

        <section
          className="cat-scene"
          aria-label={`Aperçu de ${courante.nom}`}
          style={{ width: largeurScene, minHeight: courante.hauteur, zoom: echelle }}
        >
          {Apercu ? (
            <div className="gm-root gm-pad" data-theme={theme}>
              <Garde key={courante.id}>
                <Suspense fallback={<div className="gm-skeleton" style={{ height: 40 }} />}>
                  <Apercu />
                </Suspense>
              </Garde>
            </div>
          ) : (
            <div className="cat-attente">
              <Icon name="hourglass" size={24} />
              <p>Ce composant n'est pas encore porté en React. Il apparaîtra ici dès qu'il le sera.</p>
            </div>
          )}
        </section>
        <p className="gm-field-help cat-aide">Alt + flèche haut ou bas : composant précédent ou suivant.</p>
      </main>
    </div>
  )
}
