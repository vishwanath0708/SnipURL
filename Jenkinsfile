pipeline {
    agent any
    
    tools {
        maven 'Maven-3.9'  // Configure this in Jenkins Global Tools
        jdk 'JDK-17'       // Configure this in Jenkins Global Tools
    }
    
    environment {
        // Docker Hub credentials (configure in Jenkins)
        DOCKER_REGISTRY = 'your-dockerhub-username'
        DOCKER_IMAGE = 'sentinel-gateway'
        
        // Database credentials for testing
        DB_HOST = 'localhost'
        DB_PORT = '5432'
        DB_NAME = 'testdb'
        DB_USER = 'testuser'
        DB_PASSWORD = 'testpass'
        
        // Redis credentials
        REDIS_HOST = 'localhost'
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
                    // Start PostgreSQL container for testing
                    sh '''
                        docker run -d --name test-postgres \
                            -e POSTGRES_DB=${DB_NAME} \
                            -e POSTGRES_USER=${DB_USER} \
                            -e POSTGRES_PASSWORD=${DB_PASSWORD} \
                            -p ${DB_PORT}:5432 \
                            postgres:15
                    '''
                    
                    // Start Redis container for testing
                    sh '''
                        docker run -d --name test-redis \
                            -p ${REDIS_PORT}:6379 \
                            redis:7-alpine
                    '''
                    
                    // Wait for containers to be ready
                    sleep time: 10, unit: 'SECONDS'
                }
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Run Tests') {
            steps {
                sh '''
                    mvn test \
                        -Dspring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} \
                        -Dspring.datasource.username=${DB_USER} \
                        -Dspring.datasource.password=${DB_PASSWORD} \
                        -Dspring.redis.host=${REDIS_HOST} \
                        -Dspring.redis.port=${REDIS_PORT}
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Package') {
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
                    docker.build("${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}")
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'docker-credentials') {
                        docker.image("${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}").push()
                        docker.image("${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}").push('latest')
                    }
                }
            }
        }
        
        stage('Stop and Clean Containers') {
            steps {
                script {
                    sh '''
                        docker stop test-postgres || true
                        docker rm test-postgres || true
                        docker stop test-redis || true
                        docker rm test-redis || true
                    '''
                }
            }
        }
        
        stage('Deploy with Docker Compose') {
            steps {
                script {
                    sh '''
                        # Create docker-compose.yml for production
                        cat > docker-compose.prod.yml << EOF
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - app-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - app-network

  app:
    image: ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DB_NAME}
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    networks:
      - app-network

volumes:
  postgres_data:

networks:
  app-network:
    driver: bridge
EOF
                        
                        # Deploy with docker-compose
                        docker-compose -f docker-compose.prod.yml up -d
                    '''
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Clean up containers even if build fails
                sh '''
                    docker stop test-postgres test-redis || true
                    docker rm test-postgres test-redis || true
                '''
            }
        }
        success {
            echo 'Pipeline completed successfully!'
            echo "Application deployed at: http://localhost:8080"
        }
        failure {
            echo 'Pipeline failed! Check logs for details.'
        }
    }
}
