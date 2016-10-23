import hudson.model.*
import jenkins.model.*;
import javaposse.jobdsl.plugin.*;

def jenkins = Jenkins.getInstance();

def job = new FreeStyleProject(jenkins, "Seed-Jobs");
job.setAssignedLabel(null);

job.setCustomWorkspace("/usr/share/jenkins/ref/init.groovy.d/job-dsl");
def ExecuteDslScripts.ScriptLocation scriptlocationFileSys = new ExecuteDslScripts.ScriptLocation('false', "*.groovy", null);
def ExecuteDslScripts executeDslScripts = new ExecuteDslScripts(scriptlocationFileSys, false, RemovedJobAction.IGNORE);

job.buildersList.add(executeDslScripts);

jenkins.putItem(job);
jenkins.reload();

def jobRef = jenkins.getItem(job.getName());
jenkins.getQueue().schedule(jobRef, 10);
