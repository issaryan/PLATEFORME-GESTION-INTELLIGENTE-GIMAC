import { useState } from 'react'
import { useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Button, Card, EmptyState, InlineAlert, KeyValueList, Link, Note, Select, Tag, TextArea } from '@gimac/design-system'
import type { DossierLiaison, TacheActeur } from '../../modele/parcours'
import { useDeposerLivrablePortail } from '../../hooks/parcours'
import { preuvesDe, type LivrableCR } from '../../modele/preuvesCR'
import { ChampFichier } from '../commun/ChampFichier'
import { CadreAction, useTacheLiaison } from './parcours/cadre'
import { Echeance, PERM, useGeste, useLibelleTache, usePermissionPortail } from './parcours/commun'

// POR-03 Dépôt d'un livrable du participant (portail, D-59) : ses contributions aux ateliers, le document
// de paramétrage d'essai, ses propres pièces. Le geste n'est offert que si le service le permet sur la
// tâche et si le jeton porte HOM_PORTAL_SUBMIT ; MS-DOCUMENT calcule l'empreinte et contrôle le contenu.

type Depot = { operationId: string; fichier: File; commentaire: string; retour: string; livrable?: string; service?: string }

/** D-109 : pièces que le participant dépose sur cette opération (données du référentiel, déposant PARTICIPANT). */
export const livrablesParticipant = (d: DossierLiaison, rang: string): LivrableCR[] =>
  (preuvesDe(d.typeProjet, rang)?.livrables ?? []).filter((l) => l.deposant === 'PARTICIPANT')

function Formulaire({ tache, retour, livrables = [], services = [] }: { tache: TacheActeur; retour: string; livrables?: LivrableCR[]; services?: string[] }) {
  const { t } = useTranslation()
  const deposer = useDeposerLivrablePortail()
  const geste = useGeste()
  const libelle = useLibelleTache()
  const [fichier, setFichier] = useState<File | null>(null)
  const [commentaire, setCommentaire] = useState('')
  const [controle, setControle] = useState(false)
  const [code, setCode] = useState(livrables[0]?.code ?? '')
  const [service, setService] = useState('')
  const vise = livrables.find((l) => l.code === code)
  const envoyer = () => {
    setControle(true)
    if (!fichier || (vise?.grain === 'SERVICE' && !service)) return
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
          await deposer.mutateAsync({ operationId: x.operationId, fichier: x.fichier, commentaire: x.commentaire, livrable: x.livrable, service: x.service })
          ctx.navigate(x.retour)
        },
      },
      { operationId: tache.operationId, fichier, commentaire: commentaire.trim(), retour, ...(vise ? { livrable: vise.code } : {}), ...(vise?.grain === 'SERVICE' ? { service } : {}) },
      libelle(tache),
    )
  }
  return (
    <form className="gm-col" noValidate onSubmit={(e) => { e.preventDefault(); envoyer() }}>
      {livrables.length > 1 ? (
        <Select name="livrableVise" label={t('parcoursOps.livrable.choisir')} required value={code} onChange={(v) => { setCode(v); setService('') }}
          options={livrables.map((l) => ({ value: l.code, label: `${l.code} · ${l.libelle}` }))} />
      ) : null}
      {vise?.consigne ? <Note>{t('preuves.consigne', { consigne: vise.consigne })}</Note> : null}
      {vise?.grain === 'SERVICE' ? (
        <Select name="service" label={t('preuves.service')} required value={service} onChange={setService} help={t('preuves.service.aide')}
          error={controle && !service ? t('preuves.service.requis') : undefined}
          options={[{ value: '', label: t('preuves.service.choisir') }, ...services.map((s) => ({ value: s, label: s }))]} />
      ) : null}
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
          // D-109 : pièces attendues du participant lues dans le référentiel, sinon le libellé historique.
          { key: t('pp.preuveAttendueLibelle'), value: livrablesParticipant(d, tache.rang).map((l) => l.libelle).join(' ; ') || t(`parcours.preuveAttendue.${tache.rang}`, { defaultValue: '-' }) },
          { key: t('pp.etape'), value: t(`parcours.etape.${tache.etape}`) },
          { key: t('pp.qualite'), value: <Tag>{t(`parcours.qualite.${tache.qualite}`)}</Tag> },
          { key: t('pp.echeance'), value: <Echeance date={tache.echeance} sla={tache.sla} /> },
        ]} />
        {tache.oubliSansSymptome ? <InlineAlert level="warn">{t('parcours.alerte.oubliSansSymptome')}</InlineAlert> : null}
        {peut ? <Formulaire tache={tache} retour={retour} livrables={livrablesParticipant(d, tache.rang)} services={d.perimetre.filter((s) => s.inclus).map((s) => s.serviceCode)} /> : <InlineAlert level="info">{t('pp.droitManquant')}</InlineAlert>}
      </div>
    </Card>
  )
}

export default function DepotPreuvePage() {
  const { t } = useTranslation()
  const { id = '', op = '' } = useParams()
  return <CadreAction id={id} titre={t('pp.livrable.titre')}>{(d) => <Livrable d={d} operationId={op} />}</CadreAction>
}
