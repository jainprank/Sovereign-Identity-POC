package com.idtrust.kms.key;

/**
 * Purpose of a managed key. {@code SIGNING} keys back DID verification methods and credential
 * issuance signatures; {@code ENCRYPTION} is reserved for future key-agreement/encryption use
 * cases (e.g. encrypted wallet payloads) not yet implemented by any epic.
 */
public enum KeyType {
    SIGNING,
    ENCRYPTION
}
