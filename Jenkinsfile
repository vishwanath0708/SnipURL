pipeline {
    agent any
    
    environment {
        APP_NAME = 'SnipURL'
        DOCKER_IMAGE = 'snipurl'
        DOCKER_REGISTRY = 'vishwahubballi'  // Your Docker Hub username
        
        DB_NAME = 'snipurl'
        DB_USER = 'postgres'
        DB_PASSWORD = 'secret'
        DB_PORT = '5433'  // Changed to avoid port conflict
        REDIS_PORT = '6379'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Start PostgreSQL and Redis Containers') {
            steps {
                script {
                    sh '''
                        docker stop test-postgres-snipurl 2>/dev/null || true
                        docker rm test-postgres-snipurl 2>/dev/null || true
                        docker stop test-redis-snipurl 2>/dev/null || true
                        docker rm test-redis-snipurl 2>/dev/null || true
                    '''
                    
                    sh """
                        docker run -d --name test-postgres-snipurl \
                            -e POSTGRES_DB=${DB_NAME} \
                            -e POSTGRES_USER=${DB_USER} \
                            -e POSTGRES_PASSWORD=${DB_PASSWORD} \
                            -p ${DB_PORT}:5432 \
                            postgres:15
                    """
                    
                    sh """
                        docker run -d --name test-redis-snipurl \
                            -p ${REDIS_PORT}:6379 \
                            redis:7-alpine
                    """
                    
                    sleep time: 15, unit: 'SECONDS'
                }
            }
        }
        
        stage('Build Application') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Package Application') {
            steps {
                sh 'mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
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
                    // Using docker_credentials for Docker Hub login
                    docker.withRegistry('https://index.docker.io/v1/', 'docker_credentials') {
                        sh """
                            docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}
                            docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:latest
                        """
                    }
                }
            }
        }
        
        stage('Stop Test Containers') {
            steps {
                script {
                    sh '''
                        docker stop test-postgres-snipurl 2>/dev/null || true
                        docker rm test-postgres-snipurl 2>/dev/null || true
                        docker stop test-redis-snipurl 2>/dev/null || true
                        docker rm test-redis-snipurl 2>/dev/null || true
                    '''
                }
            }
        }
        
        stage('Deploy Application') {
            steps {
                script {
                    sh '''
                        docker stop snipurl-app 2>/dev/null || true
                        docker rm snipurl-app 2>/dev/null || true
                    '''
                    
                    sh """
                        docker run -d --name snipurl-app \
                            -p 8080:8080 \
                            --restart unless-stopped \
                            ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}
                    """
                }
            }
        }
    }
    
    post {
        always {
            script {
                sh '''
                    docker stop test-postgres-snipurl 2>/dev/null || true
                    docker rm test-postgres-snipurl 2>/dev/null || true
                    docker stop test-redis-snipurl 2>/dev/null || true
                    docker rm test-redis-snipurl 2>/dev/null || true
                '''
            }
        }
        success {
            echo '=========================================='
            echo '✅ SnipURL Pipeline completed successfully!'
            echo '=========================================='
            echo "Image pushed to: ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}"
            echo "App URL: http://localhost:8080"
            echo '=========================================='
        }
        failure {
            echo '=========================================='
            echo '❌ SnipURL Pipeline failed!'
            echo '=========================================='
        }
    }
}
