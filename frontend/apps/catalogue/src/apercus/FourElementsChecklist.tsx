import { FourElementsChecklist } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 640 }}>
      <FourElementsChecklist
        elements={{
          owner: "Correspondant technique de l'institution",
          proof: 'rapport-tests-OP-2026-1187.pdf (SHA-256 vérifiée)',
          timestamp: "28/09/2026 11:20, jeton d'horodatage joint",
          validation: false,
        }}
        missing={{ validation: "Aucun validateur distinct du responsable n'a encore signé." }}
        actions={{ validation: 'Demander la validation' }}
      />
    </div>
  )
}
