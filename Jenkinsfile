pipeline {
    agent any
    tools {
        maven 'Maven 3.9.12'
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
                // Utilisation de doubles guillemets triples pour l'interpolation correcte
                sh """
                   echo "VERSION = ${VERSION}"
                   echo "PROJECT = ${params.project}"
                    echo "ENV = ${ENV}"
                """
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
                        'dvdtheque-tmdb': [dir: 'dvdtheque-tmdb-service', service: 'dvdtheque-tmdb', needsCommons: true],
                        'dvdtheque-batch': [dir: 'dvdtheque-batch-service', service: 'dvdtheque-batch', needsCommons: true],
                        'dvdtheque-allocine': [dir: 'dvdtheque-allocine-service', service: 'dvdtheque-allocine', needsCommons: true]
                        // Add others here...
                    ]

                    def config = serviceMap[params.project]
                    if (config) {
                        if (config.needsCommons) buildCommons()

                        dir(config.dir) {
                            buildService(ENV)
                            // GESTION DU CIBLAGE SPECIFIQUE
                            if (params.project == 'dvdtheque-batch' || params.project == 'dvdtheque-allocine') {
                                // Forcer uniquement sur le .108 en production
                                def target = (ENV == 'prod') ? "192.168.1.108" : null
                                deployToServers(ENV, config.dir, config.service, target)
                            } else {
                                deployToServers(ENV, config.dir, config.service)
                            }
                        }
                    } else {
                        error "Unknown project: ${params.project}"
                    }
                }
            }
        }
    }
}

/** * Helper pour déployer. Accepte une IP optionnelle (specificIp)
 */
private void deployToServers(String env, String projectDir, String serviceName, String specificIp = null) {
    def targetList = []

    if (specificIp) {
        targetList = [specificIp]
    } else {
        targetList = (env == 'prod') ? PROD_SERVERS.split(',') : DEV_SERVERS.split(',')
    }

    targetList.each { ip ->
        def cleanIp = ip.trim()
        echo "Processing ${serviceName} on ${cleanIp}"


         // Note: On remplace les tirets par des underscores pour le nom du dossier si nécessaire
         def folderName = "${serviceName}_service".replace('-', '_')

        // 1. Création dossier + Permissions (évite les erreurs de logs)
        def folderPath = "/opt/${folderName}"
        sh "ssh jenkins@${cleanIp} 'sudo mkdir -p ${folderPath}/logs && sudo chown -R dvdtheque-user:java-app-gr ${folderPath}/logs'"

        // 2. Stop Service (Sans la quote orpheline !)
        sh "ssh jenkins@${cleanIp} sudo systemctl stop ${serviceName}.service"

        // 3. Transfer Artifact (Renommé pour correspondre au fichier .service)
        sh "scp target/${projectDir}-${VERSION}.jar jenkins@${cleanIp}:${folderPath}/${projectDir}.jar"

        // 4. Start and Verify
        sh "ssh jenkins@${cleanIp} sudo systemctl start ${serviceName}.service"
        sh "ssh jenkins@${cleanIp} 'systemctl is-active ${serviceName}.service || (journalctl -u ${serviceName}.service -n 20 && exit 1)'"
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