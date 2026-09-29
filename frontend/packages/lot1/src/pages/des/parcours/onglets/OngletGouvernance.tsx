import { useTranslation } from 'react-i18next'
import { Card, DataTable, KeyValueList, type Column } from '@gimac/design-system'
import type { DossierLiaison, EtapeCode, OperationParcours, Personne, Qualite } from '../../../../modele/parcours'
import { useOperationsParcours } from '../../../../hooks/parcours'
import { Donnees } from '../../liaisons/communs'
import { Valeur } from '../communs'

// Onglet « Gouvernance » (CR §3.6) : intervenants de chaque étape selon leur qualité (R responsable,
// V validateur, C consulté, I informé, E externe), tels que les services les attribuent aux opérations ;
// interlocuteurs du participant et tiers (banque partenaire, personnalisateur).

type LigneRaci = { etape: EtapeCode } & Record<Qualite, Personne[]>
const QUALITES: Qualite[] = ['R', 'V', 'C', 'I', 'E']

/** Regroupe, par étape, les intervenants distincts que servent les opérations (simple regroupement d'affichage). */
function parEtape(ops: OperationParcours[]): LigneRaci[] {
  const lignes = new Map<EtapeCode, LigneRaci>()
  const ajouter = (l: Personne[], p: Personne | null) => {
    if (p && !l.some((x) => x.id === p.id)) l.push(p)
  }
  ops.forEach((o) => {
    const l = lignes.get(o.etape) ?? { etape: o.etape, R: [], V: [], C: [], I: [], E: [] }
    ajouter(l.R, o.raci.R)
    ajouter(l.V, o.raci.V)
    o.raci.C.forEach((p) => ajouter(l.C, p))
    o.raci.I.forEach((p) => ajouter(l.I, p))
    o.raci.E.forEach((p) => ajouter(l.E, p))
    lignes.set(o.etape, l)
  })
  return [...lignes.values()]
}

function Personnes({ l }: { l: Personne[] }) {
  const { t } = useTranslation()
  if (!l.length) return <span className="gm-field-help">{t('parcoursGouvernance.aucun')}</span>
  return (
    <ul className="gm-col" style={{ gap: 0, listStyle: 'none', margin: 0, padding: 0 }}>
      {l.map((p) => (
        <li key={p.id} title={t(`parcours.fonction.${p.fonction}`)}>{p.nom}</li>
      ))}
    </ul>
  )
}

export function OngletGouvernance({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const ops = useOperationsParcours(dossier.id)
  const colonnes: Column<LigneRaci>[] = [
    { key: 'etape', header: t('parcoursEtapes.etape'), render: (l) => t(`parcours.etape.${l.etape}`) },
    ...QUALITES.map((q) => ({ key: q, header: `${q} · ${t(`parcours.qualite.${q}`)}`, render: (l: LigneRaci) => <Personnes l={l[q]} /> })),
  ]
  return (
    <div className="gm-col">
      <div className="gm-row" style={{ alignItems: 'stretch', gap: 'var(--gm-space-4)' }}>
        <div style={{ flex: '1 1 300px', minWidth: 0 }}>
          <Card title={t('parcoursGouvernance.gimac')}>
            <KeyValueList
              items={[
                { key: t('parcoursDossier.conducteur'), value: <Valeur>{dossier.conducteur?.nom}</Valeur> },
                { key: t('parcoursDossier.responsableFonctionnel'), value: <Valeur>{dossier.responsableFonctionnel?.nom}</Valeur> },
                { key: t('parcoursDossier.responsableTechnique'), value: <Valeur>{dossier.responsableTechnique?.nom}</Valeur> },
              ]}
            />
          </Card>
        </div>
        <div style={{ flex: '1 1 300px', minWidth: 0 }}>
          <Card title={t('parcoursGouvernance.participant')}>
            {dossier.interlocuteursParticipant.length ? (
              <KeyValueList items={dossier.interlocuteursParticipant.map((i) => ({ key: i.fonction, value: i.nom }))} />
            ) : (
              <span className="gm-field-help">{t('parcoursGouvernance.aucunInterlocuteur')}</span>
            )}
          </Card>
        </div>
        <div style={{ flex: '1 1 300px', minWidth: 0 }}>
          <Card title={t('parcoursGouvernance.tiers')}>
            <KeyValueList
              items={[
                { key: t('parcours.fonction.BANQUE_PARTENAIRE'), value: dossier.banquePartenaireNom ?? <span className="gm-field-help">{t('parcoursDossier.sansBanque')}</span> },
                { key: t('parcours.fonction.PERSONNALISATEUR'), value: dossier.personnalisateurNom ?? <span className="gm-field-help">{t('parcoursGouvernance.sansPersonnalisateur')}</span> },
              ]}
            />
          </Card>
        </div>
      </div>
      <Card title={t('parcoursGouvernance.raci')}>
        <div className="gm-col">
          <span className="gm-field-help">{t('parcoursGouvernance.raci.aide')}</span>
          <Donnees requete={ops} estVide={(l) => l.length === 0} vide={t('parcoursEtapes.aucuneOperation')}>
            {(l) => <DataTable columns={colonnes} rows={parEtape(l)} rowKey={(x) => x.etape} density="compact" />}
          </Donnees>
        </div>
      </Card>
    </div>
  )
}
