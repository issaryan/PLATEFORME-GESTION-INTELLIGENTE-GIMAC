import trame from '@gimac/design-system/assets/motif/trame.svg'

export default function Motif() {
  return (
    <div className="cat-planche">
      <div>
        <h2 className="gm-type-h2">Fichier trame.svg (période 32 px)</h2>
        <img src={trame} alt="" width={512} height={256} style={{ display: 'block', border: 'var(--gm-border-1) solid var(--gm-color-border)' }} />
      </div>
      <div>
        <h2 className="gm-type-h2">Classe utilitaire .gm-pattern (suit le thème)</h2>
        <div className="gm-pattern" aria-hidden="true" style={{ width: 240, height: 160 }} />
      </div>
    </div>
  )
}
