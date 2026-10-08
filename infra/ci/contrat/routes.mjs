#!/usr/bin/env node
// Contrôle du contrat des routes entre le frontend et le backend (docs/lot1/correspondance.md).
//
// Pour chaque appel HTTP des couches de présentation (http.get|post|put|delete|blob, fetch, EventSource
// dans frontend/packages/*/src et frontend/apps/*/src, hors tests, services simulés et catalogue) :
//   1. le chemin est reconstitué depuis le code (constantes, préfixes P.*, fonctions de chemin) ;
//   2. la route de la passerelle qui le prend en charge est cherchée (application.yml de MS-GATEWAY,
//      prédicats Path= et filtre RewritePath=) ;
//   3. le chemin réécrit doit correspondre à une route d'un contrôleur du service cible
//      (@RequestMapping de la classe + @GetMapping/@PostMapping/@PutMapping/@PatchMapping/@DeleteMapping),
//      ou à un contrôleur de la passerelle elle-même pour /api/**, /bff/{couche}/live et /bff/{couche}/presence ;
//   4. la politique OPA (lot1_authz.rego) ne doit pas refuser la route par construction (chemin du portail
//      ou de la messagerie non listé), et la permission qu'elle exige doit figurer dans le @PreAuthorize
//      du contrôleur.
// Écarts connus : infra/ci/contrat/ecarts-connus.txt liste, motif à l'appui, les routes des écrans antérieurs au
// modèle du parcours (D-56, D-57) que le backend ne sert pas ; elles sont signalées sans bloquer. Toute autre route
// sans contrepartie bloque, comme un écart connu qui a disparu (la liste doit être tenue à jour).
// Sortie : tableau (texte ou Markdown avec --markdown) ; code 1 si une route n'a pas de contrepartie.
//
//   node infra/ci/contrat/routes.mjs              # contrôle, liste des écarts
//   node infra/ci/contrat/routes.mjs --markdown   # tableau route par route (correspondance.md)
//   node infra/ci/contrat/routes.mjs --json       # inventaire complet
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs'
import { createRequire } from 'node:module'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const RACINE = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')
const FRONTEND = join(RACINE, 'frontend')
const exiger = createRequire(join(FRONTEND, 'package.json'))
const ts = exiger('typescript')
const yaml = exiger('js-yaml')

const args = new Set(process.argv.slice(2))
const JOKER = '{}'

// ---------------------------------------------------------------------------------------------------
// 1. Frontend : appels HTTP et reconstitution des chemins
// ---------------------------------------------------------------------------------------------------
function fichiers(dossier, filtre, acc = []) {
  if (!existsSync(dossier)) return acc
  for (const nom of readdirSync(dossier)) {
    const p = join(dossier, nom)
    if (nom === 'node_modules' || nom.startsWith('dist')) continue
    if (statSync(p).isDirectory()) fichiers(p, filtre, acc)
    else if (filtre(p)) acc.push(p)
  }
  return acc
}

const estSource = (p) =>
  /\.(ts|tsx)$/.test(p) && !/\.(test|spec)\.tsx?$/.test(p) && !/\.d\.ts$/.test(p) && !/[\\/](simule|test|__tests__)[\\/]/.test(p) && !/simule\.tsx?$/.test(p)

const sources = []
for (const groupe of ['packages', 'apps']) {
  const base = join(FRONTEND, groupe)
  for (const nom of readdirSync(base)) {
    if (nom === 'catalogue') continue // catalogue vivant, hors production
    sources.push(...fichiers(join(base, nom, 'src'), estSource))
  }
}

const modules = new Map()
function module(chemin) {
  if (!modules.has(chemin)) {
    const texte = readFileSync(chemin, 'utf8')
    modules.set(chemin, ts.createSourceFile(chemin, texte, ts.ScriptTarget.Latest, true, chemin.endsWith('x') ? ts.ScriptKind.TSX : ts.ScriptKind.TS))
  }
  return modules.get(chemin)
}

function resoudreModule(depuis, specifieur) {
  let base
  if (specifieur.startsWith('@gimac/')) {
    const [, nom, ...reste] = specifieur.split('/')
    base = reste.length ? join(FRONTEND, 'packages', nom, 'src', ...reste) : join(FRONTEND, 'packages', nom, 'src', 'index')
  } else if (specifieur.startsWith('.')) base = resolve(dirname(depuis), specifieur)
  else return null
  for (const c of [base, `${base}.ts`, `${base}.tsx`, join(base, 'index.ts'), join(base, 'index.tsx')]) if (existsSync(c) && statSync(c).isFile()) return c
  return null
}

/** Déclaration d'un nom dans un module : constante, fonction, import ou réexportation. */
function declaration(chemin, nom, vus = new Set()) {
  const cle = `${chemin}#${nom}`
  if (vus.has(cle)) return null
  vus.add(cle)
  const sf = module(chemin)
  let trouve = null
  const visiter = (n) => {
    if (trouve) return
    if (ts.isVariableDeclaration(n) && ts.isIdentifier(n.name) && n.name.text === nom && n.initializer) trouve = { sf, noeud: n.initializer }
    else if (ts.isFunctionDeclaration(n) && n.name?.text === nom) trouve = { sf, noeud: n }
    else ts.forEachChild(n, visiter)
  }
  visiter(sf)
  if (trouve) return trouve
  for (const st of sf.statements) {
    if (ts.isImportDeclaration(st) && st.importClause?.namedBindings && ts.isNamedImports(st.importClause.namedBindings)) {
      for (const el of st.importClause.namedBindings.elements) {
        if (el.name.text === nom) {
          const cible = resoudreModule(chemin, st.moduleSpecifier.text)
          if (cible) return declaration(cible, (el.propertyName ?? el.name).text, vus)
        }
      }
    }
    if (ts.isExportDeclaration(st) && st.moduleSpecifier) {
      const cible = resoudreModule(chemin, st.moduleSpecifier.text)
      if (!cible) continue
      if (!st.exportClause) {
        const d = declaration(cible, nom, vus)
        if (d) return d
      } else if (ts.isNamedExports(st.exportClause)) {
        for (const el of st.exportClause.elements) if (el.name.text === nom) return declaration(cible, (el.propertyName ?? el.name).text, vus)
      }
    }
  }
  return null
}

/** Valeur textuelle d'une expression ; tout ce qui n'est pas constant devient le joker {}. */
function valeur(noeud, sf, portee = new Map(), profondeur = 0) {
  if (!noeud || profondeur > 20) return JOKER
  const v = (n, s = sf, p = portee) => valeur(n, s, p, profondeur + 1)
  if (ts.isParenthesizedExpression(noeud) || ts.isAsExpression(noeud) || ts.isNonNullExpression(noeud) || ts.isSatisfiesExpression?.(noeud)) return v(noeud.expression)
  if (ts.isStringLiteral(noeud) || ts.isNoSubstitutionTemplateLiteral(noeud)) return noeud.text
  if (ts.isTemplateExpression(noeud)) return noeud.head.text + noeud.templateSpans.map((s) => v(s.expression) + s.literal.text).join('')
  if (ts.isBinaryExpression(noeud) && noeud.operatorToken.kind === ts.SyntaxKind.PlusToken) return v(noeud.left) + v(noeud.right)
  // a ?? b : la valeur par défaut, seule connue du code (adresse du flux, par exemple).
  if (ts.isBinaryExpression(noeud) && noeud.operatorToken.kind === ts.SyntaxKind.QuestionQuestionToken) {
    const g = v(noeud.left)
    return g === JOKER ? v(noeud.right) : g
  }
  // c ? a : b (préfixe selon la couche, par exemple) : les deux branches sont contrôlées.
  if (ts.isConditionalExpression(noeud)) return `⟦${v(noeud.whenTrue)}¦${v(noeud.whenFalse)}⟧`
  if (ts.isIdentifier(noeud)) {
    if (portee.has(noeud.text)) return portee.get(noeud.text)
    const d = declaration(sf.fileName, noeud.text)
    if (!d || ts.isFunctionDeclaration(d.noeud) || ts.isArrowFunction(d.noeud)) return JOKER
    return valeur(d.noeud, d.sf, new Map(), profondeur + 1)
  }
  if (ts.isPropertyAccessExpression(noeud) && ts.isIdentifier(noeud.expression)) {
    const d = declaration(sf.fileName, noeud.expression.text)
    let objet = d?.noeud
    while (objet && (ts.isAsExpression(objet) || ts.isSatisfiesExpression?.(objet))) objet = objet.expression
    if (objet && ts.isObjectLiteralExpression(objet)) {
      const prop = objet.properties.find((p) => ts.isPropertyAssignment(p) && p.name.getText(d.sf) === noeud.name.text)
      if (prop) return valeur(prop.initializer, d.sf, new Map(), profondeur + 1)
    }
    return JOKER
  }
  if (ts.isCallExpression(noeud) && ts.isIdentifier(noeud.expression)) {
    const nom = noeud.expression.text
    if (nom === 'encodeURIComponent' || nom === 'String') return JOKER
    const d = declaration(sf.fileName, nom)
    const f = d?.noeud
    if (f && (ts.isArrowFunction(f) || ts.isFunctionDeclaration(f) || ts.isFunctionExpression(f))) {
      const p = new Map()
      f.parameters.forEach((param, i) => {
        if (ts.isIdentifier(param.name)) p.set(param.name.text, noeud.arguments[i] ? v(noeud.arguments[i]) : JOKER)
      })
      let corps = f.body
      if (corps && ts.isBlock(corps)) {
        const retour = corps.statements.find((s) => ts.isReturnStatement(s))
        corps = retour?.expression
      }
      return corps ? valeur(corps, d.sf, p, profondeur + 1) : JOKER
    }
    return JOKER
  }
  return JOKER
}

/** Développe les alternatives ⟦a¦b⟧ d'une expression conditionnelle en autant de chemins. */
function alternatives(texte) {
  const m = /⟦([^⟦⟧]*)⟧/.exec(texte)
  if (!m) return [texte]
  return m[1].split('¦').flatMap((branche) => alternatives(texte.slice(0, m.index) + branche + texte.slice(m.index + m[0].length)))
}

const METHODES_HTTP = { get: 'GET', post: 'POST', put: 'PUT', patch: 'PATCH', delete: 'DELETE', blob: 'GET' }

function methodeFetch(init, sf) {
  if (init && ts.isObjectLiteralExpression(init)) {
    const m = init.properties.find((p) => ts.isPropertyAssignment(p) && p.name.getText(sf) === 'method')
    if (m) return valeur(m.initializer, sf).toUpperCase()
  }
  return 'GET'
}

/** Nom de l'adaptateur appelant (propriété d'objet ou fonction englobante), pour le tableau. */
function appelant(n, sf) {
  for (let p = n.parent; p; p = p.parent) {
    if (ts.isPropertyAssignment(p) || ts.isMethodDeclaration(p)) return p.name.getText(sf)
    if (ts.isVariableDeclaration(p) && ts.isIdentifier(p.name)) return p.name.text
    if (ts.isFunctionDeclaration(p) && p.name) return p.name.text
  }
  return ''
}

const appels = []
for (const chemin of sources) {
  const sf = module(chemin)
  const visiter = (n) => {
    let methode = null
    let arg = null
    if (ts.isCallExpression(n)) {
      const c = n.expression
      if (ts.isPropertyAccessExpression(c) && ts.isIdentifier(c.expression) && c.expression.text === 'http' && METHODES_HTTP[c.name.text]) {
        methode = METHODES_HTTP[c.name.text]
        arg = n.arguments[0]
      } else if (ts.isIdentifier(c) && c.text === 'fetch' && n.arguments[0]) {
        methode = methodeFetch(n.arguments[1], sf)
        arg = n.arguments[0]
      }
    } else if (ts.isNewExpression(n) && ts.isIdentifier(n.expression) && n.expression.text === 'EventSource') {
      methode = 'GET'
      arg = n.arguments?.[0]
    }
    if (methode && arg) {
      // fetch(url, …) du client HTTP du socle : chemin transmis par ses appelants, déjà recensés.
      const valeurBrute = valeur(arg, sf)
      for (const brut of alternatives(valeurBrute)) if (!(brut === JOKER && relative(FRONTEND, chemin).includes('socle/src/http/'))) {
        const chemin_ = brut.split('?')[0].replace(/\/+$/, '') || brut
        const source = `${relative(RACINE, chemin)}:${sf.getLineAndCharacterOfPosition(n.getStart(sf)).line + 1}`
        // Préfixe d'une couche quelconque (/bff/{couche}/…, « Mon compte ») : contrôlé pour chacune des trois.
        const couches = chemin_.startsWith(`/bff/${JOKER}/`) ? ['des', 'portail', 'console'].map((c) => chemin_.replace(`/bff/${JOKER}/`, `/bff/${c}/`)) : [chemin_]
        for (const c of couches) appels.push({ methode, chemin: c, source, appelant: appelant(n, sf) })
      }
    }
    ts.forEachChild(n, visiter)
  }
  visiter(sf)
}

// ---------------------------------------------------------------------------------------------------
// 2. Passerelle : routes déclarées (application.yml)
// ---------------------------------------------------------------------------------------------------
const configPasserelle = yaml.loadAll(readFileSync(join(RACINE, 'ms-gateway/src/main/resources/config/application.yml'), 'utf8')).find((d) => d?.spring?.cloud?.gateway)
const routesPasserelle = configPasserelle.spring.cloud.gateway.server.webflux.routes.map((r) => {
  const chemins = (r.predicates ?? []).filter((p) => typeof p === 'string' && p.startsWith('Path=')).flatMap((p) => p.slice(5).split(','))
  const reecriture = (r.filters ?? []).find((f) => typeof f === 'string' && f.startsWith('RewritePath='))
  const strip = (r.filters ?? []).find((f) => typeof f === 'string' && f.startsWith('StripPrefix='))
  const service = /\$\{gimac\.services\.([a-z]+)\}/.exec(r.uri)?.[1] ?? (r.uri === 'lb://gateway' ? 'gateway' : null)
  let regle = null
  if (reecriture) {
    const [de, vers] = reecriture.slice('RewritePath='.length).split(/,\s*/)
    regle = { de: new RegExp(de.replace(/\(\?<([a-z]+)>/g, '(?<$1>')), vers: vers.replace(/\$\{([a-z]+)\}/g, '$<$1>') }
  }
  return { id: r.id, service, chemins, regle, strip: strip ? Number(strip.split('=')[1]) : 0 }
})

/** Motif Spring PathPattern (/a/{x}/**) en expression régulière. */
function motifPasserelle(m) {
  const re = m
    .split('/')
    .map((s) => (s === '**' ? '§§' : s === '*' || /^\{[^}]+\}$/.test(s) ? '[^/]+' : s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    .join('/')
    .replace('/§§', '(/.*)?')
  return new RegExp(`^${re}$`)
}

const echantillon = (chemin) => chemin.split('/').map((s) => (s.includes(JOKER) ? s.replaceAll(JOKER, 'x0') : s)).join('/')

function routePasserelle(chemin) {
  const ex = echantillon(chemin)
  return routesPasserelle.find((r) => r.chemins.some((c) => motifPasserelle(c).test(ex)))
}

function reecrire(route, chemin) {
  // Les jokers survivent à la réécriture : ils sont remplacés par un jeton sans barre oblique.
  const jeton = '\u0001'
  const ex = chemin.replaceAll(JOKER, jeton)
  if (route.regle) return ex.replace(route.regle.de, route.regle.vers).replaceAll(jeton, JOKER)
  if (route.strip) return '/' + ex.split('/').slice(route.strip + 1).join('/').replaceAll(jeton, JOKER)
  return chemin
}

// ---------------------------------------------------------------------------------------------------
// 3. Backend : contrôleurs Spring
// ---------------------------------------------------------------------------------------------------
const MAPPINGS = { GetMapping: 'GET', PostMapping: 'POST', PutMapping: 'PUT', PatchMapping: 'PATCH', DeleteMapping: 'DELETE' }

function cheminAnnotation(args) {
  if (!args) return ['']
  const corps = args.trim()
  const m = /(?:^|[,(\s])(?:value|path)\s*=\s*(\{[^}]*\}|"[^"]*")/.exec(corps) ?? (/^\s*(\{[^}]*\}|"[^"]*")/.exec(corps) ? [null, /^\s*(\{[^}]*\}|"[^"]*")/.exec(corps)[1]] : null)
  if (!m) return ['']
  return [...m[1].matchAll(/"([^"]*)"/g)].map((x) => x[1])
}

const routesBackend = []
for (const dossier of readdirSync(RACINE).filter((d) => /^ms-[a-z]+$/.test(d))) {
  const service = dossier.slice(3)
  for (const f of fichiers(join(RACINE, dossier, 'src/main/java'), (p) => p.endsWith('.java'))) {
    const texte = readFileSync(f, 'utf8')
    if (!/@(Rest)?Controller\b/.test(texte)) continue
    const classe = /((?:@\w+(?:\([^)]*\))?\s*)*)(?:public\s+)?(?:final\s+)?class\s+\w+/.exec(texte)
    const prefixe = classe ? cheminAnnotation(/@RequestMapping\(([^)]*)\)/.exec(classe[1])?.[1])[0] ?? '' : ''
    const re = /@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)(?:\(((?:[^()]|\([^()]*\))*)\))?([\s\S]*?)(?:public|protected|private)\s+[\w<>[\],.?\s]+\s+(\w+)\s*\(/g
    for (const m of texte.matchAll(re)) {
      const pre = /@PreAuthorize\("([^"]*)"\)/.exec(m[3])?.[1] ?? null
      const permissions = pre ? [...pre.matchAll(/'([A-Z_]+)'/g)].map((x) => x[1]) : []
      for (const c of cheminAnnotation(m[2])) {
        routesBackend.push({
          service,
          methode: MAPPINGS[m[1]],
          chemin: (prefixe + c).replace(/\/+$/, '') || '/',
          permissions,
          preAuthorize: pre,
          source: `${relative(RACINE, f)}:${texte.slice(0, m.index).split('\n').length}`,
          methodeJava: m[4],
        })
      }
    }
  }
}

const segments = (c) => c.split('/').filter(Boolean)
function correspond(frontend, backend) {
  const a = segments(frontend)
  const b = segments(backend)
  if (a.length !== b.length) return false
  return a.every((s, i) => {
    const t = b[i]
    if (/^\{[^}]+\}$/.test(t)) return true
    if (s.includes(JOKER)) return new RegExp(`^${s.split(JOKER).map((x) => x.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('[^/]+')}$`).test(t)
    return s === t
  })
}
/** Une route littérale l'emporte sur une variable (comme Spring) : /liaisons/conducteurs avant /liaisons/{id}. */
const precision = (r) => segments(r.chemin).filter((s) => !/^\{/.test(s)).length

// ---------------------------------------------------------------------------------------------------
// 4. OPA : règles à permission (lot1_authz.rego)
// ---------------------------------------------------------------------------------------------------
const rego = readFileSync(join(RACINE, 'infra/docker-compose/opa/politiques/lot1_authz.rego'), 'utf8')
const reglesOpa = [...rego.matchAll(/\{"m": "([A-Z]+)", "re": `([^`]+)`, "p": "([A-Z_]+)"\}/g)].map((m) => ({ methode: m[1], re: new RegExp(m[2]), permission: m[3] }))

/** Chemins /bff servis par la passerelle elle-même : canal poussé et présence (D-72). */
const LOCALE_PASSERELLE = /^\/bff\/(des|portail|console)\/(live|presence)$/

function opa(methode, chemin) {
  const ex = echantillon(chemin)
  // Canal poussé et présence (D-72) : contrôleurs de la passerelle, hors filtre OpaAuthorize (session à deux facteurs exigée).
  if (LOCALE_PASSERELLE.test(ex)) return { decision: 'HORS_OPA' }
  const r = reglesOpa.find((x) => x.methode === methode && x.re.test(ex))
  if (r) return { decision: 'PERMISSION', permission: r.permission }
  if (/^\/bff\/[^/]+\/[^/]+\/interne(\/|$)/.test(ex)) return { decision: 'REFUS', motif: 'route interne' }
  if (/^\/bff\/(des|portail|console)\/iam\/(moi(\/.*)?|reauthentification\/defi)$/.test(ex)) return { decision: 'LIBRE_SERVICE' }
  // Catalogue des fonctionnalités (D-94) : règle lecture_catalogue_fonctionnalites, tout compte interne à deux facteurs.
  if (methode === 'GET' && /^\/bff\/(des|console)\/iam\/configuration\/fonctionnalites$/.test(ex)) return { decision: 'COMPTE_INTERNE' }
  if (ex === '/bff/des/document/documents' && methode === 'POST') return { decision: 'DEPOT', permission: 'PAR_ADHESION_INSTRUCT|HOM_EVIDENCE_SUBMIT|DOC_DEPOSIT' }
  // Dépôts fragmentés (D-74) depuis le DES et la console : règle route_depot_fragmente (permissions du dépôt ou COL_MESSAGE_SEND).
  if (
    (methode === 'POST' && /^\/bff\/(des|console)\/document\/depots(\/[^/]+\/fin)?$/.test(ex)) ||
    (methode === 'PUT' && /^\/bff\/(des|console)\/document\/depots\/[^/]+\/parties\/[^/]+$/.test(ex)) ||
    (['GET', 'DELETE'].includes(methode) && /^\/bff\/(des|console)\/document\/depots\/[^/]+$/.test(ex))
  )
    return { decision: 'DEPOT', permission: 'PAR_ADHESION_INSTRUCT|HOM_EVIDENCE_SUBMIT|HOM_STEP_MANAGE|CER_CHANGE_RECORD_LINK|DOC_DEPOSIT|COL_MESSAGE_SEND' }
  if (/^\/bff\/(des|console)\/document\/depots(\/|$)/.test(ex)) return { decision: 'REFUS', motif: 'chemin de dépôt fragmenté sans règle' }
  if (ex.startsWith('/bff/portail/')) return { decision: 'REFUS', motif: 'chemin du portail sans règle' }
  if (/^\/bff\/des\/collaboration\/messagerie(\/|$)/.test(ex)) return { decision: 'REFUS', motif: 'chemin de messagerie sans règle' }
  if (ex.startsWith('/bff/des/')) return { decision: 'PREAUTHORIZE' }
  if (ex.startsWith('/bff/console/')) return { decision: 'REFUS', motif: 'chemin de la console sans règle' }
  return { decision: 'HORS_OPA' }
}

// ---------------------------------------------------------------------------------------------------
// 5. Rapprochement
// ---------------------------------------------------------------------------------------------------
const vus = new Set()
const lignes = []
for (const a of appels) {
  const cle = `${a.methode} ${a.chemin}`
  if (vus.has(cle)) continue
  vus.add(cle)
  const l = { ...a, passerelle: null, service: null, cheminService: null, controleur: null, opa: null, ecarts: [] }
  if (!a.chemin.startsWith('/')) {
    l.ecarts.push('chemin non résolu dans le code')
    lignes.push(l)
    continue
  }
  let candidats
  const locale = LOCALE_PASSERELLE.test(a.chemin.replaceAll(JOKER, 'x'))
  const route = a.chemin.startsWith('/bff/') && !locale ? routePasserelle(a.chemin) : null
  if (a.chemin.startsWith('/bff/') && !route && !locale) {
    l.ecarts.push('aucune route de la passerelle')
  } else if (route) {
    l.passerelle = route.id
    l.service = route.service
    l.cheminService = reecrire(route, a.chemin)
    candidats = routesBackend.filter((r) => r.service === route.service)
  } else {
    l.passerelle = '(passerelle)'
    l.service = 'gateway'
    l.cheminService = a.chemin
    candidats = routesBackend.filter((r) => r.service === 'gateway')
  }
  if (candidats) {
    const trouves = candidats.filter((r) => r.methode === a.methode && correspond(l.cheminService, r.chemin)).sort((x, y) => precision(y) - precision(x))
    if (trouves.length) l.controleur = trouves[0]
    else {
      const autres = candidats.filter((r) => correspond(l.cheminService, r.chemin)).map((r) => r.methode)
      l.ecarts.push(autres.length ? `méthode ${a.methode} absente du service (existe : ${[...new Set(autres)].join(', ')})` : `aucune route ${a.methode} ${l.cheminService} dans ms-${l.service}`)
    }
  }
  if (a.chemin.startsWith('/bff/')) {
    l.opa = opa(a.methode, a.chemin)
    if (l.opa.decision === 'REFUS') l.ecarts.push(`OPA refuse : ${l.opa.motif}`)
    if (l.opa.decision === 'PERMISSION' && l.controleur?.permissions.length && !l.controleur.permissions.includes(l.opa.permission)) {
      l.ecarts.push(`OPA exige ${l.opa.permission}, absent du @PreAuthorize (${l.controleur.permissions.join(', ')})`)
    }
  }
  lignes.push(l)
}
lignes.sort((x, y) => (x.chemin + x.methode).localeCompare(y.chemin + y.methode))

// Écarts connus (« MÉTHODE chemin  # motif », {…} pour un segment variable).
const fichierConnus = join(RACINE, 'infra/ci/contrat/ecarts-connus.txt')
const connus = new Map(
  (existsSync(fichierConnus) ? readFileSync(fichierConnus, 'utf8') : '')
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l && !l.startsWith('#'))
    .map((l) => {
      const [route, ...motif] = l.split('#')
      return [route.trim().replaceAll('{…}', JOKER), motif.join('#').trim()]
    }),
)
for (const l of lignes) {
  const cle = `${l.methode} ${l.chemin}`
  if (l.ecarts.length && connus.has(cle)) {
    l.connu = connus.get(cle)
  }
}
const connusResolus = [...connus.keys()].filter((c) => !lignes.some((l) => `${l.methode} ${l.chemin}` === c && l.ecarts.length))

const permission = (l) => {
  if (!l.opa) return l.controleur?.permissions.join(' ou ') || (l.controleur ? 'session' : '')
  if (l.opa.decision === 'PERMISSION') return l.opa.permission
  if (l.opa.decision === 'LIBRE_SERVICE') return 'titulaire (2 facteurs)'
  if (l.opa.decision === 'COMPTE_INTERNE') return 'compte interne (2 facteurs)'
  if (l.opa.decision === 'DEPOT') return l.opa.permission.split('|').join(' ou ')
  if (l.opa.decision === 'PREAUTHORIZE') return l.controleur?.permissions.join(' ou ') || '2 facteurs'
  return '—'
}

if (args.has('--json')) {
  console.log(JSON.stringify({ appels: lignes, routesBackend: routesBackend.length }, null, 2))
} else if (args.has('--markdown')) {
  console.log('| Méthode | Chemin du frontend | Route de la passerelle | Service : chemin | Permission (OPA, puis @PreAuthorize) | État |')
  console.log('|---|---|---|---|---|---|')
  for (const l of lignes) {
    const svc = l.service ? `${l.service === 'gateway' ? 'MS-GATEWAY' : 'MS-' + l.service.toUpperCase()} : \`${l.controleur ? l.controleur.chemin : l.cheminService ?? ''}\`` : ''
    const etat = !l.ecarts.length ? 'conforme' : l.connu ? `écart connu : ${l.connu}` : `écart : ${l.ecarts.join(' ; ')}`
    console.log(`| ${l.methode} | \`${l.chemin.replaceAll(JOKER, '{…}')}\` | ${l.passerelle ?? '—'} | ${svc} | ${permission(l)} | ${etat} |`)
  }
} else {
  const ecarts = lignes.filter((l) => l.ecarts.length && !l.connu)
  const signales = lignes.filter((l) => l.connu)
  console.log(`Appels du frontend : ${lignes.length} routes distinctes ; contrôleurs du backend : ${routesBackend.length} routes.`)
  if (signales.length) console.log(`  ${signales.length} écart(s) connu(s), non bloquants (infra/ci/contrat/ecarts-connus.txt).`)
  for (const l of ecarts) console.log(`  ÉCART ${l.methode} ${l.chemin} (${l.source}, ${l.appelant}) : ${l.ecarts.join(' ; ')}`)
  for (const c of connusResolus) console.log(`  ÉCART CONNU RÉSOLU OU DISPARU ${c} : à retirer de infra/ci/contrat/ecarts-connus.txt`)
  if (ecarts.length || connusResolus.length) {
    console.log(`${ecarts.length + connusResolus.length} écart(s) bloquant(s).`)
    process.exit(1)
  }
  console.log('Toutes les routes appelées par le frontend ont leur contrepartie (passerelle, OPA, contrôleur).')
}
