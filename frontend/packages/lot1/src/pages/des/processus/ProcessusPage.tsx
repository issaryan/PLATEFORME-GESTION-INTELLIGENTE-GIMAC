import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router'
import { Button, CrudDialog, DataTable, EmptyState, NumberField, PageHeader, Select, Tabs, TextArea, TextField, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { ETATS_INSTANCE, ETATS_PROCESSUS, LIMITES_PROCESSUS, type EtatInstance, type EtatProcessus, type ResumeInstance, type ResumeProcessus, type VueInstances } from '../../../modele/processus'
import { useCatalogueProcessus, useEcritureProcessus, useInstances, useProcessus } from '../../../hooks/processus'
import { useOrganisation, useUnites } from '../../../hooks/pilotage'
import { usePouvoirs } from '../../../hooks/pouvoirs'
import { PERM } from '../../../permissions'
import { useRetour } from '../parcours/communs'
import { etatListe } from '../liaisons/communs'
import { useDates, useLibelleUnite } from '../activite/communs'
import { BadgeEtape, BadgeInstance, BadgeProcessus, useErreurProcessus } from './communs'
import { DialogueLancement } from './DialogueLancement'

// Processus de l'unité (D-87 §4, /des/processus) : catalogue par unité (mes unités d'abord), filtres d'unité, d'état et de
// recherche, version et état ; création d'un processus (PROCESSUS_DEFINIR) ; lancement d'une version publiée en
// fenêtre modale ; onglet « Instances » (miennes, de mes unités, lancées par moi). Les droits de chaque ligne (droits)
// viennent de MS-PILOTAGE.

const PAR_PAGE = 20

function DialogueNouveau({ unites, onFermer, onCree }: { unites: { code: string; libelle: string }[]; onFermer: () => void; onCree: (id: string) => void }) {
  const { t } = useTranslation()
  const erreur = useErreurProcessus()
  const creer = useEcritureProcessus((s, v: { code: string; unite: string; libelle: string; description: string; categorie: string; delaiCibleJours: number | null }) => s.processus.creer({ ...v, etapes: [] }))
  const [v, setV] = useState({ code: '', unite: unites[0]?.code ?? '', libelle: '', description: '', categorie: '', delai: '' })
  const [tente, setTente] = useState(false)
  const delai = v.delai.trim() === '' ? null : Number(v.delai)
  const erreurs = {
    code: !LIMITES_PROCESSUS.code.test(v.code) ? t('proc.erreur.code') : undefined,
    unite: !v.unite ? t('proc.erreur.unite') : undefined,
    libelle: !v.libelle.trim() ? t('proc.erreur.libelle') : undefined,
    delai: delai !== null && !(delai >= 0 && delai <= LIMITES_PROCESSUS.delai) ? t('proc.erreur.delai') : undefined,
  }
  const champ = (k: keyof typeof v) => (x: string) => setV((o) => ({ ...o, [k]: x }))
  const refus = erreur(creer.error)
  return (
    <CrudDialog context="create" size="md" id="gm-dlg-nouveau-processus" title={t('proc.nouveau.titre')} description={t('proc.nouveau.texte')} onClose={onFermer}
      submitLabel={t('proc.nouveau.creer')} submitting={creer.isPending} dirty={!!(v.code || v.libelle)} error={refus ? { title: refus.titre, correlationId: refus.correlationId } : undefined}
      onSubmit={() => {
        setTente(true)
        if (Object.values(erreurs).some(Boolean)) return
        creer.mutate({ code: v.code, unite: v.unite, libelle: v.libelle.trim(), description: v.description.trim(), categorie: v.categorie.trim(), delaiCibleJours: delai }, { onSuccess: (p) => onCree(p.id) })
      }}>
      <div className="gm-form-grid">
        <div className="gm-form-field">
          <TextField name="code" label={t('proc.champ.code')} help={t('proc.aide.code')} required mono maxLength={40} value={v.code} onChange={(x) => champ('code')(x.toUpperCase())} error={tente ? erreurs.code : undefined} />
        </div>
        <div className="gm-form-field">
          <Select name="unite" label={t('proc.champ.unite')} required value={v.unite} onChange={champ('unite')} error={tente ? erreurs.unite : undefined}
            options={[{ value: '', label: t('proc.choisirUnite') }, ...unites.map((u) => ({ value: u.code, label: u.libelle }))]} />
        </div>
        <div className="gm-form-field gm-form-wide">
          <TextField name="libelle" label={t('proc.champ.libelle')} required maxLength={LIMITES_PROCESSUS.libelle} value={v.libelle} onChange={champ('libelle')} error={tente ? erreurs.libelle : undefined} />
        </div>
        <div className="gm-form-field">
          <TextField name="categorie" label={t('proc.champ.categorie')} maxLength={LIMITES_PROCESSUS.categorie} value={v.categorie} onChange={champ('categorie')} />
        </div>
        <div className="gm-form-field">
          <NumberField name="delaiCibleJours" label={t('proc.champ.delaiCible')} unit={t('proc.jours')} min={0} max={365} value={v.delai} onChange={champ('delai')} error={tente ? erreurs.delai : undefined} />
        </div>
        <div className="gm-form-field gm-form-wide">
          <TextArea name="description" label={t('proc.champ.description')} rows={3} maxLength={LIMITES_PROCESSUS.description} value={v.description} onChange={champ('description')} />
        </div>
      </div>
    </CrudDialog>
  )
}

/** Lancement depuis le catalogue : la définition complète est lue avant d'ouvrir la fenêtre. */
function LancementDepuisCatalogue({ id, onFermer }: { id: string; onFermer: () => void }) {
  const navigate = useNavigate()
  const p = useProcessus(id)
  return p.data ? <DialogueLancement processus={p.data} onFermer={onFermer} onLance={(i) => navigate(`/des/processus/instances/${encodeURIComponent(i.id)}`)} /> : null
}

function Catalogue({ onLancer }: { onLancer: (id: string) => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const dates = useDates()
  const libelleUnite = useLibelleUnite()
  const organisation = useOrganisation()
  const [unite, setUnite] = useState('')
  const [etat, setEtat] = useState<EtatProcessus | ''>('')
  const [q, setQ] = useState('')
  const catalogue = useCatalogueProcessus({ unite: unite || undefined, etat: etat || undefined, q: q || undefined })
  const unites = useUnites()
  const lignes = catalogue.data ?? []
  // Mes unités d'abord : celles de mon chemin et celles que je dirige, puis les autres par libellé.
  const miennes = new Set([...(organisation.data?.chemin ?? []).map((u) => u.code), ...(organisation.data?.unitesDirigees ?? [])])
  const groupes = [...new Set(lignes.map((l) => l.unite))].sort((a, b) => Number(!miennes.has(a)) - Number(!miennes.has(b)) || libelleUnite(a).localeCompare(libelleUnite(b), 'fr'))
  const colonnes: Column<ResumeProcessus>[] = [
    {
      key: 'libelle', header: t('proc.champ.libelle'), render: (p) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <button type="button" className="gm-link" style={{ background: 'none', border: 0, padding: 0, font: 'inherit', textAlign: 'left', cursor: 'pointer' }} onClick={() => navigate(`/des/processus/${encodeURIComponent(p.id)}`)}>{p.libelle}</button>
          <span className="gm-mono gm-type-small">{p.code} · v{p.version}</span>
        </span>
      ),
    },
    { key: 'categorie', header: t('proc.champ.categorie'), render: (p) => p.categorie || '' },
    { key: 'etat', header: t('proc.champ.etat'), render: (p) => <BadgeProcessus etat={p.etat} /> },
    { key: 'nombreEtapes', header: t('proc.champ.etapes'), type: 'number' },
    { key: 'publieeLe', header: t('proc.champ.publieeLe'), render: (p) => (p.publieeLe ? dates.instant(p.publieeLe) : '') },
    {
      key: 'actions', header: '', type: 'actions', render: (p) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)', justifyContent: 'flex-end' }}>
          <Button size="sm" variant="ghost" icon={p.droits.modifier ? 'pen-line' : 'eye'} aria-label={t(p.droits.modifier ? 'proc.modifierQui' : 'proc.ouvrirQui', { libelle: p.libelle, version: p.version })}
            onClick={() => navigate(`/des/processus/${encodeURIComponent(p.id)}`)}>{t(p.droits.modifier ? 'proc.modifier' : 'proc.ouvrir')}</Button>
          {p.droits.lancer ? <Button size="sm" variant="secondary" icon="arrow-right" aria-label={t('proc.lancerQui', { libelle: p.libelle })} onClick={() => onLancer(p.id)}>{t('proc.lancer')}</Button> : null}
        </span>
      ),
    },
  ]
  return (
    <div className="gm-col">
      <div className="gm-row" style={{ gap: 'var(--gm-space-3)', flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div style={{ minWidth: 220 }}>
          <Select id="proc-filtre-unite" name="unite" label={t('proc.filtre.unite')} value={unite} onChange={setUnite} options={[{ value: '', label: t('proc.filtre.toutes') }, ...(unites.data ?? []).map((u) => ({ value: u.code, label: u.libelle }))]} />
        </div>
        <div style={{ minWidth: 200 }}>
          <Select id="proc-filtre-etat" name="etat" label={t('proc.filtre.etat')} value={etat} onChange={(v) => setEtat(v as EtatProcessus | '')} options={[{ value: '', label: t('proc.filtre.tous') }, ...ETATS_PROCESSUS.map((e) => ({ value: e, label: t(`proc.etat.${e}`) }))]} />
        </div>
        <div style={{ minWidth: 260, flex: 1 }}>
          <TextField id="proc-filtre-q" name="q" label={t('proc.filtre.recherche')} value={q} onChange={setQ} />
        </div>
      </div>
      {catalogue.isPending || catalogue.isError ? <DataTable columns={colonnes} rows={[]} rowKey={(p) => p.id} {...etatListe(catalogue, [])} onRetry={() => void catalogue.refetch()} /> : null}
      {catalogue.isSuccess && !lignes.length ? <EmptyState icon="workflow" title={t('proc.vide')} text={t('proc.videTexte')} /> : null}
      {groupes.map((u) => (
        <section key={u} className="gm-col" aria-labelledby={`gm-proc-unite-${u}`} style={{ gap: 'var(--gm-space-2)' }}>
          <h2 id={`gm-proc-unite-${u}`} className="gm-type-h3" style={{ margin: 0 }}>{libelleUnite(u)}{miennes.has(u) ? <span className="gm-field-help"> · {t('proc.monUnite')}</span> : null}</h2>
          <DataTable columns={colonnes} rows={lignes.filter((l) => l.unite === u)} rowKey={(p) => p.id} pagination={false} state="ok" />
        </section>
      ))}
    </div>
  )
}

function Instances() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const dates = useDates()
  const libelleUnite = useLibelleUnite()
  const [vue, setVue] = useState<VueInstances>('miennes')
  const [etat, setEtat] = useState<EtatInstance | ''>('')
  const [page, setPage] = useState(1)
  const instances = useInstances({ vue, etat: etat || undefined, page: page - 1, taille: PAR_PAGE })
  const lignes = instances.data?.contenu ?? []
  const colonnes: Column<ResumeInstance>[] = [
    {
      key: 'intitule', header: t('proc.instance.intitule'), render: (i) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <button type="button" className="gm-link" style={{ background: 'none', border: 0, padding: 0, font: 'inherit', textAlign: 'left', cursor: 'pointer' }} onClick={() => navigate(`/des/processus/instances/${encodeURIComponent(i.id)}`)}>{i.intitule}</button>
          <span className="gm-type-small">{i.processus.libelle} · v{i.processus.version}</span>
        </span>
      ),
    },
    { key: 'unite', header: t('proc.champ.unite'), render: (i) => libelleUnite(i.unite) },
    { key: 'etat', header: t('proc.champ.etat'), render: (i) => <BadgeInstance etat={i.etat} /> },
    { key: 'avancement', header: t('proc.instance.avancement'), render: (i) => t('proc.instance.avancementDe', { n: i.avancement.terminees, total: i.avancement.total }) },
    {
      key: 'etapesEnCours', header: t('proc.instance.enCours'), render: (i) => (
        <span className="gm-col" style={{ gap: 2 }}>{i.etapesEnCours.map((e) => <span key={e.code} className="gm-row" style={{ gap: 'var(--gm-space-1)' }}><BadgeEtape etat={e.etat} /> {e.libelle}</span>)}</span>
      ),
    },
    { key: 'lanceeLe', header: t('proc.instance.lanceeLe'), render: (i) => `${dates.instant(i.lanceeLe)} · ${i.lanceur.nomAffiche}` },
  ]
  return (
    <div className="gm-col">
      <div className="gm-row" style={{ gap: 'var(--gm-space-3)', flexWrap: 'wrap' }}>
        <div style={{ minWidth: 240 }}>
          <Select name="vue" label={t('proc.instance.vue')} value={vue} onChange={(v) => { setVue(v as VueInstances); setPage(1) }} options={(['miennes', 'unite', 'lancees'] as const).map((v) => ({ value: v, label: t(`proc.instance.vue.${v}`) }))} />
        </div>
        <div style={{ minWidth: 200 }}>
          <Select name="etatInstance" label={t('proc.filtre.etat')} value={etat} onChange={(v) => { setEtat(v as EtatInstance | ''); setPage(1) }}
            options={[{ value: '', label: t('proc.filtre.tous') }, ...ETATS_INSTANCE.map((e) => ({ value: e, label: t(`proc.instance.etat.${e}`) }))]} />
        </div>
      </div>
      <DataTable columns={colonnes} rows={lignes} rowKey={(i) => i.id} total={instances.data?.total} page={page} pageSize={PAR_PAGE} onPage={setPage} emptyText={t(`proc.instance.vide.${vue}`)}
        {...etatListe(instances, lignes)} onRetry={() => void instances.refetch()} />
    </div>
  )
}

export default function ProcessusPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const permissions = usePermissions()
  const [params, setParams] = useSearchParams()
  const onglet = params.get('vue') === 'instances' ? 'instances' : 'catalogue'
  const pouvoirs = usePouvoirs()
  const unites = useUnites()
  const organisation = useOrganisation()
  const [nouveau, setNouveau] = useState(false)
  const [lancer, setLancer] = useState<string | null>(null)
  const [retour] = useRetour()
  // Unités où définir un processus : l'administration toutes, sinon celles que je dirige (PROCESSUS_DEFINIR de portée UNITES).
  const admin = aPermission(permissions, PERM.profilEditer)
  const dirigees = new Set([...(pouvoirs.data?.unitesDirigees ?? []), ...(organisation.data?.unitesDirigees ?? [])])
  const definissables = (unites.data ?? []).filter((u) => u.active && (admin || (pouvoirs.data?.actions.PROCESSUS_DEFINIR === 'UNITES' && dirigees.has(u.code))))
  return (
    <div className="gm-col">
      <PageHeader title={t('proc.titre')} subtitle={t('proc.sousTitre')}
        actions={definissables.length ? <Button variant="primary" icon="plus" onClick={() => setNouveau(true)}>{t('proc.nouveau.titre')}</Button> : undefined} />
      <Tabs items={[{ key: 'catalogue', label: t('proc.onglet.catalogue') }, { key: 'instances', label: t('proc.onglet.instances') }]} value={onglet}
        onChange={(k) => setParams(k === 'instances' ? { vue: 'instances' } : {})} />
      {retour}
      <div role="tabpanel" aria-label={t(`proc.onglet.${onglet}`)}>
        {onglet === 'catalogue' ? <Catalogue onLancer={setLancer} /> : <Instances />}
      </div>
      {nouveau ? <DialogueNouveau unites={definissables} onFermer={() => setNouveau(false)} onCree={(id) => navigate(`/des/processus/${encodeURIComponent(id)}`)} /> : null}
      {lancer ? <LancementDepuisCatalogue id={lancer} onFermer={() => setLancer(null)} /> : null}
    </div>
  )
}
