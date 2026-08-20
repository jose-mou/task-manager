import { describe, expect, it } from 'vitest'
import { changeOwnPassword } from './usersClient'
import { ApiUnauthorizedError, ApiValidationError } from './errors'
import { loginAs } from '../test/authHelpers'

describe('users API client', () => {
  it('throws ApiUnauthorizedError when there is no session', async () => {
    await expect(
      changeOwnPassword({ currentPassword: 'admin', newPassword: 'sup3r-s3cret-pass' }),
    ).rejects.toBeInstanceOf(ApiUnauthorizedError)
  })

  it('changes the password for the authenticated user', async () => {
    loginAs('ADMIN', 'admin')

    await expect(
      changeOwnPassword({ currentPassword: 'admin', newPassword: 'sup3r-s3cret-pass' }),
    ).resolves.toBeUndefined()
  })

  it('throws ApiValidationError naming currentPassword when it does not match', async () => {
    loginAs('ADMIN', 'admin')

    try {
      await changeOwnPassword({ currentPassword: 'wrong', newPassword: 'sup3r-s3cret-pass' })
      expect.unreachable('changeOwnPassword should have thrown')
    } catch (error) {
      expect(error).toBeInstanceOf(ApiValidationError)
      const fields = (error as ApiValidationError).fieldErrors.map((e) => e.field)
      expect(fields).toContain('currentPassword')
    }
  })

  it('throws ApiValidationError naming newPassword when it is too short', async () => {
    loginAs('ADMIN', 'admin')

    try {
      await changeOwnPassword({ currentPassword: 'admin', newPassword: 'short' })
      expect.unreachable('changeOwnPassword should have thrown')
    } catch (error) {
      expect(error).toBeInstanceOf(ApiValidationError)
      const fields = (error as ApiValidationError).fieldErrors.map((e) => e.field)
      expect(fields).toContain('newPassword')
    }
  })
})
