import { useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useMesLiaisonsPortail } from '../../hooks/parcours'
import { EnTete } from './parcours/commun'
import { TableLiaisons } from './parcours/listes'
import { SuiviProjet } from './parcours/SuiviProjet'

// POR-03 Mes liaisons et suivi de mon projet (portail des participants, D-56 à D-59) : liste des liaisons
// de l'institution du jeton, puis suivi d'une liaison. La projection du portail ne porte aucune donnée
// interne ; les responsables du GIMAC y sont désignés par leur seule fonction.

function Liste() {
  const { t } = useTranslation()
  const q = useMesLiaisonsPortail()
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
      <EnTete titre={t('pp.liaisons.titre')} sousTitre={t('pp.liaisons.aide')} />
      <TableLiaisons liaisons={q.data ?? []} requete={q} />
    </div>
  )
}

export default function MaLiaisonPage() {
  const { id } = useParams()
  return id ? <SuiviProjet id={id} /> : <Liste />
}
