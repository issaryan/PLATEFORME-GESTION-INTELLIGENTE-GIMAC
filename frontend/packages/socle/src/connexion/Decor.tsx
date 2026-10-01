import { useMemo } from 'react'

/** Nom de la plateforme (D-67) : le moabi, arbre géant des forêts d'Afrique centrale, image de durabilité et de collaboration. */
export const NOM_PLATEFORME = 'Moabi'

// Décor de l'écran de connexion (D-67) : les cernes du bois derrière la carte d'accès, l'arbre moabi (racines,
// fût, houppier, feuilles) derrière le nom, et une feuille à la place du point du i. Le nom est guilloché en mode
// clair, en contour en mode sombre. Traits fins aux couleurs des jetons --gm-*, tracés une fois (générateur
// pseudo-aléatoire à graine fixe : même dessin à chaque affichage), purement décoratifs.

function aleatoire(graine: number) {
  let g = graine
  return () => (g = (g * 16807) % 2147483647) / 2147483647
}

type Trait = { d: string; classe?: string; ep?: number }

/** Cernes de croissance : coupe d'un tronc, cernes irréguliers et excentrés, quelques rayons et une fente. */
export function tracerCernes(cx: number, cy: number): Trait[] {
  const alea = aleatoire(23)
  const harmoniques = Array.from({ length: 5 }, (_, i) => [0.012 + (alea() * 0.02) / (i + 1), alea() * 6.28] as const)
  const traits: Trait[] = []
  let r = 6
  for (let i = 1; r < 1300; i++) {
    r += 7 + alea() * 12 + (i < 12 ? 4 : 0)
    let d = ''
    for (let a = 0; a <= 360; a += 1.5) {
      const th = (a * Math.PI) / 180
      let rr = r
      harmoniques.forEach(([amp, ph], k) => { rr += r * amp * Math.sin((k + 2) * th + ph) })
      const fente = Math.abs(((a - 212 + 540) % 360) - 180)
      if (fente < 3 && r > 140) rr -= (3 - fente) * 5
      d += `${d ? 'L' : 'M'}${(cx + rr * Math.cos(th) * 1.06).toFixed(1)} ${(cy + rr * Math.sin(th)).toFixed(1)}`
    }
    traits.push({ d: `${d}Z`, classe: i % 10 === 0 ? 'gm-cerne-fort' : undefined })
  }
  for (let k = 0; k < 14; k++) {
    const th = alea() * 6.28, r0 = 60 + alea() * 300, r1 = r0 + 80 + alea() * 260
    const point = (rr: number) => `${(cx + rr * Math.cos(th) * 1.06).toFixed(1)} ${(cy + rr * Math.sin(th)).toFixed(1)}`
    traits.push({ d: `M${point(r0)}L${point(r1)}`, classe: 'gm-cerne-rayon' })
  }
  return traits
}

type Feuille = { transform: string; limbe: string; nervures: string; bleue: boolean; ep: number }

/** Feuille de moabi au trait : limbe obovale, nervure centrale et nervures secondaires. */
export function tracerFeuille(x: number, y: number, l: number, angle: number, ep = 1.2, largeur = 0.42) {
  const lw = l * largeur
  const limbe = `M0 0 C${lw * 0.5} ${-l * 0.08} ${lw} ${-l * 0.55} ${lw * 0.55} ${-l * 0.86} C${lw * 0.35} ${-l * 0.98} ${lw * 0.1} ${-l} 0 ${-l}`
    + ` C${-lw * 0.1} ${-l} ${-lw * 0.35} ${-l * 0.98} ${-lw * 0.55} ${-l * 0.86} C${-lw} ${-l * 0.55} ${-lw * 0.5} ${-l * 0.08} 0 0Z`
  let nervures = `M0 ${l * 0.12}L0 ${-l * 0.96}`
  for (let k = 1; k <= 6; k++) {
    const yy = -l * (0.12 + k * 0.12), d = lw * (0.45 + 0.4 * Math.sin((k / 7) * Math.PI))
    nervures += `M0 ${yy}Q${d * 0.5} ${yy - l * 0.04} ${d * 0.85} ${yy - l * 0.12}M0 ${yy}Q${-d * 0.5} ${yy - l * 0.04} ${-d * 0.85} ${yy - l * 0.12}`
  }
  return { transform: `translate(${x} ${y}) rotate(${angle})`, limbe, nervures, ep }
}

/** Arbre moabi : racines-contreforts, fût haut et nu, houppier large et aplati en parasol, feuilles groupées. */
export function tracerArbre(x0: number, sol: number, ech: number, fut: number) {
  const alea = aleatoire(31)
  const bois: Trait[] = []
  const L = (d: string, ep = 1) => { bois.push({ d, ep }) }
  const haut = sol - fut * ech, dh = 10 * ech, db = 16 * ech
  L(`M${x0 - dh} ${haut} C${x0 - dh} ${sol - 220 * ech} ${x0 - db} ${sol - 70 * ech} ${x0 - db * 2.6} ${sol}`, 1.4)
  L(`M${x0 + dh} ${haut} C${x0 + dh} ${sol - 220 * ech} ${x0 + db} ${sol - 70 * ech} ${x0 + db * 2.6} ${sol}`, 1.4)
  for (let k = -1; k <= 1; k++) {
    L(`M${x0 + k * dh * 0.45} ${haut + 40 * ech} C${x0 + k * dh * 0.45} ${sol - 240 * ech} ${x0 + k * db * 0.55} ${sol - 90 * ech} ${x0 + k * db} ${sol - 6 * ech}`, 0.6)
  }
  // Contreforts ailés, puis racines traçantes et ligne du sol.
  for (const [s, l, hh] of [[-1, 64, 46], [-1, 120, 22], [1, 70, 40], [1, 128, 20]]) {
    L(`M${x0 + s * db * 0.7} ${sol - hh * ech * 2.4} C${x0 + s * (db + l * 0.2 * ech)} ${sol - hh * ech * 0.9} ${x0 + s * l * 0.62 * ech} ${sol - 1} ${x0 + s * l * ech} ${sol + 3 * ech}`, 1.1)
  }
  for (let k = 0; k < 6; k++) {
    const s = k % 2 ? 1 : -1, l = 110 + alea() * 120
    L(`M${x0 + s * l * 0.4 * ech} ${sol + 2} Q${x0 + s * l * 0.7 * ech} ${sol + (6 + alea() * 8) * ech} ${x0 + s * l * ech} ${sol + (12 + alea() * 14) * ech}`, 0.6)
  }
  let terre = ''
  for (let x = x0 - 300 * ech; x <= x0 + 300 * ech; x += 8) terre += `${terre ? 'L' : 'M'}${x.toFixed(0)} ${(sol + 2 * Math.sin(x / 29)).toFixed(1)}`
  L(terre, 0.6)
  // Houppier : charpentières étagées qui montent puis s'étalent vers l'horizontale ; feuilles au bout des rameaux.
  const bouts: [number, number][] = []
  const branche = (x: number, y: number, ang: number, len: number, ep: number, n: number) => {
    const a = (ang * Math.PI) / 180, x2 = x + Math.cos(a) * len, y2 = y + Math.sin(a) * len * 0.62
    L(`M${x.toFixed(1)} ${y.toFixed(1)}Q${(x + Math.cos(a) * len * 0.45).toFixed(1)} ${(y + Math.sin(a) * len * 0.42 - 6 * ech).toFixed(1)} ${x2.toFixed(1)} ${y2.toFixed(1)}`, ep)
    if (n === 0) { bouts.push([x2, y2]); return }
    if (n === 1 && alea() > 0.35) bouts.push([x2, y2 - 4 * ech])
    const dehors = ang < -90 ? -1 : 1
    branche(x2, y2, ang + dehors * (14 + alea() * 10), len * 0.7, ep * 0.7, n - 1)
    branche(x2, y2, ang - dehors * (18 + alea() * 12), len * 0.62, ep * 0.7, n - 1)
  }
  for (const [ang, dy] of [[-152, 70], [-30, 70], [-130, 34], [-52, 34], [-108, 0], [-74, 0]]) {
    branche(x0 + (ang < -90 ? -1 : 1) * dh * 0.6, haut + dy * ech, ang, (dy ? 160 : 140) * ech, 2, 3)
  }
  L(`M${x0} ${haut + 70 * ech}L${x0} ${haut}`, 1.6)
  const feuilles: Feuille[] = bouts.flatMap(([x, y], i) => Array.from({ length: 5 }, (_, k) => ({
    ...tracerFeuille(x, y, (20 + (k % 3) * 5) * ech, -150 + k * 75 + (i % 3) * 10, 0.9, 0.34),
    bleue: i % 5 === 0,
  })))
  return { bois, feuilles }
}

/** Cernes du bois derrière la carte d'accès, effacés vers la droite pour laisser l'arbre seul. */
export function Cernes() {
  const traits = useMemo(() => tracerCernes(0, 0), [])
  return (
    <svg className="gm-acces-cernes" viewBox="-1300 -1300 1950 2600" aria-hidden="true" focusable="false">
      <defs>
        <linearGradient id="gm-cernes-fondu" gradientUnits="userSpaceOnUse" x1="97" x2="340" y1="0" y2="0">
          <stop offset="0" stopColor="#fff" />
          <stop offset="1" stopColor="#fff" stopOpacity="0" />
        </linearGradient>
        <mask id="gm-cernes-masque" maskUnits="userSpaceOnUse" x="-1300" y="-1300" width="1950" height="2600">
          <rect x="-1300" y="-1300" width="1950" height="2600" fill="url(#gm-cernes-fondu)" />
        </mask>
      </defs>
      <g mask="url(#gm-cernes-masque)">
        {traits.map((c, i) => <path key={i} d={c.d} className={c.classe} />)}
      </g>
    </svg>
  )
}

/** Nom de la plateforme sur l'arbre moabi : guilloché (clair) ou en contour (sombre), feuille sur le i. */
export function Marque({ sombre, signature }: { sombre: boolean; signature: string }) {
  const ondes = useMemo(tracerOndes, [])
  const arbre = useMemo(() => tracerArbre(276, 535, 1, 560), [])
  const feuille = useMemo(() => tracerFeuille(525.5, 48, 54, 14, 1.7), [])
  // « ı » sans point : la feuille en tient lieu.
  const texte = <text x="0" y="168" className="gm-marque-texte">{'Moabı'}</text>
  return (
    <div className="gm-acces-marque">
      <svg viewBox="0 0 600 210" role="img" aria-label={NOM_PLATEFORME} data-style={sombre ? 'contour' : 'guilloche'}>
        <g className="gm-marque-arbre" aria-hidden="true">
          {arbre.bois.map((b, i) => <path key={i} d={b.d} strokeWidth={b.ep} />)}
          {arbre.feuilles.map((f, i) => (
            <g key={i} transform={f.transform} className={f.bleue ? 'gm-arbre-feuille gm-arbre-feuille-bleue' : 'gm-arbre-feuille'} strokeWidth={f.ep}>
              <path d={f.limbe} /><path d={f.nervures} strokeWidth={f.ep * 0.6} />
            </g>
          ))}
        </g>
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
        <g transform={feuille.transform} className="gm-marque-feuille" strokeWidth={feuille.ep}>
          <path d={feuille.limbe} /><path d={feuille.nervures} strokeWidth={feuille.ep * 0.6} />
        </g>
      </svg>
      <div className="gm-filet" aria-hidden="true"><i /><i /><i /></div>
      <span className="gm-acces-signature">{signature}</span>
    </div>
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
