pipeline {
    agent any
    tools {
        maven 'Maven 3.9.6'
        jdk 'jdk21'
    }
    environment {
        // Define as a comma-separated string
        DEV_SERVERS = '192.168.1.105'
        PROD_SERVERS = '192.168.1.108,192.168.1.106'

        JAVA_OPTS = '-Djava.io.tmpdir=/var/tmp/exportDir'
        GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
        ENV = "${params.env_deploy}"
        VERSION = getArtifactVersion(ENV, GIT_COMMIT_SHORT)
    }
    stages {
        stage('Initialize') {
            steps {
                echo "Deploying ${params.project} to ${ENV} version ${VERSION}"
                gitCheckout(ENV)
            }
        }

        stage('Build and Deploy Service') {
            steps {
                script {
                    // Mapping project names to their directory and service names
                    def serviceMap = [
                        'service-config': [dir: 'config-service', service: 'dvdtheque-server-config', needsCommons: false],
                        'eureka':         [dir: 'discovery-service', service: 'dvdtheque-discovery-server', needsCommons: false],
                        'api-gateway':    [dir: 'api-gateway-service', service: 'dvdtheque-api-gateway-server', needsCommons: false],
                        'dvdtheque-rest': [dir: 'dvdtheque-service', service: 'dvdtheque-rest', needsCommons: true],
                        'dvdtheque-tmdb': [dir: 'dvdtheque-tmdb-service', service: 'dvdtheque-tmdb', needsCommons: true]
                        // Add others here...
                    ]

                    def config = serviceMap[params.project]
                    if (config) {
                        if (config.needsCommons) buildCommons()

                        dir(config.dir) {
                            buildService(ENV)
                            deployToServers(ENV, config.dir, config.service)
                        }
                    } else {
                        error "Unknown project: ${params.project}"
                    }
                }
            }
        }
    }
}

/** * Helper to deploy to multiple IPs based on environment
 */
private void deployToServers(String env, String projectDir, String serviceName) {
    // Use .split(',') to turn the string back into a list for iteration
    def targetList = (env == 'prod') ? PROD_SERVERS.split(',') : DEV_SERVERS.split(',')

    targetList.each { ip ->
        echo "Deploying to ${ip.trim()}..."
        // SSH/SCP commands here
    }
}
/** * Helper to checkout the correct git branch based on environment
 */
private void gitCheckout(String env){
    if(env == "dev"){
        sh """
            git checkout develop
            git pull
        """
    }
    if(env == "prod"){
        sh """
            git checkout main
            git pull
        """
   }
}
/** * Helper to determine artifact version based on environment
 */
private String getArtifactVersion(String env,String gitCommit){
	if(env == "dev"){
		return "${gitCommit}-SNAPSHOT"
	}
	if(env == "prod"){
		return "${gitCommit}"
	}
	return ""
}

/** * Helper to build commons module
 */
private void buildCommons(){
    dir("dvdtheque-commons") {
        sh """
            mvn -B clean install
        """
    }
}

/** * Helper to build service based on environment
 */
private void buildService(String env){
    if(env == "dev"){
        sh """
            mvn -B org.codehaus.mojo:versions-maven-plugin:2.8.1:set -DnewVersion=${VERSION}
            mvn -B clean test -Darguments="${JAVA_OPTS}"
            mvn -B clean install -DskipTests
        """
    }
    if(env == "prod"){
        sh """
            mvn -B org.codehaus.mojo:versions-maven-plugin:2.8.1:set -DnewVersion=${VERSION}
            mvn -B clean install -DskipTests
        """
    }
}