import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CrudDialog, DataTable, Icon, InlineAlert, MultiSelect, NumberField, Select, StatusBadge, Switch, Tag, TextField, type Column, type IconName } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { libelleServi, trierFonctionnalites, useTraduire, type Application, type DefinitionFonctionnalite, type SaisieFonctionnalite } from '@gimac/lot1'
import { useFonctionnalites, useModifierFonctionnalite, usePermissionsRealm } from '../hooks'
import { useActes } from './actes'
import { Annonce } from './Annonce'

// Catalogue des fonctionnalités (D-94, docs/lot1/ecrans-par-configuration.md §2 et §6.2) : le RSSI ou le super
// administrateur (IAM_PROFILE_EDIT) renomme, regroupe, réordonne, masque une fonctionnalité et change les permissions
// qui l'ouvrent, avec motif et clé de sécurité (PUT /configuration/fonctionnalites/{cle}). La barre latérale, les gardes
// et le tableau de bord de chaque utilisateur suivent sans nouvelle construction. Widgets et droits restent tels quels.

const APPLICATIONS: Application[] = ['des', 'console', 'portail']

/** Choix multiple de permissions du domaine (permissions internes des services exclues). */
function ChoixPermissions({ name, label, help, value, onChange, options }: { name: string; label: string; help?: string; value: string[]; onChange: (v: string[]) => void; options: string[] }) {
  const [q, setQ] = useState('')
  const filtre = q.trim().toUpperCase()
  return (
    <MultiSelect name={name} label={label} help={help} value={value} onChange={onChange} query={q} onQuery={setQ} suggestions
      options={options.filter((p) => !filtre || p.includes(filtre)).map((p) => ({ value: p, label: p }))} />
  )
}

function DialogueFonctionnalite({ f, groupes, onFermer, onContinuer }: { f: DefinitionFonctionnalite; groupes: string[]; onFermer: () => void; onContinuer: (s: SaisieFonctionnalite) => void }) {
  const { t } = useTranslation()
  const tr = useTraduire()
  const realm = usePermissionsRealm()
  const disponibles = [...new Set([...(realm.data ?? []), ...f.permissions.tous, ...f.permissions.unDe])].filter((p) => !p.startsWith('INTERNAL_')).sort()
  const [initiale] = useState(() => ({ libelle: libelleServi(tr, f.libelle), groupe: f.groupe ?? '', ordre: String(f.ordre), icone: f.icone, visible: f.visible, tous: f.permissions.tous, unDe: f.permissions.unDe }))
  const [v, setV] = useState(initiale)
  const [tente, setTente] = useState(false)
  // Libellé inchangé : la clé de traduction servie est gardée (le texte n'est envoyé que s'il a été modifié).
  const libelle = v.libelle === libelleServi(tr, f.libelle) ? f.libelle : v.libelle.trim()
  const erreurs = {
    libelle: !v.libelle.trim() || v.libelle.length > 200 ? t('console.fct.erreur.libelle') : undefined,
    ordre: !/^\d{1,6}$/.test(v.ordre) ? t('console.fct.erreur.ordre') : undefined,
    icone: !/^[a-z0-9-]{1,40}$/.test(v.icone) ? t('console.fct.erreur.icone') : undefined,
  }
  const saisie: SaisieFonctionnalite = {
    cle: f.cle, application: f.application, module: f.module, libelle, groupe: v.groupe.trim() || null, ordre: Number(v.ordre), icone: v.icone, visible: v.visible,
    permissions: { tous: v.tous, unDe: v.unDe }, widgets: f.widgets, droits: f.droits,
  }
  const dirty = JSON.stringify(v) !== JSON.stringify(initiale)
  return (
    <CrudDialog context="edit" size="lg" id="gm-dlg-fonctionnalite" title={t('console.fct.modifier.titre')} subject={<span className="gm-mono">{f.cle}</span>} description={t('console.fct.modifier.texte')}
      onClose={onFermer} reauth dirty={dirty} submitLabel={t('console.fct.modifier.enregistrer')} submitIcon="pen-line"
      onSubmit={() => {
        setTente(true)
        if (Object.values(erreurs).some(Boolean)) return
        onContinuer(saisie)
      }}>
      <div className="gm-form-grid">
        <div className="gm-form-field gm-form-wide">
          <TextField name="libelle" label={t('console.fct.libelle')} help={t('console.fct.aide.libelle')} required maxLength={200} value={v.libelle} onChange={(x) => setV((o) => ({ ...o, libelle: x }))} error={tente ? erreurs.libelle : undefined} />
        </div>
        <div className="gm-form-field">
          <Select name="groupe" label={t('console.fct.groupe')} help={t('console.fct.aide.groupe')} value={v.groupe} onChange={(x) => setV((o) => ({ ...o, groupe: x }))}
            options={[{ value: '', label: t('console.fct.horsBarre') }, ...groupes.map((g) => ({ value: g, label: libelleGroupe(t, tr, f.application, g) }))]} />
        </div>
        <div className="gm-form-field">
          <NumberField name="ordre" label={t('console.fct.ordre')} help={t('console.fct.aide.ordre')} min={0} value={v.ordre} onChange={(x) => setV((o) => ({ ...o, ordre: x }))} error={tente ? erreurs.ordre : undefined} />
        </div>
        <div className="gm-form-field">
          <TextField name="icone" label={t('console.fct.icone')} help={t('console.fct.aide.icone')} mono maxLength={40} value={v.icone} onChange={(x) => setV((o) => ({ ...o, icone: x.trim() }))} error={tente ? erreurs.icone : undefined} />
        </div>
        <div className="gm-form-field">
          <Switch label={t('console.fct.visible')} checked={v.visible} onChange={(x) => setV((o) => ({ ...o, visible: x }))} />
        </div>
        <div className="gm-form-field gm-form-wide">
          <ChoixPermissions name="unDe" label={t('console.fct.unDe')} help={t('console.fct.aide.unDe')} value={v.unDe} onChange={(x) => setV((o) => ({ ...o, unDe: x }))} options={disponibles} />
        </div>
        <div className="gm-form-field gm-form-wide">
          <ChoixPermissions name="tous" label={t('console.fct.tous')} help={t('console.fct.aide.tous')} value={v.tous} onChange={(x) => setV((o) => ({ ...o, tous: x }))} options={disponibles} />
        </div>
      </div>
      {!v.tous.length && !v.unDe.length ? <InlineAlert level="warn" title={t('console.fct.sansPermission')} /> : null}
      {!v.visible ? <InlineAlert level="info" title={t('console.fct.masquee')} /> : null}
    </CrudDialog>
  )
}

/** Titre d'un groupe de la barre latérale : traduit s'il est connu, sinon le code créé par la configuration. */
function libelleGroupe(t: (k: string) => string, tr: ReturnType<typeof useTraduire>, application: Application, g: string) {
  const cle = application === 'console' ? `console.nav.groupe.${g}` : `nav.groupe.${g}`
  return g === 'accueil' ? t('console.fct.accueil') : tr.existe(cle) ? t(cle) : g
}

/** Liste du catalogue par application ; modification réservée à IAM_PROFILE_EDIT. */
export function CatalogueFonctionnalites() {
  const { t } = useTranslation()
  const tr = useTraduire()
  const permissions = usePermissions()
  const q = useFonctionnalites()
  const modifier = useModifierFonctionnalite()
  const actes = useActes()
  const [application, setApplication] = useState<Application>('des')
  const [ouverte, setOuverte] = useState<DefinitionFonctionnalite | null>(null)
  const edition = aPermission(permissions, 'IAM_PROFILE_EDIT')
  const lignes = trierFonctionnalites((q.data ?? []).filter((f) => f.application === application))
  const groupes = [...new Set(lignes.map((f) => f.groupe).filter((g): g is string => !!g))]
  const predicat = (f: DefinitionFonctionnalite) => [
    ...(f.permissions.tous.length ? [t('console.fct.predicat.tous', { l: f.permissions.tous.join(', ') })] : []),
    ...(f.permissions.unDe.length ? [t('console.fct.predicat.unDe', { l: f.permissions.unDe.join(', ') })] : []),
  ]
  const colonnes: Column<DefinitionFonctionnalite>[] = [
    { key: 'ordre', header: t('console.fct.ordre'), type: 'number', width: 80 },
    {
      key: 'libelle', header: t('console.fct.libelle'),
      // Icône toujours à gauche du libellé (recette ACC-08) : ligne sans retour, texte seul replié.
      render: (f) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'nowrap', alignItems: 'flex-start' }}>
          <span style={{ flex: 'none', display: 'inline-flex' }}><Icon name={f.icone as IconName} size={16} /></span>
          <span className="gm-col" style={{ gap: 0, minWidth: 0 }}><span>{libelleServi(tr, f.libelle)}</span><span className="gm-mono gm-type-small" style={{ overflowWrap: 'anywhere' }}>{f.cle}</span></span>
        </span>
      ),
    },
    { key: 'groupe', header: t('console.fct.groupe'), render: (f) => (f.groupe ? libelleGroupe(t, tr, f.application, f.groupe) : <span className="gm-field-help">{t('console.fct.horsBarre')}</span>) },
    { key: 'permissions', header: t('console.fct.permissions'), render: (f) => (predicat(f).length ? <span className="gm-col" style={{ gap: 0 }}>{predicat(f).map((x) => <span key={x} className="gm-mono gm-type-small">{x}</span>)}</span> : <span className="gm-field-help">{t('console.fct.toutCompte')}</span>) },
    { key: 'visible', header: t('console.fct.etat'), render: (f) => <StatusBadge status={f.visible ? 'ok' : 'neutral'} label={t(f.visible ? 'console.fct.visible' : 'console.fct.masqueeCourt')} /> },
    { key: 'droits', header: t('console.fct.droits'), render: (f) => (f.droits.length ? <Tag>{t('console.fct.nDroits', { count: f.droits.length })}</Tag> : null) },
    {
      key: 'actions', header: '', type: 'actions',
      render: (f) => (edition ? <Button size="sm" variant="secondary" icon="pen-line" aria-label={t('console.fct.modifierQui', { cle: f.cle })} onClick={() => setOuverte(f)}>{t('console.fct.modifier.bouton')}</Button> : null),
    },
  ]
  return (
    <div className="gm-col">
      <p className="gm-field-help" style={{ margin: 0 }}>{t(edition ? 'console.fct.texte' : 'console.fct.lecture')}</p>
      <div style={{ maxWidth: 320 }}>
        <Select name="application" label={t('console.fct.application')} value={application} onChange={(x) => setApplication(x as Application)}
          options={APPLICATIONS.map((a) => ({ value: a, label: t(`console.fct.application.${a}`) }))} />
      </div>
      <Annonce retour={actes.retour} />
      <DataTable columns={colonnes} rows={lignes} rowKey={(f) => f.cle} pagination={false} state={q.isPending ? 'loading' : q.isError ? 'error' : lignes.length ? 'ok' : 'empty'}
        error={q.error ?? undefined} onRetry={() => void q.refetch()} emptyText={t('console.fct.vide')} />
      {ouverte ? (
        <DialogueFonctionnalite f={ouverte} groupes={groupes} onFermer={() => setOuverte(null)} onContinuer={(s) => {
          const f = ouverte
          setOuverte(null)
          actes.sensible({
            titre: t('console.fct.modifier.titre'),
            objet: [{ key: t('console.fct.cle'), value: f.cle, mono: true }, { key: t('console.fct.libelle'), value: libelleServi(tr, s.libelle) }],
            contenu: s,
            succes: t('console.fct.modifier.fait', { cle: f.cle }),
            executer: (motif, assertion) => modifier.mutateAsync({ cle: f.cle, f: s, motif, assertion }),
          })
        }} />
      ) : null}
      {actes.dialogue}
    </div>
  )
}
