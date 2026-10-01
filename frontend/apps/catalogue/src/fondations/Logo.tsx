import couleur from '@gimac/design-system/assets/logo/gimac-couleur.png'
import bleu from '@gimac/design-system/assets/logo/gimac-bleu.png'
import blanc from '@gimac/design-system/assets/logo/gimac-blanc.png'
import logoCouleur from '@gimac/design-system/assets/logo/gimac-logo-couleur.png'
import logoBlanc from '@gimac/design-system/assets/logo/gimac-logo-blanc.png'

const VERSIONS = [
  { src: couleur, nom: 'gimac-couleur.png', usage: 'Rapports imprimés (version avec la bande des drapeaux)', fond: 'var(--gm-white)' },
  { src: bleu, nom: 'gimac-bleu.png', usage: 'Barre supérieure sur fond clair', fond: 'var(--gm-white)' },
  { src: blanc, nom: 'gimac-blanc.png', usage: 'Bandeaux bleus avec la bande des drapeaux', fond: 'var(--gm-blue-700)' },
  { src: logoBlanc, nom: 'gimac-logo-blanc.png', usage: 'Logo principal sans drapeaux : navigation latérale (dégradé bleu), barre supérieure en sombre', fond: 'var(--gm-blue-700)' },
  { src: logoCouleur, nom: 'gimac-logo-couleur.png', usage: 'Logo principal sans drapeaux : barre supérieure sur fond clair', fond: 'var(--gm-white)' },
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
