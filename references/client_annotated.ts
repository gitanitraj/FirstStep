/* IM-05 annotated mirror.
 * What: controlled help-only Category/Topic browsing and its resident contract.
 * Why: consume IM-01/03/04 eligibility without duplicating classification or identity.
 * Interaction: shared helpFor, lifecycle-filtered services, HTTP DTOs and React navigation.
 * Alternatives: rejected all-content Topic counts, capped inventory and inferred Flyer activation.
 */
interface ApiResponse<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  errorMessage: string | null;
}

/** Preserve the server error code so browse pages distinguish invalid routes from outages. */
export class ApiError extends Error {
  constructor(public readonly code: string | null, message: string) { super(message); }
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(path);
  const body = (await res.json()) as ApiResponse<T>;
  if (!body.success) {
    throw new ApiError(body.errorCode, body.errorMessage ?? 'Request failed');
  }
  return body.data;
}

export async function apiPost<TReq, TRes>(path: string, payload: TReq): Promise<TRes> {
  const res = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  const body = (await res.json()) as ApiResponse<TRes>;
  if (!body.success) {
    throw new ApiError(body.errorCode, body.errorMessage ?? 'Request failed');
  }
  return body.data;
}
