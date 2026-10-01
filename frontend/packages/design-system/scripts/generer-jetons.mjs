// Génère src/jetons/tokens.css à partir de src/jetons/tokens.json, source unique des jetons
// du système de conception GIMAC DES (DS-GIMAC-01, sections 3 à 5).
//   node scripts/generer-jetons.mjs            écrit le fichier
//   node scripts/generer-jetons.mjs --verifier échoue si le fichier n'est pas à jour
import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const racine = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'jetons')
const jetons = JSON.parse(readFileSync(join(racine, 'tokens.json'), 'utf8'))

// Un alias « {gm-grey-100} » devient une référence à la propriété personnalisée.
const valeur = (v) => String(v).replace(/^\{([A-Za-z0-9_.-]+)\}$/, 'var(--$1)')

const [clair, sombre] = jetons.color.themes.map((t) => t.id)
const racineClair = []
const racineSombre = []
for (const t of jetons.color.tokens) {
  if (typeof t.value === 'string') {
    racineClair.push(`  --${t.name}: ${valeur(t.value)};`)
    continue
  }
  racineClair.push(`  --${t.name}: ${valeur(t.value[clair])};`)
  if (t.value[sombre] !== undefined) racineSombre.push(`  --${t.name}: ${valeur(t.value[sombre])};`)
}

const familles = ['spacing', 'radius', 'border', 'duration', 'breakpoint', 'size'].filter((f) => jetons[f])
const autres = familles.flatMap((f) => [
  `  /* ${f} */`,
  ...jetons[f].tokens.map((t) => `  --${t.name}: ${t.value};`),
])

const polices = Object.entries(jetons.type.families).map(([nom, pile]) => `  --font-${nom}: ${pile};`)

const classes = jetons.type.groups.flatMap((g) =>
  g.styles.map((s) => {
    const regles = [
      `font-family: var(--font-${g.family})`,
      `font-size: ${s.fontSize}`,
      `line-height: ${s.lineHeight}`,
      `font-weight: ${s.fontWeight}`,
    ]
    if (s.letterSpacing) regles.push(`letter-spacing: ${s.letterSpacing}`, 'text-transform: uppercase')
    if (/stat|number/.test(s.name)) regles.push('font-variant-numeric: tabular-nums')
    if (g.family === 'mono') regles.push('font-variant-ligatures: none')
    return `.${s.name} { ${regles.join('; ')}; }`
  }),
)

const css = `/*
 * Jetons de conception GIMAC DES, générés par scripts/generer-jetons.mjs depuis tokens.json.
 * Ne pas modifier à la main. Source : ${jetons.meta.source}.
 * Le mode sombre redéfinit la couche sémantique et la palette de données, rien d'autre ;
 * il est activé par data-theme="dark" sur la racine (ou sur un conteneur).
 */
:root {
${racineClair.join('\n')}
${autres.join('\n')}
  /* familles typographiques */
${polices.join('\n')}
}

[data-theme="dark"] {
${racineSombre.join('\n')}
}

/* Échelle typographique (14 rôles) */
${classes.join('\n')}
`

const cible = join(racine, 'tokens.css')
if (process.argv.includes('--verifier')) {
  const actuel = readFileSync(cible, 'utf8')
  if (actuel !== css) {
    console.error('tokens.css n\'est pas à jour : lancer npm run jetons')
    process.exit(1)
  }
  console.log('tokens.css à jour')
} else {
  writeFileSync(cible, css)
  console.log(`tokens.css écrit (${jetons.color.tokens.length} couleurs, ${classes.length} styles de texte)`)
}
