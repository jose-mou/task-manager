import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  deleteService,
  listServices,
  registerService,
  renameService,
  rotateServiceCredentials,
} from '../api/servicesClient'
import {
  ApiForbiddenError,
  ApiUnauthorizedError,
  ApiValidationError,
} from '../api/errors'
import type { Service, ServiceCredentials } from '../api/types'
import { FormField } from '../components/molecules/FormField'
import { Input } from '../components/atoms/Input'
import { CredentialDialog } from '../components/organisms/CredentialDialog'
import { useAuth } from '../auth/AuthContext'

export function ServicesAdminPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const [services, setServices] = useState<Service[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const [newName, setNewName] = useState('')
  const [registerError, setRegisterError] = useState<string | null>(null)
  const [registering, setRegistering] = useState(false)

  const [renamingId, setRenamingId] = useState<string | null>(null)
  const [renameValue, setRenameValue] = useState('')
  // Id of the service whose row action is in flight; its buttons stay disabled
  // until it settles, so a rotation cannot be fired twice by a double click.
  const [pendingId, setPendingId] = useState<string | null>(null)

  // The one-time credentials of the last register/rotate call. Local state
  // only: there is nowhere this is persisted, so it is gone for good once the
  // dialog is closed or the page is left.
  const [credentials, setCredentials] = useState<ServiceCredentials | null>(null)

  const handleAuthError = useCallback(
    (error: unknown): boolean => {
      if (error instanceof ApiUnauthorizedError || error instanceof ApiForbiddenError) {
        logout()
        navigate('/login')
        return true
      }
      return false
    },
    [logout, navigate],
  )

  const loadServices = useCallback(() => {
    listServices()
      .then((result) => setServices(result))
      .catch((error) => {
        if (!handleAuthError(error)) setLoadError('Could not load the services.')
      })
  }, [handleAuthError])

  useEffect(() => {
    loadServices()
  }, [loadServices])

  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setRegisterError(null)
    setRegistering(true)
    try {
      const result = await registerService({ name: newName })
      setCredentials(result)
      setNewName('')
      loadServices()
    } catch (error) {
      if (handleAuthError(error)) return
      if (error instanceof ApiValidationError) {
        setRegisterError(error.fieldErrors[0]?.message ?? 'Invalid service name.')
      } else {
        setRegisterError('Could not register the service.')
      }
    } finally {
      setRegistering(false)
    }
  }

  function startRename(service: Service) {
    setRenamingId(service.id)
    setRenameValue(service.name)
    setActionError(null)
  }

  async function runRowAction(id: string, action: () => Promise<void>, failure: string) {
    setActionError(null)
    setPendingId(id)
    try {
      await action()
    } catch (error) {
      if (!handleAuthError(error)) setActionError(failure)
    } finally {
      setPendingId(null)
    }
  }

  function confirmRename(id: string) {
    return runRowAction(
      id,
      async () => {
        await renameService(id, { name: renameValue })
        setRenamingId(null)
        loadServices()
      },
      'Could not rename the service.',
    )
  }

  function handleDelete(id: string) {
    return runRowAction(
      id,
      async () => {
        await deleteService(id)
        loadServices()
      },
      'Could not delete the service.',
    )
  }

  function handleRotate(id: string) {
    return runRowAction(
      id,
      async () => {
        setCredentials(await rotateServiceCredentials(id))
      },
      'Could not rotate the credentials.',
    )
  }

  return (
    <section>
      <h1>Services</h1>

      {/* `noValidate` like every other form of the app: the API contract is the
          single source of validation truth and its 400 payload is what the
          user sees. */}
      <form onSubmit={handleRegister} noValidate>
        <FormField
          htmlFor="new-service-name"
          label="New service name"
          error={registerError ?? undefined}
        >
          {(control) => (
            <Input
              {...control}
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
            />
          )}
        </FormField>
        <button type="submit" disabled={registering}>
          Register service
        </button>
      </form>

      {loadError && <p role="alert">{loadError}</p>}
      {actionError && <p role="alert">{actionError}</p>}

      {services === null && !loadError && <p>Loading services…</p>}

      {services !== null && services.length === 0 && <p>No services registered yet.</p>}

      {services !== null && services.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {services.map((service) => (
              <tr key={service.id}>
                <td>
                  {renamingId === service.id ? (
                    <input
                      aria-label={`Rename ${service.name}`}
                      value={renameValue}
                      onChange={(e) => setRenameValue(e.target.value)}
                    />
                  ) : (
                    service.name
                  )}
                </td>
                <td>
                  {renamingId === service.id ? (
                    <>
                      <button
                        type="button"
                        disabled={pendingId === service.id}
                        onClick={() => confirmRename(service.id)}
                      >
                        Save
                      </button>
                      <button type="button" onClick={() => setRenamingId(null)}>
                        Cancel
                      </button>
                    </>
                  ) : (
                    <>
                      <button type="button" onClick={() => startRename(service)}>
                        Rename
                      </button>
                      <button
                        type="button"
                        disabled={pendingId === service.id}
                        onClick={() => handleRotate(service.id)}
                      >
                        Rotate credentials
                      </button>
                      <button
                        type="button"
                        disabled={pendingId === service.id}
                        onClick={() => handleDelete(service.id)}
                      >
                        Delete
                      </button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {credentials && (
        <CredentialDialog credentials={credentials} onClose={() => setCredentials(null)} />
      )}
    </section>
  )
}
