package com.idtrust.did.provider.didkey;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.did.Did;
import com.idtrust.did.provider.DidCreationRequest;
import com.idtrust.did.provider.DidMethod;
import com.idtrust.did.provider.DidMethodProvider;
import com.idtrust.did.provider.DidProviderResult;
import com.idtrust.did.provider.Ed25519DidDocuments;
import com.idtrust.did.provider.Ed25519Multibase;
import com.idtrust.kms.crypto.KeyAlgorithm;
import com.idtrust.kms.key.KeyService;
import com.idtrust.kms.key.KeyType;
import com.idtrust.kms.key.ManagedKey;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * did:key (FR-DID-*): the identifier is computed directly from an Ed25519 public key, using the
 * multicodec prefix {@code 0xed 0x01} followed by the raw 32-byte public key, base58btc-encoded
 * with a {@code z} multibase prefix - {@code did:key:z...}. Because this is a pure function of
 * the key, resolution never requires stored state and works for any did:key, not only ones this
 * platform created.
 */
@Component
public class DidKeyProvider implements DidMethodProvider {

    private final KeyService keyService;

    public DidKeyProvider(KeyService keyService) {
        this.keyService = keyService;
    }

    @Override
    public DidMethod method() {
        return DidMethod.KEY;
    }

    @Override
    public DidProviderResult create(DidCreationRequest request) {
        ManagedKey key = keyService.generate(request.tenantId(), KeyType.SIGNING, KeyAlgorithm.ED25519, null);
        keyService.linkToDid(key.getId(), request.didId());

        String multibase = Ed25519Multibase.encode(key.getPublicKey());
        String did = "did:key:" + multibase;
        Map<String, Object> document = Ed25519DidDocuments.build(did, multibase);
        return new DidProviderResult(did, document, key.getId());
    }

    @Override
    public Map<String, Object> resolve(String did) {
        String multibase = stripPrefix(did);
        return Ed25519DidDocuments.build(did, multibase);
    }

    @Override
    public DidProviderResult update(Did did) {
        throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                "did:key cannot rotate its key without changing the DID identifier; create a new DID instead");
    }

    @Override
    public void deactivate(Did did) {
        // No-op: did:key resolution is pure computation from the identifier, so there is no
        // external document to un-publish. Deactivation is enforced locally via Did.status,
        // which stops the platform from using this DID for new signing/issuance operations.
    }

    private String stripPrefix(String did) {
        String prefix = "did:key:";
        if (!did.startsWith(prefix)) {
            throw new ApiException(ApiErrorCode.INVALID_DID, "Not a did:key identifier: " + did);
        }
        return did.substring(prefix.length());
    }
}
