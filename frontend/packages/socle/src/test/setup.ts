import { configure } from '@testing-library/react'

// Requêtes et rendus asynchrones : délai de Testing Library allongé sous la charge de la suite (suites des paquets lancées
// ensemble), comme dans les autres paquets ; une attente qui n'aboutit pas échoue toujours.
configure({ asyncUtilTimeout: 5000 })
