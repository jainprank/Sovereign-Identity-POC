package com.idtrust.identity.invitation;

/** FR-ORG-007 (Invite User) / FR-ORG-008 (Accept Invitation) lifecycle. */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED
}
