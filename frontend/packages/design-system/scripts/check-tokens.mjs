// Contrôle « aucune valeur en dur » (DS-GIMAC-01 §12.2) : échoue si une feuille de style ou un
// composant contient une couleur hexadécimale, une couleur rgb()/hsl() ou une police nommée.
// Seules exceptions : celles du système publié, reprises à l'identique et listées ci-dessous.
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { dirname, join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const src = join(dirname(fileURLToPath(import.meta.url)), '..', 'src')
const EXCEPTIONS = [
  // Motif de trame : gm-earth-700 à 8 % d'opacité (§5.6), tel que publié.
  { fichier: 'styles/00-base.css', valeur: 'rgba(122, 62, 29, 0.08)' },
  // Anneau du spinner sur un bouton principal du CRUD, tel que publié.
  { fichier: 'styles/17-crud.css', valeur: 'rgba(255, 255, 255, 0.3)' },
]
const IGNORES = ['jetons/', 'assets/']
const MOTIFS = [
  { nom: 'couleur hexadécimale', re: /#[0-9a-fA-F]{3,8}\b(?![-\w])/g },
  { nom: 'couleur fonctionnelle', re: /\b(?:rgba?|hsla?|oklch)\([^)]*\)/g },
  { nom: 'police nommée', re: /font-family\s*:\s*(?!var\(|inherit)[^;"'}]*["']?[A-Z][^;}]*/g },
  { nom: 'police nommée', re: /fontFamily\s*:\s*["'](?!var\()[^"']+["']/g },
]

function fichiers(dir) {
  return readdirSync(dir).flatMap((n) => {
    const p = join(dir, n)
    return statSync(p).isDirectory() ? fichiers(p) : /\.(css|tsx?)$/.test(n) ? [p] : []
  })
}

const fautes = []
for (const f of fichiers(src)) {
  const rel = relative(src, f).split('\\').join('/')
  if (IGNORES.some((i) => rel.startsWith(i)) || rel === 'reset.css') continue
  const lignes = readFileSync(f, 'utf8').split('\n')
  lignes.forEach((ligne, i) => {
    if (/^\s*(\/\/|\*|\/\*)/.test(ligne)) return
    for (const m of MOTIFS) {
      for (const trouve of ligne.match(m.re) ?? []) {
        if (m.nom === 'couleur hexadécimale' && /&#|\\u|['"`]#\//.test(ligne)) continue
        if (EXCEPTIONS.some((e) => e.fichier === rel && trouve.includes(e.valeur))) continue
        fautes.push(`${rel}:${i + 1} ${m.nom} : ${trouve.trim()}`)
      }
    }
  })
}
if (fautes.length) {
  console.error(`Valeurs en dur interdites (${fautes.length}) :\n` + fautes.join('\n'))
  process.exit(1)
}
console.log('Aucune valeur en dur : seuls les jetons --gm-* sont consommés.')
