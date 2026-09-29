import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, DataTable, Dialog, PermissionPicker, StatusBadge, type Column, type PermissionGroup } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { useLibelleProfil } from '@gimac/compte'
import { useEffet, usePermissionsRealm, useProfils } from '../hooks'
import { DOMAINES_PERMISSIONS, type Profil } from '../modele/configuration'
import { Annonce } from '../composants/Annonce'
import { useActes } from '../composants/actes'
import { etatListe } from '../composants/formats'

// Profils et permissions (D-58, D-62) : chaque profil par fonction, ses titulaires et sa composition.
// Lecture avec IAM_ACCOUNT_READ ; composition modifiée avec IAM_PROFILE_EDIT, motif et réauthentification
// FIDO2. Un profil privilégié (super administrateur, RSSI) n'est modifiable qu'avec IAM_ROLE_PRIVILEGED_GRANT.
// Les permissions internes des services (INTERNAL_*) ne sont jamais proposées.

/** Permissions du domaine, groupées par préfixe (HOM, CER, PAR, IAM, PLATFORM). */
function groupesPermissions(permissions: string[], libelle: (d: string) => string): PermissionGroup[] {
  return DOMAINES_PERMISSIONS.map((d) => ({
    key: d,
    label: libelle(d),
    options: permissions.filter((p) => p.startsWith(`${d}_`)).sort().map((p) => ({ value: p, label: p })),
  })).filter((g) => g.options.length > 0)
}

function Composition({ profil, modifiable, onFermer, onContinuer }: { profil: Profil; modifiable: boolean; onFermer: () => void; onContinuer: (p: string[]) => void }) {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfil()
  const permissions = usePermissionsRealm()
  const [choix, setChoix] = useState<string[]>(profil.permissions.filter((p) => !p.startsWith('INTERNAL_')))
  return (
    <Dialog open size="lg" id="gm-dlg-composition" title={t('console.profils.composition', { profil: libelleProfil(profil.code) })}
      description={t(modifiable ? 'console.profils.composition.texte' : 'console.profils.composition.lecture')} onClose={onFermer}
      footer={[
        <Button key="a" variant="secondary" onClick={onFermer}>{t(modifiable ? 'commun.annuler' : 'commun.fermer')}</Button>,
        modifiable ? <Button key="b" variant="primary" icon="pen-line" onClick={() => onContinuer([...choix].sort())}>{t('console.profils.enregistrer')}</Button> : null,
      ]}>
      <PermissionPicker label={t('console.profils.permissions')} readOnly={!modifiable} value={choix} onChange={setChoix}
        groups={groupesPermissions(permissions.data ?? profil.permissions, (d) => t(`console.domaine.${d}`))} />
    </Dialog>
  )
}

export default function ProfilsPage() {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const libelleProfil = useLibelleProfil()
  const profils = useProfils()
  const actes = useActes()
  const definir = useEffet((s, v: { code: string; permissions: string[]; motif: string; assertion: string }) => s.configuration.definirPermissions(v.code, v.permissions, v.motif, v.assertion))
  const [ouvert, setOuvert] = useState<Profil | null>(null)
  const edition = aPermission(permissions, 'IAM_PROFILE_EDIT')
  const privilegie = aPermission(permissions, 'IAM_ROLE_PRIVILEGED_GRANT')
  const modifiable = (p: Profil) => edition && (!p.privilegie || privilegie)
  const lignes = profils.data ?? []

  const colonnes: Column<Profil>[] = [
    { key: 'code', header: t('console.profils.profil'), render: (p) => <span className="gm-col" style={{ gap: 0 }}><span>{libelleProfil(p.code)}</span><span className="gm-mono gm-type-small">{p.code}</span></span> },
    { key: 'description', header: t('console.profils.description'), render: (p) => p.description ?? '—' },
    { key: 'privilegie', header: t('console.profils.nature'), render: (p) => (p.privilegie ? <StatusBadge status="warn" icon="lock" label={t('console.profils.privilegie')} /> : <StatusBadge status="neutral" label={t('console.profils.standard')} />) },
    { key: 'titulaires', header: t('console.profils.titulaires'), type: 'number' },
    { key: 'permissions', header: t('console.profils.permissions'), type: 'number', render: (p) => p.permissions.filter((x) => !x.startsWith('INTERNAL_')).length },
    {
      key: 'actions', header: '', type: 'actions',
      render: (p) => <Button size="sm" variant={modifiable(p) ? 'secondary' : 'ghost'} onClick={() => setOuvert(p)}>{t(modifiable(p) ? 'console.profils.modifier' : 'console.profils.voir')}</Button>,
    },
  ]

  return (
    <div className="gm-col">
      <h1 className="gm-heading-display">{t('console.profils.titre')}</h1>
      <p style={{ margin: 0 }}>{t('console.profils.texte')}</p>
      <Annonce retour={actes.retour} />
      <DataTable columns={colonnes} rows={lignes} rowKey={(p) => p.code} onRetry={() => void profils.refetch()} emptyText={t('console.profils.aucun')} {...etatListe(profils, lignes)} />
      {ouvert ? (
        <Composition profil={ouvert} modifiable={modifiable(ouvert)} onFermer={() => setOuvert(null)} onContinuer={(choix) => {
          const p = ouvert
          setOuvert(null)
          actes.sensible({
            titre: t('console.profils.composition', { profil: libelleProfil(p.code) }),
            objet: [{ key: t('console.profils.profil'), value: p.code, mono: true }, { key: t('console.profils.permissions'), value: String(choix.length) }],
            contenu: { profil: p.code, permissions: choix },
            succes: t('console.profils.enregistre', { profil: libelleProfil(p.code) }),
            executer: (motif, assertion) => definir.mutateAsync({ code: p.code, permissions: choix, motif, assertion }),
          })
        }} />
      ) : null}
      {actes.dialogue}
    </div>
  )
}
