import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, DataTable, Dialog, InlineAlert, KeyValueList, Tag, TextArea } from '@gimac/design-system'
import type { Liaison, ModificationPerimetre, PerimetreProjet } from '../../../modele/homologation'
import { useHistoriquePerimetre, useModifierPerimetre, usePerimetreLiaison } from '../../../hooks/liaisons'
import { Donnees, useErreurService } from './communs'
import { decrire, grouper } from '../../../dictionnaires/services-perimetre'
import { ResumeChangements, SelecteurServices } from './selecteur-services'

// Périmètre du projet de certification (D-133) dans l'onglet « Périmètre » de la liaison : services inclus et hors
// périmètre, version et gestionnaire ; modification (services souscrits proposés, motif obligatoire) offerte au seul
// compte que MS-HOMOLOGATION déclare habilité (gestionnaire, super administrateur, chargé de l'implémentation SNA) ;
// historique paginé (auteur, permission utilisée, avant, après, motif). Le service décide, l'écran affiche son refus.

const TAILLE = 10

function FormulairePerimetre({ liaison, perimetre, onFermer }: { liaison: Liaison; perimetre: PerimetreProjet; onFermer: () => void }) {
  const { t } = useTranslation()
  const modifier = useModifierPerimetre()
  const erreur = useErreurService()
  const avant = perimetre.services.filter((s) => s.inclus && s.code !== 'TRANSVERSE').map((s) => s.code)
  const [services, setServices] = useState(avant)
  const [motif, setMotif] = useState('')
  const [manque, setManque] = useState<string | undefined>()
  const inchange = avant.length === services.length && avant.every((c) => services.includes(c))
  const valider = () => {
    if (!motif.trim()) return setManque(t('perimetreProjet.motif.requis'))
    setManque(undefined)
    modifier.mutate({ id: liaison.id, services, motif: motif.trim() }, { onSuccess: onFermer })
  }
  const serveur = erreur(modifier.error)
  return (
    <Dialog
      title={t('perimetreProjet.form.titre')}
      subtitle={liaison.intituleProjet ?? liaison.institutionNom}
      size="lg"
      onClose={onFermer}
      footer={
        <>
          <Button variant="secondary" onClick={onFermer}>{t('perimetreProjet.annuler')}</Button>
          <Button onClick={valider} disabled={modifier.isPending || inchange}>{t('perimetreProjet.enregistrer')}</Button>
        </>
      }
    >
      <div className="gm-col">
        <InlineAlert level="info">{t('perimetreProjet.form.aide')}</InlineAlert>
        <SelecteurServices proposables={perimetre.proposables} valeur={services} onChange={setServices} typeProjet={liaison.typeProjet} />
        <ResumeChangements avant={avant} apres={services} typeProjet={liaison.typeProjet} />
        <TextArea label={t('perimetreProjet.motif')} value={motif} onChange={setMotif} required maxLength={500} error={manque} help={t('perimetreProjet.motif.aide')} />
        {serveur ? <InlineAlert level="crit" title={serveur.message} correlationId={serveur.correlationId} /> : null}
      </div>
    </Dialog>
  )
}

function Historique({ liaison }: { liaison: Liaison }) {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)
  const historique = useHistoriquePerimetre(liaison.id, page, TAILLE)
  const lignes = historique.data?.contenu ?? []
  const { i18n } = useTranslation()
  const liste = (c: string[]) => c.filter((x) => x !== 'TRANSVERSE').map((x) => decrire(x, liaison.typeProjet, i18n.language).libelle).join(', ')
  return (
    <Card title={t('perimetreProjet.historique')}>
      <DataTable<ModificationPerimetre>
        rowKey={(r) => String(r.version)}
        rows={lignes}
        total={historique.data?.total ?? 0}
        page={page}
        pageSize={TAILLE}
        onPage={setPage}
        state={historique.isLoading ? 'loading' : historique.error ? 'error' : lignes.length === 0 ? 'empty' : 'ok'}
        emptyText={t('perimetreProjet.historique.vide')}
        columns={[
          { key: 'version', header: t('perimetreProjet.version'), type: 'number' },
          { key: 'modifieLe', header: t('perimetreProjet.date'), type: 'datetime' },
          { key: 'auteurNom', header: t('perimetreProjet.auteur'), render: (r) => r.auteurNom ?? r.auteurId },
          { key: 'permissionUtilisee', header: t('perimetreProjet.role'), render: (r) => t(`droits.permission.${r.permissionUtilisee}`, { defaultValue: r.permissionUtilisee }) },
          { key: 'avant', header: t('perimetreProjet.avant'), render: (r) => liste(r.avant) },
          { key: 'apres', header: t('perimetreProjet.apres'), render: (r) => liste(r.apres) },
          { key: 'motif', header: t('perimetreProjet.motif') },
        ]}
      />
    </Card>
  )
}

/** Services du périmètre rangés par interface et par famille, nom usuel en clair, code en infobulle. */
function ServicesGroupes({ perimetre, typeProjet }: { perimetre: PerimetreProjet; typeProjet: string }) {
  const { t, i18n } = useTranslation()
  const inclus = new Set(perimetre.services.filter((s) => s.inclus).map((s) => s.code))
  return (
    <div className="gm-col">
      {grouper(perimetre.services.map((s) => s.code), typeProjet, i18n.language).flatMap((g) =>
        g.familles.map((f) => (
          <div key={`${g.code}-${f.code}`} className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
            <span className="gm-field-label">{`${g.libelle} · ${f.libelle}`}</span>
            <span className="gm-row">
              {f.services.map((s) => (
                <span key={s.code} title={t('perimetreProjet.code', { code: s.code })}>
                  <Tag>{s.libelle + (inclus.has(s.code) ? '' : ` (${t('perimetreProjet.hors')})`)}</Tag>
                </span>
              ))}
            </span>
          </div>
        )),
      )}
    </div>
  )
}

export function GestionPerimetre({ liaison }: { liaison: Liaison }) {
  const { t } = useTranslation()
  const perimetre = usePerimetreLiaison(liaison.id)
  const [ouvert, setOuvert] = useState(false)
  return (
    <Donnees requete={perimetre} vide={t('perimetre.vide')}>
      {(p) => (
        <div className="gm-col">
          <Card
            title={t('perimetreProjet.titre')}
            actions={p.modifiable ? <Button icon="pen-line" onClick={() => setOuvert(true)}>{t('perimetreProjet.modifier')}</Button> : undefined}
          >
            <KeyValueList
              items={[
                { key: t('perimetreProjet.version'), value: String(p.version) },
                { key: t('perimetreProjet.gestionnaire'), value: p.gestionnaireNom ?? t('perimetreProjet.gestionnaire.aucun') },
                {
                  key: t('perimetreProjet.services'),
                  value: <ServicesGroupes perimetre={p} typeProjet={liaison.typeProjet} />,
                },
              ]}
            />
            {p.modifiable ? null : <InlineAlert level="info">{t('perimetreProjet.lectureSeule')}</InlineAlert>}
          </Card>
          <Historique liaison={liaison} />
          {ouvert ? <FormulairePerimetre liaison={liaison} perimetre={p} onFermer={() => setOuvert(false)} /> : null}
        </div>
      )}
    </Donnees>
  )
}
