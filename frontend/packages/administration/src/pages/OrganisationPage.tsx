import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, ConfirmDialog, CrudDialog, EmptyState, KeyValueList, PageHeader, Select, StatusBadge, TextField, TreeView, type TreeNode } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { useMessageErreur, type MessageErreur } from '@gimac/compte'
import { TYPES_UNITE, type TypeUnite, type Unite } from '@gimac/lot1'
import { useEffet, useUnitesOrganisation } from '../hooks'
import { Annonce } from '../composants/Annonce'
import type { Retour } from '../composants/actes'
import { Donnees } from '../composants/communs'
import { enArbre } from '../composants/rattachement'

// Organisation du GIMAC (D-76, docs/lot1/mon-activite.md §1.1, /des/administration/organisation) : catalogue
// des unités organisationnelles de MS-IAM en arbre (direction, départements, services, pôles, cellules),
// lu avec IAM_ACCOUNT_READ ; création, modification et désactivation en fenêtre modale avec IAM_ACCOUNT_EDIT.
// Une unité désactivée n'est plus proposée au rattachement ; ses rattachements restent lisibles.

const CHAMPS_UNITE: Record<string, keyof Unite> = { code: 'code', libelle: 'libelle', parent: 'parent', type: 'type' }

function DialogueUnite({ unite, unites, onFermer, onFait }: { unite?: Unite; unites: Unite[]; onFermer: () => void; onFait: (u: Unite) => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const enregistrer = useEffet((s, u: Unite) => (unite ? s.organisation.modifierUnite(unite.code, u) : s.organisation.creerUnite(u)))
  const [initiale] = useState(() => ({ code: unite?.code ?? '', libelle: unite?.libelle ?? '', type: unite?.type ?? 'SERVICE' as TypeUnite, parent: unite?.parent ?? '' }))
  const [v, setV] = useState(initiale)
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const [erreur, setErreur] = useState<MessageErreur | null>(null)
  const champ = (k: keyof typeof v) => (x: string) => setV((o) => ({ ...o, [k]: x }))
  // Parent : une unité active, ni l'unité elle-même ni l'une de ses sous-unités ; aucun pour une direction.
  const descendants = (code: string): string[] => unites.filter((u) => u.parent === code).flatMap((u) => [u.code, ...descendants(u.code)])
  const exclus = unite ? [unite.code, ...descendants(unite.code)] : []
  const parents = enArbre(unites.filter((u) => u.active && !exclus.includes(u.code)))
  const soumettre = async () => {
    setErreurs({})
    setErreur(null)
    const code = v.code.trim().toUpperCase()
    const local: Record<string, string> = {}
    if (!/^[A-Z][A-Z0-9_]{1,31}$/.test(code)) local.code = t('console.org.unite.erreur.code')
    if (!v.libelle.trim()) local.libelle = t('console.org.unite.erreur.libelle')
    if (v.type !== 'DIRECTION' && !v.parent) local.parent = t('console.org.unite.erreur.parent')
    if (Object.keys(local).length) return setErreurs(local)
    try {
      onFait(await enregistrer.mutateAsync({ code, libelle: v.libelle.trim(), type: v.type, parent: v.type === 'DIRECTION' ? null : v.parent || null, active: unite?.active ?? true }))
    } catch (x) {
      const m = message(x)
      const details = (x as { details?: { champ?: string } }).details
      const c = m.code === 'ORG_UNITE_INVALIDE' ? CHAMPS_UNITE[details?.champ ?? ''] : m.code === 'ORG_UNITE_EXISTANTE' ? 'code' : m.code === 'ORG_UNITE_INCONNUE' || m.code === 'ORG_CYCLE' ? 'parent' : undefined
      if (c) setErreurs({ [c]: m.texte })
      else setErreur(m)
    }
  }
  return (
    <CrudDialog
      context={unite ? 'edit' : 'create'}
      size="md"
      id="gm-dlg-unite"
      title={t(unite ? 'console.org.unite.modifier' : 'console.org.unite.nouvelle')}
      subject={unite ? <span className="gm-mono">{unite.code}</span> : undefined}
      description={t('console.org.unite.texte')}
      onClose={onFermer}
      onSubmit={() => void soumettre()}
      submitLabel={t(unite ? 'console.org.unite.enregistrer' : 'console.org.unite.creer')}
      submitting={enregistrer.isPending}
      dirty={JSON.stringify(v) !== JSON.stringify(initiale)}
      error={erreur ? { title: erreur.texte, correlationId: erreur.correlationId } : undefined}
    >
      <div className="gm-form-grid">
        <div className="gm-form-field">
          <TextField name="code" label={t('console.org.unite.code')} help={t('console.org.unite.codeAide')} mono required maxLength={32} readOnly={!!unite}
            value={v.code} onChange={(x) => champ('code')(x.toUpperCase())} error={erreurs.code} />
        </div>
        <div className="gm-form-field">
          <Select name="type" label={t('console.org.unite.type')} value={v.type} onChange={champ('type')} options={TYPES_UNITE.map((x) => ({ value: x, label: t(`console.org.type.${x}`) }))} />
        </div>
        <div className="gm-form-field gm-form-wide">
          <TextField name="libelle" label={t('console.org.unite.libelle')} required maxLength={120} value={v.libelle} onChange={champ('libelle')} error={erreurs.libelle} />
        </div>
        {v.type !== 'DIRECTION' ? (
          <div className="gm-form-field gm-form-wide">
            <Select name="parent" label={t('console.org.unite.parent')} required value={v.parent} onChange={champ('parent')} error={erreurs.parent}
              options={[{ value: '', label: t('console.org.unite.choisirParent') }, ...parents.map(({ unite: u, profondeur }) => ({ value: u.code, label: `${'  '.repeat(profondeur)}${u.libelle}` }))]} />
          </div>
        ) : null}
      </div>
    </CrudDialog>
  )
}

export default function OrganisationPage() {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const permissions = usePermissions()
  const edition = aPermission(permissions, 'IAM_ACCOUNT_EDIT')
  const unites = useUnitesOrganisation()
  const etat = useEffet((s, u: Unite) => s.organisation.modifierUnite(u.code, u))
  const [choisie, setChoisie] = useState<string | undefined>()
  const [dialogue, setDialogue] = useState<'nouvelle' | 'modifier' | 'etat' | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const fait = (texte: string) => {
    setDialogue(null)
    setRetour({ niveau: 'info', message: { texte } })
  }
  return (
    <div className="gm-col">
      <PageHeader
        title={t('console.org.titre')}
        subtitle={t('console.org.sousTitre')}
        actions={edition ? <Button variant="primary" icon="plus" onClick={() => { setRetour(null); setDialogue('nouvelle') }}>{t('console.org.unite.nouvelle')}</Button> : undefined}
      />
      <Annonce retour={retour} />
      <Donnees requete={unites} estVide={(l) => l.length === 0} vide={t('console.org.vide')}>
        {(l) => {
          const noeud = (u: Unite): TreeNode => ({
            key: u.code, label: u.libelle, code: u.code, icon: 'landmark',
            badge: u.active ? undefined : { status: 'neutral', label: t('console.org.inactive') },
            children: l.filter((x) => x.parent === u.code).sort((a, b) => a.libelle.localeCompare(b.libelle, 'fr')).map(noeud),
          })
          const racines = l.filter((u) => !u.parent || !l.some((x) => x.code === u.parent))
          const u = l.find((x) => x.code === choisie)
          return (
            <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-4)', flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 320px', minWidth: 0 }}>
                <Card title={t('console.org.arbre')}>
                  <TreeView label={t('console.org.arbre')} nodes={racines.map(noeud)} expanded={l.map((x) => x.code)} selected={choisie} onSelect={(k) => { setRetour(null); setChoisie(k) }} />
                </Card>
              </div>
              <div style={{ flex: '1 1 320px', minWidth: 0 }}>
                <Card title={u ? u.libelle : t('console.org.detail')}
                  actions={u && edition ? (
                    <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
                      <Button size="sm" icon="pen-line" onClick={() => setDialogue('modifier')}>{t('console.org.unite.modifier')}</Button>
                      <Button size="sm" variant={u.active ? 'danger' : 'secondary'} onClick={() => setDialogue('etat')}>{t(u.active ? 'console.org.unite.desactiver' : 'console.org.unite.reactiver')}</Button>
                    </span>
                  ) : undefined}>
                  {u ? (
                    <KeyValueList
                      items={[
                        { key: t('console.org.unite.code'), value: u.code, mono: true },
                        { key: t('console.org.unite.type'), value: t(`console.org.type.${u.type}`) },
                        { key: t('console.org.unite.parent'), value: l.find((x) => x.code === u.parent)?.libelle ?? t('console.org.unite.sansParent') },
                        { key: t('console.org.unite.etat'), value: <StatusBadge status={u.active ? 'ok' : 'neutral'} label={t(u.active ? 'console.org.active' : 'console.org.inactive')} /> },
                        { key: t('console.org.unite.sousUnites'), value: l.filter((x) => x.parent === u.code).length },
                      ]}
                    />
                  ) : <EmptyState icon="landmark" title={t('console.org.choisir')} text={t('console.org.choisirTexte')} />}
                </Card>
              </div>
              {dialogue === 'nouvelle' ? <DialogueUnite unites={l} onFermer={() => setDialogue(null)} onFait={(x) => { setChoisie(x.code); fait(t('console.org.unite.creee', { libelle: x.libelle })) }} /> : null}
              {dialogue === 'modifier' && u ? <DialogueUnite unite={u} unites={l} onFermer={() => setDialogue(null)} onFait={(x) => fait(t('console.org.unite.modifiee', { libelle: x.libelle }))} /> : null}
              {dialogue === 'etat' && u ? (
                <ConfirmDialog danger={u.active} title={t(u.active ? 'console.org.unite.desactiver' : 'console.org.unite.reactiver')}
                  description={t(u.active ? 'console.org.unite.desactiverTexte' : 'console.org.unite.reactiverTexte')}
                  confirmLabel={t(u.active ? 'console.org.unite.desactiver' : 'console.org.unite.reactiver')}
                  object={[{ key: t('console.org.unite.libelle'), value: u.libelle }, { key: t('console.org.unite.code'), value: u.code }]}
                  onClose={() => setDialogue(null)}
                  onConfirm={() => {
                    setDialogue(null)
                    etat.mutateAsync({ ...u, active: !u.active }).then(
                      () => setRetour({ niveau: 'info', message: { texte: t(u.active ? 'console.org.unite.desactivee' : 'console.org.unite.reactivee', { libelle: u.libelle }) } }),
                      (x: unknown) => setRetour({ niveau: 'crit', message: message(x) }),
                    )
                  }} />
              ) : null}
            </div>
          )
        }}
      </Donnees>
    </div>
  )
}
