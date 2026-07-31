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
          onClick={() => auth.signinRedirect()}
        >
          Sign in
        </button>
      </div>
    </div>
  );
}
