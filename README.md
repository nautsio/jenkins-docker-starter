Dockerized Jenkins: Automatic Bootstrap with Job- and Pipeline DSL
=================================================================

## Background

This repository contains example code on how to automatically bootstrap and configure Jenkins, while running in a Docker
container. It contains groovy code that configures Jenkins on startup and provides examples of how to use the Job- and
Pipeline DSL to automatically configure Jenkins.

## Setup

The following sections describe each part of the Jenkins docker setup in detail.

#### Dockerfile

We inherit from the Jenkins Alpine base image that is available on the Docker Hub. This significantly cuts down on the
work that we would have to do to setup Jenkins from scratch. Next up is the installation of packages that we need, like
Terraform and Ansible, in order to run our jobs. We try to be smart and not install anything more than we really need.

The Jenkins base image provides a script to automatically install plugins, including all required dependencies. This
provides us with a really nice way to install the plugins that we need and nothing more. Since we are configuring
Jenkins automatically we disable the setup wizard. Next up, we copy all the groovy setup scripts to the appropriate
location. This makes sure that they are executed on Jenkins startup. Lastly, we copy some ssh configs for the bastion
host settings and a known_hosts file that contains the key signatures of services that the jobs require (e.g. git
server, bastion host, etc.).

The ```build.sh``` script creates a docker image that will fully configure Jenkins on startup.

#### Instance and Seed configuration

We need to configure some Jenkins setting on startup. The ```jenkins.groovy``` script contains some code to setup
security of the Jenkins instance. You could configure any Jenkins setting that you need here. The ```seed.groovy```
script creates the seed job that will create all other jobs. It creates a freestyle job that will execute any groovy
files that are found in the specified location. It is configured to automatically start building on Jenkins boot.

At this point we have a fully configured Jenkins instance. Let's take a look at the Job- and Pipeline DSL scripts.

#### Job- and Pipeline DSL

In the ```job-dsl``` directory you can find scripts that will create all the jobs that we need. The ```jobs.groovy```
will set up a number of views/folders/jobs that correspond to the different environments that we support. It makes
liberal use of the Job DSL possibilities and tries to be succinct by using loops. The job commands are rather simple shell
scripts that contain some substitution markers. It is recommended to keep your jobs configs as simple as possible and
put the complexity in dedicated build systems. In the job configs we reference credentials that we put in manually or
get from some external source.

The Terraform pipeline is created with the Pipeline DSL plugin. You have the option to put a dedicated Jenkinsfile in
your repository but it is also possible to just create a pipeline, using the pipeline DSL, with the Job DSL. This gives
you more freedom while maybe sacrificing 'convention-over-configuration'. I've found that complex job setups cannot be
easily captured in a Jenkinsfile that you have to put in the root of your repo. This is an alternative approach.

The pipeline itself is really straightforward and provides a nice 'plan-and-apply' workflow for Terraform, again using
credentials from the credentials plugin.

## Upgrades

The approach taken here is to put an increasing version number behind the main Jenkins base image (e.g. 2.7.4-1,
2.7.4-2, etc.) This provides for an easy way to iterate on your Jenkins configuration. If you map your Jenkins home
directory (/var/jenkins_home in the container) to the host system then you can just replace the Jenkins container with
the new version and all jobs will be updated automatically to the new config on restart, while keeping any history and
settings that were there already. This makes for a nice, fast upgrade workflow.

## Caveats

Although the job configuration is fully automatic it does require that you enter the necessary credentials manually with
the credentials plugin. If you want, you could replace this with something like Vault but that would require additional
setup with an external dependency.

I hope this repository provides a nice overview of what it takes to create a dockerized Jenkins that is easily
maintainable and configured fully automatically.
