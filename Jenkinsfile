pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'vishwahubballi'
        DOCKER_IMAGE = 'snipurl'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "testing the CICD pipeline"
            }
        }
        
        stage('Package') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} ."
                    sh "docker tag ${DOCKER_IMAGE}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}"
                    sh "docker tag ${DOCKER_IMAGE}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:latest"
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                script {
                    // Using shell commands instead of docker.withRegistry
                    withCredentials([usernamePassword(credentialsId: 'docker_credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh """
                            echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin
                            docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}
                            docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:latest
                            docker logout
                        """
                    }
                }
            }
        }
    }
}
