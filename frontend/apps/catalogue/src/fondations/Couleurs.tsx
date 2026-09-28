import jetons from '@gimac/design-system/tokens.json'

interface JetonCouleur {
  name: string
  value: string | Record<string, string>
  usage?: string
}

const couleurs = jetons.color.tokens as JetonCouleur[]
const primitives = couleurs.filter((t) => typeof t.value === 'string')
const semantiques = couleurs.filter((t) => typeof t.value !== 'string' && t.name.startsWith('gm-color-'))
const donnees = couleurs.filter((t) => t.name.startsWith('gm-data-'))

function Nuance({ t }: { t: JetonCouleur }) {
  return (
    <div className="cat-nuance">
      <div className="cat-nuance-pastille" style={{ background: `var(--${t.name})` }} />
      <div className="cat-nuance-texte">
        <span className="gm-mono">{t.name}</span>
        <span>{typeof t.value === 'string' ? t.value : `${t.value.light} · ${t.value.dark}`}</span>
      </div>
    </div>
  )
}

export default function Couleurs() {
  return (
    <div className="cat-planche">
      <div>
        <h2 className="gm-type-h2">Couche sémantique, la seule consommée par les composants</h2>
        <div className="cat-nuancier">{semantiques.map((t) => <Nuance key={t.name} t={t} />)}</div>
      </div>
      <div>
        <h2 className="gm-type-h2">Palette de données, dans l'ordre fixe</h2>
        <div className="cat-nuancier">{donnees.map((t) => <Nuance key={t.name} t={t} />)}</div>
      </div>
      <div>
        <h2 className="gm-type-h2">Primitives</h2>
        <div className="cat-nuancier">{primitives.map((t) => <Nuance key={t.name} t={t} />)}</div>
      </div>
    </div>
  )
}
