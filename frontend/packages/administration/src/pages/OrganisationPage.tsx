import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, ConfirmDialog, EmptyState, KeyValueList, PageHeader, StatusBadge, TreeView, type TreeNode } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { useMessageErreur } from '@gimac/compte'
import type { Unite } from '@gimac/lot1'
import { useEffet, useUnitesOrganisation } from '../hooks'
import { Annonce } from '../composants/Annonce'
import type { Retour } from '../composants/actes'
import { Donnees } from '../composants/communs'
import { DialogueUnite, useResponsables } from '../composants/unites'

// Organisation du GIMAC (D-76, docs/lot1/mon-activite.md §1.1, /des/administration/organisation) : catalogue
// des unités organisationnelles de MS-IAM en arbre (direction, départements, services, pôles, cellules),
// lu avec IAM_ACCOUNT_READ ; création, modification et désactivation en fenêtre modale avec IAM_ACCOUNT_EDIT.
// Une unité désactivée n'est plus proposée au rattachement ; ses rattachements restent lisibles.
// D-81 §2 : responsable de l'unité (compte interne actif, ou aucun), qui dirige l'unité et ses sous-unités.

export default function OrganisationPage() {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const permissions = usePermissions()
  const edition = aPermission(permissions, 'IAM_ACCOUNT_EDIT')
  const unites = useUnitesOrganisation()
  const responsables = useResponsables()
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
                        { key: t('console.org.unite.responsable'), value: u.responsableId ? responsables.find((c) => c.id === u.responsableId)?.nomAffiche ?? u.responsableId : t('console.org.unite.sansResponsable') },
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
