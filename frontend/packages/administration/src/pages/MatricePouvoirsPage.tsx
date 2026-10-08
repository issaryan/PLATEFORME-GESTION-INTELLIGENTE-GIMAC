import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { CheckboxGroup, IconButton, CrudDialog, DataTable, PageHeader, Select, type Column } from '@gimac/design-system'
import { aPermission, usePermissions, useLibelleProfil } from '@gimac/socle'
import { ACTIONS_POUVOIR, PORTEES, completerActions, porteesAdmisesParDefaut, type ActionPouvoir, type ActionsPouvoir, type MatricePouvoirs, type PouvoirsProfil, type Portee } from '@gimac/lot1'
import { BadgePortee } from '../composants/profils'
import { useEffet, useLibellesProfils, useMatricePouvoirs, useProfils } from '../hooks'
import { Annonce } from '../composants/Annonce'
import { useActes } from '../composants/actes'
import { etatListe } from '../composants/formats'

// Matrice des pouvoirs (D-81 §3, /console/matrice-pouvoirs et /des/administration/matrice-pouvoirs) : pour chaque
// profil et chaque action, la portée (aucune, subordonnés directs, équipe, équipe et unités dirigées), et les
// profils qu'un titulaire peut proposer pour un subordonné (jamais un profil privilégié). Lecture IAM_ACCOUNT_READ ;
// modification d'une ligne en fenêtre modale avec IAM_PROFILE_EDIT, un motif et la clé de sécurité (FIDO2).

// D-86 §4 et §5 : libellé de chaque ligne servi par MS-IAM (catalogue de l'interface à défaut), actions PROCESSUS_DEFINIR
// et PROCESSUS_LANCER (processus d'unité, D-87) ; une action absente d'une ligne vaut AUCUNE. ?profil=<code> met la
// ligne en évidence (lien depuis la fiche d'un profil et le panneau « Pouvoirs » de l'organigramme).

/** Portées proposées pour une action (D-92) : `porteesAdmises` servies par MS-IAM ; TOUTES seulement pour les processus. */
const admises = (m: MatricePouvoirs | undefined, a: ActionPouvoir): readonly Portee[] => m?.porteesAdmises?.[a] ?? porteesAdmisesParDefaut(a)

function DialogueLigne({ ligne, demandables, matrice, onFermer, onContinuer }: {
  ligne: PouvoirsProfil; demandables: string[]; matrice?: MatricePouvoirs; onFermer: () => void; onContinuer: (e: { actions: ActionsPouvoir; profilsDemandables: string[] }) => void
}) {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfil(useLibellesProfils())
  const [actions, setActions] = useState<ActionsPouvoir>(completerActions(ligne.actions))
  const [profils, setProfils] = useState<string[]>(ligne.profilsDemandables)
  const dirty = JSON.stringify(actions) !== JSON.stringify(completerActions(ligne.actions)) || [...profils].sort().join() !== [...ligne.profilsDemandables].sort().join()
  return (
    <CrudDialog context="edit" size="lg" id="gm-dlg-matrice" title={t('console.matrice.modifier', { profil: libelleProfil(ligne.profil, ligne.libelle) })} subject={<span className="gm-mono">{ligne.profil}</span>}
      description={t('console.matrice.modifierTexte')} onClose={onFermer} onSubmit={() => onContinuer({ actions, profilsDemandables: [...profils].sort() })}
      submitLabel={t('console.profils.enregistrer')} submitIcon="pen-line" reauth dirty={dirty} submitDisabled={!dirty} submitDisabledReason={t('console.matrice.inchangee')}>
      <div className="gm-form-grid">
        {ACTIONS_POUVOIR.map((a) => (
          <div key={a} className="gm-form-field">
            <Select name={a} label={t(`console.matrice.action.${a}`)} help={t(`console.matrice.aide.${a}`)} value={actions[a]} onChange={(v) => setActions((o) => ({ ...o, [a]: v as Portee }))}
              options={admises(matrice, a).map((p) => ({ value: p, label: t(`console.matrice.portee.${p}`) }))} />
          </div>
        ))}
      </div>
      <CheckboxGroup id="gm-matrice-demandables" label={t('console.matrice.demandables')} value={profils} onChange={setProfils}
        options={demandables.map((p) => ({ value: p, label: libelleProfil(p) }))} />
    </CrudDialog>
  )
}

export default function MatricePouvoirsPage() {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const libelleProfil = useLibelleProfil(useLibellesProfils())
  const matrice = useMatricePouvoirs()
  const profils = useProfils()
  const actes = useActes()
  const modifier = useEffet((s, v: { profil: string; actions: ActionsPouvoir; profilsDemandables: string[]; motif: string; assertion: string }) =>
    s.pouvoirs.modifierMatrice(v.profil, { actions: v.actions, profilsDemandables: v.profilsDemandables }, v.motif, v.assertion))
  const [ouverte, setOuverte] = useState<PouvoirsProfil | null>(null)
  const [params] = useSearchParams()
  const enEvidence = params.get('profil')
  const nom = (l: PouvoirsProfil) => libelleProfil(l.profil, l.libelle)
  const edition = aPermission(permissions, 'IAM_PROFILE_EDIT')
  const lignes = matrice.data?.profils ?? []
  // Profils demandables : jamais un profil privilégié, jamais un profil d'institution.
  const demandables = (profils.data ?? []).filter((p) => !p.privilegie && p.code !== 'ROLE_CORRESPONDANT').map((p) => p.code)
  const colonnes: Column<PouvoirsProfil>[] = [
    { key: 'profil', header: t('console.profils.profil'), render: (l) => <span className="gm-col" style={{ gap: 0 }}><span>{nom(l)}</span><span className="gm-mono gm-type-small">{l.profil}</span></span> },
    ...ACTIONS_POUVOIR.map((a): Column<PouvoirsProfil> => ({ key: a, header: t(`console.matrice.court.${a}`), render: (l) => <BadgePortee portee={completerActions(l.actions)[a]} /> })),
    { key: 'demandables', header: t('console.matrice.demandablesCourt'), type: 'number', render: (l) => l.profilsDemandables.length },
    ...(edition ? [{
      key: 'actions', header: '', type: 'actions' as const,
      render: (l: PouvoirsProfil) => <IconButton size="sm" variant="secondary" icon="pen-line" label={t('console.matrice.modifier', { profil: nom(l) })} onClick={() => setOuverte(l)} />,
    }] : []),
  ]
  return (
    <div className="gm-col">
      <PageHeader title={t('console.matrice.titre')} subtitle={t('console.matrice.texte')} />
      <Annonce retour={actes.retour} />
      <div className="gm-row" role="list" aria-label={t('console.matrice.legende')} style={{ gap: 'var(--gm-space-3)', flexWrap: 'wrap' }}>
        {PORTEES.map((p) => <span role="listitem" key={p} className="gm-row" style={{ gap: 'var(--gm-space-1)' }}><BadgePortee portee={p} /><span className="gm-field-help">{t(`console.matrice.porteeAide.${p}`)}</span></span>)}
      </div>
      <DataTable columns={colonnes} rows={lignes} rowKey={(l) => l.profil} pageSize={25} density="compact" selected={enEvidence ? [enEvidence] : undefined} onRetry={() => void matrice.refetch()} emptyText={t('console.matrice.vide')} {...etatListe(matrice, lignes)} />
      {ouverte ? (
        <DialogueLigne ligne={ouverte} demandables={demandables} matrice={matrice.data} onFermer={() => setOuverte(null)} onContinuer={(e) => {
          const l = { ...ouverte, actions: completerActions(ouverte.actions) }
          setOuverte(null)
          actes.sensible({
            titre: t('console.matrice.modifier', { profil: nom(l) }),
            objet: [
              { key: t('console.profils.profil'), value: l.profil, mono: true },
              ...ACTIONS_POUVOIR.filter((a) => e.actions[a] !== l.actions[a]).map((a) => ({ key: t(`console.matrice.action.${a}`), value: `${t(`console.matrice.portee.${l.actions[a]}`)} → ${t(`console.matrice.portee.${e.actions[a]}`)}` })),
              { key: t('console.matrice.demandables'), value: String(e.profilsDemandables.length) },
            ],
            contenu: { profil: l.profil, ...e },
            succes: t('console.matrice.enregistree', { profil: nom(l) }),
            executer: (motif, assertion) => modifier.mutateAsync({ profil: l.profil, ...e, motif, assertion }),
          })
        }} />
      ) : null}
      {actes.dialogue}
    </div>
  )
}
