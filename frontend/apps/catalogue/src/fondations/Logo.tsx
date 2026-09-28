import couleur from '@gimac/design-system/assets/logo/gimac-couleur.png'
import bleu from '@gimac/design-system/assets/logo/gimac-bleu.png'
import blanc from '@gimac/design-system/assets/logo/gimac-blanc.png'

const VERSIONS = [
  { src: couleur, nom: 'gimac-couleur.png', usage: 'Connexion, rapports, portail', fond: 'var(--gm-white)' },
  { src: bleu, nom: 'gimac-bleu.png', usage: 'Barre supérieure sur fond clair', fond: 'var(--gm-white)' },
  { src: blanc, nom: 'gimac-blanc.png', usage: 'Barre supérieure en sombre, bandeaux bleus', fond: 'var(--gm-blue-700)' },
]

export default function Logo() {
  return (
    <div className="cat-logos">
      {VERSIONS.map((v) => (
        <figure key={v.nom} className="cat-logo" style={{ margin: 0 }}>
          <div className="cat-logo-fond" style={{ background: v.fond }}>
            <img src={v.src} alt={`Logo GIMAC, ${v.nom}`} />
          </div>
          <figcaption className="cat-logo-legende">
            <span className="gm-mono">{v.nom}</span> · {v.usage}
          </figcaption>
        </figure>
      ))}
    </div>
  )
}
