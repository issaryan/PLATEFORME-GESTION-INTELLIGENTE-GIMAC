import { useCallback, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { ReactFlowProvider } from '@xyflow/react'
import { Button, ConfirmDialog, CrudDialog, DataTable, Icon, InlineAlert, KeyValueList, NumberField, PageHeader, Select, TextArea, TextField, cx, type Column } from '@gimac/design-system'
import { EtatsRequete, estApiError, type ApiError } from '@gimac/socle'
import { LIMITES_PROCESSUS, etapeVierge, type AnomalieProcessus, type EtapeProcessus, type Initiateurs, type Processus, type SaisieProcessus, type SaisiePublic } from '../../../modele/processus'
import { useControleProcessus, useEcritureProcessus, usePorteeProcessus, useProcessus, useSupprimerProcessus } from '../../../hooks/processus'
import { usePouvoirs } from '../../../hooks/pouvoirs'
import { useDates, useLibelleUnite } from '../activite/communs'
import { CanevasProcessus, type CarteEtape } from './CanevasProcessus'
import { ModaleEtape, type OngletEtape } from './ModaleEtape'
import { DialogueLancement } from './DialogueLancement'
import { ModaleLancement } from './ModaleLancement'
import { BadgeGravite, BadgeProcessus, delais, useErreurProcessus, useResumeInitiateurs, useResumeIntervenant } from './communs'
import { useLibelleProfilEquipe } from '../activite/dialoguesEquipe'
import { useUnites } from '../../../hooks/pilotage'

// Modélisateur d'un processus d'unité (D-87 §4, /des/processus/:id) : canevas générique partagé avec le modélisateur des
// référentiels (D-77) ou vue liste équivalente au clavier ; une carte par étape, les prédécesseurs en arcs ; modale
// d'étape à onglets ; contrôle (anomalies BLOQUANTE, LEVABLE, AVERTISSEMENT), publication (levée motivée des
// avertissements), versions, nouvelle version, retrait, lancement. Brouillon : chaque geste enregistre la définition
// (PUT /processus/{id}) ; version publiée ou retirée : lecture seule, sauf le public d'une version publiée (D-103,
// PUT /processus/{id}/public, motif obligatoire, sans nouvelle version) pour qui a `droits.modifierPublic`.

type Modale = { creation: true } | { creation: false; code: string; onglet?: OngletEtape }
type Confirmation = 'publier' | 'lever' | 'retirer' | 'supprimer' | null

const sansDetail = ({ personnesDetail: _d, ...i }: Initiateurs): Initiateurs => i

export const saisieDe = (p: Processus, etapes = p.etapes): SaisieProcessus => ({
  code: p.code, unite: p.unite, libelle: p.libelle, description: p.description, categorie: p.categorie, delaiCibleJours: p.delaiCibleJours,
  // D-106 : délai maximal du processus (absent d'un service antérieur).
  ...(p.delaiMaxJours !== undefined ? { delaiMaxJours: p.delaiMaxJours } : {}), etapes,
  // D-98 : initiateurs et formulaire de lancement repartent avec chaque enregistrement (absents d'un service antérieur).
  // D-100 : le détail des personnes désignées (noms) est une vue du service, jamais renvoyé.
  ...(p.initiateurs ? { initiateurs: sansDetail(p.initiateurs) } : {}), ...(p.formulaire ? { formulaire: p.formulaire } : {}),
})

/**
 * Recette HOM-15 : message d'anomalie du contrôle (MS-PILOTAGE) rendu lisible : profils (ROLE_*) et unités nommés par leur
 * libellé, jamais par leur code. Mise en forme seulement : le service décide de l'anomalie.
 */
/** Clé provisoire libre d'une étape nouvelle (ETAPE_n) ; le service la remplace par le code généré (D-136). */
export const cleLibre = (etapes: { code: string }[]): number => {
  let n = etapes.length + 1
  while (etapes.some((e) => e.code === `ETAPE_${n}`)) n++
  return n
}
const echapper = (x: string) => x.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
function useMessageLisible() {
  const libelleProfil = useLibelleProfilEquipe()
  const libelleUnite = useLibelleUnite()
  const unites = useUnites()
  return useCallback((m: string) => {
    let s = m.replace(/\bROLE_[A-Z0-9_]+\b/g, (r) => libelleProfil(r))
    const codes = (unites.data ?? []).map((u) => u.code).sort((a, b) => b.length - a.length)
    if (codes.length) s = s.replace(new RegExp(`(unité |\\(|, )(${codes.map(echapper).join('|')})(?=[ ,;:.)]|$)`, 'g'), (_x, avant: string, c: string) => avant + libelleUnite(c))
    return s
  }, [libelleProfil, libelleUnite, unites.data])
}

export default function ModelisateurProcessusPage() {
  const { id } = useParams()
  const { t } = useTranslation()
  const p = useProcessus(id)
  return (
    <EtatsRequete etat={p} emptyTitle={t('commun.vide')} errorTitle={t('proc.erreur.lecture')} retryLabel={t('commun.reessayer')}>
      {(x) => <ReactFlowProvider><Espace key={x.id} processus={x} /></ReactFlowProvider>}
    </EtatsRequete>
  )
}

function Proprietes({ processus, enCours, erreur, onFermer, onEnregistrer }: { processus: Processus; enCours: boolean; erreur?: { titre: string; correlationId?: string }; onFermer: () => void; onEnregistrer: (s: SaisieProcessus) => void }) {
  const { t } = useTranslation()
  const initial = {
    libelle: processus.libelle, description: processus.description, categorie: processus.categorie,
    delai: processus.delaiCibleJours === null ? '' : String(processus.delaiCibleJours),
    delaiMax: processus.delaiMaxJours === null || processus.delaiMaxJours === undefined ? '' : String(processus.delaiMaxJours),
  }
  const [v, setV] = useState(initial)
  const [tente, setTente] = useState(false)
  const delai = v.delai.trim() === '' ? null : Number(v.delai)
  const delaiMax = v.delaiMax.trim() === '' ? null : Number(v.delaiMax)
  // D-106 : délai maximal facultatif, entre le délai cible et 365 jours.
  const erreurs = {
    libelle: !v.libelle.trim() ? t('proc.erreur.libelle') : undefined,
    delai: delai !== null && !(delai >= 0 && delai <= 365) ? t('proc.erreur.delai') : undefined,
    delaiMax: delaiMax !== null && !(Number.isInteger(delaiMax) && delaiMax >= (delai ?? 0) && delaiMax <= 365) ? t('proc.erreur.delaiMax') : undefined,
  }
  return (
    <CrudDialog context="edit" size="md" id="gm-dlg-proprietes" title={t('proc.proprietes.titre')} subject={<span className="gm-mono">{processus.code} · v{processus.version}</span>} onClose={onFermer}
      submitLabel={t('proc.enregistrer')} submitting={enCours} dirty={JSON.stringify(v) !== JSON.stringify(initial)}
      error={erreur ? { title: erreur.titre, correlationId: erreur.correlationId } : undefined}
      onSubmit={() => {
        setTente(true)
        if (erreurs.libelle || erreurs.delai || erreurs.delaiMax) return
        onEnregistrer({ ...saisieDe(processus), libelle: v.libelle.trim(), description: v.description.trim(), categorie: v.categorie.trim(), delaiCibleJours: delai, delaiMaxJours: delaiMax })
      }}>
      <TextField name="libelle" label={t('proc.champ.libelle')} required maxLength={LIMITES_PROCESSUS.libelle} value={v.libelle} onChange={(x) => setV((o) => ({ ...o, libelle: x }))} error={tente ? erreurs.libelle : undefined} />
      <TextField name="categorie" label={t('proc.champ.categorie')} maxLength={LIMITES_PROCESSUS.categorie} value={v.categorie} onChange={(x) => setV((o) => ({ ...o, categorie: x }))} />
      <NumberField name="delaiCibleJours" label={t('proc.champ.delaiCible')} unit={t('proc.jours')} min={0} max={365} value={v.delai} onChange={(x) => setV((o) => ({ ...o, delai: x }))} error={tente ? erreurs.delai : undefined} help={t('proc.aide.delaiCibleProcessus')} />
      <NumberField name="delaiMaxJours" label={t('proc.champ.delaiMaxProcessus')} unit={t('proc.jours')} min={0} max={365} value={v.delaiMax} onChange={(x) => setV((o) => ({ ...o, delaiMax: x }))} error={tente ? erreurs.delaiMax : undefined} help={t('proc.aide.delaiMaxProcessus')} />
      <TextArea name="description" label={t('proc.champ.description')} rows={4} maxLength={LIMITES_PROCESSUS.description} value={v.description} onChange={(x) => setV((o) => ({ ...o, description: x }))} />
    </CrudDialog>
  )
}

function Espace({ processus: p }: { processus: Processus }) {
  const { t } = useTranslation()
  const lisible = useMessageLisible()
  const navigate = useNavigate()
  const dates = useDates()
  const libelleUnite = useLibelleUnite()
  const resume = useResumeIntervenant()
  const erreur = useErreurProcessus()
  const pouvoirs = usePouvoirs()
  const [params, setParams] = useSearchParams()
  const vue = params.get('vue') === 'liste' ? 'liste' : 'canevas'
  const lecture = !p.droits.modifier
  // D-103 : le public se règle sur un brouillon comme sur une version publiée (repli d'un service antérieur : modifier).
  const reglerPublic = p.droits.modifierPublic ?? p.droits.modifier
  const publicPublie = reglerPublic && p.etat === 'PUBLIEE'
  // D-92 : droits servis par MS-PILOTAGE, portée tous domaines, ou PROCESSUS_DEFINIR sur une unité dirigée.
  const portee = usePorteeProcessus()
  const peutDefinir = p.droits.modifier || p.droits.publier || p.droits.retirer || !!portee.data?.definirToutesUnites
    || (pouvoirs.data?.actions.PROCESSUS_DEFINIR === 'UNITES' && pouvoirs.data.unitesDirigees.includes(p.unite))
  const enregistrer = useEcritureProcessus((s, v: SaisieProcessus) => s.processus.modifier(p.id, v))
  const publier = useEcritureProcessus((s, v: { motif?: string; leverAvertissements?: boolean }) => s.processus.publier(p.id, v))
  const nouvelleVersion = useEcritureProcessus((s) => s.processus.nouvelleVersion(p.id))
  const retirer = useEcritureProcessus((s, motif: string) => s.processus.retirer(p.id, motif))
  const modifierPublic = useEcritureProcessus((s, v: SaisiePublic) => s.processus.modifierPublic(p.id, v))
  const supprimer = useSupprimerProcessus()
  const controle = useControleProcessus()
  const [modale, setModale] = useState<Modale | null>(null)
  const [proprietes, setProprietes] = useState(false)
  const [lancement, setLancement] = useState(false)
  // D-100 : accès direct au réglage du public (?reglage=public, depuis la liste ou la création).
  const [reglageLancement, setReglageLancement] = useState(params.get('reglage') === 'public')
  const nouveau = params.get('cree') === '1'
  const fermerReglage = () => {
    setReglageLancement(false)
    if (params.get('reglage')) setParams((x) => { const n = new URLSearchParams(x); n.delete('reglage'); return n }, { replace: true })
  }
  const resumeInitiateurs = useResumeInitiateurs()
  const [confirmation, setConfirmation] = useState<Confirmation>(null)
  const [anomalies, setAnomalies] = useState<AnomalieProcessus[] | null>(null)
  const [focus, setFocus] = useState<string | null>(null)
  const [echec, setEchec] = useState<unknown>(null)
  const [annonce, setAnnonce] = useState('')

  const sauver = useCallback((etapes: EtapeProcessus[], message: string, apres?: () => void) => {
    setEchec(null)
    enregistrer.mutate(saisieDe(p, etapes), { onSuccess: () => { setAnnonce(message); apres?.() }, onError: setEchec })
  }, [enregistrer, p])
  const relier = useCallback((de: string, vers: string) => {
    if (p.etapes.find((e) => e.code === vers)?.predecesseurs.includes(de)) return
    sauver(p.etapes.map((e) => (e.code === vers ? { ...e, predecesseurs: [...e.predecesseurs, de] } : e)), t('proc.annonce.relie', { de, vers }))
  }, [p.etapes, sauver, t])
  const delier = useCallback((de: string, vers: string) => sauver(p.etapes.map((e) => (e.code === vers ? { ...e, predecesseurs: e.predecesseurs.filter((x) => x !== de) } : e)), t('proc.annonce.delie', { de, vers })), [p.etapes, sauver, t])
  const placer = useCallback((l: { code: string; x: number; y: number }[]) => sauver(p.etapes.map((e) => {
    const x = l.find((y) => y.code === e.code)
    return x ? { ...e, position: { x: x.x, y: x.y } } : e
  }), t('proc.annonce.place')), [p.etapes, sauver, t])
  const ouvrir = useCallback((code: string) => setModale({ creation: false, code }), [])
  const controler = () => {
    setEchec(null)
    controle.mutate(p.id, { onSuccess: (r) => { setAnomalies(r.anomalies); setAnnonce(r.publiable ? t('proc.controle.publiable') : t('proc.controle.nonPubliable')) }, onError: setEchec })
  }
  const surPublication = (e: ApiError) => {
    const d = (e.details as { anomalies?: AnomalieProcessus[] } | undefined)?.anomalies
    if (e.code === 'PIL_PROCESSUS_AVERTISSEMENTS') {
      setAnomalies(d ?? [])
      setConfirmation('lever')
      return
    }
    if (d) setAnomalies(d)
    setEchec(e)
  }

  const surlignes = useMemo(() => new Set([...(anomalies ?? []).map((a) => a.etape).filter((x): x is string => !!x), ...(focus ? [focus] : [])]), [anomalies, focus])
  const cartes = useMemo<CarteEtape[]>(() => p.etapes.map((e) => ({
    code: e.code, libelle: e.libelle, predecesseurs: e.predecesseurs, position: e.position,
    lignes: [
      resume(e.intervenant, p.personnes),
      t('proc.carte.elements', { livrables: e.livrables.length, signatures: e.signatures.length, champs: e.champs.length }),
      `${delais(t, e.delaiJours, e.delaiMaxJours)}${e.validateur ? ` · ${t('proc.carte.valide')}` : ''}`,
    ],
    anomalies: (anomalies ?? []).filter((a) => a.etape === e.code && a.gravite !== 'AVERTISSEMENT').length,
  })), [p.etapes, p.personnes, anomalies, resume, t])
  const etape = modale && !modale.creation ? p.etapes.find((e) => e.code === modale.code) : null
  const refus = erreur(echec)
  // D-103 : refus du réglage du public, avec les anomalies bloquantes (422 PIL_PROCESSUS_ANOMALIES) restituées dans la modale.
  const echecPublic = enregistrer.error ?? modifierPublic.error
  const anomaliesPublic = estApiError(echecPublic) ? (echecPublic.details as { anomalies?: AnomalieProcessus[] } | undefined)?.anomalies ?? [] : []
  const erreurPublic = (() => {
    const e = erreur(echecPublic)
    if (!e) return undefined
    return {
      ...e,
      ...(e.code === 'PIL_PROCESSUS_ANOMALIES' && modifierPublic.error ? { titre: t('dem.modele.anomaliesPublic') } : {}),
      detail: anomaliesPublic.length ? (
        <ul data-testid="anomalies-public" style={{ margin: 0, paddingLeft: 'var(--gm-space-4)' }}>
          {anomaliesPublic.map((a, i) => <li key={`${a.code}-${i}`}><strong>{t(`proc.anomalie.${a.code}`, { defaultValue: a.code })}</strong> · {lisible(a.message)}</li>)}
        </ul>
      ) : undefined,
    }
  })()
  const basculerVue = (v: 'canevas' | 'liste') => setParams((x) => {
    const n = new URLSearchParams(x)
    if (v === 'liste') n.set('vue', 'liste')
    else n.delete('vue')
    return n
  })

  const colonnes: Column<EtapeProcessus & Record<string, unknown>>[] = [
    { key: 'code', header: t('proc.champ.code'), type: 'mono', width: 120 },
    { key: 'libelle', header: t('proc.champ.libelle') },
    { key: 'intervenant', header: t('proc.intervenant.executant'), render: (e) => resume(e.intervenant, p.personnes) },
    { key: 'validateur', header: t('proc.intervenant.validateur'), render: (e) => (e.validateur ? resume(e.validateur, p.personnes) : '') },
    { key: 'predecesseurs', header: t('proc.champ.predecesseurs'), render: (e) => e.predecesseurs.join(', ') },
    { key: 'elements', header: t('proc.champ.elements'), render: (e) => t('proc.carte.elements', { livrables: e.livrables.length, signatures: e.signatures.length, champs: e.champs.length }) },
    { key: 'delaiJours', header: t('proc.champ.delais'), render: (e) => delais(t, e.delaiJours, e.delaiMaxJours) },
    {
      key: 'actions', header: '', type: 'actions', render: (e) => (
        <Button size="sm" variant="ghost" icon={lecture ? 'eye' : 'pen-line'} aria-label={t(lecture ? 'proc.etape.consulterQui' : 'proc.etape.modifierQui', { code: e.code })} onClick={() => ouvrir(e.code)}>
          {t(lecture ? 'proc.etape.consulter' : 'proc.etape.modifier')}
        </Button>
      ),
    },
  ]

  return (
    <div className="gm-col" data-testid="modelisateur-processus" style={{ gap: 'var(--gm-space-3)' }}>
      <PageHeader title={p.libelle} subtitle={`${p.code} · v${p.version} · ${libelleUnite(p.unite)}`}
        actions={<span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}><BadgeProcessus etat={p.etat} /><Button size="sm" variant="ghost" icon="arrow-left" onClick={() => navigate('/des/processus')}>{t('proc.retourCatalogue')}</Button></span>} />
      <div role="toolbar" aria-label={t('proc.outils')} className="gm-mod-outils">
        <div className="gm-row" role="group" aria-label={t('proc.vue.titre')} style={{ gap: 0 }}>
          {(['canevas', 'liste'] as const).map((v) => (
            <button key={v} type="button" className={cx('gm-btn', 'gm-btn-sm', vue === v ? 'gm-btn-primary' : 'gm-btn-secondary')} aria-pressed={vue === v} onClick={() => basculerVue(v)}>
              <Icon name={v === 'canevas' ? 'workflow' : 'menu'} size={20} />{t(`proc.vue.${v}`)}
            </button>
          ))}
        </div>
        {!lecture ? <Button size="sm" icon="plus" onClick={() => setModale({ creation: true })}>{t('proc.etape.ajouter')}</Button> : null}
        {!lecture ? <Button size="sm" icon="pen-line" onClick={() => setProprietes(true)}>{t('proc.proprietes.titre')}</Button> : null}
        <Button size="sm" icon="users" onClick={() => setReglageLancement(true)}>{t('dem.modele.bouton')}</Button>
        {/* D-106 (§12) : le contrôle relève de la définition ; le public seul ne le voit pas. */}
        {peutDefinir ? <Button size="sm" icon="shield-check" loading={controle.isPending} onClick={controler}>{t('proc.controler')}</Button> : null}
        {p.droits.publier ? <Button size="sm" variant="primary" icon="check" onClick={() => setConfirmation('publier')}>{t('proc.publier')}</Button> : null}
        {p.droits.lancer ? <Button size="sm" variant="primary" icon="arrow-right" onClick={() => setLancement(true)}>{t('proc.lancer')}</Button> : null}
        {(p.droits.nouvelleVersion ?? (p.etat !== 'BROUILLON' && peutDefinir)) ? <Button size="sm" icon="copy" loading={nouvelleVersion.isPending}
          onClick={() => nouvelleVersion.mutate(undefined, { onSuccess: (n) => navigate(`/des/processus/${encodeURIComponent(n.id)}`), onError: setEchec })}>{t('proc.nouvelleVersion')}</Button> : null}
        {p.droits.retirer ? <Button size="sm" variant="danger" onClick={() => setConfirmation('retirer')}>{t('proc.retirer')}</Button> : null}
        {p.droits.modifier ? <Button size="sm" variant="ghost" icon="trash" onClick={() => setConfirmation('supprimer')}>{t('proc.supprimer')}</Button> : null}
        {p.versions.length > 1 ? (
          <Select name="version" label={t('proc.versions')} value={p.id} onChange={(v) => navigate(`/des/processus/${encodeURIComponent(v)}`)}
            options={p.versions.map((v) => ({ value: v.id, label: `v${v.version} · ${t(`proc.etat.${v.etat}`)}` }))} />
        ) : null}
      </div>
      <KeyValueList items={[
        { key: t('proc.champ.etapes'), value: String(p.etapes.length) },
        {
          key: t('dem.resume.titre'), value: (
            <span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }} data-testid="public-processus">
              <span>{resumeInitiateurs(p.initiateurs, p.unite)}</span>
              <Button size="sm" variant="ghost" icon={reglerPublic ? 'pen-line' : 'eye'} aria-label={t(reglerPublic ? 'dem.resume.reglerQui' : 'dem.resume.voirQui', { libelle: p.libelle })}
                onClick={() => setReglageLancement(true)}>{t(reglerPublic ? 'dem.resume.regler' : 'dem.resume.voir')}</Button>
            </span>
          ),
        },
        { key: t('dem.modele.formulaire'), value: t('dem.lancer.champs', { count: p.formulaire?.champs.length ?? 0 }) },
        ...(p.categorie ? [{ key: t('proc.champ.categorie'), value: p.categorie }] : []),
        ...(p.delaiCibleJours !== null ? [{ key: t('proc.champ.delaiCible'), value: t('proc.carte.delai', { n: p.delaiCibleJours }) }] : []),
        ...(p.delaiMaxJours !== null && p.delaiMaxJours !== undefined ? [{ key: t('proc.champ.delaiMaxProcessus'), value: t('proc.carte.delai', { n: p.delaiMaxJours }) }] : []),
        ...(p.publieeLe ? [{ key: t('proc.champ.publieeLe'), value: `${dates.instant(p.publieeLe)}${p.publieePar ? ` · ${p.publieePar.nomAffiche}` : ''}${p.motifPublication ? ` · ${p.motifPublication}` : ''}` }] : []),
        ...(p.retireeLe ? [{ key: t('proc.champ.retireeLe'), value: `${dates.instant(p.retireeLe)}${p.motifRetrait ? ` · ${p.motifRetrait}` : ''}` }] : []),
      ]} />
      {p.description ? <p className="gm-field-help" style={{ margin: 0 }}>{p.description}</p> : null}
      {nouveau && !lecture ? (
        <InlineAlert level="info" title={t('dem.creation.titre')}
          action={<Button size="sm" icon="users" onClick={() => setReglageLancement(true)}>{t('dem.creation.regler')}</Button>}>
          {t('dem.creation.texte', { public: resumeInitiateurs(p.initiateurs, p.unite) })}
        </InlineAlert>
      ) : null}
      {lecture ? <InlineAlert level="info">{t(p.etat === 'BROUILLON' ? 'proc.lectureSeule' : publicPublie ? 'proc.lecturePublieePublic' : 'proc.lecturePubliee')}</InlineAlert> : null}
      {p.avertissements.map((a, i) => <InlineAlert key={i} level={a.code === 'COUVERTURE_HORS_UNITE' ? 'info' : 'warn'} title={t(`proc.anomalie.${a.code}`, { defaultValue: lisible(a.message) })}>{a.etape ? `${a.etape} · ${lisible(a.message)}` : lisible(a.message)}</InlineAlert>)}
      {refus ? (
        <InlineAlert level="crit" title={refus.titre} correlationId={refus.correlationId}
          action={<Button size="sm" variant="ghost" icon="x" onClick={() => setEchec(null)}>{t('commun.fermer')}</Button>} />
      ) : null}
      <div className="gm-sr-only" role="status" aria-live="polite">{annonce}</div>
      {anomalies ? (
        <section aria-label={t('proc.controle.titre')} className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
          <div className="gm-row" style={{ justifyContent: 'space-between' }}>
            <h2 className="gm-type-h3" style={{ margin: 0 }}>{t('proc.controle.titre')}</h2>
            <Button size="sm" variant="ghost" icon="x" onClick={() => { setAnomalies(null); setFocus(null) }}>{t('proc.controle.masquer')}</Button>
          </div>
          {anomalies.length ? (
            <ul className="gm-proc-anomalies">
              {anomalies.map((a, i) => (
                <li key={`${a.code}-${i}`}>
                  <button type="button" className="gm-proc-anomalie" onClick={() => { if (a.etape) { setFocus(a.etape); setModale({ creation: false, code: a.etape }) } }}>
                    <BadgeGravite gravite={a.gravite} />
                    <strong>{t(`proc.anomalie.${a.code}`, { defaultValue: a.code })}</strong>
                    <span>{[a.etape, a.element].filter(Boolean).join(' · ')}{a.message ? ` · ${lisible(a.message)}` : ''}</span>
                  </button>
                </li>
              ))}
            </ul>
          ) : <InlineAlert level="info">{t('proc.controle.conforme')}</InlineAlert>}
        </section>
      ) : null}
      {vue === 'canevas' ? (
        <CanevasProcessus cartes={cartes} lecture={lecture} surlignes={surlignes} libelleOuvrir={t(lecture ? 'proc.etape.consulter' : 'proc.etape.modifier')} testId="processus-canevas"
          onOuvrir={ouvrir} onRelier={relier} onDelier={delier} onPlacer={placer} />
      ) : (
        <div data-testid="processus-liste" className="gm-proc-liste">
          <DataTable columns={colonnes} rows={p.etapes as (EtapeProcessus & Record<string, unknown>)[]} rowKey={(e) => e.code} state={p.etapes.length ? 'ok' : 'empty'} density="compact"
            selected={[...surlignes]} emptyText={t('proc.etape.aucune')} />
        </div>
      )}
      {!p.etapes.length && vue === 'canevas' ? <p className="gm-field-help" style={{ margin: 0 }}>{t('proc.etape.aucune')}</p> : null}
      {modale && (modale.creation || etape) ? (
        <ModaleEtape key={modale.creation ? 'creation' : modale.code} etape={modale.creation ? etapeVierge(cleLibre(p.etapes)) : etape!} creation={modale.creation} lecture={lecture}
          etapes={p.etapes} uniteProcessus={p.unite} enCours={enregistrer.isPending} erreurServeur={enregistrer.error} ongletInitial={modale.creation ? undefined : modale.onglet}
          onFermer={() => { setModale(null); enregistrer.reset() }}
          onEnregistrer={(e) => {
            const etapes = modale.creation ? [...p.etapes, e] : p.etapes.map((x) => (x.code === e.code ? e : x))
            sauver(etapes, t(modale.creation ? 'proc.annonce.ajoutee' : 'proc.annonce.enregistree', { code: e.code }), () => setModale(null))
          }}
          onSupprimer={() => {
            const code = (modale as { code: string }).code
            sauver(p.etapes.filter((x) => x.code !== code).map((x) => ({ ...x, predecesseurs: x.predecesseurs.filter((c) => c !== code) })), t('proc.annonce.supprimee', { code }), () => setModale(null))
          }} />
      ) : null}
      {proprietes ? <Proprietes processus={p} enCours={enregistrer.isPending} erreur={erreur(enregistrer.error)} onFermer={() => setProprietes(false)}
        onEnregistrer={(s) => enregistrer.mutate(s, { onSuccess: () => { setProprietes(false); setAnnonce(t('proc.annonce.proprietes')) } })} /> : null}
      {reglageLancement ? (
        <ModaleLancement initiateurs={p.initiateurs} formulaire={p.formulaire} lecture={lecture} lecturePublic={!reglerPublic} motifRequis={publicPublie}
          enCours={enregistrer.isPending || modifierPublic.isPending} erreur={erreurPublic}
          onFermer={() => { fermerReglage(); enregistrer.reset(); modifierPublic.reset() }}
          onEnregistrer={(initiateurs, formulaire, motif) => {
            if (!lecture) {
              enregistrer.mutate({ ...saisieDe(p), initiateurs, formulaire }, { onSuccess: () => { fermerReglage(); setAnnonce(t('dem.modele.enregistre')) } })
              return
            }
            // D-103 : version publiée, seul le public part (la séparation des fonctions reste celle de la définition).
            const { regles, unites, profils, permissions, personnes } = initiateurs
            modifierPublic.mutate({ initiateurs: { regles, unites, profils, permissions, personnes }, motif }, { onSuccess: () => { fermerReglage(); setAnnonce(t('dem.modele.publicEnregistre')) } })
          }} />
      ) : null}
      {lancement ? <DialogueLancement processus={p} onFermer={() => setLancement(false)} onLance={(i) => navigate(`/des/processus/instances/${encodeURIComponent(i.id)}`)} /> : null}
      {confirmation === 'publier' ? (
        <ConfirmDialog open title={t('proc.publication.titre')} description={t('proc.publication.texte')} confirmLabel={t('proc.publier')}
          object={[{ key: t('proc.champ.code'), value: `${p.code} · v${p.version}`, mono: true }, { key: t('proc.champ.etapes'), value: String(p.etapes.length) }]}
          onClose={() => setConfirmation(null)} onConfirm={() => {
            setConfirmation(null)
            setEchec(null)
            publier.mutate({}, { onSuccess: () => setAnnonce(t('proc.annonce.publiee')), onError: surPublication })
          }} />
      ) : null}
      {confirmation === 'lever' ? (
        <ConfirmDialog open reason title={t('proc.publication.lever')} description={t('proc.publication.leverTexte')} confirmLabel={t('proc.publication.leverEtPublier')}
          consequences={(anomalies ?? []).filter((a) => a.gravite === 'LEVABLE').map((a) => `${t(`proc.anomalie.${a.code}`, { defaultValue: a.code })} · ${a.etape ?? ''} · ${a.message}`)}
          onClose={() => setConfirmation(null)} onConfirm={(motif) => {
            setConfirmation(null)
            publier.mutate({ motif, leverAvertissements: true }, { onSuccess: () => { setAnomalies(null); setAnnonce(t('proc.annonce.publiee')) }, onError: surPublication })
          }} />
      ) : null}
      {confirmation === 'retirer' ? (
        <ConfirmDialog open danger reason title={t('proc.retrait.titre')} description={t('proc.retrait.texte')} confirmLabel={t('proc.retirer')}
          object={[{ key: t('proc.champ.code'), value: `${p.code} · v${p.version}`, mono: true }]} onClose={() => setConfirmation(null)}
          onConfirm={(motif) => { setConfirmation(null); retirer.mutate(motif, { onSuccess: () => setAnnonce(t('proc.annonce.retiree')), onError: setEchec }) }} />
      ) : null}
      {confirmation === 'supprimer' ? (
        <ConfirmDialog open danger irreversible title={t('proc.suppression.titre')} description={t('proc.suppression.texte')} confirmLabel={t('proc.supprimer')}
          object={[{ key: t('proc.champ.code'), value: `${p.code} · v${p.version}`, mono: true }]} onClose={() => setConfirmation(null)}
          onConfirm={() => { setConfirmation(null); supprimer.mutate(p.id, { onSuccess: () => navigate('/des/processus'), onError: setEchec }) }} />
      ) : null}
    </div>
  )
}
