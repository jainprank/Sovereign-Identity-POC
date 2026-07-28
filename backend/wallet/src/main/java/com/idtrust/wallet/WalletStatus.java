package com.idtrust.wallet;

/**
 * Lifecycle status of a {@link Wallet}. Only {@code ACTIVE} is reachable in this pass;
 * {@code LOCKED} is reserved for FR-WAL-020/021 (Wallet Lock/Unlock, V1-priority), mirroring how
 * {@code CredentialStatus} reserved values ahead of the epic that implements them.
 */
public enum WalletStatus {
    ACTIVE,
    LOCKED
}
