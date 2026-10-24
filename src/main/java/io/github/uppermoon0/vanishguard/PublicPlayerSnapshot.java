package io.github.uppermoon0.vanishguard;

/**
 * A server-thread-produced view for public query handlers, which run on other
 * threads. Never iterate the mutable authoritative player list from Netty or
 * the GameSpy query thread.
 */
public interface PublicPlayerSnapshot {
    int vanishguard$getPublicPlayerCount();

    /** Returns an independent copy; callers cannot modify the published view. */
    String[] vanishguard$getPublicPlayerNames();
}
