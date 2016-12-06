FROM jenkins:2.7.4-alpine

# Switch to root user for package installation
USER root

ENV TERRAFORM_VERSION=0.7.4
ENV TERRAFORM_SHA256SUM=8950ab77430d0ec04dc315f0d2d0433421221357b112d44aa33ed53cbf5838f6

# Install Ansible
RUN apk add --update jq make git curl openssl-dev libffi-dev musl-dev gcc python-dev python py-pip && \
    pip install ansible boto awscli marathon hvac && \
    curl https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip > terraform_${TERRAFORM_VERSION}_linux_amd64.zip && \
    echo "${TERRAFORM_SHA256SUM}  terraform_${TERRAFORM_VERSION}_linux_amd64.zip" > terraform_${TERRAFORM_VERSION}_SHA256SUMS && \
    sha256sum -c --status terraform_${TERRAFORM_VERSION}_SHA256SUMS && \
    unzip terraform_${TERRAFORM_VERSION}_linux_amd64.zip -d /bin && \
    rm -f terraform_${TERRAFORM_VERSION}_linux_amd64.zip && \
    apk del openssl-dev libffi-dev musl-dev gcc python-dev py-pip

# Set the correct timezone
RUN apk add -U tzdata
RUN cp /usr/share/zoneinfo/Europe/Amsterdam /etc/localtime

# Switch back to non-privileged Jenkins user
USER jenkins

# Install Jenkins Plugins
RUN /usr/local/bin/install-plugins.sh job-dsl git cloudbees-folder credentials credentials-binding ansicolor timestamper workflow-aggregator

# Disable the setup wizard
ENV JAVA_OPTS=-Djenkins.install.runSetupWizard=false

# Copy seed job
COPY *.groovy /usr/share/jenkins/ref/init.groovy.d/

# Copy job generation scripts
COPY job-dsl/ /usr/share/jenkins/ref/init.groovy.d/job-dsl/

# Create SSH config with bastion host location and known host keys
COPY ssh_config /usr/share/jenkins/ref/.ssh/config
COPY known_hosts /usr/share/jenkins/ref/.ssh/known_hosts
