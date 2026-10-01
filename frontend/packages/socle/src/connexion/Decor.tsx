import { useMemo } from 'react'

/** Nom de la plateforme (D-67) : le moabi, arbre géant des forêts d'Afrique centrale, image de durabilité et de collaboration. */
export const NOM_PLATEFORME = 'Moabi'

// Décor de l'écran de connexion (D-67) : courbes de niveau du relief sur toute la page et nom de la plateforme
// en grand, guilloché en mode clair, en contour en mode sombre. Traits fins aux couleurs des jetons --gm-*,
// tracés une fois (générateur pseudo-aléatoire à graine fixe : même dessin à chaque affichage), purement décoratifs.

function aleatoire(graine: number) {
  let g = graine
  return () => (g = (g * 16807) % 2147483647) / 2147483647
}

/** Anneaux déformés autour de quelques sommets ; une courbe sur cinq est une courbe maîtresse. */
export function tracerRelief(largeur: number, hauteur: number) {
  const sommets: [number, number, number][] = [
    [largeur * 0.74, hauteur * 0.28, 1.15],
    [largeur * 0.97, hauteur * 0.92, 0.9],
    [largeur * 0.08, hauteur * 1.05, 0.7],
    [largeur * 0.4, hauteur * -0.08, 0.6],
  ]
  const alea = aleatoire(11)
  const courbes: { d: string; maitresse: boolean }[] = []
  for (const [cx, cy, f] of sommets) {
    const harmoniques = Array.from({ length: 6 }, (_, i) => [0.035 + (alea() * 0.05) / (i + 1), alea() * 6.28] as const)
    for (let i = 1; i <= 26; i++) {
      const r0 = i * 22 * f
      let d = ''
      for (let a = 0; a <= 360; a += 2) {
        const th = (a * Math.PI) / 180
        let r = r0
        harmoniques.forEach(([amp, ph], k) => { r += r0 * amp * Math.sin((k + 2) * th + ph + i * 0.06) })
        d += `${d ? 'L' : 'M'}${(cx + r * Math.cos(th)).toFixed(1)} ${(cy + r * Math.sin(th) * 0.78).toFixed(1)}`
      }
      courbes.push({ d: `${d}Z`, maitresse: i % 5 === 0 })
    }
  }
  return courbes
}

export function Relief() {
  const courbes = useMemo(() => tracerRelief(1440, 900), [])
  return (
    <svg className="gm-acces-relief" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice" aria-hidden="true" focusable="false">
      {courbes.map((c, i) => <path key={i} d={c.d} className={c.maitresse ? 'gm-relief-maitresse' : undefined} />)}
    </svg>
  )
}

/** Ondes fines qui remplissent les lettres du nom (guillochage, comme le filigrane d'un billet). */
export function tracerOndes() {
  const ondes: { d: string; terre: boolean }[] = []
  for (let k = 0; k < 64; k++) {
    let d = ''
    for (let x = -10; x <= 620; x += 3) {
      const y = k * 3.4 + 4 * Math.sin(x / 26 + k * 0.35) + 2 * Math.sin(x / 9 - k * 0.7)
      d += `${d ? 'L' : 'M'}${x} ${y.toFixed(1)}`
    }
    ondes.push({ d, terre: k % 6 === 0 })
  }
  return ondes
}

/** Nom de la plateforme : guilloché (clair) ou en contour (sombre), filet tricolore et signature. */
export function Marque({ sombre, signature }: { sombre: boolean; signature: string }) {
  const ondes = useMemo(tracerOndes, [])
  const texte = <text x="0" y="168" className="gm-marque-texte">{NOM_PLATEFORME}</text>
  return (
    <div className="gm-acces-marque">
      <svg viewBox="0 0 600 210" role="img" aria-label={NOM_PLATEFORME} data-style={sombre ? 'contour' : 'guilloche'}>
        {sombre ? (
          <>
            <g className="gm-marque-halo">{texte}</g>
            <g className="gm-marque-contour">{texte}</g>
          </>
        ) : (
          <>
            <defs><clipPath id="gm-marque-lettres">{texte}</clipPath></defs>
            <g clipPath="url(#gm-marque-lettres)" className="gm-marque-ondes">
              {ondes.map((o, i) => <path key={i} d={o.d} className={o.terre ? 'gm-onde-terre' : undefined} />)}
            </g>
            <g className="gm-marque-lisere">{texte}</g>
          </>
        )}
      </svg>
      <div className="gm-filet" aria-hidden="true"><i /><i /><i /></div>
      <span className="gm-acces-signature">{signature}</span>
    </div>
  )
}
