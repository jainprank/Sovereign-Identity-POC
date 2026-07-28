package com.idtrust.credential.issuance;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.credential.schema.ClaimDefinition;
import com.idtrust.credential.schema.ClaimValidation;
import com.idtrust.credential.schema.SchemaDefinition;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates a raw claims map supplied at issuance time against a schema's claim catalogue
 * (FR-ISS-004 - Validate Claims): required-claim presence, primitive type matching per
 * {@code ClaimType}, and any {@link ClaimValidation} rules (pattern/min/max) declared on the
 * claim. Kept as a stateless utility rather than a Spring bean since it has no dependencies.
 */
public final class ClaimValidator {

    private ClaimValidator() {
    }

    public static void validate(SchemaDefinition schema, Map<String, Object> claims) {
        for (ClaimDefinition claim : schema.claims()) {
            Object value = claims.get(claim.name());
            if (value == null) {
                if (claim.required()) {
                    throw new ApiException(ApiErrorCode.VALIDATION_ERROR,
                            "Missing required claim: " + claim.name());
                }
                continue;
            }
            checkType(claim, value);
            checkValidation(claim, value);
        }
    }

    private static void checkType(ClaimDefinition claim, Object value) {
        boolean matches = switch (claim.type()) {
            case STRING, DATE -> value instanceof String;
            case NUMBER -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
        };
        if (!matches) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR,
                    "Claim '" + claim.name() + "' must be of type " + claim.type());
        }
    }

    private static void checkValidation(ClaimDefinition claim, Object value) {
        ClaimValidation rules = claim.validation();
        if (rules == null) {
            return;
        }
        if (value instanceof String stringValue) {
            if (rules.pattern() != null && !Pattern.matches(rules.pattern(), stringValue)) {
                throw new ApiException(ApiErrorCode.VALIDATION_ERROR,
                        "Claim '" + claim.name() + "' does not match required pattern");
            }
            if (rules.minLength() != null && stringValue.length() < rules.minLength()) {
                throw new ApiException(ApiErrorCode.VALIDATION_ERROR,
                        "Claim '" + claim.name() + "' is shorter than minLength " + rules.minLength());
            }
            if (rules.maxLength() != null && stringValue.length() > rules.maxLength()) {
                throw new ApiException(ApiErrorCode.VALIDATION_ERROR,
                        "Claim '" + claim.name() + "' is longer than maxLength " + rules.maxLength());
            }
        } else if (value instanceof Number numberValue) {
            double doubleValue = numberValue.doubleValue();
            if (rules.minValue() != null && doubleValue < rules.minValue()) {
                throw new ApiException(ApiErrorCode.VALIDATION_ERROR,
                        "Claim '" + claim.name() + "' is less than minValue " + rules.minValue());
            }
            if (rules.maxValue() != null && doubleValue > rules.maxValue()) {
                throw new ApiException(ApiErrorCode.VALIDATION_ERROR,
                        "Claim '" + claim.name() + "' is greater than maxValue " + rules.maxValue());
            }
        }
    }
}
