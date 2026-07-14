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
