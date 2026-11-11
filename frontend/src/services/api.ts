import axios from "axios";
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || "/api/v1",
  withCredentials: true,
});
let csrf: { token: string; headerName: string } | undefined;
export async function refreshCsrf() {
  csrf = (await api.get("/auth/csrf")).data;
}
api.interceptors.request.use(async (config) => {
  if (!["get", "head", "options"].includes(config.method || "get")) {
    if (!csrf) await refreshCsrf();
    config.headers.set(csrf!.headerName, csrf!.token);
  }
  return config;
});
export function errorText(e: unknown) {
  return axios.isAxiosError(e)
    ? e.response?.data?.detail ||
        e.response?.data?.message ||
        (e.response?.status === 401
          ? "The credentials or selected role are incorrect."
          : e.response?.status === 403
            ? "You do not have access to this resource."
            : e.response?.status === 429
              ? "Too many sign-in attempts. Please wait 15 minutes."
              : e.response
                ? "The request could not be completed. Check the fields and try again."
                : "Cannot reach the Java server. Check that it is running on port 8080.")
    : e instanceof Error
      ? e.message
      : "Something went wrong. Please try again.";
}
