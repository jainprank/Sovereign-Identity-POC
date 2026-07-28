package com.idtrust.credential.issuance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a credential offer for a newly issued credential (FR-ISS-009/010/011 - Credential Offer,
 * Generate QR, Deep Link). Uses an {@code openid-credential-offer://} URI scheme as forward-
 * compatible scaffolding ahead of E09's full OID4VCI wallet-claim protocol implementation, which
 * is not yet built; the offer payload here is deliberately minimal (issuer + credential reference
 * only, no grants/pre-authorized-code flow). The offer URI itself doubles as the "deep link"
 * (FR-ISS-011) - the Baseline's own response example uses a single field for both.
 */
public final class CredentialOfferGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int QR_SIZE = 300;

    private CredentialOfferGenerator() {
    }

    public record CredentialOffer(String uri, String qrCodePngBase64) {
    }

    public static CredentialOffer generate(String issuerDid, UUID credentialId) {
        String uri = buildUri(issuerDid, credentialId);
        String qrCodePngBase64 = renderQrCodePng(uri);
        return new CredentialOffer(uri, qrCodePngBase64);
    }

    private static String buildUri(String issuerDid, UUID credentialId) {
        try {
            Map<String, Object> offer = Map.of(
                    "credential_issuer", issuerDid,
                    "credential_id", credentialId.toString()
            );
            String encoded = URLEncoder.encode(MAPPER.writeValueAsString(offer), StandardCharsets.UTF_8);
            return "openid-credential-offer://?credential_offer=" + encoded;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build credential offer URI", e);
        }
    }

    private static String renderQrCodePng(String uri) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(uri, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render credential offer QR code", e);
        }
    }
}
