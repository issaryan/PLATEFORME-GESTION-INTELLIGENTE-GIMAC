import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, ConfirmDialog, CrudDialog, DataTable, DateField, EmptyState, InlineAlert, Select, TextArea, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import type { Blocage, DossierLiaison, Fonction } from '../../../../modele/parcours'
import { useDeclarerBlocage, useLeverBlocage, usePersonnes } from '../../../../hooks/parcours'
import { PERM } from '../../../../permissions'
import { useErreurService } from '../../liaisons/communs'
import { Jour, useRetour, versIso } from '../communs'

// Onglet « Blocages » (CR annexe B, D-59) : un blocage est un objet daté, non une mention libre : motif,
// depuis le, prochaine action, responsable et échéance de levée. Déclaration et levée passent par le
// service, qui les trace ; l'onglet ne propose que les gestes permis (actionsPermises et permission).

const FONCTIONS_RESPONSABLES: Fonction[] = ['CONDUCTEUR', 'DDRM', 'CHEF_DDRM', 'CHEF_DES', 'SNA', 'PRODUCTION_MONETIQUE', 'POLE_RESEAU', 'POLE_SYSTEME', 'RSSI', 'CONFORMITE', 'EXPLOITATION']
const MOTIF_MIN = 10

export function DialogueBlocage({ dossier, onFermer, onDeclare }: { dossier: DossierLiaison; onFermer: () => void; onDeclare: () => void }) {
  const { t } = useTranslation()
  const declarer = useDeclarerBlocage()
  const erreurService = useErreurService()
  const [motif, setMotif] = useState('')
  const [prochaineAction, setProchaine] = useState('')
  const [fonction, setFonction] = useState<Fonction>('CONDUCTEUR')
  const [responsableId, setResponsable] = useState('')
  const [echeance, setEcheance] = useState('')
  const [tente, setTente] = useState(false)
  const personnes = usePersonnes(fonction)
  const iso = versIso(echeance)
  const erreurs = {
    motif: motif.trim().length < MOTIF_MIN ? t('parcoursBlocages.erreur.motif', { n: MOTIF_MIN }) : undefined,
    prochaineAction: !prochaineAction.trim() ? t('parcoursBlocages.erreur.prochaineAction') : undefined,
    responsable: !responsableId ? t('parcoursBlocages.erreur.responsable') : undefined,
    echeance: !iso ? t('parcoursBlocages.erreur.echeance') : undefined,
  }
  const refus = erreurService(declarer.error)
  const soumettre = () => {
    setTente(true)
    if (Object.values(erreurs).some(Boolean) || !iso) return
    declarer.mutate({ liaisonId: dossier.id, motif: motif.trim(), prochaineAction: prochaineAction.trim(), responsableId, echeanceLevee: iso }, { onSuccess: onDeclare })
  }
  const e = (k: keyof typeof erreurs) => (tente ? erreurs[k] : undefined)
  return (
    <CrudDialog
      context="create"
      size="md"
      id="gm-blocage"
      title={t('parcoursBlocages.declarer')}
      subject={`${dossier.reference} · ${dossier.institutionNom}`}
      description={t('parcoursBlocages.description', { etape: t(`parcours.etape.${dossier.etapeCourante}`) })}
      onClose={onFermer}
      onSubmit={soumettre}
      submitLabel={t('parcoursBlocages.declarer')}
      submitIcon="flag"
      submitting={declarer.isPending}
      dirty={motif.trim() !== '' || prochaineAction.trim() !== ''}
    >
      <div className="gm-col">
        {refus ? (
          <InlineAlert level="crit" title={t('parcoursBlocages.refus')} correlationId={refus.correlationId || undefined}>
            {refus.message}
          </InlineAlert>
        ) : null}
        <TextArea name="motif" label={t('parcoursBlocages.motif')} required help={t('parcoursBlocages.motif.aide')} value={motif} error={e('motif')} onChange={setMotif} />
        <TextArea name="prochaineAction" label={t('parcoursBlocages.prochaineAction')} required rows={2} value={prochaineAction} error={e('prochaineAction')} onChange={setProchaine} />
        <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-4)' }}>
          <div style={{ flex: '1 1 200px', minWidth: 0 }}>
            <Select
              name="fonctionResponsable"
              label={t('parcoursBlocages.fonction')}
              value={fonction}
              options={FONCTIONS_RESPONSABLES.map((f) => ({ value: f, label: t(`parcours.fonction.${f}`) }))}
              onChange={(v) => {
                setFonction(v as Fonction)
                setResponsable('')
              }}
            />
          </div>
          <div style={{ flex: '1 1 200px', minWidth: 0 }}>
            <Select
              name="responsableId"
              label={t('parcoursBlocages.responsable')}
              required
              value={responsableId}
              options={[{ value: '', label: t('parcoursOuverture.choisir') }, ...(personnes.data ?? []).map((p) => ({ value: p.id, label: p.nom }))]}
              disabled={personnes.isPending}
              error={e('responsable')}
              onChange={setResponsable}
            />
          </div>
        </div>
        <DateField name="echeanceLevee" label={t('parcoursBlocages.echeanceLevee')} required value={echeance} error={e('echeance')} onChange={setEcheance} />
      </div>
    </CrudDialog>
  )
}

export function OngletBlocages({ dossier, onDeclarer }: { dossier: DossierLiaison; onDeclarer?: () => void }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const lever = useLeverBlocage()
  const erreurService = useErreurService()
  const [aLever, setALever] = useState<Blocage | null>(null)
  const [retour, annoncer] = useRetour()
  const peutLever = (b: Blocage) => b.actionsPermises.includes('LEVER') && aPermission(permissions, PERM.etapeConduire)
  const enCours = dossier.blocages.filter((b) => !b.leveLe)
  const leves = dossier.blocages.filter((b) => b.leveLe)
  const refus = erreurService(lever.error)

  const colonnes = (historique: boolean): Column<Blocage>[] => [
    { key: 'motif', header: t('parcoursBlocages.motif'), render: (b) => <span>{b.motif}</span> },
    { key: 'etape', header: t('parcoursEtapes.etape'), render: (b) => t(`parcours.etape.${b.etape}`) },
    { key: 'depuis', header: t('parcoursBlocages.depuis'), render: (b) => <Jour iso={b.depuis} /> },
    ...(historique
      ? [{ key: 'leveLe', header: t('parcoursBlocages.leveLe'), render: (b: Blocage) => <Jour iso={b.leveLe} /> }]
      : [
          { key: 'prochaineAction', header: t('parcoursBlocages.prochaineAction'), render: (b: Blocage) => <span>{b.prochaineAction}</span> },
          { key: 'responsable', header: t('parcoursBlocages.responsable'), render: (b: Blocage) => <span className="gm-col" style={{ gap: 0 }}><span>{b.responsable.nom}</span><span className="gm-field-help">{t(`parcours.fonction.${b.responsable.fonction}`)}</span></span> },
          { key: 'echeanceLevee', header: t('parcoursBlocages.echeanceLevee'), render: (b: Blocage) => <Jour iso={b.echeanceLevee} /> },
          {
            key: 'actions', header: '', type: 'actions' as const,
            render: (b: Blocage) => (peutLever(b) ? <Button size="sm" variant="secondary" icon="check" onClick={() => setALever(b)}>{t('parcoursBlocages.lever')}</Button> : null),
          },
        ]),
  ]

  return (
    <div className="gm-col">
      <Card
        title={t('parcoursBlocages.enCours', { count: enCours.length })}
        actions={onDeclarer ? <Button variant="secondary" icon="flag" onClick={onDeclarer}>{t('parcoursBlocages.declarer')}</Button> : undefined}
      >
        {refus ? (
          <InlineAlert level="crit" title={t('parcoursBlocages.refusLevee')} correlationId={refus.correlationId || undefined}>
            {refus.message}
          </InlineAlert>
        ) : null}
        {enCours.length ? (
          <DataTable columns={colonnes(false)} rows={enCours} rowKey={(b) => b.id} />
        ) : (
          <EmptyState icon="circle-check" title={t('parcoursBlocages.aucun')} text={t('parcoursBlocages.aucun.aide')} />
        )}
      </Card>
      {leves.length ? (
        <Card title={t('parcoursBlocages.historique', { count: leves.length })}>
          <DataTable columns={colonnes(true)} rows={leves} rowKey={(b) => b.id} density="compact" />
        </Card>
      ) : null}
      {aLever ? (
        <ConfirmDialog
          open
          title={t('parcoursBlocages.lever')}
          confirmLabel={t('parcoursBlocages.lever')}
          reason
          object={[
            { key: t('parcoursBlocages.motif'), value: aLever.motif },
            { key: t('parcoursBlocages.depuis'), value: <Jour iso={aLever.depuis} /> },
          ]}
          onClose={() => setALever(null)}
          onConfirm={(commentaire) =>
            lever.mutate(
              { blocageId: aLever.id, commentaire },
              {
                onSuccess: () => annoncer(t('parcoursBlocages.leveFait')),
                onSettled: () => setALever(null),
              },
            )
          }
        />
      ) : null}
      {retour}
    </div>
  )
}
