package com.idtrust.credential.statuslist;

/**
 * Mirrors W3C StatusList2021's {@code statusPurpose}. Only REVOCATION is implemented in this pass -
 * see {@code V37__status_lists_core.sql} for why SUSPENSION stays on the existing status column.
 */
public enum StatusListPurpose {
    REVOCATION
}
