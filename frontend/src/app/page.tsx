"use client";

import {
  useCallback,
  useEffect,
  useState,
  type FormEvent,
} from "react";

import {
  api,
  asArray,
  clearSession,
  getStoredUser,
  getToken,
  saveSession,
  type AuthUser,
  type Profile,
  type Recommendation,
  type Relationship,
} from "@/lib/api";

type Panel =
  | "discover"
  | "recommendations"
  | "profile"
  | "network"
  | null;

function BrandMark({ size = 34 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 40 40"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M9 28 20 10l11 18M9 28h22M20 10v18"
        stroke="#EAE0CF"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <circle cx="9" cy="28" r="4" fill="#94B4C1" />
      <circle cx="20" cy="10" r="4" fill="#EAE0CF" />
      <circle cx="31" cy="28" r="4" fill="#94B4C1" />
      <circle cx="20" cy="28" r="3" fill="#547792" />
    </svg>
  );
}

function NetworkIllustration() {
  return (
    <div className="network-art">
      <div className="art-topline">
        <span className="art-indicator" />
        PEOPLE, CONNECTED
      </div>

      <svg
        className="network-lines"
        viewBox="0 0 500 500"
        fill="none"
        aria-hidden="true"
      >
        <circle
          cx="250"
          cy="250"
          r="166"
          stroke="#547792"
          strokeOpacity=".36"
        />
        <circle
          cx="250"
          cy="250"
          r="112"
          stroke="#94B4C1"
          strokeOpacity=".3"
          strokeDasharray="3 8"
        />
        <path
          d="M95 183 231 105 391 190 331 368 146 349 95 183Z"
          stroke="#94B4C1"
          strokeWidth="1.5"
        />
        <path
          d="M95 183 331 368M231 105 146 349M95 183 391 190M231 105 250 250 391 190M146 349 250 250 331 368"
          stroke="#547792"
          strokeWidth="1.5"
        />
      </svg>

      <div className="network-person person-one">
        <span className="person-avatar avatar-one">A</span>
        <span className="person-label">Ideas</span>
      </div>

      <div className="network-person person-two">
        <span className="person-avatar avatar-two">Y</span>
        <span className="person-label">You</span>
      </div>

      <div className="network-person person-three">
        <span className="person-avatar avatar-three">C</span>
        <span className="person-label">Creators</span>
      </div>

      <div className="network-person person-four">
        <span className="person-avatar avatar-four">P</span>
        <span className="person-label">Possibilities</span>
      </div>

      <div className="network-center">
        <BrandMark size={28} />
      </div>

      <div className="art-caption">
        <span>Every connection</span>
        <span>starts somewhere.</span>
      </div>
    </div>
  );
}

function relationId(item: Relationship): string {
  return (
    item.id ||
    item.relationshipId ||
    (typeof item._id === "string" ? item._id : "")
  );
}

function otherUserId(
  item: Relationship,
  currentUserId: string
): string {
  if (item.sourceUserId === currentUserId) {
    return item.targetUserId || "";
  }

  if (item.targetUserId === currentUserId) {
    return item.sourceUserId || "";
  }

  return item.sourceUserId || item.targetUserId || "";
}

function memberName(
  userId: string,
  profiles: Record<string, Profile>
): string {
  const person = profiles[userId];

  return (
    person?.displayName ||
    person?.headline ||
    "HappenStance member"
  );
}

export default function Home() {
  const [query, setQuery] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);

  const [authMode, setAuthMode] = useState<
    "login" | "register" | null
  >(null);

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [user, setUser] = useState<AuthUser | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);

  const [results, setResults] = useState<Profile[]>([]);
  const [recommendations, setRecommendations] = useState<
    Recommendation[]
  >([]);

  const [recommendationsLoading, setRecommendationsLoading] =
    useState(false);

  const [connections, setConnections] = useState<Relationship[]>([]);
  const [incoming, setIncoming] = useState<Relationship[]>([]);

  const [memberProfiles, setMemberProfiles] = useState<
    Record<string, Profile>
  >({});

  const [selectedProfile, setSelectedProfile] =
    useState<Profile | null>(null);

  const [profileLoading, setProfileLoading] = useState(false);

  const [panel, setPanel] = useState<Panel>(null);

  const [draft, setDraft] = useState({
    headline: "",
    bio: "",
    location: "",
    industry: "",
    skills: "",
    interests: "",
    visibility: "PUBLIC",
  });

  const authenticated = Boolean(user && getToken());

  const refreshWorkspace = useCallback(async () => {
    try {
      const [p, c, r] = await Promise.all([
        api.me(),
        api.connections(),
        api.incomingRequests(),
      ]);

      const connectionList = asArray(c);
      const incomingList = asArray(r);

      setProfile(p);

      setDraft({
        headline: p.headline || "",
        bio: p.bio || "",
        location: p.location || "",
        industry: p.industry || "",
        skills: (p.skills || []).join(", "),
        interests: (p.interests || []).join(", "),
        visibility: p.visibility || "PUBLIC",
      });

      setConnections(connectionList);
      setIncoming(incomingList);

      const currentUserId = getStoredUser()?.id || "";

      const relatedIds = [
        ...connectionList.map((item) =>
          otherUserId(item, currentUserId)
        ),
        ...incomingList.map((item) => item.sourceUserId || ""),
      ].filter(Boolean);

      if (relatedIds.length) {
        const profiles = await api.publicProfiles(relatedIds);

        setMemberProfiles((previous) => {
          const updated = { ...previous };

          profiles.forEach((person) => {
            updated[person.userId] = person;
          });

          return updated;
        });
      }
    } catch (error) {
      setNotice(
        error instanceof Error
          ? error.message
          : "Unable to load your account."
      );
    }
  }, []);

  useEffect(() => {
    const stored = getStoredUser();

    if (stored && getToken()) {
      setUser(stored);
      void refreshWorkspace();
    }
  }, [refreshWorkspace]);

  async function submitAuth(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();

    setBusy(true);
    setNotice("");

    try {
      const result =
        authMode === "register"
          ? await api.register(
              name.trim(),
              email.trim(),
              password
            )
          : await api.login(email.trim(), password);

      saveSession(result);

      setUser(result.user);
      setAuthMode(null);
      setPassword("");

      setNotice(
        `Welcome${
          result.user.displayName
            ? `, ${result.user.displayName}`
            : ""
        }. You are signed in.`
      );

      await refreshWorkspace();
    } catch (error) {
      setNotice(
        error instanceof Error
          ? error.message
          : "Authentication failed."
      );
    } finally {
      setBusy(false);
    }
  }

  async function searchPeople(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();

    if (!query.trim()) {
      setNotice("Describe who you want to find.");
      return;
    }

    if (!authenticated) {
      setAuthMode("login");
      setNotice("Sign in to discover public profiles.");
      return;
    }

    setBusy(true);
    setPanel("discover");
    setNotice("");

    try {
      const data = await api.discover({
        keyword: query.trim(),
        page: 0,
        size: 12,
      });

      const people = Array.isArray(data)
        ? data
        : data.content || [];

      setResults(people);
    } catch (error) {
      setResults([]);

      setNotice(
        error instanceof Error
          ? error.message
          : "Discovery request failed."
      );
    } finally {
      setBusy(false);
    }
  }

  async function loadRecommendations() {
    if (!authenticated) {
      setAuthMode("login");
      setNotice(
        "Sign in to view personalised recommendations."
      );
      return;
    }

    setPanel("recommendations");
    setRecommendationsLoading(true);
    setNotice("");

    try {
      setRecommendations(await api.recommendations(10));
    } catch (error) {
      setRecommendations([]);

      setNotice(
        error instanceof Error
          ? error.message
          : "Unable to load recommendations."
      );
    } finally {
      setRecommendationsLoading(false);
    }
  }

  async function connect(person: Profile) {
    if (!person.userId) return;

    setBusy(true);

    try {
      await api.sendConnection(person.userId);

      setNotice(
        `Connection request sent to ${
          person.displayName || "this member"
        }.`
      );

      await refreshWorkspace();
    } catch (error) {
      setNotice(
        error instanceof Error
          ? error.message
          : "Unable to send connection request."
      );
    } finally {
      setBusy(false);
    }
  }

  async function saveProfile(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();

    setBusy(true);

    try {
      const updated = await api.updateProfile({
        headline: draft.headline,
        bio: draft.bio,
        location: draft.location,
        industry: draft.industry,
        skills: draft.skills
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean),
        interests: draft.interests
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean),
        visibility: draft.visibility as Profile["visibility"],
      });

      setProfile(updated);
      setNotice("Profile updated successfully.");
    } catch (error) {
      setNotice(
        error instanceof Error
          ? error.message
          : "Profile update failed."
      );
    } finally {
      setBusy(false);
    }
  }

  async function decide(
    item: Relationship,
    accept: boolean
  ) {
    const id = relationId(item);

    if (!id) {
      setNotice(
        "Relationship ID is missing from the API response."
      );
      return;
    }

    setBusy(true);

    try {
      if (accept) {
        await api.acceptConnection(id);
      } else {
        await api.rejectConnection(id);
      }

      await refreshWorkspace();

      setNotice(
        accept ? "Connection accepted." : "Request declined."
      );
    } catch (error) {
      setNotice(
        error instanceof Error
          ? error.message
          : "Unable to update request."
      );
    } finally {
      setBusy(false);
    }
  }

  async function removeConnection(item: Relationship) {
    const id = relationId(item);

    if (!id) {
      setNotice("Connection ID is missing.");
      return;
    }

    const confirmed = window.confirm(
      "Are you sure you want to remove this connection?"
    );

    if (!confirmed) return;

    setBusy(true);

    try {
      await api.removeConnection(id);
      await refreshWorkspace();

      setNotice("Connection removed successfully.");
    } catch (error) {
      setNotice(
        error instanceof Error
          ? error.message
          : "Unable to remove connection."
      );
    } finally {
      setBusy(false);
    }
  }

  async function viewProfile(userId: string) {
    if (!userId) return;

    setProfileLoading(true);
    setSelectedProfile(null);

    try {
      const person = await api.publicProfile(userId);
      setSelectedProfile(person);
    } catch (error) {
      setNotice(
        error instanceof Error
          ? error.message
          : "Unable to load this profile."
      );
    } finally {
      setProfileLoading(false);
    }
  }

  function logout() {
    clearSession();

    setUser(null);
    setProfile(null);
    setResults([]);
    setConnections([]);
    setIncoming([]);
    setRecommendations([]);
    setMemberProfiles({});
    setSelectedProfile(null);
    setPanel(null);

    setNotice("You have signed out.");
  }

  const updateDraft = (
    key: keyof typeof draft,
    value: string
  ) => {
    setDraft((old) => ({
      ...old,
      [key]: value,
    }));
  };

  function renderPersonCard(
    person: Profile,
    showConnect = true
  ) {
    return (
      <article className="person-card" key={person.userId}>
        <div className="person-initial">
          {(person.displayName || person.headline || "P")
            .slice(0, 1)
            .toUpperCase()}
        </div>

        <div className="person-info">
          <strong>
            {person.displayName ||
              person.headline ||
              "HappenStance member"}
          </strong>

          <span>
            {person.headline || person.industry || "Professional profile"}
          </span>

          <small>
            {[person.location, person.industry]
              .filter(Boolean)
              .join(" · ")}
          </small>

          <small>
            {(person.skills || []).slice(0, 5).join(" · ")}
          </small>

          <button
            className="quiet-button"
            onClick={() => void viewProfile(person.userId)}
          >
            View profile ↗
          </button>
        </div>

        {showConnect && (
          <button
            className="outline-button"
            onClick={() => void connect(person)}
            disabled={busy}
          >
            Connect
          </button>
        )}
      </article>
    );
  }

  return (
    <main className="site-shell">
      <header className="navbar">
        <a className="brand" href="#home" aria-label="HappenStance home">
          <span className="brand-icon">
            <BrandMark />
          </span>
          <span className="brand-name">HappenStance</span>
        </a>

        <div className="nav-actions">
          {authenticated ? (
            <>
              <button
                className="nav-text-button"
                onClick={() => void loadRecommendations()}
              >
                For you
              </button>

              <button
                className="nav-text-button"
                onClick={() => setPanel("profile")}
              >
                My profile
              </button>

              <button
                className="nav-text-button"
                onClick={() => setPanel("network")}
              >
                My network{" "}
                <span className="nav-count">
                  {connections.length}
                </span>
              </button>

              <span className="nav-user">
                {user?.displayName}
              </span>

              <button className="nav-cta" onClick={logout}>
                Sign out
              </button>
            </>
          ) : (
            <>
              <button
                className="nav-text-button"
                onClick={() => setAuthMode("login")}
              >
                Log in
              </button>

              <button
                className="nav-cta"
                onClick={() => setAuthMode("register")}
              >
                Get started <span>↗</span>
              </button>
            </>
          )}
        </div>
      </header>

      <section className="hero" id="home">
        <div className="hero-copy">
          <div className="eyebrow">
            <span className="eyebrow-dot" />
            CONNECTIONS, WITH INTENTION
          </div>

          <h1>
            Better things
            <br />
            happen through
            <br />
            <span>people.</span>
          </h1>

          <p className="hero-description">
            Meet the people who move you forward.
          </p>

          <form
            className="search-form"
            id="discover"
            onSubmit={searchPeople}
          >
            <label className="sr-only" htmlFor="people-search">
              Describe the people you want to discover
            </label>

            <svg
              className="search-icon"
              viewBox="0 0 24 24"
              fill="none"
              aria-hidden="true"
            >
              <circle
                cx="10.8"
                cy="10.8"
                r="6.8"
                stroke="currentColor"
                strokeWidth="1.7"
              />
              <path
                d="m16 16 4.2 4.2"
                stroke="currentColor"
                strokeWidth="1.7"
                strokeLinecap="round"
              />
            </svg>

            <input
              id="people-search"
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Who are you looking for?"
              autoComplete="off"
            />

            <button type="submit" aria-label="Search for people" disabled={busy}>
              {busy ? "…" : "↗"}
            </button>
          </form>

          <p className="search-notice" role="status">
            {notice ||
              "Sign in to search public profiles and build meaningful connections."}
          </p>

          {authenticated && (
            <div className="quick-links">
              <button onClick={() => void loadRecommendations()}>
                For you
              </button>

              <button onClick={() => setPanel("discover")}>
                Discovery
              </button>

              <button onClick={() => setPanel("profile")}>
                Edit profile
              </button>

              <button onClick={() => setPanel("network")}>
                Requests ({incoming.length})
              </button>
            </div>
          )}
        </div>

        <div className="hero-visual">
          <NetworkIllustration />
        </div>

        <span className="hero-index" aria-hidden="true">
          HS — 01
        </span>
      </section>

      {authMode && (
        <div
          className="modal-backdrop"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) {
              setAuthMode(null);
            }
          }}
        >
          <section
            className="dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="auth-title"
          >
            <button
              className="dialog-close"
              onClick={() => setAuthMode(null)}
              aria-label="Close"
            >
              ×
            </button>

            <div className="dialog-kicker">
              HAPPENSTANCE ACCOUNT
            </div>

            <h2 id="auth-title">
              {authMode === "login"
                ? "Welcome back."
                : "Create your account."}
            </h2>

            <p className="dialog-subtitle">
              {authMode === "login"
                ? "Sign in to discover people and manage your network."
                : "Start building a more intentional professional network."}
            </p>

            <form className="stack-form" onSubmit={submitAuth}>
              {authMode === "register" && (
                <label>
                  Full name
                  <input
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                    minLength={2}
                    maxLength={80}
                  />
                </label>
              )}

              <label>
                Email address
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                />
              </label>

              <label>
                Password
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={authMode === "register" ? 12 : 1}
                  maxLength={72}
                  autoComplete={
                    authMode === "login"
                      ? "current-password"
                      : "new-password"
                  }
                />

                {authMode === "register" && (
                  <small>At least 12 characters.</small>
                )}
              </label>

              <button
                className="solid-button"
                type="submit"
                disabled={busy}
              >
                {busy
                  ? "Please wait…"
                  : authMode === "login"
                    ? "Sign in"
                    : "Create account"}
              </button>
            </form>

            <p className="switch-auth">
              {authMode === "login"
                ? "New to HappenStance?"
                : "Already registered?"}{" "}
              <button
                onClick={() =>
                  setAuthMode(
                    authMode === "login" ? "register" : "login"
                  )
                }
              >
                {authMode === "login"
                  ? "Create an account"
                  : "Sign in"}
              </button>
            </p>
          </section>
        </div>
      )}

      {panel && (
        <div
          className="modal-backdrop"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) {
              setPanel(null);
            }
          }}
        >
          <section
            className="dialog workspace-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="panel-title"
          >
            <button
              className="dialog-close"
              onClick={() => setPanel(null)}
              aria-label="Close"
            >
              ×
            </button>

            <div className="dialog-kicker">
              YOUR HAPPENSTANCE WORKSPACE
            </div>

            <h2 id="panel-title">
              {panel === "discover"
                ? "People discovery"
                : panel === "recommendations"
                  ? "People for you"
                  : panel === "profile"
                    ? "Your profile"
                    : "Your network"}
            </h2>

            {panel === "recommendations" && (
              <div className="workspace-content">
                <p className="dialog-subtitle">
                  People selected using your profile, shared skills,
                  interests, industry and network context.
                </p>

                {recommendationsLoading ? (
                  <div className="empty-state">
                    Finding relevant people…
                  </div>
                ) : recommendations.length ? (
                  <div className="people-list">
                    {recommendations.map((person) => (
                      <article
                        className="person-card recommendation-card"
                        key={person.userId}
                      >
                        <div className="person-initial">
                          {(person.headline || person.industry || "P")
                            .slice(0, 1)
                            .toUpperCase()}
                        </div>

                        <div className="person-info">
                          <strong>
                            {person.displayName ||
                              person.headline ||
                              "HappenStance member"}
                          </strong>

                          <span>
                            {[person.industry, person.location]
                              .filter(Boolean)
                              .join(" · ") || "Professional profile"}
                          </span>

                          <small>
                            {(person.skills || []).slice(0, 5).join(" · ")}
                          </small>

                          {person.reasons?.length > 0 && (
                            <small className="recommendation-reasons">
                              {person.reasons.join(" · ")}
                            </small>
                          )}
                        </div>

                        <div className="recommendation-actions">
                          <span className="match-score">
                            {person.score}% match
                          </span>

                          <button
                            className="outline-button"
                            onClick={() => void connect(person)}
                            disabled={busy}
                          >
                            Connect
                          </button>
                        </div>
                      </article>
                    ))}
                  </div>
                ) : (
                  <div className="empty-state">
                    No recommendations yet. Complete your profile and
                    grow your network to help us find relevant people.
                  </div>
                )}

                <button
                  className="quiet-button refresh-recommendations"
                  onClick={() => void loadRecommendations()}
                  disabled={recommendationsLoading}
                >
                  Refresh recommendations ↻
                </button>
              </div>
            )}

            {panel === "discover" && (
              <div className="workspace-content">
                <p className="dialog-subtitle">
                  Public profiles matching <strong>{query}</strong>
                </p>

                {results.length ? (
                  <div className="people-list">
                    {results.map((person) => (
                      <div key={person.userId}>
                        {renderPersonCard(person)}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="empty-state">
                    {busy
                      ? "Searching…"
                      : "No matching public profiles. Try a broader search."}
                  </div>
                )}
              </div>
            )}

            {panel === "profile" && (
              <form
                className="stack-form profile-form"
                onSubmit={saveProfile}
              >
                <p className="dialog-subtitle">
                  {profile?.email || user?.email} · Update the details
                  shown on your profile.
                </p>

                <label>
                  Professional headline
                  <input
                    value={draft.headline}
                    onChange={(e) =>
                      updateDraft("headline", e.target.value)
                    }
                  />
                </label>

                <label>
                  About you
                  <textarea
                    rows={3}
                    value={draft.bio}
                    onChange={(e) =>
                      updateDraft("bio", e.target.value)
                    }
                  />
                </label>

                <div className="form-two">
                  <label>
                    Location
                    <input
                      value={draft.location}
                      onChange={(e) =>
                        updateDraft("location", e.target.value)
                      }
                    />
                  </label>

                  <label>
                    Industry
                    <input
                      value={draft.industry}
                      onChange={(e) =>
                        updateDraft("industry", e.target.value)
                      }
                    />
                  </label>
                </div>

                <label>
                  Skills <small>Comma-separated</small>
                  <input
                    value={draft.skills}
                    onChange={(e) =>
                      updateDraft("skills", e.target.value)
                    }
                  />
                </label>

                <label>
                  Interests <small>Comma-separated</small>
                  <input
                    value={draft.interests}
                    onChange={(e) =>
                      updateDraft("interests", e.target.value)
                    }
                  />
                </label>

                <label>
                  Visibility
                  <select
                    value={draft.visibility}
                    onChange={(e) =>
                      updateDraft("visibility", e.target.value)
                    }
                  >
                    <option value="PUBLIC">Public</option>
                    <option value="CONNECTIONS">
                      Connections only
                    </option>
                    <option value="PRIVATE">Private</option>
                  </select>
                </label>

                <button
                  className="solid-button"
                  type="submit"
                  disabled={busy}
                >
                  {busy ? "Saving…" : "Save profile"}
                </button>
              </form>
            )}

            {panel === "network" && (
              <div className="workspace-content">
                <div className="network-section">
                  <h3>
                    Incoming requests <span>{incoming.length}</span>
                  </h3>

                  {incoming.length ? (
                    incoming.map((item, index) => {
                      const senderId = item.sourceUserId || "";
                      const sender = memberProfiles[senderId];

                      return (
                        <article
                          className="person-card"
                          key={relationId(item) || index}
                        >
                          <div className="person-initial">
                            {memberName(senderId, memberProfiles)
                              .slice(0, 1)
                              .toUpperCase()}
                          </div>

                          <div className="person-info">
                            <strong>
                              {memberName(senderId, memberProfiles)}
                            </strong>

                            <span>
                              {sender?.headline ||
                                "Wants to connect with you"}
                            </span>

                            <small>
                              {[sender?.industry, sender?.location]
                                .filter(Boolean)
                                .join(" · ")}
                            </small>

                            <button
                              className="quiet-button"
                              onClick={() => void viewProfile(senderId)}
                            >
                              View profile ↗
                            </button>
                          </div>

                          <button
                            className="outline-button"
                            disabled={busy}
                            onClick={() => void decide(item, true)}
                          >
                            Accept
                          </button>

                          <button
                            className="quiet-button"
                            disabled={busy}
                            onClick={() => void decide(item, false)}
                          >
                            Decline
                          </button>
                        </article>
                      );
                    })
                  ) : (
                    <div className="empty-state compact">
                      No incoming requests.
                    </div>
                  )}
                </div>

                <div className="network-section">
                  <h3>
                    Connections <span>{connections.length}</span>
                  </h3>

                  {connections.length ? (
                    connections.map((item, index) => {
                      const currentUserId = user?.id || "";
                      const connectedUserId = otherUserId(
                        item,
                        currentUserId
                      );

                      const person = memberProfiles[connectedUserId];

                      return (
                        <article
                          className="person-card"
                          key={relationId(item) || index}
                        >
                          <div className="person-initial">
                            {memberName(
                              connectedUserId,
                              memberProfiles
                            )
                              .slice(0, 1)
                              .toUpperCase()}
                          </div>

                          <div className="person-info">
                            <strong>
                              {memberName(
                                connectedUserId,
                                memberProfiles
                              )}
                            </strong>

                            <span>
                              {person?.headline || "Connected"}
                            </span>

                            <small>
                              {[person?.industry, person?.location]
                                .filter(Boolean)
                                .join(" · ")}
                            </small>

                            <button
                              className="quiet-button"
                              onClick={() =>
                                void viewProfile(connectedUserId)
                              }
                            >
                              View profile ↗
                            </button>
                          </div>

                          <button
                            className="quiet-button"
                            disabled={busy}
                            onClick={() => void removeConnection(item)}
                          >
                            Remove connection
                          </button>
                        </article>
                      );
                    })
                  ) : (
                    <div className="empty-state compact">
                      Your accepted connections will appear here.
                    </div>
                  )}
                </div>
              </div>
            )}
          </section>
        </div>
      )}

      {selectedProfile && (
        <div
          className="modal-backdrop"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) {
              setSelectedProfile(null);
            }
          }}
        >
          <section
            className="dialog profile-detail-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="public-profile-title"
          >
            <button
              className="dialog-close"
              onClick={() => setSelectedProfile(null)}
              aria-label="Close"
            >
              ×
            </button>

            <div className="dialog-kicker">
              HAPPENSTANCE MEMBER
            </div>

            <div className="profile-detail-heading">
              <div className="profile-detail-avatar">
                {(selectedProfile.displayName ||
                  selectedProfile.headline ||
                  "P")
                  .slice(0, 1)
                  .toUpperCase()}
              </div>

              <div>
                <h2 id="public-profile-title">
                  {selectedProfile.displayName ||
                    selectedProfile.headline ||
                    "Professional profile"}
                </h2>

                <p className="dialog-subtitle">
                  {selectedProfile.headline || "HappenStance member"}
                </p>
              </div>
            </div>

            {profileLoading ? (
              <div className="empty-state">Loading profile…</div>
            ) : (
              <div className="public-profile-content">
                {selectedProfile.bio && (
                  <div>
                    <h3>About</h3>
                    <p>{selectedProfile.bio}</p>
                  </div>
                )}

                <div>
                  <h3>Professional details</h3>
                  <p>
                    {[selectedProfile.industry, selectedProfile.location]
                      .filter(Boolean)
                      .join(" · ") || "Not specified"}
                  </p>
                </div>

                <div>
                  <h3>Skills</h3>
                  <p>
                    {(selectedProfile.skills || []).join(" · ") ||
                      "No skills added yet."}
                  </p>
                </div>

                <div>
                  <h3>Interests</h3>
                  <p>
                    {(selectedProfile.interests || []).join(" · ") ||
                      "No interests added yet."}
                  </p>
                </div>
              </div>
            )}

            {selectedProfile.userId !== user?.id && (
              <button
                className="solid-button"
                onClick={() => void connect(selectedProfile)}
                disabled={busy}
              >
                Connect
              </button>
            )}
          </section>
        </div>
      )}
    </main>
  );
}