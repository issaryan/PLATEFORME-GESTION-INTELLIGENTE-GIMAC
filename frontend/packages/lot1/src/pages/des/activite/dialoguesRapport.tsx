import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AttachmentViewer, Button, CrudDialog, DateField, FileDropzone, InlineAlert, KeyValueList, Note, RadioGroup, Select, Skeleton, TextArea } from '@gimac/design-system'
import { estApiError, useSession, type ApiError, type Progression } from '@gimac/socle'
import { LIMITES_RAPPORT, type ModeRapport, type Rapport, type TypeRapport } from '../../../modele/pilotage'
import { ajouterJours, aujourdhui, depuisSaisie, semaineIso, versSaisie } from '../../../modele/jours'
import { useCreerRapport, useDestinatairesRapport, useDocumentRapport, useModifierRapport, useOrganisation, usePreremplir, useRapport, useSoumettre, useViser } from '../../../hooks/pilotage'
import { useServices } from '../../../services/ServicesProvider'
import { enregistrerFichier } from '../../../ressources/fichier'
import { BadgeRapport, TexteRapport, pourDialogue, useDates, useErreur } from './communs'

// Rapports quotidiens et hebdomadaires (D-76 §2.4) : création en fenêtre modale selon trois modes,
// préremplissage proposé par le service puis librement modifié, texte rédigé (Markdown simple), document
// déposé par morceaux (D-74, finalité RAPPORT_ACTIVITE) avec sa progression ; brouillon, soumission au destinataire
// annoncé avant l'envoi (n+1, chef de l'unité, chef d'une unité au-dessus, ou choisi dans la chaîne hiérarchique ;
// personne au-dessus : rapport déposé) ; consultation, aperçu du document et visa du destinataire. Tout refus est
// expliqué (message traduit par code, détail du service).

const MODES: ModeRapport[] = ['PREREMPLI', 'REDIGE', 'DOCUMENT']
const taille = (octets: number) => (octets < 1024 * 1024 ? `${Math.max(1, Math.round(octets / 1024))} Ko` : `${(octets / 1024 / 1024).toFixed(1).replace('.', ',')} Mo`)

/** Les huit dernières semaines ISO, la courante d'abord. */
const semaines = () => Array.from({ length: 8 }, (_, i) => semaineIso(ajouterJours(aujourdhui(), -7 * i)))

type Fait = (r: Rapport, message: string) => void

/** Refus du service pour la fenêtre : message traduit par code, et le détail rendu par le service s'il apporte plus. */
function useRefusRapport() {
  const erreur = useErreur()
  return (e: unknown) => {
    const r = erreur(e)
    if (!r) return undefined
    const detail = estApiError(e) && e.message && e.message !== r.titre ? e.message : undefined
    return { title: r.titre, correlationId: r.correlationId, text: detail }
  }
}

/** Annonce d'une soumission : remis à qui et pourquoi, ou déposé. */
function useAnnonceSoumission() {
  const { t } = useTranslation()
  return (r: Rapport) => (r.etat === 'DEPOSE' ? t('act.rapport.depose')
    : r.destinataire ? t('act.rapport.remis', { nom: r.destinataire.nomAffiche, motif: t(`act.rapport.motif.${r.destinataireMotif ?? 'N_PLUS_UN'}`) }) : t('act.rapport.soumis'))
}

/**
 * Nouveau rapport, ou modification d'un brouillon (`rapport`) : type et période, mode, contenu ou document ;
 * « Enregistrer le brouillon » ou « Enregistrer et soumettre ». Un rapport créé au premier essai est
 * ensuite modifié, jamais recréé (un seul rapport par période).
 */
export function DialogueRapport({ rapport, type: typeInitial = 'QUOTIDIEN', periode: periodeInitiale, onFermer, onFait }: {
  rapport?: Rapport; type?: TypeRapport; periode?: string; onFermer: () => void; onFait: Fait
}) {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useRefusRapport()
  const annonce = useAnnonceSoumission()
  const services = useServices()
  const organisation = useOrganisation()
  const creer = useCreerRapport()
  const modifier = useModifierRapport()
  const soumettre = useSoumettre()
  const preremplir = usePreremplir()
  const [type, setType] = useState<TypeRapport>(rapport?.type ?? typeInitial)
  const [jour, setJour] = useState(versSaisie(rapport?.type === 'QUOTIDIEN' ? rapport.periode : typeInitial === 'QUOTIDIEN' && periodeInitiale ? periodeInitiale : aujourdhui()))
  const [semaine, setSemaine] = useState(rapport?.type === 'HEBDOMADAIRE' ? rapport.periode : typeInitial === 'HEBDOMADAIRE' && periodeInitiale ? periodeInitiale : semaineIso(aujourdhui()))
  const [mode, setMode] = useState<ModeRapport>(rapport?.mode ?? 'PREREMPLI')
  const [contenu, setContenu] = useState(rapport?.contenu ?? '')
  const [fichier, setFichier] = useState<File | null>(null)
  const [progression, setProgression] = useState<Progression | null>(null)
  const [cree, setCree] = useState<Rapport | undefined>(rapport)
  const [enCours, setEnCours] = useState(false)
  const [echec, setEchec] = useState<unknown>(null)
  const [tente, setTente] = useState(false)
  const arret = useRef<AbortController | null>(null)
  useEffect(() => () => arret.current?.abort(), [])
  const periode = type === 'QUOTIDIEN' ? depuisSaisie(jour) : semaine
  const documentRequis = mode === 'DOCUMENT' && !fichier && !cree?.document
  const erreurs = {
    periode: !periode ? t('act.erreur.date') : undefined,
    contenu: mode !== 'DOCUMENT' && !contenu.trim() ? t('act.rapport.erreur.contenu') : contenu.length > LIMITES_RAPPORT.contenuMax ? t('act.rapport.erreur.long', { max: LIMITES_RAPPORT.contenuMax }) : undefined,
    document: documentRequis ? t('act.rapport.erreur.document') : undefined,
  }
  const enregistrer = async (etSoumettre: boolean) => {
    setTente(true)
    setEchec(null)
    if (Object.values(erreurs).some(Boolean) || !periode) return
    setEnCours(true)
    try {
      const entree = { type, periode, mode, contenu: contenu.trim() || null }
      let r = cree ?? (await creer.mutateAsync(entree))
      setCree(r)
      if (mode === 'DOCUMENT' && fichier) {
        arret.current = new AbortController()
        const depot = await services.pilotage.deposerDocument(fichier, r.id, { signal: arret.current.signal, surProgression: setProgression })
        r = await modifier.mutateAsync({ id: r.id, r: { ...entree, document: { ticket: depot.documentId } } })
        setFichier(null)
      } else if (cree) {
        r = await modifier.mutateAsync({ id: r.id, r: entree })
      }
      setCree(r)
      if (etSoumettre) r = await soumettre.mutateAsync(r.id)
      onFait(r, etSoumettre ? annonce(r) : t('act.rapport.enregistre'))
    } catch (e) {
      if (!(e instanceof DOMException && e.name === 'AbortError')) setEchec(e)
    } finally {
      setProgression(null)
      setEnCours(false)
    }
  }
  const responsable = organisation.data?.responsable
  const refus = erreur(echec ?? preremplir.error)
  const pourcent = progression ? Math.floor((progression.envoye / Math.max(progression.total, 1)) * 100) : 0
  return (
    <CrudDialog
      context={rapport ? 'edit' : 'create'}
      id="gm-dlg-rapport"
      title={t(rapport ? 'act.rapport.modifier' : 'act.rapport.nouveau')}
      subject={cree ? `${t(`act.rapport.type.${cree.type}`)} · ${dates.periode(cree.type, cree.periode)}` : undefined}
      description={responsable ? t('act.rapport.destinataire', { nom: responsable.nomAffiche }) : organisation.isSuccess ? t('act.rapport.sansResponsable') : undefined}
      onClose={() => {
        arret.current?.abort()
        onFermer()
      }}
      onSubmit={() => void enregistrer(true)}
      submitLabel={t('act.rapport.soumettre')}
      submitIcon="send"
      submitting={enCours}
      dirty={!rapport && (contenu.trim() !== '' || !!fichier)}
      error={refus}
      extraActions={<Button variant="secondary" icon="file-text" disabled={enCours} onClick={() => void enregistrer(false)}>{t('act.rapport.enregistrerBrouillon')}</Button>}
    >
      {!cree ? (
        <>
          <RadioGroup name="type" label={t('act.rapport.typeLibelle')} value={type} onChange={(v) => setType(v as TypeRapport)}
            options={(['QUOTIDIEN', 'HEBDOMADAIRE'] as const).map((x) => ({ value: x, label: t(`act.rapport.type.${x}`) }))} />
          {type === 'QUOTIDIEN' ? (
            <DateField name="periode" label={t('act.rapport.jour')} required value={jour} onChange={setJour} error={tente ? erreurs.periode : undefined} />
          ) : (
            <Select name="semaine" label={t('act.rapport.semaine')} value={semaine} onChange={setSemaine} options={semaines().map((s) => ({ value: s, label: dates.periode('HEBDOMADAIRE', s) }))} />
          )}
        </>
      ) : null}
      <RadioGroup name="mode" label={t('act.rapport.mode')} value={mode} onChange={(v) => setMode(v as ModeRapport)} options={MODES.map((m) => ({ value: m, label: t(`act.rapport.mode.${m}`) }))} />
      <Note>{t(`act.rapport.mode.${mode}.aide`)}</Note>
      {mode === 'PREREMPLI' ? (
        <span>
          <Button variant="secondary" icon="refresh" loading={preremplir.isPending} disabled={!periode}
            onClick={() => periode && preremplir.mutate({ type, periode }, { onSuccess: (r) => setContenu(r.contenu) })}>
            {t('act.rapport.preremplir')}
          </Button>
        </span>
      ) : null}
      {mode === 'DOCUMENT' ? (
        <FileDropzone
          name="document"
          label={t('act.rapport.document')}
          required
          accept={LIMITES_RAPPORT.types}
          maxSize="50 Mo"
          help={cree?.document ? t('act.rapport.documentActuel', { nom: cree.document.nom }) : undefined}
          error={tente ? erreurs.document : undefined}
          file={fichier ? { name: fichier.name, size: taille(fichier.size) } : undefined}
          state={progression ? 'uploading' : fichier ? 'done' : 'idle'}
          progress={pourcent}
          onFile={setFichier}
          onRemove={() => setFichier(null)}
        />
      ) : null}
      {progression ? <span className="gm-field-help" aria-live="polite">{t(`act.rapport.depot.${progression.phase}`, { pourcent })}</span> : null}
      <TextArea
        name="contenu"
        label={t(mode === 'DOCUMENT' ? 'act.rapport.accompagnement' : 'act.rapport.contenu')}
        help={t('act.rapport.contenuAide', { max: LIMITES_RAPPORT.contenuMax })}
        rows={mode === 'DOCUMENT' ? 3 : 12}
        required={mode !== 'DOCUMENT'}
        value={contenu}
        onChange={setContenu}
        error={tente ? erreurs.contenu : undefined}
      />
    </CrudDialog>
  )
}

/** Consultation d'un rapport : texte rendu, document (aperçu), visa ; gestes de l'auteur ou du destinataire. */
export function ConsultationRapport({ id, onFermer, onFait }: { id: string; onFermer: () => void; onFait: Fait }) {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const refusRapport = useRefusRapport()
  const annonce = useAnnonceSoumission()
  const session = useSession()
  const rapport = useRapport(id)
  const soumettre = useSoumettre()
  const viser = useViser()
  const telecharger = useDocumentRapport()
  // Aperçu des documents bureautiques joints au rapport (D-83) : conversion en PDF par MS-DOCUMENT.
  const convertir = useServices().apercu?.convertir
  const [commentaire, setCommentaire] = useState('')
  const [edition, setEdition] = useState(false)
  const [apercu, setApercu] = useState<{ blob?: Blob; erreur?: string } | null>(null)
  const r = rapport.data
  const brouillonAuteur = !!r && r.auteur.id === session?.sujet && r.etat === 'BROUILLON'
  // Avant la soumission : destinataire trouvé d'office et choix dans la chaîne hiérarchique (§2.4).
  const destinataires = useDestinatairesRapport(id, brouillonAuteur)
  const [choisi, setChoisi] = useState('')
  const defaut = destinataires.data?.destinataire?.id ?? ''
  const cible = choisi || defaut
  const motifCible = destinataires.data ? (cible === defaut ? destinataires.data.motif : 'CHOISI') : null
  if (edition && r) return <DialogueRapport rapport={r} onFermer={() => setEdition(false)} onFait={onFait} />
  const auteur = r?.auteur.id === session?.sujet
  const brouillon = auteur && r?.etat === 'BROUILLON'
  const aViser = !!r && !auteur && r.etat === 'SOUMIS'
  const geste = brouillon ? soumettre : viser
  const ouvrirDocument = () => {
    setApercu({})
    // Aperçu refermé avant la réponse : il ne se rouvre pas.
    telecharger.mutate(id, { onSuccess: (x) => setApercu((a) => (a ? { blob: x.blob } : a)), onError: (e: ApiError) => setApercu((a) => (a ? { erreur: erreur(e)?.titre } : a)) })
  }
  return (
    <>
      <CrudDialog
        context={brouillon || aViser ? 'edit' : 'detail'}
        id="gm-dlg-consultation-rapport"
        title={r ? t(`act.rapport.titre.${r.type}`, { nom: r.auteur.nomAffiche }) : t('act.rapport.consultation')}
        subject={r ? dates.periode(r.type, r.periode) : undefined}
        onClose={onFermer}
        cancelLabel={t('act.fermer')}
        submitLabel={brouillon ? t('act.rapport.soumettre') : aViser ? t('act.rapport.viser') : undefined}
        submitIcon={brouillon ? 'send' : 'check'}
        onSubmit={() => {
          if (brouillon) soumettre.mutate({ id, destinataireId: cible && cible !== defaut ? cible : undefined }, { onSuccess: (x) => onFait(x, annonce(x)) })
          else viser.mutate({ id, commentaire }, { onSuccess: (x) => onFait(x, t('act.rapport.vise')) })
        }}
        submitting={geste.isPending}
        error={brouillon ? refusRapport(geste.error) : pourDialogue(erreur(geste.error))}
        extraActions={brouillon ? <Button variant="secondary" icon="pen-line" onClick={() => setEdition(true)}>{t('act.rapport.modifier')}</Button> : undefined}
      >
        {rapport.isError ? <InlineAlert level="crit" correlationId={rapport.error.correlationId || undefined}>{erreur(rapport.error)?.titre}</InlineAlert> : !r ? <Skeleton height={240} /> : (
          <>
            <KeyValueList
              items={[
                { key: t('act.rapport.etat'), value: <BadgeRapport etat={r.etat} /> },
                { key: t('act.rapport.auteur'), value: r.auteur.nomAffiche },
                {
                  key: t('act.rapport.destinataireLibelle'),
                  value: r.etat === 'DEPOSE' ? t('act.rapport.aucunDestinataire') : r.destinataire
                    ? `${r.destinataire.nomAffiche}${r.destinataireMotif ? ` (${t(`act.rapport.motif.${r.destinataireMotif}`)})` : ''}` : t('act.rapport.pasEncore'),
                },
                { key: t('act.rapport.mode'), value: t(`act.rapport.modeValeur.${r.mode}`) },
                ...(r.soumisLe ? [{ key: t('act.rapport.soumisLe'), value: dates.instant(r.soumisLe) }] : []),
                { key: t('act.rapport.indicateurs'), value: t('act.rapport.indicateursTexte', {
                  // QUO-13 : accords au singulier et au pluriel.
                  activites: t('act.rapport.activitesFaites', { count: r.indicateurs.activitesFaites, prevues: r.indicateurs.activitesPrevues }),
                  duree: dates.duree(r.indicateurs.minutes) || '0 min', taches: t('act.rapport.tachesTerminees', { count: r.indicateurs.tachesTerminees }),
                }) },
              ]}
            />
            {brouillon && destinataires.data ? (
              destinataires.data.depot ? <InlineAlert level="info">{t('act.rapport.seraDepose')}</InlineAlert> : (
                <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
                  <InlineAlert level="info">
                    {t('act.rapport.seraRemis', { nom: destinataires.data.choix.find((c) => c.id === cible)?.nomAffiche ?? destinataires.data.destinataire?.nomAffiche ?? cible, motif: t(`act.rapport.motif.${motifCible ?? 'N_PLUS_UN'}`) })}
                  </InlineAlert>
                  {destinataires.data.choix.length > 1 ? (
                    <Select name="destinataire" label={t('act.rapport.choixDestinataire')} help={t('act.rapport.choixAide')} value={cible} onChange={setChoisi}
                      options={destinataires.data.choix.map((c) => ({ value: c.id, label: `${c.nomAffiche ?? c.id} · ${t(`act.rapport.motif.${c.lien}`)}` }))} />
                  ) : null}
                </div>
              )
            ) : brouillon && destinataires.isError ? <InlineAlert level="warn">{erreur(destinataires.error)?.titre}</InlineAlert> : null}
            {r.visa ? (
              <Note icon="circle-check" title={t('act.rapport.viseLe', { nom: r.visa.par.nomAffiche, quand: r.viseLe ? dates.instant(r.viseLe) : '' })}>
                {r.visa.commentaire ?? t('act.rapport.sansCommentaire')}
              </Note>
            ) : null}
            {r.document ? (
              <div className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
                <span>{r.document.nom}</span>
                <span className="gm-field-help">{taille(r.document.taille)}</span>
                <Button size="sm" variant="secondary" icon="eye" onClick={ouvrirDocument}>{t('act.rapport.voirDocument')}</Button>
              </div>
            ) : null}
            {r.contenu ? <div className="gm-panel"><TexteRapport contenu={r.contenu} /></div> : null}
            {aViser ? <TextArea name="commentaire" label={t('act.rapport.commentaire')} help={t('act.rapport.commentaireAide')} value={commentaire} onChange={setCommentaire} maxLength={2000} /> : null}
          </>
        )}
      </CrudDialog>
      {apercu && r?.document ? (
        <AttachmentViewer
          nom={r.document.nom}
          typeMime={r.document.typeMime}
          taille={taille(r.document.taille)}
          blob={apercu.blob}
          erreur={apercu.erreur}
          onClose={() => setApercu(null)}
          onTelecharger={() => (apercu.blob ? enregistrerFichier(apercu.blob, r.document!.nom) : undefined)}
          convertir={convertir}
        />
      ) : null}
    </>
  )
}
