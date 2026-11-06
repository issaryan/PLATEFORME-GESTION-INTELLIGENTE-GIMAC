import { describe, expect, it } from 'vitest'
import { servicesSimules } from '.'
import { magasinPouvoirs } from './pouvoirs'
import { etapeVierge } from '../../modele/processus'

const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: { status: number; code: string }) => `${e.status} ${e.code}`)

// D-138 : étape adressée à un rôle, vue et faisable par ses porteurs sans prise. Les services simulés gardent les tâches
// par session : la visibilité croisée entre porteurs est prouvée sur MS-PILOTAGE (ProcessusRoleIT).
describe('D-138 : tâche pour un rôle (services simulés)', () => {
  it('porteur : « Mes tâches » la montre pour le rôle, il la termine sans prise, un second acte reçoit 409', async () => {
    const magasin = magasinPouvoirs()
    magasin.organisation.push({ id: 'u-res-bis', nomAffiche: 'Armel Nkoulou Beti', fonction: 'Technicien réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info', actif: true })
    magasin.definirProfils('u-res-bis', ['ROLE_POLE_RESEAU'])
    const s = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-res', nom: 'Idriss Mahamat Saleh', profils: magasin.profilsDe('u-res') }, pouvoirs: magasin })
    const chef = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-resp-info', nom: 'Gaston Ndzana Ebode' }, pouvoirs: magasin })
    const p = await chef.processus.creer({ unite: 'POLE_RESEAU', libelle: 'Demande au rôle', description: '', categorie: '', delaiCibleJours: null,
      initiateurs: { regles: ['MEMBRES_UNITE'], unites: [], profils: [], permissions: [], initiateurIntervenant: true },
      etapes: [{ ...etapeVierge(1), code: 'TRAITER', libelle: 'Traiter', intervenant: { mode: 'PROFIL', profil: 'ROLE_POLE_RESEAU' } }] })
    await chef.processus.publier(p.id, { leverAvertissements: true, motif: 'Essai' })
    const i = await s.processus.lancer(p.id, { intitule: 'Au rôle', champs: {}, designations: {} })
    const etape = i.etapes[0].code
    expect(i.etapes[0]).toMatchObject({ executant: null, aPrendre: { profil: 'ROLE_POLE_RESEAU' } })
    const tache = (await s.pilotage.taches({ vue: 'moi' })).contenu.find((t) => t.id === i.etapes[0].tacheId)
    expect(tache).toMatchObject({ pourRole: true, profilRequis: 'ROLE_POLE_RESEAU', etat: 'A_PRENDRE' })
    expect((await s.processus.terminer(i.id, etape)).etapes[0].etat).not.toBe('ACTIVE')
    expect(await code(s.processus.terminer(i.id, etape))).toMatch(/^409 /)
    expect((await s.pilotage.taches({ vue: 'moi' })).contenu.some((t) => t.id === tache!.id && t.etat !== 'TERMINEE')).toBe(false)
  })
})
