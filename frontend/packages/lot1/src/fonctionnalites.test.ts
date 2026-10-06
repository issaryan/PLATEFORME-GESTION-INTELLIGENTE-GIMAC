import { describe, expect, it } from 'vitest'
import { satisfait } from '@gimac/socle'
import { FONCTIONNALITES, GROUPES, WIDGETS, fonctionnalitesOuvertes, navigationDepuis, widgetsOuverts, type Fonctionnalite } from './fonctionnalites'
import { PROFILS } from './permissions'

// D-79 : registre unique des fonctionnalités de l'application interne ; barre latérale et tableau de bord
// dérivés des permissions du jeton. Matrice attendue pour chaque profil interne du domaine « gimac ».

const cles = (fs: Fonctionnalite[]) => fs.map((f) => f.cle)
const INTERNES = Object.keys(PROFILS).filter((r) => r !== 'ROLE_CORRESPONDANT')

describe('registre des fonctionnalités', () => {
  it('clés uniques, groupes connus, chemins internes sous /des (sauf le lien vers la console)', () => {
    expect(new Set(cles(FONCTIONNALITES)).size).toBe(FONCTIONNALITES.length)
    const groupes = GROUPES.map((g) => g.cle)
    for (const f of FONCTIONNALITES) {
      expect(groupes).toContain(f.groupe)
      expect(f.chemin.startsWith('/des/') || f.cle === 'adminConsole').toBe(true)
      for (const w of f.widgets ?? []) expect(WIDGETS).toContain(w.cle)
    }
  })

  it('tout profil interne : tableau de bord, messagerie et Mon activité ; jamais d’entrée sans permission', () => {
    for (const role of INTERNES) {
      const ouvertes = cles(fonctionnalitesOuvertes(PROFILS[role]))
      expect(ouvertes, role).toEqual(expect.arrayContaining(['tableauDeBord', 'messagerie', 'notifications', 'maJournee', 'agenda', 'taches', 'rapports']))
      expect(widgetsOuverts(PROFILS[role])[0], role).toBe('maJournee')
    }
    // Correspondant (compte d'institution) : rien de l'application interne hormis l'accueil.
    expect(cles(fonctionnalitesOuvertes(PROFILS.ROLE_CORRESPONDANT))).toEqual(['tableauDeBord'])
    expect(widgetsOuverts(PROFILS.ROLE_CORRESPONDANT)).toEqual([])
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
    ['ROLE_PRODUCTION_MONETIQUE', ['maJournee', 'monTravail']],
    ['ROLE_POLE_RESEAU', ['maJournee', 'monTravail']],
    ['ROLE_POLE_SYSTEME', ['maJournee', 'monTravail']],
    ['ROLE_COLLABORATEUR', ['maJournee']],
    ['ROLE_RESPONSABLE_SERVICE', ['maJournee']],
  ])('%s : widgets %j', (role, attendus) => {
    expect(widgetsOuverts(PROFILS[role])).toEqual(attendus)
  })

  it('plateforme : administrateur système sans administration des comptes ; RSSI en lecture de l’infrastructure', () => {
    const admin = cles(fonctionnalitesOuvertes(PROFILS.ROLE_ADMIN_SYSTEME))
    expect(admin).toEqual(['tableauDeBord', 'messagerie', 'notifications', 'maJournee', 'agenda', 'taches', 'rapports', 'processus', 'supervision', 'infrastructure', 'configuration', 'journalisation', 'adminConsole'])
    expect(cles(fonctionnalitesOuvertes(PROFILS.ROLE_RSSI))).toEqual(expect.arrayContaining(['adminComptes', 'supervision', 'infrastructure', 'configuration', 'journalisation', 'courriels']))
  })

  it('barre latérale : groupes non vides dans l’ordre du registre, prédicats repris', () => {
    const nav = navigationDepuis()
    expect(nav.map((g) => g.libelleKey ?? '')).toEqual(['', 'nav.groupe.monActivite', 'nav.groupe.registre', 'nav.groupe.homologation', 'nav.groupe.administration', 'nav.groupe.plateforme'])
    const entrees = nav.flatMap((g) => g.entrees)
    expect(entrees.map((e) => e.cle)).toEqual(cles(FONCTIONNALITES))
    expect(entrees.filter((e) => satisfait(PROFILS.ROLE_ADMIN_SYSTEME, e.predicat)).map((e) => e.cle)).toEqual(cles(fonctionnalitesOuvertes(PROFILS.ROLE_ADMIN_SYSTEME)))
    expect(navigationDepuis([{ cle: 'x', libelleKey: 'x', icone: 'gauge', chemin: '/des/x', groupe: 'plateforme' }])).toEqual([
      { libelleKey: 'nav.groupe.plateforme', entrees: [{ cle: 'x', libelleKey: 'x', icone: 'gauge', chemin: '/des/x', predicat: undefined }] },
    ])
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
