import { authHandlers } from './authHandlers'
import { serviceHandlers } from './serviceHandlers'
import { taskHandlers } from './taskHandlers'

export const handlers = [...taskHandlers, ...authHandlers, ...serviceHandlers]

// Re-exported so tests can seed/reset the in-memory stores backing the mock
// server without reaching into each handler module individually.
export { resetTasks, seedTask } from './taskHandlers'
export { resetServices, seedService } from './serviceHandlers'
export { resetUsers } from './mockAuth'
