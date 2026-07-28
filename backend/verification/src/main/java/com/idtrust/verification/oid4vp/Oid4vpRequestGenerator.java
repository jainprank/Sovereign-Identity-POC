package com.idtrust.verification.oid4vp;

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

/**
 * Builds a real, presentable OID4VP request (FR-OID4VP-001 - Presentation Request, FR-VER-001/002
 * - Create/QR Verification Request), carrying the {@code client_id}, {@code nonce} and
 * {@code presentation_definition} a
 * {@code com.idtrust.verification.VerificationRequest} was created with. Mirrors
 * {@code wallet.oid4vci.Oid4vciOfferGenerator}'s shape (same ZXing QR rendering, same
 * "no base-URL config exists anywhere in this codebase" limitation - see that class's Javadoc and
 * this module's README section for the resulting {@code response_uri} caveat).
 */
public final class Oid4vpRequestGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int QR_SIZE = 300;

    private Oid4vpRequestGenerator() {
    }

    public record Request(String uri, String qrCodePngBase64) {
    }

    public static Request generate(String clientId, String requestId, String nonce,
                                     Map<String, Object> presentationDefinition) {
        String uri = buildUri(clientId, requestId, nonce, presentationDefinition);
        return new Request(uri, renderQrCodePng(uri));
    }

    private static String buildUri(String clientId, String requestId, String nonce,
                                     Map<String, Object> presentationDefinition) {
        try {
            Map<String, Object> request = Map.of(
                    "client_id", clientId,
                    "response_uri", "/api/v1/verification-requests/" + requestId + "/present",
                    "response_type", "vp_token",
                    "response_mode", "direct_post",
                    "nonce", nonce,
                    "presentation_definition", presentationDefinition
            );
            String encoded = URLEncoder.encode(MAPPER.writeValueAsString(request), StandardCharsets.UTF_8);
            return "openid4vp://?request=" + encoded;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build OID4VP request URI", e);
        }
    }

    private static String renderQrCodePng(String uri) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(uri, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render OID4VP request QR code", e);
        }
    }
}
