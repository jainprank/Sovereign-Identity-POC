# Production Keycloak image for cloud deployment (Render). Builds an optimized distribution
# with the Postgres driver baked in, then bakes the idtrust realm export into the import
# directory. `--import-realm` only imports a realm if one of that name doesn't already exist,
# so this is safe to leave in the entrypoint permanently across redeploys/restarts.

FROM quay.io/keycloak/keycloak:25.0 AS builder
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=false
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:25.0
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY idtrust-realm.json /opt/keycloak/data/import/idtrust-realm.json
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized", "--import-realm"]
