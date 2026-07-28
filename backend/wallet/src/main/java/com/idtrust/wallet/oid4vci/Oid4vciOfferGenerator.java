package com.idtrust.wallet.oid4vci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Builds a real, claimable OID4VCI credential offer (FR-OID4VCI-003/004/005 - Credential Offer, QR
 * Offer, Deep Link Offer), carrying the pre-authorized-code grant (FR-OID4VCI-006) an {@link
 * Oid4vciSession} was created with. Distinct from {@code credential.issuance.
 * CredentialOfferGenerator}'s E07 placeholder - see that class's Javadoc and {@link
 * Oid4vciSession}'s.
 */
public final class Oid4vciOfferGenerator {

    private static final String PRE_AUTHORIZED_CODE_GRANT = "urn:ietf:params:oauth:grant-type:pre-authorized_code";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int QR_SIZE = 300;

    private Oid4vciOfferGenerator() {
    }

    public record Offer(String uri, String qrCodePngBase64) {
    }

    public static Offer generate(String issuerDid, String credentialConfigurationId, String preAuthorizedCode) {
        String uri = buildUri(issuerDid, credentialConfigurationId, preAuthorizedCode);
        return new Offer(uri, renderQrCodePng(uri));
    }

    private static String buildUri(String issuerDid, String credentialConfigurationId, String preAuthorizedCode) {
        try {
            Map<String, Object> offer = Map.of(
                    "credential_issuer", issuerDid,
                    "credential_configuration_ids", List.of(credentialConfigurationId),
                    "grants", Map.of(PRE_AUTHORIZED_CODE_GRANT, Map.of("pre-authorized_code", preAuthorizedCode))
            );
            String encoded = URLEncoder.encode(MAPPER.writeValueAsString(offer), StandardCharsets.UTF_8);
            return "openid-credential-offer://?credential_offer=" + encoded;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build OID4VCI credential offer URI", e);
        }
    }

    private static String renderQrCodePng(String uri) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(uri, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render OID4VCI offer QR code", e);
        }
    }
}
