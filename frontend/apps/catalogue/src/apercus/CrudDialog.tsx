import { useState } from 'react'
import { CrudDialog, Select, TextField } from '@gimac/design-system'
import { Scene } from '../outils/Scene'

// Modale CRUD standard, contexte « modification » : sous-titre de contexte (l'objet concerné), formulaire,
// pied Annuler et action principale ; un formulaire modifié demande confirmation avant l'abandon.
export default function Apercu() {
  const [nom, setNom] = useState('Carine Ebongue Ndjock')
  const [langue, setLangue] = useState('fr')
  return (
    <Scene hauteur={520} titre="Comptes">
      <CrudDialog
        inline
        context="edit"
        title="Modifier la fiche"
        subject="c.ebongue · Carine Ebongue Ndjock"
        description="Données descriptives du compte ; l'identifiant n'est pas modifiable."
        submitLabel="Enregistrer la fiche"
        dirty={nom !== 'Carine Ebongue Ndjock' || langue !== 'fr'}
        onSubmit={() => undefined}
        onClose={() => undefined}
      >
        <div className="gm-form-grid">
          <div className="gm-form-field"><TextField name="nom" label="Nom affiché" value={nom} onChange={setNom} /></div>
          <div className="gm-form-field">
            <Select name="langue" label="Langue" value={langue} onChange={setLangue} options={[{ value: 'fr', label: 'Français' }, { value: 'en', label: 'English' }, { value: 'es', label: 'Español' }]} />
          </div>
        </div>
      </CrudDialog>
    </Scene>
  )
}
