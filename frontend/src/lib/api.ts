const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080"
).replace(/\/$/, "");

export type AuthUser = {
  id: string;
  email: string;
  displayName: string;
};

export type AuthResponse = {
  tokenType: string;
  accessToken: string;
  expiresInMs: number;
  user: AuthUser;
};

export type Profile = {
  userId: string;
  email?: string;
  displayName?: string;
  headline?: string;
  bio?: string;
  location?: string;
  industry?: string;
  skills?: string[];
  interests?: string[];
  visibility?: "PUBLIC" | "CONNECTIONS" | "PRIVATE";
  updatedAt?: string;
};

export type DiscoveryPage = {
  content?: Profile[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
};

export type Recommendation = Profile & {
  score: number;
  reasons: string[];
};

export type Relationship = {
  id?: string;
  relationshipId?: string;
  sourceUserId?: string;
  targetUserId?: string;
  status?: string;
  type?: string;
  displayName?: string;
  email?: string;
  [key: string]: unknown;
};

const TOKEN_KEY = "happenstance_access_token";
const USER_KEY = "happenstance_user";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;

  return window.localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser(): AuthUser | null {
  if (typeof window === "undefined") return null;

  const raw = window.localStorage.getItem(USER_KEY);

  if (!raw) return null;

  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export function saveSession(data: AuthResponse): void {
  window.localStorage.setItem(TOKEN_KEY, data.accessToken);
  window.localStorage.setItem(USER_KEY, JSON.stringify(data.user));
}

export function clearSession(): void {
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(USER_KEY);
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  authenticated = true
): Promise<T> {
  const headers = new Headers(options.headers);

  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (authenticated) {
    const token = getToken();

    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  let response: Response;

  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
      cache: "no-store",
    });
  } catch {
    throw new Error(
      `Cannot reach HappenStance API at ${API_BASE}. Start the Spring Boot backend and check CORS configuration.`
    );
  }

  const responseText = await response.text();

  let payload: unknown = null;

  if (responseText) {
    try {
      payload = JSON.parse(responseText);
    } catch {
      payload = responseText;
    }
  }

  if (!response.ok) {
    const body = payload as
      | {
          message?: string;
          error?: string;
          detail?: string;
        }
      | string
      | null;

    const message =
      typeof body === "string"
        ? body
        : body?.message || body?.detail || body?.error;

    if (response.status === 401 && authenticated) {
      clearSession();
    }

    throw new Error(message || `Request failed (${response.status})`);
  }

  return payload as T;
}

export const api = {
  // Authentication
  register: (displayName: string, email: string, password: string) =>
    request<AuthResponse>(
      "/api/v1/auth/register",
      {
        method: "POST",
        body: JSON.stringify({ displayName, email, password }),
      },
      false
    ),

  login: (email: string, password: string) =>
    request<AuthResponse>(
      "/api/v1/auth/login",
      {
        method: "POST",
        body: JSON.stringify({ email, password }),
      },
      false
    ),

  // Current user profile
  me: () => request<Profile>("/api/v1/profiles/me"),

  updateProfile: (data: Partial<Profile>) =>
    request<Profile>("/api/v1/profiles/me", {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Profile discovery
  discover: (params: {
    keyword?: string;
    skill?: string;
    industry?: string;
    location?: string;
    page?: number;
    size?: number;
  }) => {
    const query = new URLSearchParams();

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== "") {
        query.set(key, String(value));
      }
    });

    return request<DiscoveryPage | Profile[]>(
      `/api/v1/discovery/profiles?${query.toString()}`
    );
  },

  // Individual public profile
  publicProfile: (userId: string) =>
    request<Profile>(
      `/api/v1/discovery/profiles/${encodeURIComponent(userId)}`
    ),

  // Fetch several public profiles together
  publicProfiles: async (userIds: string[]): Promise<Profile[]> => {
    const uniqueIds = [...new Set(userIds.filter(Boolean))];

    const results = await Promise.allSettled(
      uniqueIds.map((userId) =>
        request<Profile>(
          `/api/v1/discovery/profiles/${encodeURIComponent(userId)}`
        )
      )
    );

    return results
      .filter(
        (result): result is PromiseFulfilledResult<Profile> =>
          result.status === "fulfilled"
      )
      .map((result) => result.value);
  },

  // Recommendations
  recommendations: (limit = 10) =>
    request<Recommendation[]>(
      `/api/v1/recommendations?limit=${Math.min(
        50,
        Math.max(1, limit)
      )}`
    ),

  // Networking
  sendConnection: (targetUserId: string) =>
    request<Relationship>(
      `/api/v1/relationships/${encodeURIComponent(targetUserId)}`,
      { method: "POST" }
    ),

  connections: () =>
    request<Relationship[] | { content?: Relationship[] }>(
      "/api/v1/relationships"
    ),

  incomingRequests: () =>
    request<Relationship[] | { content?: Relationship[] }>(
      "/api/v1/relationships/requests/incoming"
    ),

  acceptConnection: (relationshipId: string) =>
    request<Relationship>(
      `/api/v1/relationships/${encodeURIComponent(
        relationshipId
      )}/accept`,
      { method: "PATCH" }
    ),

  rejectConnection: (relationshipId: string) =>
    request<Relationship>(
      `/api/v1/relationships/${encodeURIComponent(
        relationshipId
      )}/reject`,
      { method: "PATCH" }
    ),

  removeConnection: (relationshipId: string) =>
    request<void>(
      `/api/v1/relationships/${encodeURIComponent(relationshipId)}`,
      { method: "DELETE" }
    ),
};

export function asArray<T>(
  value: T[] | { content?: T[] } | null | undefined
): T[] {
  if (Array.isArray(value)) {
    return value;
  }

  return value?.content ?? [];
}