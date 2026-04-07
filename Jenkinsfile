pipeline {
    agent any
    
    environment {
        APP_NAME = 'SnipURL'
        DOCKER_IMAGE = 'snipurl'
        DOCKER_REGISTRY = 'vishwanathhubballi'  // Your Docker Hub username
        
        DB_NAME = 'snipurl'
        DB_USER = 'postgres'
        DB_PASSWORD = 'secret'
        DB_PORT = '5432'
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
        
        // SKIP TESTS - No test stage
        
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
                    sh "docker tag ${DOCKER_IMAGE}:${BUILD_NUMBER} ${DOCKER_IMAGE}:latest"
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                script {
                    // Using your Docker Credentials
                    docker.withRegistry('https://index.docker.io/v1/', 'Docker Credentials') {
                        sh """
                            docker tag ${DOCKER_IMAGE}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}
                            docker tag ${DOCKER_IMAGE}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:latest
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
                        docker stop snipurl-postgres 2>/dev/null || true
                        docker rm snipurl-postgres 2>/dev/null || true
                        docker stop snipurl-redis 2>/dev/null || true
                        docker rm snipurl-redis 2>/dev/null || true
                    '''
                    
                    sh """
                        docker run -d --name snipurl-postgres \
                            -e POSTGRES_DB=${DB_NAME} \
                            -e POSTGRES_USER=${DB_USER} \
                            -e POSTGRES_PASSWORD=${DB_PASSWORD} \
                            -p ${DB_PORT}:5432 \
                            -v snipurl-postgres-data:/var/lib/postgresql/data \
                            postgres:15
                    """
                    
                    sh """
                        docker run -d --name snipurl-redis \
                            -p ${REDIS_PORT}:6379 \
                            --restart unless-stopped \
                            redis:7-alpine
                    """
                    
                    sleep time: 10, unit: 'SECONDS'
                    
                    sh """
                        docker run -d --name snipurl-app \
                            -p 8080:8080 \
                            --link snipurl-postgres:postgres \
                            --link snipurl-redis:redis \
                            -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${DB_NAME} \
                            -e SPRING_DATASOURCE_USERNAME=${DB_USER} \
                            -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
                            -e SPRING_DATA_REDIS_HOST=redis \
                            -e SPRING_DATA_REDIS_PORT=${REDIS_PORT} \
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
            echo "App URL: http://localhost:8080"
            echo "PostgreSQL: localhost:5432 (user: postgres, password: secret)"
            echo "Redis: localhost:6379"
            echo '=========================================='
        }
        failure {
            echo '=========================================='
            echo '❌ SnipURL Pipeline failed!'
            echo '=========================================='
            echo 'Check the console logs above for errors.'
            echo '=========================================='
        }
    }
}
