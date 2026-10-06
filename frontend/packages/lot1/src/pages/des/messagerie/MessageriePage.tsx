import { createContext, useContext, useEffect, useMemo, useRef, useState, type CSSProperties, type KeyboardEvent, type PointerEvent, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { AttachmentViewer, Avatar, Button, ConfirmDialog, ConversationList, Dialog, EmptyState, Icon, IconButton, InlineAlert, Link, MessageComposer, MessageThread, PageHeader, type EnvoiPiece, type MembreMention, Pagination, Presence, SearchInput, SearchSelect, Skeleton, Tag, TextField, type ThreadMessage, Note } from '@gimac/design-system'
import { aPermission, urlAvatar, useBarreLaterale, useEnLigne, useLectureConversation, usePermissions, usePresence, useSaisies, useSession, type ApiError } from '@gimac/socle'
import type { Conversation, Correspondant, EtiquetteLiaison, EvenementGroupe, LectureMessage, MembreMentionne, Message, PieceJointe } from '../../../modele/messagerie'
import { LIMITES_MESSAGERIE, MOTIF_LIAISON } from '../../../modele/messagerie'
import { useAccusesLecture, useAjouterMembres, useAnnuaire, useConversation, useConversations, useDemarrer, useEnvoyer, useFil, useLecturesMessage, useMarquerLu, usePresenceMessagerie, useRenommerGroupe, useRetirerMembre, useSignalerSaisie, useTelechargerPiece, useTransferer } from '../../../hooks/messagerie'
import { useLiaisonsParcours } from '../../../hooks/parcours'
import { PERM } from '../../../permissions'
import { enregistrerFichier } from '../../../ressources/fichier'
import { useServices } from '../../../services/ServicesProvider'

// Messagerie instantanée de travail (D-64, /des/messagerie) : conversations à gauche, fil ou nouveau message
// à droite, comme un client de messagerie. Les destinataires viennent de l'annuaire de la plateforme ; une
// liaison citée devient un lien vers son dossier ; le service applique les droits, le masquage RG-52 et les bornes.
// Adresse : ?conversation=<id> ouvre un fil ; ?nouveau=1[&liaison=<id>][&destinataire=<id>] prépare un message
// direct ; ?nouveau=groupe ouvre la création d'un groupe de discussion nommé (D-68), géré ensuite par son
// administrateur (nom, membres) ; les pièces jointes s'ouvrent dans une fenêtre d'aperçu.
// Ouverte à tout membre interne du GIMAC, depuis le DES et depuis la console (RSSI, super administrateur) :
// dans la console, les liaisons ne se citent pas (la couche n'y a pas accès), leurs liens mènent au DES.
// Comme une messagerie grand public (D-72) : point vert des collègues en ligne, « Vu … » sinon, et
// « … écrit » dans la liste et sous le fil pendant qu'un membre tape.

/** Couche de présentation qui affiche la messagerie : préfixe des photos, citation des liaisons. */
type CoucheMessagerie = 'des' | 'console'
const Couche = createContext<CoucheMessagerie>('des')

const PAR_PAGE = 20
const taille = (octets: number) => (octets < 1024 * 1024 ? `${Math.max(1, Math.round(octets / 1024))} Ko`
  : octets < 1024 ** 3 ? `${(octets / 1024 / 1024).toFixed(1).replace('.', ',')} Mo`
  : `${(octets / 1024 ** 3).toFixed(1).replace('.', ',')} Go`)

function useQuand() {
  const { i18n } = useTranslation()
  return (iso: string) => {
    const d = new Date(iso)
    const jour = new Date().toDateString() === d.toDateString()
    return new Intl.DateTimeFormat(i18n.language, jour ? { hour: '2-digit', minute: '2-digit' } : { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }).format(d)
  }
}

/** Photo de profil d'une personne de l'annuaire (docs/lot1/mon-compte.md §1), initiales à défaut. */
function usePhoto() {
  const couche = useContext(Couche)
  return (p: Pick<Correspondant, 'id' | 'avatarVersion'> | undefined) => (p ? urlAvatar(couche, p.id, p.avatarVersion) : undefined)
}

/**
 * « Aminata écrit… » d'une conversation (D-72) : prénoms des membres qui écrivent ; « écrit… » seul dans
 * la liste pour un échange direct, comme une messagerie grand public.
 */
function useTexteSaisie() {
  const { t } = useTranslation()
  const saisies = useSaisies()
  return (c: Conversation, moi: string | undefined, liste = false): string | undefined => {
    const noms = saisies(c.id)
      .filter((id) => id !== moi)
      .map((id) => c.membres.find((m) => m.id === id)?.nom.split(' ')[0])
      .filter((n): n is string => Boolean(n))
    if (noms.length === 0) return undefined
    if (liste && c.type !== 'GROUPE') return t('msg.ecritDirect')
    return noms.length === 1 ? t('msg.ecrit', { nom: noms[0] }) : t('msg.ecrivent', { noms: noms.join(', ') })
  }
}

/** Interlocuteur d'un échange direct : l'autre membre (sa photo illustre la conversation). */
export function interlocuteur(c: Conversation, moi: string | undefined): Correspondant | undefined {
  return c.type === 'DIRECTE' && !c.titre ? c.membres.find((m) => m.id !== moi) : undefined
}

/** Titre d'une conversation : son objet, sinon l'autre membre (échange direct), sinon les membres. */
export function titreConversation(c: Conversation, moi: string | undefined): string {
  if (c.titre) return c.titre
  const autres = c.membres.filter((m) => m.id !== moi)
  return (autres.length ? autres : c.membres).map((m) => m.nom).join(', ')
}

/**
 * Référence de liaison : lien vers son dossier dans le DES ; texte simple dans la console, servie sur une autre
 * origine et sans accès aux liaisons.
 */
function RefLiaison({ href, children }: { href: string; children: string }) {
  return useContext(Couche) === 'des' ? <Link href={href} mono>{children}</Link> : <span className="gm-mono">{children}</span>
}

/**
 * Corps d'un message : les références de liaison citées deviennent des liens vers leur dossier ; les mentions
 * (D-82) « @Prénom Nom » des membres mentionnés sont mises en évidence, la sienne plus marquée. Le texte n'est
 * jamais interprété comme du HTML : seuls des nœuds texte et des éléments React sont produits.
 */
export function CorpsMessage({ contenu, etiquettes, mentions = [], moi }: { contenu: string; etiquettes: EtiquetteLiaison[]; mentions?: MembreMentionne[]; moi?: string }) {
  type Repere = { debut: number; fin: number; rendu: (cle: number) => ReactNode }
  const reperes: Repere[] = []
  for (const m of contenu.matchAll(MOTIF_LIAISON)) {
    const debut = m.index ?? 0
    const e = etiquettes.find((x) => x.reference === m[0])
    reperes.push({ debut, fin: debut + m[0].length, rendu: (cle) => <RefLiaison key={cle} href={e ? `/des/parcours/${encodeURIComponent(e.liaisonId)}` : `/des/parcours?q=${encodeURIComponent(m[0])}`}>{m[0]}</RefLiaison> })
  }
  // Noms les plus longs d'abord : « @Paul Mbarga Atangana » avant un éventuel « @Paul Mbarga ».
  for (const p of [...mentions].sort((a, b) => b.nom.length - a.nom.length)) {
    const texte = `@${p.nom}`
    for (let i = contenu.indexOf(texte); i >= 0; i = contenu.indexOf(texte, i + texte.length)) {
      if (reperes.some((r) => i < r.fin && i + texte.length > r.debut)) continue
      reperes.push({ debut: i, fin: i + texte.length, rendu: (cle) => <span key={cle} className={p.id === moi ? 'gm-mention gm-mention-moi' : 'gm-mention'}>{texte}</span> })
    }
  }
  reperes.sort((a, b) => a.debut - b.debut)
  const morceaux: ReactNode[] = []
  let dernier = 0
  for (const r of reperes) {
    if (r.debut > dernier) morceaux.push(contenu.slice(dernier, r.debut))
    morceaux.push(r.rendu(r.debut))
    dernier = r.fin
  }
  if (dernier < contenu.length) morceaux.push(contenu.slice(dernier))
  return <>{morceaux}</>
}

function Etiquettes({ etiquettes }: { etiquettes: EtiquetteLiaison[] }) {
  if (!etiquettes.length) return null
  return (
    <>
      {etiquettes.map((e) => (
        <RefLiaison key={e.liaisonId} href={`/des/parcours/${encodeURIComponent(e.liaisonId)}`}>{`# ${e.reference}`}</RefLiaison>
      ))}
    </>
  )
}

function useMessageErreur() {
  const { t } = useTranslation()
  return (e: ApiError | null | undefined) => (e ? t(`msg.erreur.${e.code}`, { defaultValue: e.message || t('msg.erreur.envoi') }) : undefined)
}

/** Noms joints : « A », « A et B », « A, B et C ». */
function useJoindre() {
  const { t } = useTranslation()
  return (noms: string[]) => (noms.length < 2 ? noms.join('') : `${noms.slice(0, -1).join(', ')}${t('msg.evt.et')}${noms[noms.length - 1]}`)
}

/** Texte d'un événement de groupe (D-68), à la première personne quand l'utilisateur en est l'auteur. */
export function useTexteEvenement() {
  const { t } = useTranslation()
  const joindre = useJoindre()
  return (e: EvenementGroupe, auteurNom: string, moi: boolean) =>
    t(moi ? `msg.evt.moi.${e.type}` : `msg.evt.${e.type}`, { auteur: auteurNom, cibles: joindre(e.cibles) })
}

/**
 * Annuaire de la plateforme (D-68) : comptes internes actifs, réels (MS-IAM), cherchés par nom ou fonction,
 * avec photo et fonction ; choix d'une personne (message direct) ou de plusieurs (groupe).
 */
export function Annuaire({ choisis, onChoisis, multiple, exclus = [] }: { choisis: Correspondant[]; onChoisis: (c: Correspondant[]) => void; multiple: boolean; exclus?: string[] }) {
  const { t } = useTranslation()
  const photo = usePhoto()
  const erreur = useMessageErreur()
  const [q, setQ] = useState('')
  const [differee, setDifferee] = useState('')
  useEffect(() => {
    const d = window.setTimeout(() => setDifferee(q.trim()), 250)
    return () => window.clearTimeout(d)
  }, [q])
  const annuaire = useAnnuaire(differee)
  const personnes = (annuaire.data?.contenu ?? []).filter((p) => !exclus.includes(p.id))
  const choisi = (id: string) => choisis.some((c) => c.id === id)
  const basculer = (p: Correspondant) => {
    if (multiple) onChoisis(choisi(p.id) ? choisis.filter((c) => c.id !== p.id) : [...choisis, p])
    else onChoisis(choisi(p.id) ? [] : [p])
  }
  return (
    <section className="gm-annuaire" aria-label={t('msg.annuaire')}>
      <SearchInput label={t('msg.annuaire.recherche')} value={q} onChange={setQ} />
      <span className="gm-field-help">{t('msg.annuaire.aide')}</span>
      {multiple && choisis.length ? (
        <div className="gm-row gm-annuaire-choisis" aria-live="polite">
          <span className="gm-field-help">{t('msg.annuaire.selection', { count: choisis.length })}</span>
          {choisis.map((c) => <Tag key={c.id} onRemove={() => onChoisis(choisis.filter((x) => x.id !== c.id))}>{c.nom}</Tag>)}
        </div>
      ) : null}
      <ul className="gm-annuaire-liste" aria-label={t('msg.annuaire')} aria-busy={annuaire.isFetching}>
        {annuaire.isPending ? <li><Skeleton height={160} /></li> : annuaire.isError ? <li><InlineAlert level="crit">{erreur(annuaire.error) ?? ''}</InlineAlert></li> : personnes.length === 0 ? (
          <li className="gm-annuaire-vide">{t('msg.annuaire.aucun')}</li>
        ) : personnes.map((p) => (
          <li key={p.id}>
            <button type="button" className="gm-annuaire-personne" aria-pressed={choisi(p.id)} onClick={() => basculer(p)}>
              <Avatar name={p.nom} src={photo(p)} size="sm" decorative />
              <span className="gm-annuaire-identite">
                <strong>{p.nom}</strong>
                {p.fonction ? <span className="gm-field-help">{p.fonction}</span> : null}
              </span>
              {choisi(p.id) ? <span className="gm-annuaire-coche"><Icon name="check" size={16} /><span className="gm-sr-only">{t('msg.annuaire.choisi')}</span></span> : null}
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}

/** Choix des liaisons citées : recherche dans la liste des liaisons, étiquettes retirables. */
function CiterLiaisons({ valeur, onChange }: { valeur: EtiquetteLiaison[]; onChange: (e: EtiquetteLiaison[]) => void }) {
  const { t } = useTranslation()
  const [q, setQ] = useState('')
  const liaisons = useLiaisonsParcours({ page: 1, taille: 8, recherche: q.trim() || undefined })
  const options = (liaisons.data?.contenu ?? []).filter((l) => !valeur.some((e) => e.liaisonId === l.id)).map((l) => ({ value: l.id, code: l.reference, label: l.institutionNom }))
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
      {valeur.length ? (
        <div className="gm-row" style={{ flexWrap: 'wrap', gap: 'var(--gm-space-1)' }}>
          {valeur.map((e) => <Tag key={e.liaisonId} onRemove={() => onChange(valeur.filter((x) => x.liaisonId !== e.liaisonId))}>{`# ${e.reference}`}</Tag>)}
        </div>
      ) : null}
      {valeur.length < LIMITES_MESSAGERIE.etiquettesMax ? (
        <SearchSelect
          label={t('msg.citer')}
          help={t('msg.citerAide')}
          query={q}
          onQuery={setQ}
          loading={liaisons.isFetching}
          options={options}
          onChange={(id) => {
            const l = liaisons.data?.contenu.find((x) => x.id === id)
            if (l) onChange([...valeur, { liaisonId: l.id, reference: l.reference }])
            setQ('')
          }}
        />
      ) : null}
    </div>
  )
}

/** Zone de saisie commune au fil et au nouveau message : texte, pièces jointes, liaisons citées. */
/** Pièce envoyée par morceaux depuis la zone de saisie (D-74) : son avancement, puis son ticket. */
type PieceEnvoyee = { cle: string; fichier: File; phase: EnvoiPiece['phase']; envoye: number; documentId?: string; erreur?: string; arret: AbortController }

/**
 * Zone de saisie : chaque pièce choisie part aussitôt par morceaux (claim check, D-74), avec sa progression ;
 * « Envoyer » attend qu'elles soient prêtes, puis le message ne porte que leurs tickets (`documents`).
 * `reference` : la conversation, ou « nouvelle-conversation » avant sa création.
 */
function Saisie({ envoi, erreur, onEnvoyer, onFrappe, reference, etiquettesInitiales = [], reponse, onAnnulerReponse, membres = [] }: {
  envoi: boolean
  erreur?: string
  onEnvoyer: (e: { contenu: string; fichiers: File[]; documents: string[]; etiquettes: EtiquetteLiaison[]; reponseA?: string | null; mentions?: string[] }, vider: () => void) => void
  /** Frappe dans le champ (D-72) : signal « en train d'écrire », limité par l'appelant. */
  onFrappe?: () => void
  reference: string
  etiquettesInitiales?: EtiquetteLiaison[]
  /** Message auquel on répond (D-82) : cité au-dessus du champ, envoyé en « reponseA ». */
  reponse?: Message | null
  onAnnulerReponse?: () => void
  /** Membres mentionnables avec « @ » (D-82), l'utilisateur exclu. */
  membres?: Correspondant[]
}) {
  const { t } = useTranslation()
  const services = useServices()
  const photo = usePhoto()
  // Mentions choisies dans la liste ; à l'envoi, seules restent celles dont « @Nom » figure encore au texte.
  const [mentions, setMentions] = useState<MembreMentionne[]>([])
  const messageErreur = useMessageErreur()
  const [texte, setTexte] = useState('')
  const [pieces, setPieces] = useState<PieceEnvoyee[]>([])
  const [etiquettes, setEtiquettes] = useState<EtiquetteLiaison[]>(etiquettesInitiales)
  const [citer, setCiter] = useState(etiquettesInitiales.length > 0)
  const citable = useContext(Couche) === 'des'
  // Envois encore en cours au démontage (conversation quittée) : abandonnés côté serveur.
  const enCours = useRef(pieces)
  enCours.current = pieces
  useEffect(() => () => enCours.current.forEach((p) => p.phase !== 'pret' && p.arret.abort()), [])
  const maj = (cle: string, m: Partial<PieceEnvoyee>) => setPieces((l) => l.map((p) => (p.cle === cle ? { ...p, ...m } : p)))
  const ajouter = (fichiers: File[]) => {
    const nouvelles = fichiers.map((fichier): PieceEnvoyee => ({ cle: `${fichier.name}-${fichier.size}-${Math.random().toString(36).slice(2)}`, fichier, phase: 'envoi', envoye: 0, arret: new AbortController() }))
    setPieces((l) => [...l, ...nouvelles])
    nouvelles.forEach((p) => {
      if (p.fichier.size > LIMITES_MESSAGERIE.tailleMaxDepotOctets) {
        maj(p.cle, { phase: 'erreur', erreur: t('msg.depot.tropLourd') })
        return
      }
      // Type contrôlé avant tout envoi : un refus après plusieurs gigaoctets envoyés serait absurde.
      const ext = p.fichier.name.slice(p.fichier.name.lastIndexOf('.')).toLowerCase()
      if (!p.fichier.name.includes('.') || !LIMITES_MESSAGERIE.typesDepot.split(',').includes(ext)) {
        maj(p.cle, { phase: 'erreur', erreur: t('msg.erreur.COL_ATTACHMENT_TYPE') })
        return
      }
      services.messagerie
        .deposer(p.fichier, reference, { signal: p.arret.signal, surProgression: (g) => maj(p.cle, { phase: g.phase, envoye: g.envoye }) })
        .then((r) => maj(p.cle, { phase: 'pret', documentId: r.documentId, envoye: p.fichier.size }))
        .catch((e: unknown) => {
          if (e instanceof DOMException && e.name === 'AbortError') return
          maj(p.cle, { phase: 'erreur', erreur: messageErreur(e as ApiError) })
        })
    })
  }
  const retirer = (cle: string) => {
    setPieces((l) => {
      l.find((p) => p.cle === cle && p.phase !== 'pret')?.arret.abort()
      return l.filter((p) => p.cle !== cle)
    })
  }
  const vider = () => {
    setTexte('')
    setPieces([])
    setEtiquettes([])
    setCiter(false)
    setMentions([])
    onAnnulerReponse?.()
  }
  const mentionsRetenues = [...new Set(mentions.filter((m) => texte.includes(`@${m.nom}`)).map((m) => m.id))].slice(0, LIMITES_MESSAGERIE.mentionsMax)
  return (
    <MessageComposer
      value={texte}
      onChange={(v) => {
        setTexte(v)
        if (v.trim()) onFrappe?.()
      }}
      fichiers={[]}
      onFichiers={ajouter}
      maxFichiers={LIMITES_MESSAGERIE.piecesMax - pieces.length}
      accept={LIMITES_MESSAGERIE.typesDepot}
      envois={pieces.map((p) => ({
        cle: p.cle, nom: p.fichier.name, phase: p.phase, erreur: p.erreur,
        pourcent: p.fichier.size ? Math.floor((p.envoye / p.fichier.size) * 100) : 100,
        detail: t('msg.depot.detail', { envoye: taille(p.envoye), total: taille(p.fichier.size) }),
      }))}
      onAnnulerEnvoi={retirer}
      sending={envoi}
      error={erreur}
      onSend={() => onEnvoyer({
        contenu: texte, fichiers: [], documents: pieces.flatMap((p) => (p.documentId ? [p.documentId] : [])), etiquettes,
        ...(reponse ? { reponseA: reponse.id } : {}),
        ...(mentionsRetenues.length ? { mentions: mentionsRetenues } : {}),
      }, vider)}
      reponse={reponse && onAnnulerReponse ? {
        cle: reponse.id,
        auteur: reponse.moi ? t('msg.vous') : reponse.auteur.nom,
        extrait: reponse.contenu,
        pieces: reponse.piecesJointes.length,
        onAnnuler: onAnnulerReponse,
      } : undefined}
      membres={membres.map((m): MembreMention => ({ id: m.id, nom: m.nom, detail: m.fonction, avatar: photo(m) }))}
      onMention={(m) => setMentions((l) => (l.some((x) => x.id === m.id) ? l : [...l, { id: m.id, nom: m.nom }]))}
      extras={
        <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          {!citable ? null : citer ? <CiterLiaisons valeur={etiquettes} onChange={setEtiquettes} /> : (
            <span>
              <Button size="sm" variant="ghost" icon="route" onClick={() => setCiter(true)}>{t('msg.citer')}</Button>
            </span>
          )}
          <span className="gm-field-help">{t('msg.limitesDepot', { max: LIMITES_MESSAGERIE.piecesMax })}</span>
        </div>
      }
    />
  )
}

/**
 * Transfert d'une pièce (D-74) : choix d'une autre conversation ; le message transféré porte le ticket du
 * document, sans copie ni nouvel envoi.
 */
function TransfererPiece({ piece, depuis, onFermer, onTransfere }: { piece: PieceJointe; depuis: string; onFermer: () => void; onTransfere: (conversationId: string) => void }) {
  const { t } = useTranslation()
  const session = useSession()
  const [recherche, setRecherche] = useState('')
  const liste = useConversations({ recherche: recherche.trim() || undefined, page: 0 })
  const transferer = useTransferer()
  const erreur = useMessageErreur()
  const cibles = (liste.data?.contenu ?? []).filter((c) => c.id !== depuis)
  return (
    <Dialog open size="md" id="gm-transfert-piece" title={t('msg.transfert.titre')} subtitle={`${piece.nom} · ${taille(piece.taille)}`} onClose={onFermer}
      footer={<Button variant="ghost" onClick={onFermer}>{t('commun.annuler')}</Button>}>
      <div className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>
        <Note>{t('msg.transfert.aide')}</Note>
        <SearchInput label={t('msg.recherche')} value={recherche} onChange={setRecherche} />
        {transferer.isError ? <InlineAlert level="crit">{erreur(transferer.error)}</InlineAlert> : null}
        {liste.isPending ? <Skeleton height={160} /> : (
          <ConversationList
            empty={t('msg.aucune')}
            onSelect={(cid) => transferer.mutate({ conversationId: cid, pieceId: piece.id }, { onSuccess: () => onTransfere(cid) })}
            items={cibles.map((c) => ({ id: c.id, titre: titreConversation(c, session?.sujet), groupe: c.type === 'GROUPE', sousTitre: c.type === 'GROUPE' ? t('msg.membres', { count: c.membres.length }) : undefined }))}
          />
        )}
      </div>
    </Dialog>
  )
}

/**
 * Détail « Lu par » d'un de mes messages de groupe (D-88) : lecteurs avec l'heure de lecture (« heure inconnue »
 * pour une lecture antérieure aux accusés), puis les membres qui ne l'ont pas encore lu. Monté à l'ouverture.
 */
function LuPar({ conversationId, messageId }: { conversationId: string; messageId: string }) {
  const { t } = useTranslation()
  const quand = useQuand()
  const lectures = useLecturesMessage(conversationId, messageId)
  if (lectures.isPending) return <Skeleton height={48} />
  if (lectures.isError) return <span className="gm-field-help">{t('msg.lecture.erreur')}</span>
  const lus = lectures.data.filter((l) => l.lu)
  const autres = lectures.data.filter((l) => !l.lu)
  return (
    <>
      {lus.length ? (
        <ul className="gm-lu-par" aria-label={t('msg.lecture.lecteurs')}>
          {lus.map((l) => (
            <li key={l.membre.id}>
              <span className="gm-lu-par-nom">{l.membre.nom}</span>
              <span className="gm-lu-par-quand gm-num">{l.luLe ? quand(l.luLe) : t('msg.lecture.heureInconnue')}</span>
            </li>
          ))}
        </ul>
      ) : <span className="gm-field-help">{t('msg.lecture.personne')}</span>}
      {autres.length ? (
        <>
          <span className="gm-lu-par-sous">{t('msg.lecture.pasEncore')}</span>
          <ul className="gm-lu-par" aria-label={t('msg.lecture.pasEncore')}>
            {autres.map((l) => <li key={l.membre.id}><span className="gm-lu-par-nom">{l.membre.nom}</span></li>)}
          </ul>
        </>
      ) : null}
    </>
  )
}

/** Accusé de lecture d'un de mes messages pour le fil (D-88) : détail « Lu par » en groupe seulement. */
const accuse = (l: LectureMessage | null | undefined, groupe: boolean, detail: () => ReactNode) =>
  l ? { etat: l.etat, lus: l.lus, destinataires: l.destinataires, ...(groupe ? { detail: detail() } : {}) } : undefined

/** Pages plus anciennes chargées au plus pour retrouver un message cité ou désigné par un lien (D-82). */
const PAGES_RECHERCHE_MAX = 10

function Fil({ id, peutEcrire, onQuitte, onAller, cible, commandes }: {
  id: string; peutEcrire: boolean; onQuitte: () => void; onAller: (conversationId: string) => void
  /** Message à atteindre (lien d'une notification de mention ou de réponse, D-82). */
  cible?: string | null
  /** Commandes de l'espace (liste repliée, D-84), dans l'en-tête du fil. */
  commandes?: ReactNode
}) {
  const { t } = useTranslation()
  const [reponse, setReponse] = useState<Message | null>(null)
  // Message recherché (citation ouverte ou lien), puis mis en évidence un instant ; annonce s'il reste introuvable.
  const [recherche, setRecherche] = useState<string | null>(cible ?? null)
  const [evidence, setEvidence] = useState<string>()
  const [annonce, setAnnonce] = useState<string>()
  const pagesChargees = useRef(0)
  // Fil gardé en cache d'une visite précédente : relu une fois avant de chercher dans les pages plus anciennes.
  const relu = useRef(false)
  useEffect(() => {
    if (cible) setRecherche(cible)
  }, [cible])
  const texteEvenement = useTexteEvenement()
  const [gestion, setGestion] = useState(false)
  const [apercu, setApercu] = useState<{ piece: PieceJointe; blob?: Blob; erreur?: string } | null>(null)
  const [transfert, setTransfert] = useState<PieceJointe | null>(null)
  const services = useServices()
  const session = useSession()
  const quand = useQuand()
  const conversation = useConversation(id)
  const fil = useFil(id)
  const envoyer = useEnvoyer(id)
  const lu = useMarquerLu()
  const telecharger = useTelechargerPiece()
  const erreur = useMessageErreur()
  const photo = usePhoto()
  const couche = useContext(Couche)
  const frappe = useSignalerSaisie(id)
  const texteSaisie = useTexteSaisie()
  const enLigne = useEnLigne()
  const autre = conversation.data ? interlocuteur(conversation.data, session?.sujet) : undefined
  const presence = usePresence(autre?.id)
  // Pages : la plus récente d'abord ; le fil se lit du plus ancien au plus récent.
  const messages: Message[] = useMemo(() => (fil.data ?? []).slice().reverse().flatMap((p) => p.contenu), [fil.data])
  const nonLus = conversation.data?.nonLus ?? 0
  const dernier = messages[messages.length - 1]?.id
  useEffect(() => {
    if (nonLus > 0 && dernier) lu.mutate(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, nonLus, dernier])
  // Notifications de la plateforme (D-80) : ouvrir la conversation marque ses notifications lues, et de
  // nouveau à chaque message reçu pendant qu'elle est ouverte (POST /notifications/lecture, référence de la conversation).
  useLectureConversation(id, dernier)
  // Accusés de lecture poussés pour cette conversation (D-88 §4).
  useAccusesLecture(conversation.data)
  const plusAnciensRestants = fil.data?.[fil.data.length - 1]?.plusAnciens ?? false
  useEffect(() => {
    // Relecture en cours (fil gardé en cache d'une visite précédente) : on attend le fil à jour.
    if (!recherche || !fil.data || fil.isFetching) return
    if (messages.some((m) => m.id === recherche)) {
      setEvidence(recherche)
      setRecherche(null)
      pagesChargees.current = 0
      relu.current = false
    } else if (!relu.current) {
      relu.current = true
      void fil.refetch()
    } else if (plusAnciensRestants && pagesChargees.current < PAGES_RECHERCHE_MAX) {
      // Message plus ancien : les pages précédentes sont chargées jusqu'à le trouver.
      pagesChargees.current += 1
      void fil.fetchNextPage()
    } else {
      setAnnonce(t('msg.citation.nonCharge'))
      setRecherche(null)
      pagesChargees.current = 0
      relu.current = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recherche, messages, plusAnciensRestants, fil.isFetching])
  useEffect(() => {
    if (!evidence) return undefined
    const d = window.setTimeout(() => setEvidence(undefined), 2_000)
    return () => window.clearTimeout(d)
  }, [evidence])
  useEffect(() => {
    if (!annonce) return undefined
    const d = window.setTimeout(() => setAnnonce(undefined), 4_000)
    return () => window.clearTimeout(d)
  }, [annonce])
  if (conversation.isError) return <InlineAlert level="crit">{erreur(conversation.error)}</InlineAlert>
  if (!conversation.data || !fil.data) return <Skeleton height={320} />
  const c = conversation.data
  const vus: ThreadMessage[] = messages.map((m) => m.evenement ? {
    id: m.id, auteur: m.auteur.nom, quand: quand(m.envoyeLe), evenement: true, contenu: texteEvenement(m.evenement, m.auteur.nom, m.moi),
  } : ({
    id: m.id, auteur: m.auteur.nom, quand: quand(m.envoyeLe), mine: m.moi, avatar: photo(m.auteur),
    contenu: <CorpsMessage contenu={m.contenu} etiquettes={m.etiquettes} mentions={m.mentions} moi={session?.sujet} />,
    mentionneMoi: m.mentionneMoi,
    citation: m.reponseA ? {
      auteur: m.reponseA.auteur?.nom ?? null,
      mine: Boolean(m.reponseA.auteur) && m.reponseA.auteur?.id === session?.sujet,
      extrait: m.reponseA.extrait,
      pieces: m.reponseA.piecesJointes,
      supprime: m.reponseA.supprime,
      onOuvrir: () => {
        setAnnonce(undefined)
        setRecherche(m.reponseA!.id)
      },
    } : undefined,
    etiquettes: m.etiquettes.length ? <Etiquettes etiquettes={m.etiquettes} /> : undefined,
    pieces: m.piecesJointes.map((p) => ({ id: p.id, nom: p.nom, taille: taille(p.taille) })),
    lecture: m.moi ? accuse(m.lecture, c.type === 'GROUPE', () => <LuPar conversationId={id} messageId={m.id} />) : undefined,
  }))
  const derniere = fil.data[fil.data.length - 1]
  const pieceDe = (pid: string) => messages.flatMap((m) => m.piecesJointes).find((x) => x.id === pid)
  const ouvrirPiece = (pid: string) => {
    const piece = pieceDe(pid)
    if (!piece) return
    // Pièce volumineuse (D-74) : ni aperçu ni chargement en mémoire ; le navigateur la télécharge en flux.
    const lien = piece.taille > LIMITES_MESSAGERIE.apercuMaxOctets ? services.messagerie.lienPiece(pid) : null
    if (lien) {
      const a = document.createElement('a')
      a.href = lien
      a.download = piece.nom
      a.click()
      return
    }
    setApercu({ piece })
    telecharger.mutate(pid, {
      onSuccess: (r) => setApercu((a) => (a && a.piece.id === pid ? { ...a, blob: r.blob } : a)),
      onError: (e) => setApercu((a) => (a && a.piece.id === pid ? { ...a, erreur: erreur(e) } : a)),
    })
  }
  return (
    <>
    <MessageThread
      titre={titreConversation(c, session?.sujet)}
      avatar={autre ? <Presence enLigne={presence.enLigne}><Avatar name={autre.nom} src={photo(autre)} decorative /></Presence> : undefined}
      sousTitre={autre
        ? (presence.enLigne ? t('msg.enLigne') : presence.vuLe ? t('msg.vuLe', { quand: quand(presence.vuLe) }) : t('msg.horsLigne'))
        : [
            t('msg.membres', { count: c.membres.length }),
            ...(() => {
              const n = c.membres.filter((m) => m.id !== session?.sujet && enLigne(m.id)).length
              return n ? [t('msg.membresEnLigne', { count: n })] : []
            })(),
            c.membres.map((m) => (m.id === session?.sujet ? t('msg.vous') : m.nom)).join(', '),
          ].join(' · ')}
      ecrit={texteSaisie(c, session?.sujet)}
      actions={(
        <>
          {c.liaison && couche === 'des' ? <Link href={`/des/parcours/${encodeURIComponent(c.liaison.liaisonId)}`}>{t('msg.ouvrirLiaison', { reference: c.liaison.reference })}</Link> : null}
          {c.type === 'GROUPE' ? <Button size="sm" variant="secondary" icon="users" onClick={() => setGestion(true)}>{t('msg.groupe.gerer')}</Button> : null}
          {commandes}
        </>
      )}
      messages={vus}
      plusAnciens={derniere?.plusAnciens}
      onPlusAnciens={() => void fil.fetchNextPage()}
      onPiece={ouvrirPiece}
      onRepondre={peutEcrire ? (mid) => setReponse(messages.find((m) => m.id === mid) ?? null) : undefined}
      enEvidence={evidence}
      annonce={annonce}
      onTransferer={peutEcrire ? (pid) => setTransfert(pieceDe(pid) ?? null) : undefined}
      saisie={peutEcrire ? (
        <Saisie
          key={id}
          envoi={envoyer.isPending}
          erreur={envoyer.isError ? erreur(envoyer.error) : telecharger.isError ? erreur(telecharger.error) : undefined}
          onEnvoyer={(e, vider) => envoyer.mutate(e, { onSuccess: vider })}
          onFrappe={frappe}
          reference={id}
          reponse={reponse}
          onAnnulerReponse={() => setReponse(null)}
          membres={c.membres.filter((m) => m.id !== session?.sujet)}
        />
      ) : undefined}
    />
    {gestion ? <GererGroupe c={c} onFermer={() => setGestion(false)} onQuitte={onQuitte} /> : null}
    {transfert ? <TransfererPiece piece={transfert} depuis={id} onFermer={() => setTransfert(null)} onTransfere={(cid) => { setTransfert(null); onAller(cid) }} /> : null}
    {apercu ? (
      <AttachmentViewer
        nom={apercu.piece.nom}
        typeMime={apercu.piece.typeMime}
        taille={taille(apercu.piece.taille)}
        blob={apercu.blob}
        erreur={apercu.erreur}
        onClose={() => setApercu(null)}
        onTelecharger={() => (apercu.blob ? enregistrerFichier(apercu.blob, apercu.piece.nom) : undefined)}
        convertir={services.apercu?.convertir}
      />
    ) : null}
    </>
  )
}

function NouveauMessage({ liaisonId, destinataire, onCree, onAnnuler, commandes }: { liaisonId: string | null; destinataire: string | null; onCree: (c: Conversation) => void; onAnnuler: () => void; commandes?: ReactNode }) {
  const { t } = useTranslation()
  const [choisis, setChoisis] = useState<Correspondant[]>([])
  const demarrer = useDemarrer()
  const erreur = useMessageErreur()
  const photo = usePhoto()
  // Destinataire proposé par l'adresse (« Écrire à » depuis un dossier) : retrouvé dans l'annuaire.
  const propose = useAnnuaire('', Boolean(destinataire))
  useEffect(() => {
    const p = propose.data?.contenu.find((x) => x.id === destinataire)
    if (p) setChoisis((c) => (c.length ? c : [p]))
  }, [propose.data, destinataire])
  const liaison = useLiaisonsParcours({ page: 1, taille: 1, recherche: liaisonId ?? undefined }, useContext(Couche) === 'des' && Boolean(liaisonId))
  const rattachee = liaisonId ? liaison.data?.contenu.find((l) => l.id === liaisonId) : undefined
  const personne = choisis[0]
  return (
    <section className="gm-chat gm-fil" aria-label={t('msg.nouveau.titre')} style={{ height: '100%' }}>
      <header className="gm-chat-head">
        <strong>{t('msg.nouveau.titre')}</strong>
        <span className="gm-row" style={{ marginLeft: 'auto' }}><Button size="sm" variant="ghost" icon="x" onClick={onAnnuler}>{t('msg.annuler')}</Button>{commandes}</span>
      </header>
      <div className="gm-col gm-pad" style={{ gap: 'var(--gm-space-3)', overflow: 'auto', flex: 1 }}>
        {personne ? (
          <div className="gm-annuaire-destinataire">
            <span className="gm-field-label">{t('msg.destinataire')}</span>
            <div className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
              <Avatar name={personne.nom} src={photo(personne)} decorative />
              <span className="gm-annuaire-identite"><strong>{personne.nom}</strong>{personne.fonction ? <span className="gm-field-help">{personne.fonction}</span> : null}</span>
              <span style={{ marginLeft: 'auto' }}><IconButton variant="ghost" size="sm" icon="x" label={t('msg.annuler')} onClick={() => setChoisis([])} /></span>
            </div>
          </div>
        ) : (
          <>
            <span className="gm-field-help">{t('msg.destinataireAide')}</span>
            <Annuaire choisis={choisis} onChoisis={setChoisis} multiple={false} />
          </>
        )}
        {liaisonId ? <Note>{t('msg.liaisonRattachee', { reference: rattachee?.reference ?? liaisonId })} · {t('msg.liaisonAide')}</Note> : null}
      </div>
      {/* Remontée une fois la liaison rattachée connue : elle est alors citée d'emblée. */}
      {personne ? (
        <Saisie
          key={rattachee?.id ?? 'sans-liaison'}
          reference="nouvelle-conversation"
          membres={[personne]}
          envoi={demarrer.isPending}
          erreur={demarrer.isError ? erreur(demarrer.error) : undefined}
          etiquettesInitiales={rattachee ? [{ liaisonId: rattachee.id, reference: rattachee.reference }] : []}
          onEnvoyer={(e, vider) => demarrer.mutate(
            { ...e, destinataires: [personne.id], titre: null, liaisonId: rattachee?.id ?? liaisonId },
            { onSuccess: (c) => { vider(); onCree(c) } },
          )}
        />
      ) : null}
    </section>
  )
}

/** Création d'un groupe de discussion nommé (D-68) : nom, membres choisis dans l'annuaire. */
function NouveauGroupe({ liaisonId, onCree, onAnnuler, commandes }: { liaisonId: string | null; onCree: (c: Conversation) => void; onAnnuler: () => void; commandes?: ReactNode }) {
  const { t } = useTranslation()
  const [nom, setNom] = useState('')
  const [membres, setMembres] = useState<Correspondant[]>([])
  const [tente, setTente] = useState(false)
  const demarrer = useDemarrer()
  const erreur = useMessageErreur()
  const creer = () => {
    setTente(true)
    if (!nom.trim() || membres.length === 0) return
    demarrer.mutate(
      { contenu: '', etiquettes: [], fichiers: [], destinataires: membres.map((m) => m.id), titre: nom.trim(), liaisonId, groupe: true },
      { onSuccess: onCree },
    )
  }
  return (
    <section className="gm-chat gm-fil" aria-label={t('msg.groupe.titre')} style={{ height: '100%' }}>
      <header className="gm-chat-head">
        <Icon name="users" size={20} />
        <strong>{t('msg.groupe.titre')}</strong>
        <span className="gm-row" style={{ marginLeft: 'auto' }}><Button size="sm" variant="ghost" icon="x" onClick={onAnnuler}>{t('msg.annuler')}</Button>{commandes}</span>
      </header>
      <div className="gm-col gm-pad" style={{ gap: 'var(--gm-space-3)', overflow: 'auto', flex: 1 }}>
        <TextField name="nom-groupe" label={t('msg.groupe.nom')} help={t('msg.groupe.nomAide')} required value={nom} onChange={setNom} error={tente && !nom.trim() ? t('msg.erreur.COL_GROUP_TITLE_REQUIRED') : undefined} />
        <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          <span className="gm-field-label">{t('msg.groupe.membres')}</span>
          <span className="gm-field-help">{t('msg.groupe.membresAide')}</span>
          {tente && membres.length === 0 ? <InlineAlert level="crit">{t('msg.erreur.COL_RECIPIENT_REQUIRED')}</InlineAlert> : null}
          <Annuaire choisis={membres} onChoisis={setMembres} multiple />
        </div>
        {demarrer.isError ? <InlineAlert level="crit">{erreur(demarrer.error)}</InlineAlert> : null}
      </div>
      <footer className="gm-chat-input gm-row" style={{ justifyContent: 'flex-end' }}>
        <Button variant="primary" icon="users" loading={demarrer.isPending} onClick={creer}>{t('msg.groupe.creer')}</Button>
      </footer>
    </section>
  )
}

/** Membres et réglages d'un groupe (D-68) : l'administrateur le renomme, ajoute et retire des membres ; chacun peut le quitter. */
function GererGroupe({ c, onFermer, onQuitte }: { c: Conversation; onFermer: () => void; onQuitte: () => void }) {
  const { t } = useTranslation()
  const session = useSession()
  const photo = usePhoto()
  const erreur = useMessageErreur()
  const moi = session?.sujet
  const admin = Boolean(c.membres.find((m) => m.id === moi)?.administrateur)
  const [nom, setNom] = useState(c.titre ?? '')
  const [ajouts, setAjouts] = useState<Correspondant[]>([])
  const [confirmer, setConfirmer] = useState<{ quitter: true } | { retirer: Correspondant } | null>(null)
  const renommer = useRenommerGroupe(c.id)
  const ajouter = useAjouterMembres(c.id)
  const retirer = useRetirerMembre(c.id)
  const enEchec = [renommer, ajouter, retirer].find((m) => m.isError)
  return (
    <>
      <Dialog
        size="lg"
        id="gm-groupe"
        title={c.titre ?? t('msg.groupe.details')}
        subtitle={`${t('msg.groupe.details')} · ${t('msg.membres', { count: c.membres.length })}`}
        onClose={onFermer}
        footer={(
          <>
            <Button variant="danger" icon="log-out" onClick={() => setConfirmer({ quitter: true })}>{t('msg.groupe.quitter')}</Button>
            <Button variant="ghost" onClick={onFermer}>{t('commun.fermer', { defaultValue: 'Fermer' })}</Button>
          </>
        )}
      >
        <div className="gm-col" style={{ gap: 'var(--gm-space-4)' }}>
          {enEchec ? <InlineAlert level="crit">{erreur(enEchec.error)}</InlineAlert> : null}
          {admin ? (
            <div className="gm-row" style={{ alignItems: 'flex-end', gap: 'var(--gm-space-2)' }}>
              <div style={{ flex: 1 }}><TextField name="renommer-groupe" label={t('msg.groupe.nom')} value={nom} onChange={setNom} required /></div>
              <Button variant="secondary" icon="pen-line" disabled={!nom.trim() || nom.trim() === c.titre} loading={renommer.isPending} onClick={() => renommer.mutate(nom.trim())}>{t('msg.groupe.renommer')}</Button>
            </div>
          ) : <Note>{t('msg.groupe.lectureSeule')}</Note>}
          <section className="gm-col" style={{ gap: 'var(--gm-space-2)' }} aria-labelledby="gm-groupe-membres">
            <h3 id="gm-groupe-membres" className="gm-type-h3" style={{ margin: 0 }}>{t('msg.groupe.membres')}</h3>
            <ul className="gm-annuaire-liste gm-groupe-membres">
              {c.membres.map((m) => (
                <li key={m.id} className="gm-annuaire-personne gm-groupe-membre">
                  <Avatar name={m.nom} src={photo(m)} size="sm" decorative />
                  <span className="gm-annuaire-identite">
                    <strong>{m.id === moi ? `${m.nom} (${t('msg.vous')})` : m.nom}</strong>
                    {m.fonction ? <span className="gm-field-help">{m.fonction}</span> : null}
                  </span>
                  {m.administrateur ? <Tag>{t('msg.groupe.administrateur')}</Tag> : null}
                  {admin && m.id !== moi ? <IconButton variant="ghost" size="sm" icon="trash" label={t('msg.groupe.retirer', { nom: m.nom })} onClick={() => setConfirmer({ retirer: m })} /> : null}
                </li>
              ))}
            </ul>
          </section>
          {admin ? (
            <section className="gm-col" style={{ gap: 'var(--gm-space-2)' }} aria-labelledby="gm-groupe-ajouter">
              <h3 id="gm-groupe-ajouter" className="gm-type-h3" style={{ margin: 0 }}>{t('msg.groupe.ajouter')}</h3>
              <Annuaire choisis={ajouts} onChoisis={setAjouts} multiple exclus={c.membres.map((m) => m.id)} />
              <span>
                <Button variant="primary" icon="plus" disabled={ajouts.length === 0} loading={ajouter.isPending}
                  onClick={() => ajouter.mutate(ajouts.map((a) => a.id), { onSuccess: () => setAjouts([]) })}>
                  {t('msg.groupe.ajouterBouton', { count: Math.max(ajouts.length, 1) })}
                </Button>
              </span>
            </section>
          ) : null}
        </div>
      </Dialog>
      {confirmer ? (
        <ConfirmDialog
          danger
          title={'quitter' in confirmer ? t('msg.groupe.quitter') : t('msg.groupe.retirer', { nom: confirmer.retirer.nom })}
          description={'quitter' in confirmer ? t('msg.groupe.quitterConfirmation', { titre: c.titre ?? '' }) : t('msg.groupe.retirerConfirmation', { nom: confirmer.retirer.nom })}
          confirmLabel={'quitter' in confirmer ? t('msg.groupe.quitter') : t('msg.groupe.retirer', { nom: confirmer.retirer.nom })}
          onClose={() => setConfirmer(null)}
          onConfirm={() => {
            // Promesse plutôt que rappel : la relecture du groupe quitté (404) démonte ce panneau avant que
            // les rappels de mutate ne s'exécutent ; la navigation hors du fil doit avoir lieu quand même.
            if ('quitter' in confirmer) retirer.mutateAsync(moi ?? '').then(onQuitte, () => undefined)
            else retirer.mutate(confirmer.retirer.id, { onSuccess: () => setConfirmer(null) })
          }}
        />
      ) : null}
    </>
  )
}

/** Préférences de l'espace (D-84), gardées par navigateur ; stockage indisponible (navigation privée) : défauts. */
const CLES_ESPACE = { largeur: 'gm.messagerie.largeurListe', repliee: 'gm.messagerie.listeRepliee', elargie: 'gm.messagerie.elargie' } as const
export const LARGEUR_LISTE = { min: 240, max: 480, defaut: 320, pas: 16 } as const

function lirePreference(cle: string): string | null {
  try {
    return window.localStorage.getItem(cle)
  } catch {
    return null
  }
}
function ecrirePreference(cle: string, valeur: string) {
  try {
    window.localStorage.setItem(cle, valeur)
  } catch {
    // Stockage refusé : la préférence vaut pour la visite seulement.
  }
}
const borner = (n: number) => Math.min(LARGEUR_LISTE.max, Math.max(LARGEUR_LISTE.min, Math.round(n)))

/** Écran assez large pour deux colonnes (au-delà, comportement d'origine : une colonne à la fois). */
function useDeuxColonnes() {
  const requete = '(min-width: 961px)'
  const lire = () => (typeof window.matchMedia === 'function' ? window.matchMedia(requete).matches : true)
  const [deux, setDeux] = useState(lire)
  useEffect(() => {
    if (typeof window.matchMedia !== 'function') return undefined
    const m = window.matchMedia(requete)
    const suivre = () => setDeux(m.matches)
    m.addEventListener?.('change', suivre)
    return () => m.removeEventListener?.('change', suivre)
  }, [])
  return deux
}

/**
 * Largeur du fil (D-84, docs/lot1/messagerie-espace.md) : largeur de la liste (240 à 480 px), liste repliée,
 * barre latérale de l'application réduite (« Élargir »), rendue à son état précédent en quittant la messagerie.
 */
function useEspace() {
  const [largeur, setLargeur] = useState(() => {
    const n = Number(lirePreference(CLES_ESPACE.largeur))
    return n ? borner(n) : LARGEUR_LISTE.defaut
  })
  const [repliee, setRepliee] = useState(() => lirePreference(CLES_ESPACE.repliee) === '1')
  const [elargie, setElargie] = useState(() => lirePreference(CLES_ESPACE.elargie) === '1')
  const barre = useBarreLaterale()
  const avant = useRef(barre.reduite)
  const reduire = useRef(barre.reduire)
  reduire.current = barre.reduire
  useEffect(() => {
    reduire.current(elargie || avant.current)
  }, [elargie])
  useEffect(() => () => reduire.current(avant.current), [])
  return {
    largeur, repliee, elargie, barreActive: barre.active,
    changerLargeur: (n: number, garder = true) => {
      const b = borner(n)
      setLargeur(b)
      if (garder) ecrirePreference(CLES_ESPACE.largeur, String(b))
    },
    garderLargeur: (n: number) => ecrirePreference(CLES_ESPACE.largeur, String(borner(n))),
    replier: (v: boolean) => {
      setRepliee(v)
      ecrirePreference(CLES_ESPACE.repliee, v ? '1' : '0')
    },
    elargir: (v: boolean) => {
      setElargie(v)
      ecrirePreference(CLES_ESPACE.elargie, v ? '1' : '0')
    },
  }
}

/** Séparateur déplaçable (souris, doigt, flèches, Origine et Fin) ; double clic : largeur par défaut. */
function Separateur({ largeur, onChange, onFin }: { largeur: number; onChange: (n: number, garder?: boolean) => void; onFin: (n: number) => void }) {
  const { t } = useTranslation()
  const glisse = useRef<{ x: number; largeur: number; derniere: number } | null>(null)
  const [actif, setActif] = useState(false)
  const touche = (e: KeyboardEvent<HTMLDivElement>) => {
    const cible = e.key === 'ArrowLeft' ? largeur - LARGEUR_LISTE.pas : e.key === 'ArrowRight' ? largeur + LARGEUR_LISTE.pas
      : e.key === 'Home' ? LARGEUR_LISTE.min : e.key === 'End' ? LARGEUR_LISTE.max : null
    if (cible === null) return
    e.preventDefault()
    onChange(cible)
  }
  const fin = (e: PointerEvent<HTMLDivElement>) => {
    const g = glisse.current
    if (!g) return
    glisse.current = null
    setActif(false)
    e.currentTarget.releasePointerCapture?.(e.pointerId)
    onFin(g.derniere)
  }
  return (
    <div
      role="separator"
      tabIndex={0}
      aria-orientation="vertical"
      aria-label={t('msg.espace.separateur')}
      aria-valuenow={largeur}
      aria-valuemin={LARGEUR_LISTE.min}
      aria-valuemax={LARGEUR_LISTE.max}
      className={actif ? 'gm-messagerie-separateur gm-glisse' : 'gm-messagerie-separateur'}
      onKeyDown={touche}
      onDoubleClick={() => onChange(LARGEUR_LISTE.defaut)}
      onPointerDown={(e) => {
        e.preventDefault()
        e.currentTarget.setPointerCapture?.(e.pointerId)
        glisse.current = { x: e.clientX, largeur, derniere: largeur }
        setActif(true)
      }}
      onPointerMove={(e) => {
        const g = glisse.current
        if (!g) return
        g.derniere = borner(g.largeur + e.clientX - g.x)
        onChange(g.derniere, false)
      }}
      onPointerUp={fin}
      onPointerCancel={fin}
    />
  )
}

export default function MessageriePage({ couche = 'des' }: { couche?: CoucheMessagerie }) {
  return <Couche.Provider value={couche}><Messagerie /></Couche.Provider>
}

function Messagerie() {
  const { t } = useTranslation()
  usePresenceMessagerie()
  const enLigne = useEnLigne()
  const texteSaisie = useTexteSaisie()
  const photo = usePhoto()
  const session = useSession()
  const permissions = usePermissions()
  const peutEcrire = aPermission(permissions, PERM.messageEnvoyer)
  const quand = useQuand()
  const [params, setParams] = useSearchParams()
  const ouverte = params.get('conversation') ?? undefined
  const nouveau = params.get('nouveau') === '1' && peutEcrire
  const nouveauGroupe = params.get('nouveau') === 'groupe' && peutEcrire
  const texteEvenement = useTexteEvenement()
  const [saisie, setSaisie] = useState('')
  const [recherche, setRecherche] = useState('')
  const [page, setPage] = useState(1)
  useEffect(() => {
    const d = window.setTimeout(() => {
      setRecherche(saisie.trim())
      setPage(1)
    }, 300)
    return () => window.clearTimeout(d)
  }, [saisie])
  const liste = useConversations({ recherche: recherche || undefined, page: page - 1 })
  const aller = (maj: Record<string, string | null>) => {
    const p = new URLSearchParams()
    Object.entries(maj).forEach(([k, v]) => v && p.set(k, v))
    setParams(p)
  }
  const erreur = useMessageErreur()
  const espace = useEspace()
  const deuxColonnes = useDeuxColonnes()
  // Liste repliée seulement quand un fil (ou un nouveau message) occupe l'écran : sinon rien ne la rouvrirait.
  const repliee = deuxColonnes && espace.repliee && Boolean(ouverte || nouveau || nouveauGroupe)
  const basculeListe = deuxColonnes ? (
    <span className="gm-messagerie-commande">
      <IconButton size="sm" variant="ghost" icon={repliee ? 'panel-left-open' : 'panel-left-close'} label={t(repliee ? 'msg.espace.deplier' : 'msg.espace.replier')}
        aria-expanded={!repliee} onClick={() => espace.replier(!repliee)} />
    </span>
  ) : null
  const elargir = espace.barreActive && deuxColonnes ? (
    <span className="gm-messagerie-commande" title={espace.elargie ? undefined : t('msg.espace.elargirAide')}>
      <Button variant="ghost" icon={espace.elargie ? 'minimize-2' : 'maximize-2'} onClick={() => espace.elargir(!espace.elargie)}>
        {t(espace.elargie ? 'msg.espace.retablir' : 'msg.espace.elargir')}
      </Button>
    </span>
  ) : null
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-4)' }}>
      <PageHeader
        title={t('msg.titre')}
        meta={<span>{t('msg.sousTitre')}</span>}
        actions={elargir || peutEcrire ? (
          <>
            {elargir}
            {peutEcrire ? <Button variant="secondary" icon="users" onClick={() => aller({ nouveau: 'groupe' })}>{t('msg.nouveauGroupe')}</Button> : null}
            {peutEcrire ? <Button variant="primary" icon="plus" onClick={() => aller({ nouveau: '1' })}>{t('msg.nouveau')}</Button> : null}
          </>
        ) : undefined}
      />
      <div className={repliee ? 'gm-messagerie gm-messagerie-espace gm-messagerie-repliee' : 'gm-messagerie gm-messagerie-espace'}
        style={{ '--gm-messagerie-liste': `${espace.largeur}px` } as CSSProperties}>
        <aside className="gm-messagerie-liste" aria-label={t('msg.titre')}>
          <div className="gm-pad"><SearchInput label={t('msg.recherche')} value={saisie} onChange={setSaisie} width="100%" /></div>
          <div className="gm-messagerie-defile">
            {liste.isPending ? <Skeleton height={240} /> : liste.isError ? <InlineAlert level="crit">{erreur(liste.error) ?? ''}</InlineAlert> : (
              <ConversationList
                selected={ouverte}
                onSelect={(id) => aller({ conversation: id })}
                empty={t('msg.aucune')}
                items={liste.data.contenu.map((c) => ({
                  id: c.id,
                  titre: titreConversation(c, session?.sujet),
                  sousTitre: c.liaison ? t('msg.liaisonRattachee', { reference: c.liaison.reference }) : c.type === 'GROUPE' ? t('msg.membres', { count: c.membres.length }) : undefined,
                  extrait: !c.dernierMessage ? '' : c.dernierMessage.evenement
                    ? texteEvenement(c.dernierMessage.evenement, c.dernierMessage.auteurNom, c.dernierMessage.auteurNom === session?.nom)
                    : `${c.dernierMessage.auteurNom === session?.nom ? t('msg.vous') : c.dernierMessage.auteurNom.split(' ')[0]} : ${c.dernierMessage.extrait || t('msg.piecesJointes', { count: c.dernierMessage.piecesJointes })}`,
                  quand: c.dernierMessage ? quand(c.dernierMessage.envoyeLe) : undefined,
                  nonLus: c.nonLus,
                  groupe: c.type === 'GROUPE',
                  avatar: photo(interlocuteur(c, session?.sujet)),
                  enLigne: enLigne(interlocuteur(c, session?.sujet)?.id),
                  saisie: texteSaisie(c, session?.sujet, true),
                  lecture: c.dernierMessage?.evenement ? undefined : accuse(c.dernierMessage?.lecture, false, () => null),
                }))}
              />
            )}
          </div>
          <Pagination total={liste.data?.total ?? 0} page={page} pageSize={PAR_PAGE} onPage={setPage} />
        </aside>
        {deuxColonnes ? <Separateur largeur={espace.largeur} onChange={espace.changerLargeur} onFin={espace.garderLargeur} /> : null}
        {/* État vide sur le fond texturé du fil (motif de trame atténué, D-66), le texte sur fond plein. */}
        <div className={nouveau || nouveauGroupe || ouverte ? 'gm-messagerie-fil' : 'gm-messagerie-fil gm-trame-fil'}>
          {nouveauGroupe ? (
            <NouveauGroupe commandes={basculeListe} liaisonId={params.get('liaison')} onCree={(c) => aller({ conversation: c.id })} onAnnuler={() => aller({})} />
          ) : nouveau ? (
            <NouveauMessage
              commandes={basculeListe}
              key={`${params.get('liaison')}-${params.get('destinataire')}`}
              liaisonId={params.get('liaison')}
              destinataire={params.get('destinataire')}
              onCree={(c) => aller({ conversation: c.id })}
              onAnnuler={() => aller({})}
            />
          ) : ouverte ? (
            <Fil key={ouverte} id={ouverte} cible={params.get('message')} commandes={basculeListe} peutEcrire={peutEcrire} onQuitte={() => aller({})} onAller={(cid) => aller({ conversation: cid })} />
          ) : (
            <EmptyState icon="messages-square" title={liste.data?.total === 0 ? t('msg.aucune') : t('msg.choisir')} text={liste.data?.total === 0 ? t('msg.aucuneAide') : t('msg.choisirAide')} />
          )}
        </div>
      </div>
    </div>
  )
}

