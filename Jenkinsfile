pipeline {
    agent any
    tools {
        maven 'Maven 3.9.6'
        jdk 'jdk17'
    }
    environment {
    	def DEV_SERVER1_IP = '192.168.1.103'
    	def DEV_SERVER2_IP = '192.168.1.105'
    	def PROD_SERVER1_IP = '192.168.1.108'
    	def PROD_SERVER2_IP = '192.168.1.106'
    	def JAVA_OPTS='-Djava.io.tmpdir=/var/tmp/exportDir'
    	GIT_COMMIT_SHORT = sh(
                script: "printf \$(git rev-parse --short HEAD)",
                returnStdout: true
        )

        def ENV = "${env_deploy}"
        def VERSION = getArtifactVersion(ENV,GIT_COMMIT_SHORT)
        def ARTIFACT = "dvdtheque-${VERSION}.jar"

    }
    stages {
        stage ('Initialize') {
            steps {
                sh '''
                    echo "VERSION = ${VERSION}"
                    echo "PROD_SERVER1_IP = ${PROD_SERVER1_IP}"
                    echo "PROD_SERVER2_IP = ${PROD_SERVER2_IP}"
                    echo "DEV_SERVER1_IP = ${DEV_SERVER1_IP}"
                    echo "DEV_SERVER2_IP = ${DEV_SERVER2_IP}"
                    echo "VERSION = ${VERSION}"
                    echo "ARTIFACT = ${ARTIFACT}"
                    echo "project = ${project}"
                    echo "ENV = ${ENV}"
                '''
            }
        }
        stage('Clone repository') {
			steps {
				script {
					checkout scm
				}
			}
		}
		stage('Building dvdtheque-service on dev env') {
		    when {
                expression { params.project == 'dvdtheque-rest' && params.env_deploy == 'dev'}
            }
		    steps {
                echo "Building dvdtheque-service on dev env"
                dir("dvdtheque-commons") {
                    sh """
                        mvn -B clean install
                    """
                }
                dir("dvdtheque-service") {
                    sh """
                        mvn -B org.codehaus.mojo:versions-maven-plugin:2.8.1:set -DnewVersion=${VERSION}
                        mvn -B test -Darguments="${JAVA_OPTS}"
                        mvn -B install -DskipTests
                    """
                    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl stop dvdtheque-rest.service'
                    sh 'ssh jenkins@$DEV_SERVER1_IP sudo systemctl stop dvdtheque-rest.service'
                    sh """
                        scp target/dvdtheque-service-${VERSION}.jar jenkins@${DEV_SERVER1_IP}:/opt/dvdtheque_rest_service/dvdtheque-rest-services.jar
                    """
                    sh """
                        scp target/dvdtheque-service-${VERSION}.jar jenkins@${DEV_SERVER2_IP}:/opt/dvdtheque_rest_service/dvdtheque-rest-services.jar
                    """
                    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl start dvdtheque-rest.service'
                    sh 'ssh jenkins@$DEV_SERVER1_IP sudo systemctl start dvdtheque-rest.service'
                    sh 'ssh jenkins@$DEV_SERVER1_IP sudo systemctl status dvdtheque-rest.service'
                    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl status dvdtheque-rest.service'
                }
            }
		}
        stage('Building dvdtheque-allocine-service on dev env') {
            when {
                expression { params.project == 'dvdtheque-allocine' && params.env_deploy == 'dev'}
            }
            steps {
                echo "Building dvdtheque-allocine-service on dev env"
                dir("dvdtheque-commons") {
                    sh """
                        mvn -B clean install
                    """
                }
                dir("dvdtheque-allocine-service") {
                    sh """
                        mvn -B org.codehaus.mojo:versions-maven-plugin:2.8.1:set -DnewVersion=${VERSION}
                        #mvn -B test -Darguments="${JAVA_OPTS}"
                        mvn -B install -DskipTests
                    """
                    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl stop dvdtheque-allocine.service'
                    sh """
                        scp target/dvdtheque-allocine-service-${VERSION}.jar jenkins@${DEV_SERVER2_IP}:/opt/dvdtheque_allocine_service/dvdtheque-allocine-service.jar
                    """
                    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl start dvdtheque-allocine.service'
                    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl status dvdtheque-allocine.service'
                }
            }
        }
        stage('Building dvdtheque-tmdb-service on dev env') {
            when {
                expression { params.project == 'dvdtheque-tmdb' && params.env_deploy == 'dev'}
            }
            steps {
                echo "Building dvdtheque-tmdb-service on dev env"
                dir("dvdtheque-commons") {
                    sh """
                        mvn -B clean install
                    """
                }
                dir("dvdtheque-tmdb-service") {
                    sh """
                        mvn -B org.codehaus.mojo:versions-maven-plugin:2.8.1:set -DnewVersion=${VERSION}
                        mvn -B test -Darguments="${JAVA_OPTS}"
                        mvn -B install -DskipTests
                    """
                    sh 'ssh jenkins@$DEV_SERVER1_IP sudo systemctl stop dvdtheque-tmdb.service'
                    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl stop dvdtheque-tmdb.service'
 			 		sh """
 			 			scp target/dvdtheque-tmdb-service-${VERSION}.jar jenkins@${DEV_SERVER1_IP}:/opt/dvdtheque_tmdb_service/dvdtheque-tmdb-service.jar
 			 		"""
 			 		sh """
 			 			scp target/dvdtheque-tmdb-service-${VERSION}.jar jenkins@${DEV_SERVER2_IP}:/opt/dvdtheque_tmdb_service/dvdtheque-tmdb-service.jar
 			 		"""
                    sh 'ssh jenkins@$DEV_SERVER1_IP sudo systemctl start dvdtheque-tmdb.service'
                    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl start dvdtheque-tmdb.service'
                    sh 'ssh jenkins@$DEV_SERVER1_IP sudo systemctl status dvdtheque-tmdb.service'
                    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl status dvdtheque-tmdb.service'
                }
            }
        }

	    stage('Stopping Dev Batch service') {
        	when {
                branch 'develop'
            }
        	steps {
	       		sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl stop dvdtheque-batch.service'
	       	}
	    }
	    stage('Stopping Prod1 Rest service') {
        	when {
                branch 'master'
            }
        	steps {
        		sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl stop dvdtheque-rest.service'
	       	}
	    }
	    stage('Stopping Prod2 Rest service') {
        	when {
                branch 'master'
            }
        	steps {
        		sh 'ssh jenkins@$PROD_SERVER2_IP sudo systemctl stop dvdtheque-rest.service'
	       	}
	    }
	    stage('Stopping Prod1 Tmdb service') {
        	when {
                branch 'master'
            }
        	steps {
        		sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl stop dvdtheque-tmdb.service'
	       	}
	    }
	    stage('Stopping Prod2 Tmdb service') {
        	when {
                branch 'master'
            }
        	steps {
        		sh 'ssh jenkins@$PROD_SERVER2_IP sudo systemctl stop dvdtheque-tmdb.service'
	       	}
	    }
	    stage('Stopping Prod2 Allocine service') {
        	when {
                branch 'master'
            }
        	steps {
        		sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl stop dvdtheque-allocine.service'
	       	}
	    }
	    stage('Stopping Prod Batch service') {
        	when {
                branch 'master'
            }
        	steps {
        		sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl stop dvdtheque-batch.service'
	       	}
	    }

        stage('Copying develop dvdtheque-tmdb-service') {
	    	when {
                branch 'develop'
            }
            steps {
                script {
			 		sh """
			 			scp dvdtheque-tmdb-service/target/$TMDB_ARTIFACT jenkins@${DEV_SERVER1_IP}:/opt/dvdtheque_tmdb_service/dvdtheque-tmdb-service.jar
			 		"""
			 		sh """
			 			scp dvdtheque-tmdb-service/target/$TMDB_ARTIFACT jenkins@${DEV_SERVER2_IP}:/opt/dvdtheque_tmdb_service/dvdtheque-tmdb-service.jar
			 		"""
			 	}
            }
        }

        stage('Copying develop dvdtheque-batch') {
	    	when {
                branch 'develop'
            }
            steps {
                script {
			 		sh """
			 			scp dvdtheque-batch/target/$BATCH_ARTIFACT jenkins@${DEV_SERVER2_IP}:/opt/dvdtheque_batch_service/dvdtheque-batch.jar
			 		"""
			 	}
            }
        }
        stage('Copying production dvdtheque-rest-services') {
	    	when {
                branch 'master'
            }
            steps {
                script {
                	sh """
			 			scp dvdtheque-rest-services/target/$ARTIFACT jenkins@${PROD_SERVER1_IP}:/opt/dvdtheque_rest_service/dvdtheque-rest-services.jar
			 		"""
			 		sh """
			 			scp dvdtheque-rest-services/target/$ARTIFACT jenkins@${PROD_SERVER2_IP}:/opt/dvdtheque_rest_service/dvdtheque-rest-services.jar
			 		"""
			 	}
            }
        }
        stage('Copying production dvdtheque-tmdb-service') {
	    	when {
                branch 'master'
            }
            steps {
                script {
                	sh """
			 			scp dvdtheque-tmdb-service/target/$TMDB_ARTIFACT jenkins@${PROD_SERVER1_IP}:/opt/dvdtheque_tmdb_service/dvdtheque-tmdb-service.jar
			 		"""
			 		sh """
			 			scp dvdtheque-tmdb-service/target/$TMDB_ARTIFACT jenkins@${PROD_SERVER2_IP}:/opt/dvdtheque_tmdb_service/dvdtheque-tmdb-service.jar
			 		"""
			 	}
            }
        }
        stage('Copying production dvdtheque-allocine-service') {
	    	when {
                branch 'master'
            }
            steps {
                script {
                	sh """
			 			scp dvdtheque-allocine-service/target/$ALLOCINE_ARTIFACT jenkins@${PROD_SERVER1_IP}:/opt/dvdtheque_allocine_service/dvdtheque-allocine-service.jar
			 		"""
			 	}
            }
        }
        stage('Copying production dvdtheque-batch-service') {
	    	when {
                branch 'master'
            }
            steps {
                script {
                	sh """
			 			scp dvdtheque-batch/target/$BATCH_ARTIFACT jenkins@${PROD_SERVER1_IP}:/opt/dvdtheque_batch_service/dvdtheque-batch.jar
			 		"""
			 	}
            }
        }
        stage('Sarting Dev1 Rest service') {
        	when {
                branch 'develop'
            }
        	steps {
	        	sh 'ssh jenkins@$DEV_SERVER1_IP sudo systemctl start dvdtheque-rest.service'
	        }
   		}
   		stage('Sarting Dev2 Rest service') {
   			when {
                branch 'develop'
            }
        	steps {
	        	sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl start dvdtheque-rest.service'
	        }
   		}
   		stage('Sarting Dev1 Tmdb service') {
        	when {
                branch 'develop'
            }
        	steps {
	        	sh 'ssh jenkins@$DEV_SERVER1_IP sudo systemctl start dvdtheque-tmdb.service'
	        }
   		}
   		stage('Sarting Dev2 Tmdb service') {
   			when {
                branch 'develop'
            }
        	steps {
	        	sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl start dvdtheque-tmdb.service'
	        }
   		}
   		stage('Sarting Dev2 Allocine service') {
        	when {
                branch 'develop'
            }
        	steps {
	        	sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl start dvdtheque-allocine.service'
	        }
   		}
   		stage('Sarting Dev Batch service') {
   			when {
                branch 'develop'
            }
        	steps {
	        	sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl start dvdtheque-batch.service'
	        }
   		}
   		stage('Sarting Prod1 Rest service') {
        	when {
                branch 'master'
            }
        	steps {
	        	sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl start dvdtheque-rest.service'
	        }
   		}
   		stage('Sarting Prod2 Rest service') {
   			when {
                branch 'master'
            }
        	steps {
	        	sh 'ssh jenkins@$PROD_SERVER2_IP sudo systemctl start dvdtheque-rest.service'
	        }
   		}
   		stage('Sarting Prod1 tmdb service') {
        	when {
                branch 'master'
            }
        	steps {
	        	sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl start dvdtheque-tmdb.service'
	        }
   		}
   		stage('Sarting Prod2 tmdb service') {
   			when {
                branch 'master'
            }
        	steps {
	        	sh 'ssh jenkins@$PROD_SERVER2_IP sudo systemctl start dvdtheque-tmdb.service'
	        }
   		}
   		stage('Sarting Prod2 Allocine service') {
   			when {
                branch 'master'
            }
        	steps {
	        	sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl start dvdtheque-allocine.service'
	        }
   		}
   		stage('Sarting Prod2 Batch service') {
   			when {
                branch 'master'
            }
        	steps {
	        	sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl start dvdtheque-batch.service'
	        }
   		}

		stage('Check status Dev1 Tmdb service') {
   			when {
                branch 'develop'
            }
			steps {
				script {
				    sh 'ssh jenkins@$DEV_SERVER1_IP sudo systemctl status dvdtheque-tmdb.service'
			    }
			}
		}
		stage('Check status Dev2 Tmdb service') {
			when {
                branch 'develop'
            }
			steps {
				script {
				    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl status dvdtheque-tmdb.service'
			    }
			}
		}

		stage('Check status Dev Batch service') {
			when {
                branch 'develop'
            }
			steps {
				script {
				    sh 'ssh jenkins@$DEV_SERVER2_IP sudo systemctl status dvdtheque-batch.service'
			    }
			}
		}
		stage('Check status Prod1 Rest service') {
			when {
                branch 'master'
            }
			steps {
				script {
				    sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl status dvdtheque-rest.service'
			    }
			}
		}
		stage('Check status Prod2 Rest service') {
			when {
                branch 'master'
            }
			steps {
				script {
				    sh 'ssh jenkins@$PROD_SERVER2_IP sudo systemctl status dvdtheque-rest.service'
			    }
			}
		}
		stage('Check status Prod1 tmdb service') {
			when {
                branch 'master'
            }
			steps {
				script {
				    sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl status dvdtheque-tmdb.service'
			    }
			}
		}
		stage('Check status Prod2 tmdb service') {
			when {
                branch 'master'
            }
			steps {
				script {
				    sh 'ssh jenkins@$PROD_SERVER2_IP sudo systemctl status dvdtheque-tmdb.service'
			    }
			}
		}
		stage('Check status Prod2 Allocine tmdb service') {
			when {
                branch 'master'
            }
			steps {
				script {
				    sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl status dvdtheque-allocine.service'
			    }
			}
		}
		stage('Check status Prod2 Batch tmdb service') {
			when {
                branch 'master'
            }
			steps {
				script {
				    sh 'ssh jenkins@$PROD_SERVER1_IP sudo systemctl status dvdtheque-batch.service'
			    }
			}
		}
    }
}

private String getArtifactVersion(String env,String gitCommit){
	if(env == "dev"){
		return "${gitCommit}-SNAPSHOT"
	}
	if(env == "prod"){
		return "${gitCommit}"
	}
	return ""
}