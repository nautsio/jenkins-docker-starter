// Repository with Terraform and Ansible configuration
repo = 'https://github.com/nautsio/jenkins-docker-starter'

// Available environments
environments = ["Acceptance", "Production", "Management"]

// Server types that are targeted by Ansible
types = [
    "Acceptance": ["Master", "Elasticsearch"],
    "Production": ["Master", "Elasticsearch"],
    "Management": ["Elasticsearch", "Logstash", "Jenkins", "Monitoring"]
]

// Create environment folder and views
def createEnvFoldersAndViews(String env) {
    // Create environment folder
    folder(env)

    // Create environment views
    listView("${env}/Ansible") {
        jobs {
            regex(/Provision.*/)
        }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
  	listView("${env}/Terraform") {
        jobs {
            regex(/Terraform.*/)
        }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
    if (!env.equals('Management')) {
        listView("${env}/Marathon") {
            jobs {
                regex(/Deploy.*/)
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
        }
    }
}

// Create Ansible Jobs
def createAnsibleRunJobs(String env, String type) {

    def ansibleCommand = '''
# Move to the Ansible directory
cd ansible

# Replace the marker with the actual location of the Bastion SSH private key
sed -i s~%%BASTION_SSH_KEY%%~$BASTION_SSH_KEY~g ssh_aws.cfg

# Write out the Ansible vault password to a file
echo $ANSIBLE_VAULT > vault.pwd

# Make sure it is not executable and limited in permissions
chmod 600 vault.pwd

# Run the appropriate Ansible playbook with the instance private key specified
ANSIBLE_FORCE_COLOR=true ansible-playbook --private-key=$INSTANCE_SSH_KEY --vault-password-file ./vault.pwd -i environments/aws '''

    job("${env}/Provision-${type}") {
        description("Provision servers in environment ${env} with type ${type}.")
        parameters {
            stringParam('ANSIBLE_OPTIONS', '', 'A command line option string that will be appended to the Ansible playbook command')
        }
        scm {
            git {
                remote {
                    url(repo)
                }
                branch('master')
            }
        }
        triggers {
            cron('H/30 * * * *')
        }
        wrappers {
            colorizeOutput()
            timestamps()
            credentialsBinding {
              string("AWS_ACCESS_KEY_ID", "aws-${env.toLowerCase()}-access-key-id")
              string("AWS_SECRET_ACCESS_KEY", "aws-${env.toLowerCase()}-secret-access-key")
              string("ANSIBLE_VAULT", "ansible-vault")
              file("INSTANCE_SSH_KEY", "aws-${env.toLowerCase()}-ssh-key")
              file("BASTION_SSH_KEY", "aws-bastion-ssh-key")
            }
        }
        steps {
            shell(ansibleCommand + "${type.toLowerCase()}.yml --extra-vars @environments/aws/env_vars/${env.toLowerCase()}.yml \$ANSIBLE_OPTIONS")
        }
    }
}

// Create Terraform Jobs
def createTerraformJobs(String env) {
    pipelineJob("${env}/Terraform") {
        definition {
            cps {
                sandbox(false)
                pipeline = readFileFromWorkspace('terraform.pipeline')
                script(pipeline.replaceAll("%%ENV%%", env.toLowerCase()))
            }
        }
    }
}

// Create Marathon Jobs
def createMarathonJobs(String env) {

    // Don't deploy to management environment
    if (env.equals('Management')) {
        return
    }

    def marathonCommand = '''
# Move to the Marathon directory
cd marathon

# Run deploy script
python deploy.py %%ENV%%'''

    def shortEnvs = [
        Acceptance: 'acc',
        Production: 'prd'
    ]

    job("${env}/Deploy") {
        description("Deploy environment ${name}.")
        scm {
            git {
                remote {
                    url(repo)
                }
                branch('master')
            }
        }
        wrappers {
            colorizeOutput()
            timestamps()
            credentialsBinding {
              string("VAULT", "ansible-vault")
            }
        }
        steps {
            shell(marathonCommand.replaceAll(/%%SHORT_ENV%%/, shortEnvs[env]).replaceAll(/%%ENV%%/, env.toLowerCase()))
        }
    }
}

// Iterate over the environments and create views, folders andjobs.
environments.each { env ->
    createEnvFoldersAndViews("${env}")

    types[env].each { type ->
        createAnsibleRunJobs(env, type)
    }
 	createTerraformJobs(env)
    createMarathonJobs(env)
}
