import { ApiError, avatarsSimules } from '@gimac/socle'
import type { Conversation, Correspondant, EtiquetteLiaison, Message, PieceJointe } from '../../modele/messagerie'
import { LIMITES_MESSAGERIE } from '../../modele/messagerie'
import type { ServiceMessagerie } from '../ports'

// Messagerie simulée (D-64) : mêmes formes de réponse et mêmes codes de refus que MS-COLLABORATION
// (docs/lot1/messagerie.md). Ses règles ne sont qu'une imitation : membres seuls, conversation directe
// réutilisée, masquage des numéros de carte (RG-52), bornes du contrat, non-lus par membre.

type Attendre = <T>(v: T | (() => T)) => Promise<T>
type Refus = (status: number, code: string, message: string) => ApiError

/** Annuaire de démonstration : comptes internes actifs du GIMAC (identifiants des profils de démonstration). */
export const ANNUAIRE: Correspondant[] = [
  { id: 'U-001', nom: 'Paul Mbarga Atangana', fonction: 'Chef du département DES', profils: ['ROLE_CHEF_DES'] },
  { id: 'U-020', nom: 'Bertrand Nguema Obiang', fonction: 'Chef de la DDRM', profils: ['ROLE_CHEF_DDRM'] },
  { id: 'sim-charge', nom: 'Fatimé Abakar Hissein', fonction: 'Chargée de la DDRM', profils: ['ROLE_CHARGE_DDRM'] },
  { id: 'U-201', nom: 'Aïcha Mahamat Nour', fonction: 'Conductrice de projet', profils: ['ROLE_CONDUCTEUR_PROJET'] },
  { id: 'U-101', nom: 'Joël Ekotto Mballa', fonction: 'Ingénieur d’homologation', profils: ['ROLE_INGENIEUR_HOMOLOGATION'] },
  { id: 'u-sna', nom: 'Rachel Biyong Mengue', fonction: 'Normes et Agréments', profils: ['ROLE_SNA'] },
  { id: 'u-prod', nom: 'Serge Ondoua Essomba', fonction: 'Production monétique', profils: ['ROLE_PRODUCTION_MONETIQUE'] },
  { id: 'u-res', nom: 'Idriss Mahamat Saleh', fonction: 'Pôle réseau', profils: ['ROLE_POLE_RESEAU'] },
  { id: 'u-sys', nom: 'Chantal Ngo Bassong', fonction: 'Pôle système', profils: ['ROLE_POLE_SYSTEME'] },
  { id: 'u-rssi', nom: 'Hervé Kamga Tchoupo', fonction: 'Responsable de la sécurité des systèmes d’information', profils: ['ROLE_RSSI'] },
  { id: 'u-conf', nom: 'Mireille Obame Nze', fonction: 'Conformité', profils: ['ROLE_CONFORMITE'] },
  { id: 'u-dg', nom: 'Jean-Baptiste Ella Mintsa', fonction: 'Direction générale', profils: ['ROLE_DG'] },
]

/** Comptes de l'annuaire qui ont déposé une photo de profil (démonstration) ; les autres gardent leurs initiales. */
export const PHOTOS_DEMO = ['U-020', 'sim-charge', 'U-201', 'u-sna', 'u-rssi', 'u-conf', 'u-dg']

/** Version courante de la photo d'une personne (photos de démonstration et dépôts faits dans « Mon compte »). */
const avecPhoto = <P extends Correspondant>(p: P): P => ({ ...p, avatarVersion: avatarsSimules.version(p.id) })

const LIAISON = /^LIA-\d{4}-\d{3,6}$/
const TYPES = new Set(LIMITES_MESSAGERIE.types.split(','))
const extension = (nom: string) => nom.slice(nom.lastIndexOf('.')).toLowerCase()

/** RG-52 : une suite de 13 à 19 chiffres (espaces ou tirets admis) passant le contrôle de Luhn est masquée (6 + 4). */
export function masquerNumerosCarte(texte: string): string {
  return texte.replace(/\b\d(?:[ -]?\d){12,18}\b/g, (brut) => {
    const chiffres = brut.replace(/\D/g, '')
    if (chiffres.length < 13 || chiffres.length > 19 || !luhn(chiffres)) return brut
    return chiffres.slice(0, 6) + '*'.repeat(chiffres.length - 10) + chiffres.slice(-4)
  })
}

function luhn(chiffres: string): boolean {
  let somme = 0
  for (let i = 0; i < chiffres.length; i++) {
    let d = Number(chiffres[chiffres.length - 1 - i])
    if (i % 2 === 1) d = d * 2 > 9 ? d * 2 - 9 : d * 2
    somme += d
  }
  return somme % 10 === 0
}

type EtatConversation = Omit<Conversation, 'dernierMessage' | 'nonLus'> & { messages: Message[]; lus: Map<string, number> }

export function messagerieSimulee(attendre: Attendre, refus: Refus, moi: Correspondant = ANNUAIRE[0]): ServiceMessagerie {
  let sequence = 0
  PHOTOS_DEMO.forEach((p) => avatarsSimules.demo(p))
  const id = (prefixe: string) => `${prefixe}-${(++sequence).toString().padStart(4, '0')}`
  const ilYA = (minutes: number) => new Date(Date.UTC(2026, 8, 29, 16, 0) - minutes * 60_000).toISOString()
  const personne = (pid: string) => ANNUAIRE.find((p) => p.id === pid) ?? (pid === moi.id ? moi : undefined)
  const pieces = new Map<string, { piece: PieceJointe; blob: Blob; conversationId: string }>()
  const conversations: EtatConversation[] = []

  const ajouterMessage = (c: EtatConversation, auteur: Correspondant, contenu: string, envoyeLe: string, etiquettes: EtiquetteLiaison[] = [], fichiers: File[] = []): Message => {
    const m: Message = {
      id: id('MSG'), conversationId: c.id, auteur, moi: auteur.id === moi.id, contenu: masquerNumerosCarte(contenu.trim()), envoyeLe, etiquettes,
      piecesJointes: fichiers.map((f) => {
        const piece: PieceJointe = { id: id('PJ'), nom: f.name, taille: f.size, typeMime: f.type || 'application/octet-stream', empreinte: `sim${f.size.toString(16).padStart(8, '0')}` }
        pieces.set(piece.id, { piece, blob: f, conversationId: c.id })
        return piece
      }),
    }
    c.messages.push(m)
    c.misAJourLe = envoyeLe
    // L'auteur a lu son propre message.
    c.lus.set(auteur.id, c.messages.length)
    return m
  }
  const creer = (membres: Correspondant[], titre: string | null, liaison: EtiquetteLiaison | null, creeeLe: string): EtatConversation => {
    const c: EtatConversation = { id: id('CONV'), type: membres.length === 2 && !titre && !liaison ? 'DIRECTE' : 'GROUPE', titre, liaison, membres, messages: [], lus: new Map(), misAJourLe: creeeLe, creeeLe }
    conversations.push(c)
    return c
  }

  // Jeu de démonstration : trois conversations où figure l'utilisateur courant, dont une rattachée à une liaison.
  const autre = (pid: string) => (pid === moi.id ? ANNUAIRE.find((p) => p.id !== moi.id)! : personne(pid)!)
  const cond = autre('U-201')
  const sna = autre('u-sna')
  const rssi = autre('u-rssi')
  const d1 = creer([moi, cond], null, null, ilYA(300))
  ajouterMessage(d1, cond, 'Bonjour, le procès-verbal de LIA-2026-027 est établi à 100 %. Pouvez-vous le signer aujourd’hui ?', ilYA(290), [{ liaisonId: 'LIA-2026-027', reference: 'LIA-2026-027' }])
  ajouterMessage(d1, moi, 'Bien reçu, je le signe cet après-midi.', ilYA(280))
  ajouterMessage(d1, cond, 'Merci. Le participant signera ensuite depuis son portail.', ilYA(45))
  const g1 = creer([moi, sna, cond, rssi], 'Essais de LIA-2026-030', { liaisonId: 'LIA-2026-030', reference: 'LIA-2026-030' }, ilYA(1500))
  ajouterMessage(g1, sna, 'Les essais sont suspendus : l’élément de sécurité du participant doit être renouvelé. Rapport joint.', ilYA(1490), [{ liaisonId: 'LIA-2026-030', reference: 'LIA-2026-030' }],
    [new File(['Rapport des essais'], 'rapport-essais-LIA-2026-030.pdf', { type: 'application/pdf' })])
  ajouterMessage(g1, rssi, 'Je prends contact avec le correspondant pour la nouvelle cérémonie.', ilYA(120))
  ajouterMessage(g1, sna, 'Le blocage est déclaré sur la liaison ; échéance de levée au 27 septembre.', ilYA(20))
  const d2 = creer([moi, rssi], null, null, ilYA(4000))
  ajouterMessage(d2, moi, 'Pouvez-vous confirmer les attestations PCI DSS attendues pour LIA-2026-019 ?', ilYA(3990), [{ liaisonId: 'LIA-2026-019', reference: 'LIA-2026-019' }])
  ajouterMessage(d2, rssi, 'Attestation reçue et en cours de vérification.', ilYA(3000))
  // Non-lus de démonstration : le dernier message de d1 et les deux derniers de g1.
  d1.lus.set(moi.id, 2)
  g1.lus.set(moi.id, 1)
  d2.lus.set(moi.id, 2)

  const nonLusDe = (c: EtatConversation) => c.messages.length - Math.min(c.lus.get(moi.id) ?? 0, c.messages.length)
  const vue = (c: EtatConversation): Conversation => {
    const dernier = c.messages[c.messages.length - 1]
    const { messages: _m, lus: _l, ...reste } = c
    const copie = structuredClone(reste)
    return {
      ...copie,
      membres: copie.membres.map(avecPhoto),
      dernierMessage: dernier ? { id: dernier.id, auteurNom: dernier.auteur.nom, extrait: dernier.contenu.slice(0, 120), envoyeLe: dernier.envoyeLe, piecesJointes: dernier.piecesJointes.length } : null,
      nonLus: nonLusDe(c),
    }
  }
  const sienne = (cid: string) => {
    const c = conversations.find((x) => x.id === cid && x.membres.some((m) => m.id === moi.id))
    if (!c) throw refus(404, 'COL_CONVERSATION_NOT_FOUND', 'Conversation introuvable.')
    return c
  }
  const valider = (contenu: string, etiquettes: EtiquetteLiaison[], fichiers: File[]) => {
    if (!contenu.trim() && fichiers.length === 0) throw refus(422, 'COL_MESSAGE_EMPTY', 'Le message est vide.')
    if (contenu.trim().length > LIMITES_MESSAGERIE.contenuMax) throw refus(422, 'COL_MESSAGE_TOO_LONG', 'Le message dépasse 4 000 caractères.')
    if (etiquettes.length > LIMITES_MESSAGERIE.etiquettesMax) throw refus(422, 'COL_TOO_MANY_TAGS', 'Dix liaisons au plus peuvent être citées.')
    if (etiquettes.some((e) => !LIAISON.test(e.reference))) throw refus(422, 'COL_TAG_INVALID', 'Référence de liaison invalide.')
    if (fichiers.length > LIMITES_MESSAGERIE.piecesMax) throw refus(422, 'COL_TOO_MANY_ATTACHMENTS', 'Cinq pièces jointes au plus.')
    if (fichiers.some((f) => f.size > LIMITES_MESSAGERIE.tailleMaxOctets)) throw refus(422, 'COL_ATTACHMENT_TOO_LARGE', 'Une pièce jointe dépasse 20 Mo.')
    if (fichiers.some((f) => !TYPES.has(extension(f.name)))) throw refus(422, 'COL_ATTACHMENT_TYPE', 'Type de pièce jointe non admis.')
  }
  const miennes = () => conversations.filter((c) => c.membres.some((m) => m.id === moi.id)).sort((a, b) => b.misAJourLe.localeCompare(a.misAJourLe))

  return {
    annuaire: (recherche, page = 0, taille = 20) => attendre(() => {
      const r = recherche.trim().toLowerCase()
      const l = ANNUAIRE.filter((p) => p.id !== moi.id && (!r || `${p.nom} ${p.fonction ?? ''}`.toLowerCase().includes(r)))
      return { contenu: l.slice(page * taille, (page + 1) * taille).map(avecPhoto), total: l.length, page, taille }
    }),
    conversations: (q) => attendre(() => {
      const r = (q.recherche ?? '').trim().toLowerCase()
      const l = miennes()
        .filter((c) => !q.liaisonId || c.liaison?.liaisonId === q.liaisonId || c.messages.some((m) => m.etiquettes.some((e) => e.liaisonId === q.liaisonId)))
        .filter((c) => !r || [c.titre ?? '', c.liaison?.reference ?? '', ...c.membres.map((m) => m.nom), ...c.messages.map((m) => m.contenu)].join(' ').toLowerCase().includes(r))
      const page = q.page ?? 0
      const taille = q.taille ?? 20
      return { contenu: l.slice(page * taille, (page + 1) * taille).map(vue), total: l.length, page, taille }
    }),
    conversation: (cid) => attendre(() => vue(sienne(cid))),
    messages: (cid, avant) => attendre(() => {
      const c = sienne(cid)
      const fin = avant ? c.messages.findIndex((m) => m.id === avant) : c.messages.length
      const debut = Math.max(0, fin - 50)
      return { contenu: structuredClone(c.messages.slice(debut, fin)).map((m) => ({ ...m, auteur: avecPhoto(m.auteur) })), plusAnciens: debut > 0 }
    }),
    demarrer: (n) => attendre(() => {
      const destinataires = [...new Set(n.destinataires)]
      if (destinataires.length === 0) throw refus(422, 'COL_RECIPIENT_REQUIRED', 'Choisissez au moins un destinataire.')
      if (destinataires.includes(moi.id)) throw refus(422, 'COL_RECIPIENT_SELF', 'Vous ne pouvez pas vous écrire à vous-même.')
      if (destinataires.length > LIMITES_MESSAGERIE.destinatairesMax) throw refus(422, 'COL_TOO_MANY_RECIPIENTS', 'Cinquante destinataires au plus.')
      const membres = destinataires.map((d) => {
        const p = personne(d)
        if (!p) throw refus(422, 'COL_RECIPIENT_UNKNOWN', 'Destinataire inconnu de l’annuaire.')
        return p
      })
      valider(n.contenu, n.etiquettes, n.fichiers)
      const titre = n.titre?.trim() || null
      const liaison = n.liaisonId ? { liaisonId: n.liaisonId, reference: n.liaisonId } : null
      // Deux personnes, sans titre ni liaison : la conversation directe existante est réutilisée.
      const existante = !titre && !liaison && membres.length === 1
        ? conversations.find((c) => c.type === 'DIRECTE' && c.membres.some((m) => m.id === moi.id) && c.membres.some((m) => m.id === membres[0].id))
        : undefined
      const c = existante ?? creer([moi, ...membres], titre, liaison, new Date().toISOString())
      ajouterMessage(c, moi, n.contenu, new Date().toISOString(), n.etiquettes, n.fichiers)
      return vue(c)
    }),
    envoyer: (cid, e) => attendre(() => {
      const c = sienne(cid)
      valider(e.contenu, e.etiquettes, e.fichiers)
      const m = structuredClone(ajouterMessage(c, moi, e.contenu, new Date().toISOString(), e.etiquettes, e.fichiers))
      return { ...m, auteur: avecPhoto(m.auteur) }
    }),
    marquerLu: (cid) => attendre(() => {
      const c = sienne(cid)
      c.lus.set(moi.id, c.messages.length)
    }),
    nonLus: () => attendre(() => ({ total: miennes().reduce((n, c) => n + nonLusDe(c), 0) })),
    // Le contenu n'est pas copié (un Blob ne se clone pas) : contrôle d'accès simulé, puis le fichier tel quel.
    telechargerPiece: (pid) => attendre(() => {
      const p = pieces.get(pid)
      if (!p) throw refus(404, 'COL_ATTACHMENT_NOT_FOUND', 'Pièce jointe introuvable.')
      sienne(p.conversationId)
      return pid
    }).then((ok) => ({ blob: pieces.get(ok)!.blob, empreinte: pieces.get(ok)!.piece.empreinte })),
  }
}
