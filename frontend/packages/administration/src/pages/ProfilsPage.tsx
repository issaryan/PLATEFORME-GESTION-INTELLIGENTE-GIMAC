import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, CrudDialog, DataTable, InlineAlert, PageHeader, StatusBadge, Tabs, TextArea, TextField, type Column } from '@gimac/design-system'
import { aPermission, usePermissions, useLibelleProfil } from '@gimac/socle'
import { dateHeure } from '@gimac/compte'
import { ArbreFonctionnalites, ApercuBarreLaterale, DialogueComparaison, DialoguePresentation, FicheProfil, compositionDe } from '../composants/profils'
import { useEffet, useLibellesProfils, useProfils } from '../hooks'
import { LIMITES_PROFIL, type Profil } from '../modele/configuration'
import { Annonce } from '../composants/Annonce'
import { CatalogueFonctionnalites } from '../composants/fonctionnalites'
import { useActes } from '../composants/actes'
import { etatListe } from '../composants/formats'

// Profils et permissions (D-58, D-62) : chaque profil par fonction, ses titulaires et sa composition.
// Lecture avec IAM_ACCOUNT_READ ; composition modifiée avec IAM_PROFILE_EDIT, motif et réauthentification
// FIDO2. Un profil privilégié (super administrateur, RSSI) n'est modifiable qu'avec IAM_ROLE_PRIVILEGED_GRANT.
// Les permissions internes des services (INTERNAL_*) ne sont jamais proposées.
// D-81 §5 : création d'un profil (code ROLE_…, description, permissions ; privilégié s'il porte une permission IAM_*
// ou PLATFORM_*, création alors réservée à IAM_ROLE_PRIVILEGED_GRANT) et suppression d'un profil créé sans
// titulaire, en fenêtre modale puis signature (motif et clé de sécurité). Les profils livrés ne se suppriment pas.
// D-86 §2 : libellé et description réels de chaque profil (livré ou créé), modifiables en fenêtre modale puis signature
// (PUT /configuration/profils/{code}) ; dernière modification affichée. D-90 : fiche du profil à onglets
// (Fonctionnalités, Droits, Pouvoirs, Organisation), création par fonctionnalité, comparaison de deux profils.

const CODE_PROFIL = /^ROLE_[A-Z][A-Z0-9_]{2,40}$/
const privilegiee = (p: string) => /^(IAM|PLATFORM)_/.test(p)
type Nouveau = { code: string; libelle: string; description: string; permissions: string[] }

function NouveauProfil({ onFermer, onContinuer }: { onFermer: () => void; onContinuer: (p: Nouveau) => void }) {
  const { t } = useTranslation()
  const [v, setV] = useState<Nouveau>({ code: 'ROLE_', libelle: '', description: '', permissions: [] })
  const [tente, setTente] = useState(false)
  const erreurs = {
    code: !CODE_PROFIL.test(v.code.trim()) ? t('console.profils.nouveau.erreurCode') : undefined,
    libelle: v.libelle.trim().length > LIMITES_PROFIL.libelleMax ? t('console.profils.erreur.libelle', { max: LIMITES_PROFIL.libelleMax }) : undefined,
    description: !v.description.trim() ? t('console.profils.nouveau.erreurDescription') : v.description.trim().length > LIMITES_PROFIL.descriptionMax
      ? t('console.profils.erreur.description', { max: LIMITES_PROFIL.descriptionMax }) : undefined,
  }
  const privilegie = v.permissions.some(privilegiee)
  return (
    <CrudDialog context="create" size="lg" id="gm-dlg-nouveau-profil" title={t('console.profils.nouveau')} description={t('console.profils.nouveau.texte')} onClose={onFermer}
      onSubmit={() => {
        setTente(true)
        if (erreurs.code || erreurs.libelle || erreurs.description) return
        onContinuer({ code: v.code.trim(), libelle: v.libelle.trim(), description: v.description.trim(), permissions: [...v.permissions].sort() })
      }}
      submitLabel={t('console.profils.enregistrer')} submitIcon="plus" reauth dirty={v.code !== 'ROLE_' || !!v.libelle || !!v.description || !!v.permissions.length}>
      <div className="gm-form-grid">
        <div className="gm-form-field">
          <TextField name="code" label={t('console.profils.nouveau.code')} help={t('console.profils.nouveau.codeAide')} required mono maxLength={46} value={v.code}
            onChange={(x) => setV((o) => ({ ...o, code: x.toUpperCase() }))} error={tente ? erreurs.code : undefined} />
        </div>
        <div className="gm-form-field">
          <TextField name="libelle" label={t('console.profils.libelle')} help={t('console.profils.nouveau.libelleAide')} maxLength={LIMITES_PROFIL.libelleMax} value={v.libelle}
            onChange={(x) => setV((o) => ({ ...o, libelle: x }))} error={tente ? erreurs.libelle : undefined} />
        </div>
      </div>
      <TextArea name="description" label={t('console.profils.description')} required rows={2} maxLength={LIMITES_PROFIL.descriptionMax} value={v.description}
        onChange={(x) => setV((o) => ({ ...o, description: x }))} error={tente ? erreurs.description : undefined} />
      {privilegie ? <InlineAlert level="warn" title={t('console.profils.nouveau.privilegie')} /> : null}
      <p className="gm-field-help" style={{ margin: 0 }}>{t('console.profils.nouveau.fonctionnalites')}</p>
      <div className="gm-fct">
        <ArbreFonctionnalites permissions={v.permissions} onChange={(x) => setV((o) => ({ ...o, permissions: x }))} />
        <ApercuBarreLaterale permissions={v.permissions} />
      </div>
    </CrudDialog>
  )
}

export default function ProfilsPage() {
  const { t, i18n } = useTranslation()
  const permissions = usePermissions()
  const libelleProfil = useLibelleProfil(useLibellesProfils())
  const profils = useProfils()
  const actes = useActes()
  const definir = useEffet((s, v: { code: string; permissions: string[]; motif: string; assertion: string }) => s.configuration.definirPermissions(v.code, v.permissions, v.motif, v.assertion))
  const creer = useEffet((s, v: Nouveau & { motif: string; assertion: string }) =>
    s.configuration.creerProfil({ code: v.code, ...(v.libelle ? { libelle: v.libelle } : {}), description: v.description, permissions: v.permissions }, v.motif, v.assertion))
  const supprimer = useEffet((s, v: { code: string; motif: string; assertion: string }) => s.configuration.supprimerProfil(v.code, v.motif, v.assertion))
  const presenter = useEffet((s, v: { code: string; libelle: string; description: string; externe?: boolean; motif: string; assertion: string }) =>
    s.configuration.modifierProfil(v.code, { libelle: v.libelle, description: v.description, ...(v.externe !== undefined ? { externe: v.externe } : {}) }, v.motif, v.assertion))
  // D-94 : catalogue des fonctionnalités, second onglet de l'écran (?vue=fonctionnalites).
  const [params, setParams] = useSearchParams()
  const vue = params.get('vue') === 'fonctionnalites' ? 'fonctionnalites' : 'profils'
  const [ouvert, setOuvert] = useState<Profil | null>(null)
  const [presente, setPresente] = useState<Profil | null>(null)
  const [nouveau, setNouveau] = useState(false)
  const [comparer, setComparer] = useState(false)
  const edition = aPermission(permissions, 'IAM_PROFILE_EDIT')
  const privilegie = aPermission(permissions, 'IAM_ROLE_PRIVILEGED_GRANT')
  const modifiable = (p: Profil) => edition && (!p.privilegie || privilegie)
  const lignes = profils.data ?? []
  const nom = (p: Profil) => libelleProfil(p.code, p.libelle)

  const colonnes: Column<Profil>[] = [
    { key: 'code', header: t('console.profils.libelle'), render: (p) => <span className="gm-col" style={{ gap: 0 }}><span>{nom(p)}</span><span className="gm-mono gm-type-small">{p.code}</span></span> },
    { key: 'description', header: t('console.profils.description'), render: (p) => p.description ?? '' },
    {
      key: 'privilegie', header: t('console.profils.nature'),
      render: (p) => (
        <span className="gm-col" style={{ gap: 2, alignItems: 'flex-start' }}>
          {p.privilegie ? <StatusBadge status="warn" icon="lock" label={t('console.profils.privilegie')} /> : <StatusBadge status="neutral" label={t('console.profils.standard')} />}
          {p.externe ? <StatusBadge status="info" icon="landmark" label={t('console.profils.externe')} /> : null}
        </span>
      ),
    },
    { key: 'titulaires', header: t('console.profils.titulaires'), type: 'number' },
    { key: 'permissions', header: t('console.profils.permissions'), type: 'number', render: (p) => compositionDe(p).length },
    {
      key: 'modifieLe', header: t('console.profils.modifie'),
      render: (p) => (p.modifieLe ? <span className="gm-col" style={{ gap: 0 }}><span>{dateHeure(p.modifieLe, i18n.language)}</span><span className="gm-type-small">{p.modifiePar?.nomAffiche ?? ''}</span></span> : <span className="gm-type-small">{t('console.profils.jamaisModifie')}</span>),
    },
    {
      // Cellule ordinaire (les boutons ne se chevauchent pas quand la ligne est basse) ; boutons alignés sur une ligne.
      key: 'actions', header: '',
      render: (p) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)', justifyContent: 'flex-end', flexWrap: 'nowrap' }}>
          <Button size="sm" variant={modifiable(p) ? 'secondary' : 'ghost'} onClick={() => setOuvert(p)}>{t(modifiable(p) ? 'console.profils.modifier' : 'console.profils.voir')}</Button>
          {modifiable(p) ? (
            <Button size="sm" variant="ghost" icon="pen-line" aria-label={t('console.profils.presentation.de', { profil: nom(p) })} onClick={() => setPresente(p)}>{t('console.profils.presentation.court')}</Button>
          ) : null}
          {/* Un profil livré ne se supprime jamais (D-86) ; un profil créé seulement sans titulaire. */}
          {modifiable(p) && p.cree && p.titulaires === 0 ? (
            <Button size="sm" variant="danger" icon="trash" aria-label={t('console.profils.supprimerQui', { profil: p.code })} onClick={() => actes.sensible({
              titre: t('console.profils.supprimer'),
              objet: [{ key: t('console.profils.profil'), value: p.code, mono: true }, { key: t('console.profils.description'), value: p.description ?? '' }],
              contenu: { profil: p.code, suppression: true },
              succes: t('console.profils.supprime', { profil: p.code }),
              executer: (motif, assertion) => supprimer.mutateAsync({ code: p.code, motif, assertion }),
            })}>{t('console.profils.supprimerCourt')}</Button>
          ) : null}
        </span>
      ),
    },
  ]

  return (
    <div className="gm-col">
      <PageHeader title={t('console.profils.titre')} subtitle={t('console.profils.texte')}
        actions={vue === 'profils' ? (
          <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
            <Button icon="git-compare" disabled={lignes.length < 2} onClick={() => setComparer(true)}>{t('console.profils.cmp.ouvrir')}</Button>
            {edition ? <Button variant="primary" icon="plus" onClick={() => setNouveau(true)}>{t('console.profils.nouveau')}</Button> : null}
          </span>
        ) : undefined} />
      <Tabs items={[{ key: 'profils', label: t('console.profils.onglet.liste') }, { key: 'fonctionnalites', label: t('console.fct.titre') }]} value={vue}
        onChange={(k) => setParams(k === 'fonctionnalites' ? { vue: 'fonctionnalites' } : {})} />
      {vue === 'fonctionnalites' ? <CatalogueFonctionnalites /> : (
        <>
          <Annonce retour={actes.retour} />
          <DataTable columns={colonnes} rows={lignes} rowKey={(p) => p.code} pageSize={25} onRetry={() => void profils.refetch()} emptyText={t('console.profils.aucun')} {...etatListe(profils, lignes)} />
        </>
      )}
      {ouvert ? (
        <FicheProfil profil={ouvert} modifiable={modifiable(ouvert)} onFermer={() => setOuvert(null)} onContinuer={(choix) => {
          const p = ouvert
          setOuvert(null)
          actes.sensible({
            titre: t('console.profils.composition', { profil: nom(p) }),
            objet: [{ key: t('console.profils.profil'), value: p.code, mono: true }, { key: t('console.profils.permissions'), value: String(choix.length) }],
            contenu: { profil: p.code, permissions: choix },
            succes: t('console.profils.enregistre', { profil: nom(p) }),
            executer: (motif, assertion) => definir.mutateAsync({ code: p.code, permissions: choix, motif, assertion }),
          })
        }} />
      ) : null}
      {presente ? (
        <DialoguePresentation profil={presente} onFermer={() => setPresente(null)} onContinuer={(x) => {
          const p = presente
          setPresente(null)
          actes.sensible({
            titre: t('console.profils.presentation.titre'),
            objet: [
              { key: t('console.profils.profil'), value: p.code, mono: true },
              { key: t('console.profils.libelle'), value: x.libelle === nom(p) ? x.libelle : `${nom(p)} → ${x.libelle}` },
              ...(x.description !== (p.description ?? '') ? [{ key: t('console.profils.description'), value: x.description || t('console.profils.sansDescription') }] : []),
              ...(x.externe !== undefined && x.externe !== !!p.externe ? [{ key: t('console.profils.externe'), value: t(x.externe ? 'console.profils.externeOui' : 'console.profils.externeNon') }] : []),
            ],
            contenu: { profil: p.code, ...x },
            succes: t('console.profils.presentation.fait', { profil: x.libelle }),
            executer: (motif, assertion) => presenter.mutateAsync({ code: p.code, ...x, motif, assertion }),
          })
        }} />
      ) : null}
      {nouveau ? (
        <NouveauProfil onFermer={() => setNouveau(false)} onContinuer={(x) => {
          setNouveau(false)
          actes.sensible({
            titre: t('console.profils.nouveau'),
            objet: [
              { key: t('console.profils.profil'), value: x.code, mono: true },
              ...(x.libelle ? [{ key: t('console.profils.libelle'), value: x.libelle }] : []),
              { key: t('console.profils.permissions'), value: String(x.permissions.length) },
              ...(x.permissions.some(privilegiee) ? [{ key: t('console.profils.nature'), value: t('console.profils.privilegie') }] : []),
            ],
            contenu: x,
            succes: t('console.profils.cree', { profil: x.code }),
            executer: (motif, assertion) => creer.mutateAsync({ ...x, motif, assertion }),
          })
        }} />
      ) : null}
      {comparer ? <DialogueComparaison profils={lignes} onFermer={() => setComparer(false)} /> : null}
      {actes.dialogue}
    </div>
  )
}
