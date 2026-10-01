import ComptesPage from './ComptesPage'

// /console/comptes/nouveau (D-62) : la liste des comptes, fenêtre modale de création ouverte. Aucune
// inscription libre ; la création reste réservée à IAM_ACCOUNT_CREATE (route gardée dans App.tsx).
export default function CreationComptePage() {
  return <ComptesPage creation />
}
