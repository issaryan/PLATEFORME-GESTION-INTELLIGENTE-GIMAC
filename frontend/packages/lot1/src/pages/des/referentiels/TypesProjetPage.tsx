import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router'
import {
  Breadcrumb, Button, Checkbox, CrudDialog, DataTable, InlineAlert, PageHeader, RadioGroup, Select, StatusBadge, Switch, Tabs, Tag, TextArea, TextField, type Column,
} from '@gimac/design-system'
import { aPermission, usePermissions, type ApiError } from '@gimac/socle'
import { useCreerFonction, useCreerTypeProjet, useFonctionsReferentiel, useModifierFonction, useModifierTypeProjet, useTypesProjet } from '../../../hooks/referentiels'
import { MODELES_TYPE_PROJET, type FonctionCatalogue, type ModeleTypeProjet, type TypeProjetVue } from '../../../modele/homologation'
import { PERM } from '../../../permissions'
import '../modelisateur/modelisateur.css'

// Types de projet et catalogue des fonctions (D-91, docs/lot1/types-projet.md §4) : données servies par MS-HOMOLOGATION
// au lieu d'énumérations. Création, modification et activation par les porteurs de HOM_REFERENTIEL_EDIT ou de
// REF_METIER_ADMIN (D-92) ; chaque type ouvre les versions de son référentiel. Le service refait tout contrôle.

const CODE_TYPE = /^[A-Z][A-Z0-9_]{2,39}$/
const CODE_FONCTION = /^[A-Z][A-Z0-9_]{1,39}$/
const PROFIL = /^ROLE_[A-Z][A-Z0-9_]+$/
const PERMISSION = /^[A-Z][A-Z0-9_]{1,79}$/

/** Droit d'écriture du référentiel : édition, ou administration de tous les domaines (D-92). */
export function useEditeurReferentiel() {
  const p = usePermissions()
  return aPermission(p, PERM.referentielModifier) || aPermission(p, PERM.referentielsMetierAdmin)
}

/** Message d'un refus du service : texte traduit par son code, sinon le message servi. */
function useRefus() {
  const { t } = useTranslation()
  return (e: ApiError | null) => (e ? { title: t(`erreur.${e.code}`, { defaultValue: e.message }), correlationId: e.correlationId } : undefined)
}

const libelleModele = (t: (k: string) => string, m: string) => t(`typesProjet.modele.${m}`)

function DialogueNouveauType({ types, onFermer, onCree }: { types: TypeProjetVue[]; onFermer: () => void; onCree: (t: TypeProjetVue) => void }) {
  const { t } = useTranslation()
  const refus = useRefus()
  const creer = useCreerTypeProjet()
  const sources = types.filter((x) => x.referentielActif)
  const [v, setV] = useState({ code: '', libelle: '', description: '', modele: 'GENERIQUE' as ModeleTypeProjet, mode: 'VIERGE' as 'VIERGE' | 'COPIE', typeSource: sources[0]?.code ?? '' })
  const [tente, setTente] = useState(false)
  const erreurs = {
    code: !CODE_TYPE.test(v.code) ? t('typesProjet.erreur.code') : undefined,
    libelle: !v.libelle.trim() || v.libelle.length > 120 ? t('typesProjet.erreur.libelle') : undefined,
    typeSource: v.mode === 'COPIE' && !v.typeSource ? t('typesProjet.erreur.source') : undefined,
  }
  return (
    <CrudDialog context="create" size="md" id="gm-dlg-nouveau-type" title={t('typesProjet.nouveau.titre')} description={t('typesProjet.nouveau.texte')} onClose={onFermer}
      submitLabel={t('typesProjet.nouveau.creer')} submitting={creer.isPending} dirty={!!(v.code || v.libelle)} error={refus(creer.error)}
      onSubmit={() => {
        setTente(true)
        if (Object.values(erreurs).some(Boolean)) return
        creer.mutate({ code: v.code, libelle: v.libelle.trim(), description: v.description.trim(), modele: v.modele, depart: v.mode === 'COPIE' ? { mode: 'COPIE', typeSource: v.typeSource } : { mode: 'VIERGE' } }, { onSuccess: onCree })
      }}>
      <TextField name="code" label={t('typesProjet.champ.code')} help={t('typesProjet.aide.code')} required mono maxLength={40} value={v.code}
        onChange={(x) => setV((o) => ({ ...o, code: x.toUpperCase() }))} error={tente ? erreurs.code : undefined} />
      <TextField name="libelle" label={t('typesProjet.champ.libelle')} required maxLength={120} value={v.libelle} onChange={(x) => setV((o) => ({ ...o, libelle: x }))} error={tente ? erreurs.libelle : undefined} />
      <TextArea name="description" label={t('typesProjet.champ.description')} rows={3} maxLength={1000} value={v.description} onChange={(x) => setV((o) => ({ ...o, description: x }))} />
      <Select name="modele" label={t('typesProjet.champ.modele')} help={t('typesProjet.aide.modele')} value={v.modele} onChange={(x) => setV((o) => ({ ...o, modele: x as ModeleTypeProjet }))}
        options={MODELES_TYPE_PROJET.map((m) => ({ value: m, label: libelleModele(t, m) }))} />
      <RadioGroup name="depart" label={t('typesProjet.champ.depart')} value={v.mode} onChange={(x) => setV((o) => ({ ...o, mode: x as 'VIERGE' | 'COPIE' }))}
        options={[{ value: 'VIERGE', label: t('typesProjet.depart.VIERGE') }, { value: 'COPIE', label: t('typesProjet.depart.COPIE') }]} />
      {v.mode === 'COPIE' ? (
        <Select name="typeSource" label={t('typesProjet.champ.typeSource')} required value={v.typeSource} onChange={(x) => setV((o) => ({ ...o, typeSource: x }))} error={tente ? erreurs.typeSource : undefined}
          options={[{ value: '', label: t('typesProjet.choisir') }, ...sources.map((x) => ({ value: x.code, label: `${x.libelle} (v${x.referentielActif?.version})` }))]} />
      ) : null}
    </CrudDialog>
  )
}

function DialogueModifierType({ type, onFermer, onFait }: { type: TypeProjetVue; onFermer: () => void; onFait: () => void }) {
  const { t } = useTranslation()
  const refus = useRefus()
  const modifier = useModifierTypeProjet()
  const [v, setV] = useState({ libelle: type.libelle, description: type.description ?? '', modele: type.modele, actif: type.actif, motif: '' })
  const [tente, setTente] = useState(false)
  const dirty = v.libelle !== type.libelle || v.description !== (type.description ?? '') || v.modele !== type.modele || v.actif !== type.actif
  const erreurs = {
    libelle: !v.libelle.trim() || v.libelle.length > 120 ? t('typesProjet.erreur.libelle') : undefined,
    motif: !v.motif.trim() ? t('typesProjet.erreur.motif') : undefined,
  }
  return (
    <CrudDialog context="edit" size="md" id="gm-dlg-modifier-type" title={t('typesProjet.modifier.titre')} subject={<span className="gm-mono">{type.code}</span>} onClose={onFermer}
      submitLabel={t('typesProjet.modifier.enregistrer')} submitting={modifier.isPending} dirty={dirty} submitDisabled={!dirty} submitDisabledReason={t('typesProjet.inchange')} error={refus(modifier.error)}
      onSubmit={() => {
        setTente(true)
        if (erreurs.libelle || erreurs.motif) return
        modifier.mutate({ code: type.code, m: { libelle: v.libelle.trim(), description: v.description.trim(), ...(v.modele !== type.modele ? { modele: v.modele } : {}), actif: v.actif, motif: v.motif.trim() } }, { onSuccess: onFait })
      }}>
      <TextField name="libelle" label={t('typesProjet.champ.libelle')} required maxLength={120} value={v.libelle} onChange={(x) => setV((o) => ({ ...o, libelle: x }))} error={tente ? erreurs.libelle : undefined} />
      <TextArea name="description" label={t('typesProjet.champ.description')} rows={3} maxLength={1000} value={v.description} onChange={(x) => setV((o) => ({ ...o, description: x }))} />
      <Select name="modele" label={t('typesProjet.champ.modele')} help={type.fige ? t('typesProjet.aide.fige') : t('typesProjet.aide.modele')} value={v.modele} disabled={type.fige}
        onChange={(x) => setV((o) => ({ ...o, modele: x as ModeleTypeProjet }))} options={MODELES_TYPE_PROJET.map((m) => ({ value: m, label: libelleModele(t, m) }))} />
      <Switch label={t('typesProjet.champ.actif')} checked={v.actif} onChange={(x) => setV((o) => ({ ...o, actif: x }))} />
      <TextArea name="motif" label={t('typesProjet.champ.motif')} required rows={2} maxLength={500} value={v.motif} onChange={(x) => setV((o) => ({ ...o, motif: x }))} error={tente ? erreurs.motif : undefined} />
    </CrudDialog>
  )
}

/** Création ou modification d'une fonction du catalogue (D-91 §5.3) ; partagée avec le modélisateur (« Nouvelle fonction »). */
export function DialogueFonction({ fonction, onFermer, onFait }: { fonction?: FonctionCatalogue; onFermer: () => void; onFait: (f: FonctionCatalogue) => void }) {
  const { t } = useTranslation()
  const refus = useRefus()
  const creer = useCreerFonction()
  const modifier = useModifierFonction()
  const m = fonction ? modifier : creer
  const [v, setV] = useState({ code: fonction?.code ?? '', libelle: fonction?.libelle ?? '', profils: (fonction?.profils ?? []).join(', '), permissions: (fonction?.permissions ?? []).join(', '),
    externe: fonction?.externe ?? false, actif: fonction?.actif ?? true, motif: '' })
  const [tente, setTente] = useState(false)
  const liste = (x: string) => x.split(/[\s,;]+/).map((y) => y.trim().toUpperCase()).filter(Boolean)
  const profils = liste(v.profils)
  // Permissions dont les porteurs peuvent tenir la fonction (§8.8) : offre des tâches à prendre.
  const permissions = liste(v.permissions)
  const erreurs = {
    code: !fonction && !CODE_FONCTION.test(v.code) ? t('typesProjet.fonction.erreur.code') : undefined,
    libelle: !v.libelle.trim() || v.libelle.length > 120 ? t('typesProjet.erreur.libelle') : undefined,
    profils: profils.some((p) => !PROFIL.test(p)) || profils.length > 20 ? t('typesProjet.fonction.erreur.profils') : !v.externe && !profils.length ? t('typesProjet.fonction.erreur.profilRequis') : undefined,
    permissions: permissions.some((p) => !PERMISSION.test(p)) || permissions.length > 20 ? t('typesProjet.fonction.erreur.permissions') : undefined,
    motif: fonction && !v.motif.trim() ? t('typesProjet.erreur.motif') : undefined,
  }
  return (
    <CrudDialog context={fonction ? 'edit' : 'create'} size="md" id="gm-dlg-fonction" title={t(fonction ? 'typesProjet.fonction.modifier' : 'typesProjet.fonction.nouvelle')}
      description={t('typesProjet.fonction.texte')} subject={fonction ? <span className="gm-mono">{fonction.code}</span> : undefined} onClose={onFermer}
      submitLabel={t(fonction ? 'typesProjet.modifier.enregistrer' : 'typesProjet.fonction.creer')} submitting={m.isPending} dirty={!!(v.code || v.libelle)} error={refus(m.error)}
      onSubmit={() => {
        setTente(true)
        if (Object.values(erreurs).some(Boolean)) return
        if (fonction) modifier.mutate({ code: fonction.code, m: { libelle: v.libelle.trim(), profils, permissions, externe: v.externe, actif: v.actif, motif: v.motif.trim() } }, { onSuccess: onFait })
        else creer.mutate({ code: v.code, libelle: v.libelle.trim(), profils, permissions, externe: v.externe }, { onSuccess: onFait })
      }}>
      {fonction ? null : (
        <TextField name="code" label={t('typesProjet.champ.code')} help={t('typesProjet.fonction.aide.code')} required mono maxLength={40} value={v.code}
          onChange={(x) => setV((o) => ({ ...o, code: x.toUpperCase() }))} error={tente ? erreurs.code : undefined} />
      )}
      <TextField name="libelle" label={t('typesProjet.champ.libelle')} required maxLength={120} value={v.libelle} onChange={(x) => setV((o) => ({ ...o, libelle: x }))} error={tente ? erreurs.libelle : undefined} />
      <TextField name="profils" label={t('typesProjet.fonction.profils')} help={t('typesProjet.fonction.aide.profils')} mono value={v.profils} onChange={(x) => setV((o) => ({ ...o, profils: x }))}
        error={tente ? erreurs.profils : undefined} />
      <TextField name="permissions" label={t('typesProjet.fonction.permissions')} help={t('typesProjet.fonction.aide.permissions')} mono value={v.permissions}
        onChange={(x) => setV((o) => ({ ...o, permissions: x }))} error={tente ? erreurs.permissions : undefined} />
      <Checkbox name="externe" label={t('typesProjet.fonction.externe')} checked={v.externe} onChange={(x) => setV((o) => ({ ...o, externe: x }))} />
      {fonction ? <Switch label={t('typesProjet.champ.actif')} checked={v.actif} onChange={(x) => setV((o) => ({ ...o, actif: x }))} /> : null}
      {fonction ? (
        <TextArea name="motif" label={t('typesProjet.champ.motif')} required rows={2} maxLength={500} value={v.motif} onChange={(x) => setV((o) => ({ ...o, motif: x }))} error={tente ? erreurs.motif : undefined} />
      ) : null}
    </CrudDialog>
  )
}

function Types({ editeur, annoncer }: { editeur: boolean; annoncer: (m: string) => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const q = useTypesProjet()
  const [nouveau, setNouveau] = useState(false)
  const [modifie, setModifie] = useState<TypeProjetVue | null>(null)
  const types = q.data ?? []
  const ouvrir = (x: TypeProjetVue) => navigate(`/des/referentiels/operations?version=${encodeURIComponent(x.brouillonId ?? x.referentielActif?.id ?? '')}`)
  const colonnes: Column<TypeProjetVue>[] = [
    { key: 'libelle', header: t('typesProjet.champ.libelle'), render: (x) => <span className="gm-col" style={{ gap: 0 }}><span className="gm-type-body-strong">{x.libelle}</span><span className="gm-mono gm-type-small">{x.code}</span></span> },
    { key: 'modele', header: t('typesProjet.champ.modele'), render: (x) => x.libelleModele ?? libelleModele(t, x.modele) },
    { key: 'actif', header: t('typesProjet.champ.etat'), render: (x) => <StatusBadge status={x.actif ? 'ok' : 'neutral'} label={t(x.actif ? 'typesProjet.actif' : 'typesProjet.inactif')} /> },
    {
      key: 'referentielActif', header: t('typesProjet.champ.referentiel'),
      render: (x) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
          {x.referentielActif ? <Tag>{t('typesProjet.versionN', { n: x.referentielActif.version })}</Tag> : <span className="gm-field-help">{t('typesProjet.aucunePublication')}</span>}
          {x.brouillonId ? <Tag>{t('typesProjet.brouillon')}</Tag> : null}
        </span>
      ),
    },
    { key: 'proposable', header: t('typesProjet.champ.proposable'), render: (x) => <StatusBadge status={x.proposable ? 'ok' : 'warn'} label={t(x.proposable ? 'typesProjet.proposable' : 'typesProjet.nonProposable')} /> },
    { key: 'nombreLiaisons', header: t('typesProjet.champ.liaisons'), type: 'number' },
    {
      key: 'actions', header: '', type: 'actions',
      render: (x) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)', justifyContent: 'flex-end' }}>
          {x.brouillonId || x.referentielActif ? <Button size="sm" variant="ghost" icon="kanban" aria-label={t('typesProjet.ouvrirQui', { libelle: x.libelle })} onClick={() => ouvrir(x)}>{t('typesProjet.ouvrir')}</Button> : null}
          {editeur ? <Button size="sm" variant="secondary" icon="pen-line" aria-label={t('typesProjet.modifierQui', { libelle: x.libelle })} onClick={() => setModifie(x)}>{t('typesProjet.modifier.bouton')}</Button> : null}
        </span>
      ),
    },
  ]
  return (
    <div className="gm-col">
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <p className="gm-field-help" style={{ margin: 0 }}>{t('typesProjet.aide')}</p>
        {editeur ? <Button variant="primary" icon="plus" onClick={() => setNouveau(true)}>{t('typesProjet.nouveau.titre')}</Button> : null}
      </div>
      <DataTable columns={colonnes} rows={types} rowKey={(x) => x.code} state={q.isPending ? 'loading' : q.isError ? 'error' : types.length ? 'ok' : 'empty'}
        error={q.error ?? undefined} onRetry={() => void q.refetch()} emptyText={t('typesProjet.vide')} />
      {nouveau ? <DialogueNouveauType types={types} onFermer={() => setNouveau(false)} onCree={(x) => { setNouveau(false); annoncer(t('typesProjet.nouveau.fait', { libelle: x.libelle })); if (x.brouillonId) navigate(`/des/referentiels/operations?version=${encodeURIComponent(x.brouillonId)}`) }} /> : null}
      {modifie ? <DialogueModifierType type={modifie} onFermer={() => setModifie(null)} onFait={() => { setModifie(null); annoncer(t('typesProjet.modifier.fait', { libelle: modifie.libelle })) }} /> : null}
    </div>
  )
}

function Fonctions({ editeur, annoncer }: { editeur: boolean; annoncer: (m: string) => void }) {
  const { t } = useTranslation()
  const q = useFonctionsReferentiel()
  const [edition, setEdition] = useState<FonctionCatalogue | 'nouvelle' | null>(null)
  const l = q.data ?? []
  const colonnes: Column<FonctionCatalogue>[] = [
    { key: 'libelle', header: t('typesProjet.champ.libelle'), render: (x) => <span className="gm-col" style={{ gap: 0 }}><span className="gm-type-body-strong">{x.libelle}</span><span className="gm-mono gm-type-small">{x.code}</span></span> },
    { key: 'profils', header: t('typesProjet.fonction.profils'), render: (x) => (x.profils.length ? <span className="gm-row" style={{ gap: 'var(--gm-space-1)', flexWrap: 'wrap' }}>{x.profils.map((p) => <Tag key={p}>{p}</Tag>)}</span> : <span className="gm-field-help">{t('typesProjet.fonction.sansProfil')}</span>) },
    {
      key: 'permissions', header: t('typesProjet.fonction.permissions'),
      render: (x) => (x.permissions?.length ? <span className="gm-row gm-tf-etiquettes" style={{ gap: 'var(--gm-space-1)', flexWrap: 'wrap' }}>{x.permissions.map((p) => <span key={p} title={p}><Tag>{t(`droits.permission.${p}`, { defaultValue: p })}</Tag></span>)}</span> : <span className="gm-field-help">{t('typesProjet.fonction.sansPermission')}</span>),
    },
    { key: 'externe', header: t('typesProjet.fonction.nature'), render: (x) => t(x.externe ? 'typesProjet.fonction.externeCourt' : 'typesProjet.fonction.interne') },
    { key: 'actif', header: t('typesProjet.champ.etat'), render: (x) => <StatusBadge status={x.actif ? 'ok' : 'neutral'} label={t(x.actif ? 'typesProjet.actif' : 'typesProjet.inactif')} /> },
    { key: 'reprise', header: t('typesProjet.fonction.origine'), render: (x) => t(x.reprise ? 'typesProjet.fonction.reprise' : 'typesProjet.fonction.creee') },
    {
      key: 'actions', header: '', type: 'actions',
      render: (x) => (editeur ? <Button size="sm" variant="secondary" icon="pen-line" aria-label={t('typesProjet.modifierQui', { libelle: x.libelle })} onClick={() => setEdition(x)}>{t('typesProjet.modifier.bouton')}</Button> : null),
    },
  ]
  return (
    <div className="gm-col">
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <p className="gm-field-help" style={{ margin: 0 }}>{t('typesProjet.fonction.aide')}</p>
        {editeur ? <Button variant="primary" icon="plus" onClick={() => setEdition('nouvelle')}>{t('typesProjet.fonction.nouvelle')}</Button> : null}
      </div>
      <DataTable columns={colonnes} rows={l} rowKey={(x) => x.code} state={q.isPending ? 'loading' : q.isError ? 'error' : l.length ? 'ok' : 'empty'}
        error={q.error ?? undefined} onRetry={() => void q.refetch()} emptyText={t('typesProjet.fonction.vide')} />
      {edition ? (
        <DialogueFonction fonction={edition === 'nouvelle' ? undefined : edition} onFermer={() => setEdition(null)}
          onFait={(f) => { setEdition(null); annoncer(t('typesProjet.fonction.fait', { libelle: f.libelle })) }} />
      ) : null}
    </div>
  )
}

export default function TypesProjetPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const editeur = useEditeurReferentiel()
  const vue = params.get('vue') === 'fonctions' ? 'fonctions' : 'types'
  const [annonce, setAnnonce] = useState('')
  return (
    <div className="gm-col">
      <Breadcrumb items={[{ label: t('referentiel.titre'), href: '/des/referentiels/operations' }, { label: t('typesProjet.titre') }]} />
      <PageHeader title={t('typesProjet.titre')} subtitle={t('typesProjet.sousTitre')} />
      {!editeur ? <InlineAlert level="info">{t('typesProjet.lecture')}</InlineAlert> : null}
      {annonce ? <InlineAlert level="info" title={annonce} /> : null}
      <Tabs items={[{ key: 'types', label: t('typesProjet.onglet.types') }, { key: 'fonctions', label: t('typesProjet.onglet.fonctions') }]} value={vue}
        onChange={(k) => { setAnnonce(''); setParams(k === 'fonctions' ? { vue: 'fonctions' } : {}) }} />
      <div role="tabpanel" aria-label={t(`typesProjet.onglet.${vue}`)}>
        {vue === 'types' ? <Types editeur={editeur} annoncer={setAnnonce} /> : <Fonctions editeur={editeur} annoncer={setAnnonce} />}
      </div>
    </div>
  )
}
