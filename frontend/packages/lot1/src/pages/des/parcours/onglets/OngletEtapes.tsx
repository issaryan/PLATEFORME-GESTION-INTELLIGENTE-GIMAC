import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, Card, DataTable, InlineAlert, KeyValueList, Link, StatusBadge, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import type { DossierLiaison, ElementEtape, EtapeCode, EtapeParcours, OperationParcours } from '../../../../modele/parcours'
import { useOperationsParcours } from '../../../../hooks/parcours'
import { PERM } from '../../../../permissions'
import { Donnees } from '../../liaisons/communs'
import { Echeance, Jour, tonEtape, useRetour, Valeur } from '../communs'
import { DialogueLivrable } from '../DialogueLivrable'
import { libelleOperation } from '../../../../adaptateurs/modelisateur'

// Onglet « Étapes » du dossier (CR §7.1, D-59) : les dix étapes du parcours et le verrou « approbation
// de la sécurité », chacune avec ses six éléments (responsable, actions, livrables, validation, preuve,
// date), qui la porte, son échéance et son SLA. L'état de chaque étape et de chaque élément est servi :
// l'interface n'en déduit aucune clôture. Étapes 4 et 5 : affectation des ressources et comptes rendus.

const ELEMENTS: ElementEtape[] = ['RESPONSABLE', 'ACTIONS', 'LIVRABLES', 'VALIDATION', 'PREUVE', 'DATE']
/** Étapes dont les livrables (compte rendu de lancement, comptes rendus d'ateliers) se déposent ici. */
const CONDUITE: EtapeCode[] = ['LANCEMENT', 'ATELIERS']

const nomEtape = (t: (k: string, o?: Record<string, unknown>) => string, e: EtapeParcours) =>
  e.rang === null ? t('parcoursEtapes.verrou', { nom: t(`parcours.etape.${e.code}`) }) : t('parcoursEtapes.numero', { n: e.rang, nom: t(`parcours.etape.${e.code}`) })

function DetailElement({ etape, element }: { etape: EtapeParcours; element: ElementEtape }) {
  const { t } = useTranslation()
  switch (element) {
    case 'RESPONSABLE':
      return <Valeur>{etape.responsable ? (etape.responsable.nom === t(`parcours.fonction.${etape.responsable.fonction}`) ? etape.responsable.nom : `${etape.responsable.nom} (${t(`parcours.fonction.${etape.responsable.fonction}`)})`) : null}</Valeur>
    case 'ACTIONS':
      return <span className="gm-num">{t('parcoursEtapes.actions', { n: etape.actionsFaites, total: etape.actionsTotal })}</span>
    case 'LIVRABLES':
      return etape.livrables.length ? (
        <ul className="gm-col" style={{ gap: 'var(--gm-space-1)', listStyle: 'none', margin: 0, padding: 0 }}>
          {etape.livrables.map((l) => (
            <li key={l.libelleKey} className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
              <StatusBadge status={l.fourni ? 'ok' : 'neutral'} label={t(l.fourni ? 'parcoursEtapes.fourni' : 'parcoursEtapes.attendu')} />
              <span>{t(l.libelleKey)}</span>
            </li>
          ))}
        </ul>
      ) : (
        <span className="gm-field-help">{t(etape.actionsTotal > 0 ? 'parcoursEtapes.livrablesOperations' : 'parcoursEtapes.aucunLivrable')}</span>
      )
    case 'VALIDATION':
      return etape.valideeLe ? (
        <span>{t('parcoursEtapes.valideePar', { nom: etape.validateur?.nom ?? '' })} · <Jour iso={etape.valideeLe} /></span>
      ) : (
        <Valeur>{etape.validateur ? t('parcoursEtapes.aValiderPar', { nom: etape.validateur.nom }) : null}</Valeur>
      )
    case 'PREUVE':
      return <span>{t(etape.elements.PREUVE ? 'parcoursEtapes.preuveDeposee' : 'parcoursEtapes.preuveAttendue')}</span>
    case 'DATE':
      return etape.closeLe ? <span>{t('parcoursEtapes.closeLe')} <Jour iso={etape.closeLe} /></span> : <Echeance iso={etape.echeance} sla={etape.sla} />
  }
}

function OperationsConduite({ dossier, etape }: { dossier: DossierLiaison; etape: EtapeCode }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const ops = useOperationsParcours(dossier.id, { etape })
  const [depot, setDepot] = useState<OperationParcours | null>(null)
  const [retour, annoncer] = useRetour()
  const peutDeposer = aPermission(permissions, PERM.etapeConduire, PERM.preuveDeposer)
  const colonnes: Column<OperationParcours>[] = [
    { key: 'rang', header: t('parcoursEtapes.rang'), render: (o) => <span className="gm-mono">{o.rang}</span> },
    {
      key: 'libelle', header: t('parcoursEtapes.operation'),
      render: (o) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <span>{libelleOperation(t, o)}</span>
          <span className="gm-field-help">{t(`parcours.preuveAttendue.${o.rang}`)}</span>
        </span>
      ),
    },
    { key: 'responsable', header: t('parcoursEtapes.responsable'), render: (o) => <Valeur>{o.raci.R ? `${o.raci.R.nom}` : null}</Valeur> },
    { key: 'etat', header: t('parcoursEtapes.etat'), render: (o) => <StatusBadge status={o.etat === 'FAITE' ? 'ok' : o.etat === 'BLOQUEE' ? 'crit' : o.etat === 'VERROUILLEE' ? 'neutral' : 'info'} label={t(`parcours.etatOp.${o.etat}`)} /> },
    { key: 'echeance', header: t('parcoursEtapes.echeance'), render: (o) => (o.etat === 'FAITE' ? <Jour iso={o.faiteLe} /> : <Echeance iso={o.echeance} sla={o.sla} />) },
    {
      key: 'livrable', header: t('parcoursEtapes.livrable'),
      render: (o) =>
        o.preuve ? (
          <span className="gm-mono">{o.preuve.nom}</span>
        ) : o.actionsPermises.includes('DEPOSER_LIVRABLE') && peutDeposer ? (
          <Button size="sm" variant="secondary" icon="upload" onClick={() => setDepot(o)}>{t('parcoursLivrable.deposer')}</Button>
        ) : (
          <span className="gm-field-help">{t(o.etat === 'VERROUILLEE' ? 'parcoursEtapes.enAttentePredecesseur' : 'parcoursEtapes.attendu')}</span>
        ),
    },
  ]
  return (
    <Card title={t(`parcoursEtapes.conduite.${etape}`)}>
      <Donnees requete={ops} estVide={(l) => l.length === 0} vide={t('parcoursEtapes.aucuneOperation')}>
        {(l) => <DataTable columns={colonnes} rows={l} rowKey={(o) => o.id} density="compact" />}
      </Donnees>
      {depot ? (
        <DialogueLivrable
          operation={depot}
          onFermer={() => setDepot(null)}
          onDepose={() => {
            setDepot(null)
            annoncer(t('parcoursLivrable.fait', { operation: t(`parcours.op.${depot.rang}`) }))
          }}
        />
      ) : null}
      {retour}
    </Card>
  )
}

export function OngletEtapes({ dossier, onAffecter }: { dossier: DossierLiaison; onAffecter?: () => void }) {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const choisie = dossier.etapes.find((e) => e.code === params.get('etape')) ?? dossier.etapes.find((e) => e.code === dossier.etapeCourante) ?? dossier.etapes[0]
  const lien = (e: EtapeParcours) => `/des/parcours/${dossier.id}?onglet=etapes&etape=${e.code}`
  const colonnes: Column<EtapeParcours>[] = [
    { key: 'code', header: t('parcoursEtapes.etape'), render: (e) => <Link href={lien(e)}>{nomEtape(t, e)}</Link> },
    { key: 'etat', header: t('parcoursEtapes.etat'), render: (e) => <StatusBadge status={tonEtape[e.etat]} label={t(`parcours.etatEtape.${e.etat}`)} /> },
    { key: 'porteurs', header: t('parcoursEtapes.porteurs'), render: (e) => <Valeur>{e.porteurs.map((f) => t(`parcours.fonction.${f}`)).join(', ')}</Valeur> },
    {
      key: 'elements', header: t('parcoursEtapes.elements'),
      render: (e) => {
        const n = ELEMENTS.filter((x) => e.elements[x]).length
        return e.etat === 'SANS_OBJET' ? <span className="gm-field-help">{t('parcours.etatEtape.SANS_OBJET')}</span> : <StatusBadge status={n === 6 ? 'ok' : 'neutral'} icon={null} label={t('parcoursEtapes.reunis', { n })} />
      },
    },
    { key: 'echeance', header: t('parcoursEtapes.echeance'), render: (e) => (e.closeLe ? <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>{t('parcoursEtapes.closeLe')} <Jour iso={e.closeLe} /></span> : <Echeance iso={e.echeance} sla={e.sla} />) },
  ]
  return (
    <div className="gm-col">
      <DataTable
        columns={colonnes}
        rows={dossier.etapes}
        rowKey={(e) => e.code}
        selected={[choisie.code]}
        onRowOpen={(e) => setParams({ onglet: 'etapes', etape: e.code }, { replace: true })}
      />
      <Card
        title={nomEtape(t, choisie)}
        actions={<StatusBadge status={tonEtape[choisie.etat]} label={t(`parcours.etatEtape.${choisie.etat}`)} />}
      >
        <div className="gm-col">
          {choisie.code === 'APPROBATION_SECURITE' ? (
            <InlineAlert level="info" action={<Link href={`/des/parcours/${dossier.id}?onglet=decision`}>{t('parcoursEtapes.voirDecision')}</Link>}>{t('parcoursEtapes.verrouExplication')}</InlineAlert>
          ) : null}
          {choisie.etat !== 'CLOSE' && choisie.etat !== 'SANS_OBJET' ? <InlineAlert level="info">{t('parcoursEtapes.regleSix')}</InlineAlert> : null}
          <KeyValueList
            items={ELEMENTS.map((x) => ({
              key: t(`parcours.element.${x}`),
              value: (
                <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
                  <StatusBadge status={choisie.elements[x] ? 'ok' : 'neutral'} label={t(choisie.elements[x] ? 'parcoursEtapes.reuni' : 'parcoursEtapes.manquant')} />
                  <DetailElement etape={choisie} element={x} />
                </span>
              ),
            }))}
          />
          {choisie.code === 'LANCEMENT' && onAffecter ? (
            <InlineAlert level="warn" title={t('parcoursAffectation.attendue')} action={<Button size="sm" variant="primary" icon="users" onClick={onAffecter}>{t('parcoursAffectation.titre')}</Button>}>
              {t('parcours.preuveAttendue.6')}
            </InlineAlert>
          ) : null}
          {!CONDUITE.includes(choisie.code) && choisie.code !== 'APPROBATION_SECURITE' && choisie.actionsTotal > 0 ? (
            <div>
              <Link href={`/des/parcours/${dossier.id}?onglet=operations&etape=${choisie.code}`}>{t('parcoursEtapes.voirOperations')}</Link>
            </div>
          ) : null}
        </div>
      </Card>
      {CONDUITE.includes(choisie.code) ? <OperationsConduite dossier={dossier} etape={choisie.code} /> : null}
    </div>
  )
}
