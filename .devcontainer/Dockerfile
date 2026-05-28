FROM yugabytedb/yugabyte:2025.2.3.0-b149 AS yugabyte-db

FROM eclipse-temurin:25-jdk-noble

ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=$USER_UID
ARG YB_VERSION=2025.2.3.0
ARG YB_BUILD=b149
ARG TARGETARCH

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bash \
        ca-certificates \
        curl \
        git \
        maven \
        openssh-client \
        sudo \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN if ! getent group "$USER_GID" >/dev/null; then groupadd --gid "$USER_GID" "$USERNAME"; fi \
    && if id -u "$USERNAME" >/dev/null 2>&1; then \
        usermod --shell /bin/bash "$USERNAME"; \
      elif getent passwd "$USER_UID" >/dev/null; then \
        existing_user="$(getent passwd "$USER_UID" | cut -d: -f1)" \
        && usermod --login "$USERNAME" --move-home --home "/home/$USERNAME" --shell /bin/bash "$existing_user"; \
      else \
        useradd --uid "$USER_UID" --gid "$USER_GID" -m "$USERNAME" --shell /bin/bash; \
      fi \
    && echo "$USERNAME ALL=(root) NOPASSWD:ALL" > "/etc/sudoers.d/$USERNAME" \
    && chmod 0440 "/etc/sudoers.d/$USERNAME"

RUN DETECTED_ARCH=$(uname -m) \
    && YB_ARCH=$([ "${TARGETARCH:-}" = "arm64" ] || [ "${DETECTED_ARCH}" = "aarch64" ] || [ "${DETECTED_ARCH}" = "arm64" ] && echo "aarch64" || echo "x86_64") \
    && YB_CLIENT_ARCHIVE="yugabyte-client-${YB_VERSION}-${YB_BUILD}-linux-${YB_ARCH}.tar.gz" \
    && YB_CLIENT_URL="https://downloads.yugabyte.com/releases/${YB_VERSION}/${YB_CLIENT_ARCHIVE}" \
    && curl -fsSL "$YB_CLIENT_URL" -o "/tmp/yugabyte-client.tar.gz" \
    && mkdir -p /opt/yugabyte-client \
    && tar -xzf "/tmp/yugabyte-client.tar.gz" -C /opt/yugabyte-client --strip-components=1 \
    && rm "/tmp/yugabyte-client.tar.gz"

COPY --from=yugabyte-db /home/yugabyte/bin/yb-admin /opt/yugabyte-db/bin/yb-admin
COPY --from=yugabyte-db /home/yugabyte/bin/yb-ts-cli /opt/yugabyte-db/bin/yb-ts-cli
COPY --from=yugabyte-db /home/yugabyte/lib /opt/yugabyte-db/lib

COPY bin/yb-admin bin/yb-ts-cli bin/ysqlsh /usr/local/bin/
RUN chmod +x /usr/local/bin/yb-admin /usr/local/bin/yb-ts-cli /usr/local/bin/ysqlsh

ENV MAVEN_CONFIG=/home/$USERNAME/.m2
ENV PATH=/workspaces/yb-multids/.devcontainer/bin:$PATH:/opt/yugabyte-client/bin:/opt/yugabyte-client/postgres/bin:/opt/yugabyte-db/bin
ENV LD_LIBRARY_PATH=/opt/yugabyte-client/lib:/opt/yugabyte-client/lib/postgres:/opt/yugabyte-client/lib/yb:/opt/yugabyte-client/lib/yb-thirdparty:/opt/yugabyte-db/lib/yb:/opt/yugabyte-db/lib/yb-thirdparty

USER $USERNAME
