interface ApiResponse<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  errorMessage: string | null;
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(path);
  const body = (await res.json()) as ApiResponse<T>;
  if (!body.success) {
    throw new Error(body.errorMessage ?? 'Request failed');
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
    throw new Error(body.errorMessage ?? 'Request failed');
  }
  return body.data;
}
