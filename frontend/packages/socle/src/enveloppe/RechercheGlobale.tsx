import { useEffect, useRef, useState } from 'react'
import { CommandPalette, type CommandResult } from '@gimac/design-system'

/** Objet trouvé par une source de la recherche globale ; « href » interne (/des/…) ou absolu. */
export type ResultatRecherche = {
  cle: string
  type: 'liaison' | 'institution' | 'conversation' | 'compte'
  libelle: string
  code?: string
  detail?: string
  href: string
}

/** Source de la recherche globale : une famille d'objets, cherchée côté service (droits appliqués par la passerelle). */
export type SourceRecherche = { cle: string; chercher: (q: string) => Promise<ResultatRecherche[]> }

/** Écran proposé par la recherche : entrée de navigation permise, libellé traduit. */
export type EcranRecherche = { cle: string; libelle: string; href: string }

const DELAI_MS = 200
const MIN_OBJETS = 2
const MAX_ECRANS = 8

const normaliser = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

// Recherche globale (Ctrl+K, DS-GIMAC-01 §7.4) : les écrans permis de la navigation, filtrés sur place,
// puis les objets des sources (liaisons, institutions, conversations, comptes) à partir de deux caractères,
// recherche différée de 200 ms ; une source en échec est ignorée sans masquer les autres. Entrée ou clic
// ouvre l'objet ; Échap ou un clic sur le voile ferme la palette.
export function RechercheGlobale({ ecrans, sources, onOuvrir, onFermer }: {
  ecrans: EcranRecherche[]
  sources: SourceRecherche[]
  onOuvrir: (href: string) => void
  onFermer: () => void
}) {
  const [q, setQ] = useState('')
  const [objets, setObjets] = useState<ResultatRecherche[]>([])
  const [chargement, setChargement] = useState(false)
  const tour = useRef(0)

  useEffect(() => {
    const terme = q.trim()
    const n = ++tour.current
    if (terme.length < MIN_OBJETS || !sources.length) {
      setObjets([])
      setChargement(false)
      return
    }
    setChargement(true)
    const minuterie = window.setTimeout(() => {
      void Promise.allSettled(sources.map((s) => s.chercher(terme))).then((r) => {
        if (n !== tour.current) return
        setObjets(r.flatMap((x) => (x.status === 'fulfilled' ? x.value : [])))
        setChargement(false)
      })
    }, DELAI_MS)
    return () => window.clearTimeout(minuterie)
  }, [q, sources])

  const terme = normaliser(q.trim())
  const ecransTrouves = ecrans.filter((e) => !terme || normaliser(e.libelle).includes(terme)).slice(0, MAX_ECRANS)
  const resultats: (CommandResult & { href: string })[] = [
    ...objets.map((o) => ({ key: `${o.type}:${o.cle}`, group: 'objets' as const, kind: o.type, code: o.code, label: o.libelle, detail: o.detail, href: o.href })),
    ...ecransTrouves.map((e) => ({ key: `ecran:${e.cle}`, group: 'ecrans' as const, kind: 'ecran' as const, label: e.libelle, href: e.href })),
  ]
  return (
    <div className="gm-overlay gm-recherche-voile" onMouseDown={(e) => e.target === e.currentTarget && onFermer()}>
      <CommandPalette
        query={q}
        onQuery={setQ}
        results={resultats}
        chargement={chargement}
        onClose={onFermer}
        onSelect={(r) => onOuvrir((r as CommandResult & { href: string }).href)}
      />
    </div>
  )
}
