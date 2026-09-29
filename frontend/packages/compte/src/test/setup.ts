import { configure } from '@testing-library/react'

// Services simulés et requêtes asynchrones : délai de Testing Library allongé sous la charge de la suite.
configure({ asyncUtilTimeout: 5000 })
