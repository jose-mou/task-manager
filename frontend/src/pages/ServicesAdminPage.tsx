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

  const [renamingId, setRenamingId] = useState<string | null>(null)
  const [renameValue, setRenameValue] = useState('')

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
    }
  }

  function startRename(service: Service) {
    setRenamingId(service.id)
    setRenameValue(service.name)
    setActionError(null)
  }

  async function confirmRename(id: string) {
    try {
      await renameService(id, { name: renameValue })
      setRenamingId(null)
      loadServices()
    } catch (error) {
      if (!handleAuthError(error)) setActionError('Could not rename the service.')
    }
  }

  async function handleDelete(id: string) {
    try {
      await deleteService(id)
      loadServices()
    } catch (error) {
      if (!handleAuthError(error)) setActionError('Could not delete the service.')
    }
  }

  async function handleRotate(id: string) {
    try {
      const result = await rotateServiceCredentials(id)
      setCredentials(result)
    } catch (error) {
      if (!handleAuthError(error)) setActionError('Could not rotate the credentials.')
    }
  }

  return (
    <section>
      <h1>Services</h1>

      <form onSubmit={handleRegister} className="form-field">
        <label htmlFor="new-service-name">New service name</label>
        <input
          id="new-service-name"
          required
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
        />
        <button type="submit">Register service</button>
        {registerError && <p role="alert">{registerError}</p>}
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
                      <button type="button" onClick={() => confirmRename(service.id)}>
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
                      <button type="button" onClick={() => handleRotate(service.id)}>
                        Rotate credentials
                      </button>
                      <button type="button" onClick={() => handleDelete(service.id)}>
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
