import { useState } from 'react'
import { useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Button, Card, EmptyState, InlineAlert, KeyValueList, Link, Tag, TextArea } from '@gimac/design-system'
import type { DossierLiaison, TacheActeur } from '../../modele/parcours'
import { useDeposerLivrablePortail } from '../../hooks/parcours'
import { ChampFichier } from '../commun/ChampFichier'
import { CadreAction, useTacheLiaison } from './parcours/cadre'
import { Echeance, PERM, useGeste, useLibelleTache, usePermissionPortail } from './parcours/commun'

// POR-03 Dépôt d'un livrable du participant (portail, D-59) : ses contributions aux ateliers, le document
// de paramétrage d'essai, ses propres pièces. Le geste n'est offert que si le service le permet sur la
// tâche et si le jeton porte HOM_PORTAL_SUBMIT ; MS-DOCUMENT calcule l'empreinte et contrôle le contenu.

type Depot = { operationId: string; fichier: File; commentaire: string; retour: string }

function Formulaire({ tache, retour }: { tache: TacheActeur; retour: string }) {
  const { t } = useTranslation()
  const deposer = useDeposerLivrablePortail()
  const geste = useGeste()
  const libelle = useLibelleTache()
  const [fichier, setFichier] = useState<File | null>(null)
  const [commentaire, setCommentaire] = useState('')
  const [controle, setControle] = useState(false)
  const envoyer = () => {
    setControle(true)
    if (!fichier) return
    geste<Depot>(
      {
        key: 'deposerLivrable', label: 'pp.livrable.deposer', icon: 'upload', variant: 'primary', permission: PERM.deposer, confirm: 'confirm',
        consequences: (x) => (
          <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
            <KeyValueList items={[{ key: t('pp.livrable.fichier'), value: x.fichier.name, mono: true }, ...(x.commentaire ? [{ key: t('pp.livrable.commentaire'), value: x.commentaire }] : [])]} />
            <span>{t('pp.livrable.consequence')}</span>
          </div>
        ),
        effet: async (x, ctx) => {
          await deposer.mutateAsync({ operationId: x.operationId, fichier: x.fichier, commentaire: x.commentaire })
          ctx.navigate(x.retour)
        },
      },
      { operationId: tache.operationId, fichier, commentaire: commentaire.trim(), retour },
      libelle(tache),
    )
  }
  return (
    <form className="gm-col" noValidate onSubmit={(e) => { e.preventDefault(); envoyer() }}>
      <ChampFichier name="livrable" label={t('pp.livrable.fichier')} help={t('pp.livrable.fichierAide')} accept="PDF, PNG, JPEG" maxSize={t('pp.tailleMax')} required
        fichier={fichier} onFichier={setFichier} error={controle && !fichier ? t('pp.livrable.fichierRequis') : undefined} />
      <TextArea name="commentaire" label={t('pp.livrable.commentaire')} help={t('pp.livrable.commentaireAide')} rows={3} maxLength={1000} value={commentaire} onChange={setCommentaire} />
      <div className="gm-row">
        <Button type="submit" variant="primary" size="lg" icon="upload">{t('pp.livrable.deposer')}</Button>
        <Link href={retour}>{t('pp.annulerRetour')}</Link>
      </div>
    </form>
  )
}

function Livrable({ d, operationId }: { d: DossierLiaison; operationId: string }) {
  const { t } = useTranslation()
  const peut = usePermissionPortail(PERM.deposer)
  const libelle = useLibelleTache()
  const { requete, tache } = useTacheLiaison(d.id, (x) => x.operationId === operationId)
  const retour = `/portail/liaisons/${d.id}`
  if (requete.isPending) return null
  if (!tache || !tache.actionsPermises?.includes('DEPOSER_LIVRABLE')) {
    return <EmptyState icon="circle-check" title={t('pp.livrable.plusAttendu')} text={t('pp.livrable.plusAttenduAide')} action={<Link href={retour}>{t('pp.retourSuivi', { reference: d.reference })}</Link>} />
  }
  return (
    <Card title={libelle(tache)}>
      <div className="gm-col">
        <KeyValueList items={[
          { key: t('pp.preuveAttendueLibelle'), value: t(`parcours.preuveAttendue.${tache.rang}`, { defaultValue: '—' }) },
          { key: t('pp.etape'), value: t(`parcours.etape.${tache.etape}`) },
          { key: t('pp.qualite'), value: <Tag>{t(`parcours.qualite.${tache.qualite}`)}</Tag> },
          { key: t('pp.echeance'), value: <Echeance date={tache.echeance} sla={tache.sla} /> },
        ]} />
        {tache.oubliSansSymptome ? <InlineAlert level="warn">{t('parcours.alerte.oubliSansSymptome')}</InlineAlert> : null}
        {peut ? <Formulaire tache={tache} retour={retour} /> : <InlineAlert level="info">{t('pp.droitManquant')}</InlineAlert>}
      </div>
    </Card>
  )
}

export default function DepotPreuvePage() {
  const { t } = useTranslation()
  const { id = '', op = '' } = useParams()
  return <CadreAction id={id} titre={t('pp.livrable.titre')}>{(d) => <Livrable d={d} operationId={op} />}</CadreAction>
}
