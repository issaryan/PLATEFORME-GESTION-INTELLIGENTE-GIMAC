import { configure } from '@testing-library/react'

// Services simulés et chargements paresseux : délai de Testing Library allongé sous la charge.
configure({ asyncUtilTimeout: 5000 })
