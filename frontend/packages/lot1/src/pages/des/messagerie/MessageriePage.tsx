import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { AttachmentViewer, Avatar, Button, ConfirmDialog, ConversationList, Dialog, EmptyState, Icon, IconButton, InlineAlert, Link, MessageComposer, MessageThread, PageHeader, Pagination, SearchInput, SearchSelect, Skeleton, Tag, TextField, type ThreadMessage, Note } from '@gimac/design-system'
import { aPermission, urlAvatar, usePermissions, useSession, type ApiError } from '@gimac/socle'
import type { Conversation, Correspondant, EtiquetteLiaison, EvenementGroupe, Message, PieceJointe } from '../../../modele/messagerie'
import { LIMITES_MESSAGERIE, MOTIF_LIAISON } from '../../../modele/messagerie'
import { useAjouterMembres, useAnnuaire, useConversation, useConversations, useDemarrer, useEnvoyer, useFil, useMarquerLu, useRenommerGroupe, useRetirerMembre, useTelechargerPiece } from '../../../hooks/messagerie'
import { useLiaisonsParcours } from '../../../hooks/parcours'
import { PERM } from '../../../permissions'
import { enregistrerFichier } from '../../../ressources/fichier'

// Messagerie instantanée de travail (D-64, /des/messagerie) : conversations à gauche, fil ou nouveau message
// à droite, comme un client de messagerie. Les destinataires viennent de l'annuaire de la plateforme ; une
// liaison citée devient un lien vers son dossier ; le service applique les droits, le masquage RG-52 et les bornes.
// Adresse : ?conversation=<id> ouvre un fil ; ?nouveau=1[&liaison=<id>][&destinataire=<id>] prépare un message
// direct ; ?nouveau=groupe ouvre la création d'un groupe de discussion nommé (D-68), géré ensuite par son
// administrateur (nom, membres) ; les pièces jointes s'ouvrent dans une fenêtre d'aperçu.
// Ouverte à tout membre interne du GIMAC, depuis le DES et depuis la console (RSSI, super administrateur) :
// dans la console, les liaisons ne se citent pas (la couche n'y a pas accès), leurs liens mènent au DES.

/** Couche de présentation qui affiche la messagerie : préfixe des photos, citation des liaisons. */
type CoucheMessagerie = 'des' | 'console'
const Couche = createContext<CoucheMessagerie>('des')

const PAR_PAGE = 20
const taille = (octets: number) => (octets < 1024 * 1024 ? `${Math.max(1, Math.round(octets / 1024))} Ko` : `${(octets / 1024 / 1024).toFixed(1).replace('.', ',')} Mo`)

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

/** Corps d'un message : les références de liaison citées deviennent des liens vers leur dossier. */
export function CorpsMessage({ contenu, etiquettes }: { contenu: string; etiquettes: EtiquetteLiaison[] }) {
  const morceaux: ReactNode[] = []
  let dernier = 0
  for (const m of contenu.matchAll(MOTIF_LIAISON)) {
    const debut = m.index ?? 0
    if (debut > dernier) morceaux.push(contenu.slice(dernier, debut))
    const e = etiquettes.find((x) => x.reference === m[0])
    morceaux.push(<RefLiaison key={debut} href={e ? `/des/parcours/${encodeURIComponent(e.liaisonId)}` : `/des/parcours?q=${encodeURIComponent(m[0])}`}>{m[0]}</RefLiaison>)
    dernier = debut + m[0].length
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
function Annuaire({ choisis, onChoisis, multiple, exclus = [] }: { choisis: Correspondant[]; onChoisis: (c: Correspondant[]) => void; multiple: boolean; exclus?: string[] }) {
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
function Saisie({ envoi, erreur, onEnvoyer, etiquettesInitiales = [] }: {
  envoi: boolean
  erreur?: string
  onEnvoyer: (e: { contenu: string; fichiers: File[]; etiquettes: EtiquetteLiaison[] }, vider: () => void) => void
  etiquettesInitiales?: EtiquetteLiaison[]
}) {
  const { t } = useTranslation()
  const [texte, setTexte] = useState('')
  const [fichiers, setFichiers] = useState<File[]>([])
  const [etiquettes, setEtiquettes] = useState<EtiquetteLiaison[]>(etiquettesInitiales)
  const [citer, setCiter] = useState(etiquettesInitiales.length > 0)
  const citable = useContext(Couche) === 'des'
  const vider = () => {
    setTexte('')
    setFichiers([])
    setEtiquettes([])
    setCiter(false)
  }
  return (
    <MessageComposer
      value={texte}
      onChange={setTexte}
      fichiers={fichiers}
      onFichiers={setFichiers}
      maxFichiers={LIMITES_MESSAGERIE.piecesMax}
      accept={LIMITES_MESSAGERIE.types}
      sending={envoi}
      error={erreur}
      onSend={() => onEnvoyer({ contenu: texte, fichiers, etiquettes }, vider)}
      extras={
        <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          {!citable ? null : citer ? <CiterLiaisons valeur={etiquettes} onChange={setEtiquettes} /> : (
            <span>
              <Button size="sm" variant="ghost" icon="route" onClick={() => setCiter(true)}>{t('msg.citer')}</Button>
            </span>
          )}
          <span className="gm-field-help">{t('msg.limites', { max: LIMITES_MESSAGERIE.piecesMax })}</span>
        </div>
      }
    />
  )
}

function Fil({ id, peutEcrire, onQuitte }: { id: string; peutEcrire: boolean; onQuitte: () => void }) {
  const { t } = useTranslation()
  const texteEvenement = useTexteEvenement()
  const [gestion, setGestion] = useState(false)
  const [apercu, setApercu] = useState<{ piece: PieceJointe; blob?: Blob; erreur?: string } | null>(null)
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
  // Pages : la plus récente d'abord ; le fil se lit du plus ancien au plus récent.
  const messages: Message[] = useMemo(() => (fil.data ?? []).slice().reverse().flatMap((p) => p.contenu), [fil.data])
  const nonLus = conversation.data?.nonLus ?? 0
  const dernier = messages[messages.length - 1]?.id
  useEffect(() => {
    if (nonLus > 0 && dernier) lu.mutate(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, nonLus, dernier])
  if (conversation.isError) return <InlineAlert level="crit">{erreur(conversation.error)}</InlineAlert>
  if (!conversation.data || !fil.data) return <Skeleton height={320} />
  const c = conversation.data
  const vus: ThreadMessage[] = messages.map((m) => m.evenement ? {
    id: m.id, auteur: m.auteur.nom, quand: quand(m.envoyeLe), evenement: true, contenu: texteEvenement(m.evenement, m.auteur.nom, m.moi),
  } : ({
    id: m.id, auteur: m.auteur.nom, quand: quand(m.envoyeLe), mine: m.moi, avatar: photo(m.auteur),
    contenu: <CorpsMessage contenu={m.contenu} etiquettes={m.etiquettes} />,
    etiquettes: m.etiquettes.length ? <Etiquettes etiquettes={m.etiquettes} /> : undefined,
    pieces: m.piecesJointes.map((p) => ({ id: p.id, nom: p.nom, taille: taille(p.taille) })),
  }))
  const derniere = fil.data[fil.data.length - 1]
  const ouvrirPiece = (pid: string) => {
    const piece = messages.flatMap((m) => m.piecesJointes).find((x) => x.id === pid)
    if (!piece) return
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
      sousTitre={[t('msg.membres', { count: c.membres.length }), c.membres.map((m) => (m.id === session?.sujet ? t('msg.vous') : m.nom)).join(', ')].join(' · ')}
      actions={(
        <>
          {c.liaison && couche === 'des' ? <Link href={`/des/parcours/${encodeURIComponent(c.liaison.liaisonId)}`}>{t('msg.ouvrirLiaison', { reference: c.liaison.reference })}</Link> : null}
          {c.type === 'GROUPE' ? <Button size="sm" variant="secondary" icon="users" onClick={() => setGestion(true)}>{t('msg.groupe.gerer')}</Button> : null}
        </>
      )}
      messages={vus}
      plusAnciens={derniere?.plusAnciens}
      onPlusAnciens={() => void fil.fetchNextPage()}
      onPiece={ouvrirPiece}
      saisie={peutEcrire ? (
        <Saisie
          key={id}
          envoi={envoyer.isPending}
          erreur={envoyer.isError ? erreur(envoyer.error) : telecharger.isError ? erreur(telecharger.error) : undefined}
          onEnvoyer={(e, vider) => envoyer.mutate(e, { onSuccess: vider })}
        />
      ) : undefined}
    />
    {gestion ? <GererGroupe c={c} onFermer={() => setGestion(false)} onQuitte={onQuitte} /> : null}
    {apercu ? (
      <AttachmentViewer
        nom={apercu.piece.nom}
        typeMime={apercu.piece.typeMime}
        taille={taille(apercu.piece.taille)}
        blob={apercu.blob}
        erreur={apercu.erreur}
        onClose={() => setApercu(null)}
        onTelecharger={() => (apercu.blob ? enregistrerFichier(apercu.blob, apercu.piece.nom) : undefined)}
      />
    ) : null}
    </>
  )
}

function NouveauMessage({ liaisonId, destinataire, onCree, onAnnuler }: { liaisonId: string | null; destinataire: string | null; onCree: (c: Conversation) => void; onAnnuler: () => void }) {
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
        <span style={{ marginLeft: 'auto' }}><Button size="sm" variant="ghost" icon="x" onClick={onAnnuler}>{t('msg.annuler')}</Button></span>
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
function NouveauGroupe({ liaisonId, onCree, onAnnuler }: { liaisonId: string | null; onCree: (c: Conversation) => void; onAnnuler: () => void }) {
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
        <span style={{ marginLeft: 'auto' }}><Button size="sm" variant="ghost" icon="x" onClick={onAnnuler}>{t('msg.annuler')}</Button></span>
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
            if ('quitter' in confirmer) retirer.mutate(moi ?? '', { onSuccess: () => { setConfirmer(null); onFermer(); onQuitte() } })
            else retirer.mutate(confirmer.retirer.id, { onSuccess: () => setConfirmer(null) })
          }}
        />
      ) : null}
    </>
  )
}

export default function MessageriePage({ couche = 'des' }: { couche?: CoucheMessagerie }) {
  return <Couche.Provider value={couche}><Messagerie /></Couche.Provider>
}

function Messagerie() {
  const { t } = useTranslation()
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
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-4)' }}>
      <PageHeader
        title={t('msg.titre')}
        meta={<span>{t('msg.sousTitre')}</span>}
        actions={peutEcrire ? (
          <>
            <Button variant="secondary" icon="users" onClick={() => aller({ nouveau: 'groupe' })}>{t('msg.nouveauGroupe')}</Button>
            <Button variant="primary" icon="plus" onClick={() => aller({ nouveau: '1' })}>{t('msg.nouveau')}</Button>
          </>
        ) : undefined}
      />
      <div className="gm-messagerie">
        <aside className="gm-messagerie-liste" aria-label={t('msg.titre')}>
          <div className="gm-pad"><SearchInput label={t('msg.recherche')} value={saisie} onChange={setSaisie} /></div>
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
                }))}
              />
            )}
          </div>
          <Pagination total={liste.data?.total ?? 0} page={page} pageSize={PAR_PAGE} onPage={setPage} />
        </aside>
        {/* État vide sur le fond texturé du fil (motif de trame atténué, D-66), le texte sur fond plein. */}
        <div className={nouveau || nouveauGroupe || ouverte ? 'gm-messagerie-fil' : 'gm-messagerie-fil gm-trame-fil'}>
          {nouveauGroupe ? (
            <NouveauGroupe liaisonId={params.get('liaison')} onCree={(c) => aller({ conversation: c.id })} onAnnuler={() => aller({})} />
          ) : nouveau ? (
            <NouveauMessage
              key={`${params.get('liaison')}-${params.get('destinataire')}`}
              liaisonId={params.get('liaison')}
              destinataire={params.get('destinataire')}
              onCree={(c) => aller({ conversation: c.id })}
              onAnnuler={() => aller({})}
            />
          ) : ouverte ? (
            <Fil key={ouverte} id={ouverte} peutEcrire={peutEcrire} onQuitte={() => aller({})} />
          ) : (
            <EmptyState icon="messages-square" title={liste.data?.total === 0 ? t('msg.aucune') : t('msg.choisir')} text={liste.data?.total === 0 ? t('msg.aucuneAide') : t('msg.choisirAide')} />
          )}
        </div>
      </div>
    </div>
  )
}

