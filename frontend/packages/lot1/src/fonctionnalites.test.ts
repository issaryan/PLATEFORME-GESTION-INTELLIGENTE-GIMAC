import { describe, expect, it } from 'vitest'
import { satisfait } from '@gimac/socle'
import {
  ACCUEIL, ECRANS_DES, WIDGETS, accesFonctionnalite, fonctionnalitesOuvertes, libelleGroupe, libelleServi, navigationDepuis, navigationRepli, predicatDe, widgetsOuverts,
  type DefinitionFonctionnalite,
} from './fonctionnalites'
import { FONCTIONNALITES_INITIALES as CATALOGUE } from './services/simule/fonctionnalites'
import { PROFILS } from './permissions'
import { PROFILS_DOMAINE } from './domaine'

// D-94 : catalogue des fonctionnalités servi par MS-IAM ; barre latérale, gardes et tableau de bord en dérivent, le code
// ne garde que la correspondance clé → écran. Matrice attendue pour chaque profil interne du domaine « gimac ».

const cles = (fs: DefinitionFonctionnalite[]) => fs.map((f) => f.cle)
const EXTERNES = new Set(PROFILS_DOMAINE.filter((p) => p.externe).map((p) => p.code))
const INTERNES = Object.keys(PROFILS).filter((r) => !EXTERNES.has(r))
const ouvertes = (p: string[]) => cles(fonctionnalitesOuvertes(CATALOGUE, 'des', p))
const widgets = (p: string[]) => widgetsOuverts(CATALOGUE, p)
const tr = { t: (k: string) => `«${k}»`, existe: (k: string) => k.startsWith('nav.') }

describe('catalogue des fonctionnalités (valeurs de départ de MS-IAM)', () => {
  it('clés uniques ; chaque entrée de barre latérale de l’application interne a un écran ; widgets connus', () => {
    expect(new Set(cles(CATALOGUE)).size).toBe(CATALOGUE.length)
    for (const f of CATALOGUE.filter((x) => x.application === 'des' && x.groupe)) expect(ECRANS_DES[f.cle], f.cle).toBeTruthy()
    for (const f of CATALOGUE) for (const w of f.widgets) expect(WIDGETS).toContain(w.cle)
    expect(Object.values(ECRANS_DES).every((e) => e.chemin.startsWith('/des/') || e.chemin.includes('/console/'))).toBe(true)
  })

  it('tout profil interne : tableau de bord, messagerie et Mon activité ; correspondant : l’accueil et ses notifications (D-93 §11.4)', () => {
    for (const role of INTERNES) {
      expect(ouvertes(PROFILS[role]), role).toEqual(expect.arrayContaining(['tableauDeBord', 'notifications', 'maJournee', 'agenda', 'taches', 'rapports']))
      expect(widgets(PROFILS[role])[0], role).toBe('maJournee')
    }
    // Profils externes (correspondant, rôles d'institution de la sandbox D-115) : l'accueil et ses notifications.
    for (const role of EXTERNES) {
      expect(ouvertes(PROFILS[role]), role).toEqual(['tableauDeBord', 'notifications'])
      expect(widgets(PROFILS[role]), role).toEqual([])
    }
    expect(EXTERNES).toContain('ROLE_CORRESPONDANT')
  })

  it.each([
    ['ROLE_ADMIN_SYSTEME', ['maJournee', 'supervision', 'infrastructure']],
    ['ROLE_SUPER_ADMIN', ['maJournee', 'administration', 'supervision', 'infrastructure']],
    ['ROLE_RSSI', ['maJournee', 'administration', 'supervision', 'infrastructure', 'monTravail']],
    ['ROLE_CHEF_DES', ['maJournee', 'cockpit', 'monTravail']],
    ['ROLE_CHEF_DDRM', ['maJournee', 'ddrm', 'cockpit', 'monTravail']],
    ['ROLE_CHARGE_DDRM', ['maJournee', 'ddrm', 'monTravail']],
    ['ROLE_DG', ['maJournee', 'cockpit', 'monTravail']],
    ['ROLE_CONFORMITE', ['maJournee', 'monTravail']],
    ['ROLE_INGENIEUR_HOMOLOGATION', ['maJournee', 'monTravail']],
    ['ROLE_CONDUCTEUR_PROJET', ['maJournee', 'monTravail']],
    ['ROLE_SNA', ['maJournee', 'monTravail']],
    ['ROLE_COLLABORATEUR', ['maJournee']],
    ['ROLE_RESPONSABLE_SERVICE', ['maJournee']],
  ])('%s : widgets %j', (role, attendus) => {
    expect(widgets(PROFILS[role])).toEqual(attendus)
  })

  it('configuration : une fonctionnalité masquée ou dont la permission change suit sans nouvelle construction', () => {
    const masque = CATALOGUE.map((f) => (f.cle === 'messagerie' ? { ...f, visible: false } : f))
    expect(cles(fonctionnalitesOuvertes(masque, 'des', PROFILS.ROLE_SNA))).not.toContain('messagerie')
    const autre = CATALOGUE.map((f) => (f.cle === 'referentiels' ? { ...f, permissions: { tous: [], unDe: ['PIL_TEAM_READ'] } } : f))
    expect(cles(fonctionnalitesOuvertes(autre, 'des', PROFILS.ROLE_SNA))).not.toContain('referentiels')
    expect(cles(fonctionnalitesOuvertes(autre, 'des', PROFILS.ROLE_CHEF_DES))).toContain('referentiels')
  })

  it('gardes : chargement, indisponible (repli prudent), ouvert, refusé, droit exigé', () => {
    const pret = { etat: 'pret' as const, fonctionnalites: CATALOGUE }
    expect(accesFonctionnalite({ etat: 'chargement', fonctionnalites: [] }, 'liaisons', PROFILS.ROLE_SNA)).toBe('chargement')
    expect(accesFonctionnalite({ etat: 'indisponible', fonctionnalites: [] }, 'liaisons', PROFILS.ROLE_SNA)).toBe('indisponible')
    expect(accesFonctionnalite(pret, 'liaisons', PROFILS.ROLE_SNA)).toBe('ouvert')
    expect(accesFonctionnalite(pret, 'adminComptes', PROFILS.ROLE_SNA)).toBe('refuse')
    expect(accesFonctionnalite(pret, 'inconnue', PROFILS.ROLE_SNA)).toBe('refuse')
    expect(accesFonctionnalite(pret, 'adminComptes', PROFILS.ROLE_RSSI, 'IAM_ACCOUNT_CREATE')).toBe('ouvert')
    expect(accesFonctionnalite(pret, 'adminComptes', ['IAM_ACCOUNT_READ'], 'IAM_ACCOUNT_CREATE')).toBe('refuse')
    expect(predicatDe(null)).toEqual({ all: [], any: [] })
  })

  it('barre latérale : groupes dans l’ordre de leur première fonctionnalité, libellés servis, prédicats repris ; repli : l’accueil', () => {
    const nav = navigationDepuis(CATALOGUE, 'des', ECRANS_DES, tr, 'nav.groupe.')
    expect(nav.map((g) => g.libelleKey ?? '')).toEqual(['', 'nav.groupe.monActivite', 'nav.groupe.processus', 'nav.groupe.homologation', 'nav.groupe.administration', 'nav.groupe.monitoring', 'nav.groupe.plateforme', 'nav.groupe.sandbox'])
    const entrees = nav.flatMap((g) => g.entrees)
    expect(entrees.filter((e) => satisfait(PROFILS.ROLE_ADMIN_SYSTEME, e.predicat)).map((e) => e.cle)).toEqual(ouvertes(PROFILS.ROLE_ADMIN_SYSTEME).filter((c) => ECRANS_DES[c]))
    expect(entrees[0]).toMatchObject({ cle: 'tableauDeBord', libelle: '«nav.tableauDeBord»', chemin: '/des/tableau-de-bord' })
    // Groupe et libellé créés par la configuration : texte tel quel.
    const cree = navigationDepuis([{ ...CATALOGUE[1], groupe: 'pilotage', libelle: 'Échanges : équipe' }], 'des', ECRANS_DES, tr, 'nav.groupe.')
    expect(cree).toEqual([{ libelleKey: 'nav.groupe.pilotage', libelle: '«nav.groupe.pilotage»', entrees: [expect.objectContaining({ libelle: 'Échanges : équipe' })] }])
    expect(libelleServi({ t: (k) => k, existe: () => false }, 'Texte')).toBe('Texte')
    expect(libelleGroupe({ t: (k) => k, existe: () => false }, 'nav.groupe.', 'pilotage')).toBe('pilotage')
    expect(navigationRepli('des')).toEqual([{ entrees: [ACCUEIL.des] }])
    expect(navigationRepli('console')).toEqual([{ entrees: [ACCUEIL.console] }])
  })
})

describe('comptes internes de démonstration', () => {
  it('un compte par profil interne, identifiants uniques ; super administrateur et administrateur système hors annuaire', async () => {
    const { PROFILS_INTERNES_DEMO, identifiantDemo } = await import('./profilsDemo')
    expect(Object.keys(PROFILS_INTERNES_DEMO).sort()).toEqual(INTERNES.sort())
    const ids = Object.values(PROFILS_INTERNES_DEMO).map((p) => p.identifiant)
    expect(new Set(ids).size).toBe(ids.length)
    expect(PROFILS_INTERNES_DEMO.ROLE_SUPER_ADMIN.identifiant).toBe('super.admin')
    expect(PROFILS_INTERNES_DEMO.ROLE_ADMIN_SYSTEME).toMatchObject({ identifiant: 'admin.systeme', permissions: PROFILS.ROLE_ADMIN_SYSTEME })
    expect(identifiantDemo('Aïcha Mahamat Nour')).toBe('a.mahamat')
    expect(identifiantDemo('Unique')).toBe('u.unique')
  })
})
