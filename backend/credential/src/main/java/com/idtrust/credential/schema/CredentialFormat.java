package com.idtrust.credential.schema;

/**
 * Credential formats a schema can be issued as (system design principle 8 - "Pluggable credential
 * formats"). Only {@code W3C_VC} is implemented in this pass (E06/E07); {@code SD_JWT_VC} is
 * reserved for E11 (Selective Disclosure), mirroring how {@code KeyProviderType}/{@code DidMethod}
 * reserve enum values for methods/providers not yet built.
 */
public enum CredentialFormat {
    W3C_VC,
    SD_JWT_VC
}
