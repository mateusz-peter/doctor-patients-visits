

Tenants are configured in application.yml in format like:
```yaml
dev.mtpeter.rsq:
  tenants:
    tenantA:
      username: test
      password: test
      url: r2dbc:postgresql://localhost/tenantA
    tenantB:
      username: test
      password: test
      url: r2dbc:postgresql://localhost/tenantB
```

Tenant id ("tenantA", "tenantB"...) needs to be provided with each http request in header `X-TenantID`, otherwise `BadRequest` is returned -- 
there is no default tenant.

## Available endpoints:

Patient endpoints:

- `GET /patients` -- list of all patients in arbitrary order. Always returns status `Ok`.
- `GET /patients/paged` -- patients wrapped in spring `Page`. Always returns `Ok` even if the page is empty.
Patients are sorted by `lastName` and `firstName`
  - queryParams: `page` and `size` -- page number and size respectively. Optional, by default it's `page=0` and `size=10`
- `GET /patients/{id}` -- returns a patient with given `id` with status `Ok`. In case of no patient with that `id`,
returns `NotFound`, if id isn't a `Long` returns `BadRequest`
- `POST /patients` -- creates a new patient. If body is a valid `PatientDTO` always returns `Ok` with a saved `Patient` in body 
- `PUT /patients/{id}` -- updates a patient. In case of no patient with given `id` `NotFound` is returned. If body is a
valid `PatientDTO`, returns updated `Patient` with `Ok` status
- `DELETE /patient/{id}` -- deletes a patient. `NotFound` returned if a patient with given `id` doesn't exist. `NoContent` on success
  - queryParams: `cascade` -- if true, deletes visits before deleting a patient. If false and there exists a visit of that patient,
`Conflict` is returned

Doctor endpoints are like patient endpoints but for doctors:

- `GET /doctors`
- `GET /doctors/paged`
- `GET /doctors/{id}`
- `POST /doctors`
- `PUT /doctors/{id}`
- `DELETE /doctors/{id}` - except deleting a doctor returns `Doctor`'s state before deletion with `Ok` on success

Visit endpoints:

- `GET /visits` -- like for patient and doctor
- `GET /visits/paged` -- paged visits like for patient and doctor. Sorted descending by `visitDate` and `visitTime`
  - queryParams:
    - `page` and `size` - like for others
    - `id` -- `id` of `Patient` to filter by, optional
- `DELETE /visit/{id}` -- cancels a visit, `NoContent` on success, `NotFound` if no visit with such `id`
- `POST /visit/{id}` -- schedules a visit. If there are no colliding visits (with the same `visitDate`, `visitTime` and `doctorId`),
returns saved visit with `Ok`. Otherwise `Conflict`. Also, `BadRequest` is returned if `visitTime` can't be formatted accurately as `HH:mm`
- `PUT /visit/{id}` -- reschedules a visit. Validated the same a the `POST` operation and trying to reschedule a visit with no changes returns `Conflict`
Also, trying to change a patient of the visit returns `BadRequest`

