import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, ConversationList, EmptyState, InlineAlert, Link, MessageComposer, MessageThread, MultiSelect, PageHeader, Pagination, SearchInput, SearchSelect, Skeleton, Tag, TextField, type ThreadMessage, Note } from '@gimac/design-system'
import { aPermission, urlAvatar, usePermissions, useSession, type ApiError } from '@gimac/socle'
import type { Conversation, Correspondant, EtiquetteLiaison, Message } from '../../../modele/messagerie'
import { LIMITES_MESSAGERIE, MOTIF_LIAISON } from '../../../modele/messagerie'
import { useAnnuaire, useConversation, useConversations, useDemarrer, useEnvoyer, useFil, useMarquerLu, useTelechargerPiece } from '../../../hooks/messagerie'
import { useLiaisonsParcours } from '../../../hooks/parcours'
import { PERM } from '../../../permissions'
import { enregistrerFichier } from '../../../ressources/fichier'

// Messagerie instantanée de travail (D-64, /des/messagerie) : conversations à gauche, fil ou nouveau message
// à droite, comme un client de messagerie. Les destinataires viennent de l'annuaire de la plateforme ; une
// liaison citée devient un lien vers son dossier ; le service applique les droits, le masquage RG-52 et les bornes.
// Adresse : ?conversation=<id> ouvre un fil ; ?nouveau=1[&liaison=<id>][&destinataire=<id>] prépare un message.
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

function Fil({ id, peutEcrire }: { id: string; peutEcrire: boolean }) {
  const { t } = useTranslation()
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
  const vus: ThreadMessage[] = messages.map((m) => ({
    id: m.id, auteur: m.auteur.nom, quand: quand(m.envoyeLe), mine: m.moi, avatar: photo(m.auteur),
    contenu: <CorpsMessage contenu={m.contenu} etiquettes={m.etiquettes} />,
    etiquettes: m.etiquettes.length ? <Etiquettes etiquettes={m.etiquettes} /> : undefined,
    pieces: m.piecesJointes.map((p) => ({ id: p.id, nom: p.nom, taille: taille(p.taille) })),
  }))
  const derniere = fil.data[fil.data.length - 1]
  return (
    <MessageThread
      titre={titreConversation(c, session?.sujet)}
      sousTitre={[t('msg.membres', { count: c.membres.length }), c.membres.map((m) => (m.id === session?.sujet ? t('msg.vous') : m.nom)).join(', ')].join(' · ')}
      actions={c.liaison && couche === 'des' ? <Link href={`/des/parcours/${encodeURIComponent(c.liaison.liaisonId)}`}>{t('msg.ouvrirLiaison', { reference: c.liaison.reference })}</Link> : undefined}
      messages={vus}
      plusAnciens={derniere?.plusAnciens}
      onPlusAnciens={() => void fil.fetchNextPage()}
      onPiece={(pid) => {
        const p = messages.flatMap((m) => m.piecesJointes).find((x) => x.id === pid)
        telecharger.mutate(pid, { onSuccess: (r) => enregistrerFichier(r.blob, p?.nom ?? pid) })
      }}
      saisie={peutEcrire ? (
        <Saisie
          key={id}
          envoi={envoyer.isPending}
          erreur={envoyer.isError ? erreur(envoyer.error) : telecharger.isError ? erreur(telecharger.error) : undefined}
          onEnvoyer={(e, vider) => envoyer.mutate(e, { onSuccess: vider })}
        />
      ) : undefined}
    />
  )
}

function NouveauMessage({ liaisonId, destinataire, onCree, onAnnuler }: { liaisonId: string | null; destinataire: string | null; onCree: (c: Conversation) => void; onAnnuler: () => void }) {
  const { t } = useTranslation()
  const [q, setQ] = useState('')
  const [destinataires, setDestinataires] = useState<string[]>(destinataire ? [destinataire] : [])
  const [connus, setConnus] = useState<Record<string, Correspondant>>({})
  const [objet, setObjet] = useState('')
  const annuaire = useAnnuaire(q.trim())
  const demarrer = useDemarrer()
  const erreur = useMessageErreur()
  const photo = usePhoto()
  const liaison = useLiaisonsParcours({ page: 1, taille: 1, recherche: liaisonId ?? undefined }, useContext(Couche) === 'des' && Boolean(liaisonId))
  const rattachee = liaisonId ? liaison.data?.contenu.find((l) => l.id === liaisonId) : undefined
  const personnes = annuaire.data?.contenu ?? []
  // Libellé de chaque destinataire choisi, même s'il sort des résultats de la recherche courante.
  useEffect(() => {
    if (personnes.length) setConnus((k) => ({ ...k, ...Object.fromEntries(personnes.map((p) => [p.id, p])) }))
  }, [personnes])
  return (
    <section className="gm-chat gm-fil" aria-label={t('msg.nouveau.titre')} style={{ height: '100%' }}>
      <header className="gm-chat-head">
        <strong>{t('msg.nouveau.titre')}</strong>
        <span style={{ marginLeft: 'auto' }}><Button size="sm" variant="ghost" icon="x" onClick={onAnnuler}>{t('msg.annuler')}</Button></span>
      </header>
      <div className="gm-col gm-pad" style={{ gap: 'var(--gm-space-3)', overflow: 'auto', flex: 1 }}>
        <MultiSelect
          label={t('msg.destinataires')}
          help={t('msg.destinatairesAide')}
          required
          suggestions
          value={destinataires}
          onChange={setDestinataires}
          query={q}
          onQuery={setQ}
          labels={Object.fromEntries(Object.values(connus).map((p) => [p.id, p.nom]))}
          avatars={Object.fromEntries(Object.values(connus).map((p) => [p.id, { name: p.nom, src: photo(p) }]))}
          options={personnes.map((p) => ({ value: p.id, label: p.fonction ? `${p.nom} · ${p.fonction}` : p.nom, avatar: { name: p.nom, src: photo(p) } }))}
        />
        <TextField name="objet" label={t('msg.objet')} help={t('msg.objetAide')} value={objet} onChange={setObjet} />
        {liaisonId ? <Note>{t('msg.liaisonRattachee', { reference: rattachee?.reference ?? liaisonId })} · {t('msg.liaisonAide')}</Note> : null}
      </div>
      {/* Remontée une fois la liaison rattachée connue : elle est alors citée d'emblée. */}
      <Saisie
        key={rattachee?.id ?? 'sans-liaison'}
        envoi={demarrer.isPending}
        erreur={demarrer.isError ? erreur(demarrer.error) : undefined}
        etiquettesInitiales={rattachee ? [{ liaisonId: rattachee.id, reference: rattachee.reference }] : []}
        onEnvoyer={(e, vider) => demarrer.mutate(
          { ...e, destinataires, titre: objet.trim() || null, liaisonId: rattachee?.id ?? liaisonId },
          { onSuccess: (c) => { vider(); onCree(c) } },
        )}
      />
    </section>
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
        actions={peutEcrire ? <Button variant="primary" icon="plus" onClick={() => aller({ nouveau: '1' })}>{t('msg.nouveau')}</Button> : undefined}
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
                  extrait: c.dernierMessage ? `${c.dernierMessage.auteurNom === session?.nom ? t('msg.vous') : c.dernierMessage.auteurNom.split(' ')[0]} : ${c.dernierMessage.extrait || t('msg.piecesJointes', { count: c.dernierMessage.piecesJointes })}` : '',
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
        <div className={nouveau || ouverte ? 'gm-messagerie-fil' : 'gm-messagerie-fil gm-trame-fil'}>
          {nouveau ? (
            <NouveauMessage
              key={`${params.get('liaison')}-${params.get('destinataire')}`}
              liaisonId={params.get('liaison')}
              destinataire={params.get('destinataire')}
              onCree={(c) => aller({ conversation: c.id })}
              onAnnuler={() => aller({})}
            />
          ) : ouverte ? (
            <Fil id={ouverte} peutEcrire={peutEcrire} />
          ) : (
            <EmptyState icon="messages-square" title={liste.data?.total === 0 ? t('msg.aucune') : t('msg.choisir')} text={liste.data?.total === 0 ? t('msg.aucuneAide') : t('msg.choisirAide')} />
          )}
        </div>
      </div>
    </div>
  )
}

