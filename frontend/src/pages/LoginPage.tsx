import { useAuth } from "react-oidc-context";

export function LoginPage() {
  const auth = useAuth();

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white shadow rounded p-8 w-96 text-center">
        <h1 className="text-xl font-semibold mb-4">IDTrust</h1>
        <p className="text-gray-600 mb-6">Sign in with your organization's Keycloak account.</p>
        <button
          className="w-full btn-primary py-2"
          onClick={() => auth.signinRedirect({ state: { returnTo: window.location.pathname + window.location.search } })}
        >
          Sign in
        </button>
        {auth.error && <p className="mt-4 text-xs text-red-600 break-all">{auth.error.message}</p>}
      </div>
    </div>
  );
}
