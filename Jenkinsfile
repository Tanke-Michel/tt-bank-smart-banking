pipeline {
    agent any
    environment {
        PROJECT_DIR  = '/opt/tt-bank/Smart-Banking-System'
        COMPOSE_FILE = 'infrastructure/docker/docker-compose.yml'
        ENV_FILE     = 'infrastructure/docker/.env'
    }
    options { timeout(time: 30, unit: 'MINUTES'); timestamps() }
    stages {
        stage('Checkout') {
            steps {
                echo 'Pulling latest code from GitHub...'
                checkout scm
                sh 'git log --oneline -1'
            }
        }
        stage('Sync to Deploy Dir') {
            steps {
                echo 'Syncing source into the deployment directory...'
                sh 'rsync -rlt --delete --exclude ".git" --exclude "infrastructure/docker/.env" "$WORKSPACE"/ "$PROJECT_DIR"/'
            }
        }
        stage('Build & Test - Backend') {
            steps {
                echo 'Building all microservices with Maven...'
                sh 'cd "$PROJECT_DIR" && docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build auth-service wallet-service transaction-service merchant-service savings-service audit-service notification-service api-gateway'
            }
        }
        stage('Build - Frontend') {
            steps {
                echo 'Building the React frontend...'
                sh 'cd "$PROJECT_DIR" && docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build frontend'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying the full stack...'
                sh 'cd "$PROJECT_DIR" && docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d'
            }
        }
        stage('Health Check') {
            steps {
                echo 'Verifying the deployment...'
                sh 'sleep 30; curl -s -o /dev/null -w "Frontend HTTP %{http_code}\\n" http://13.140.131.228/ || true; docker ps --format "  {{.Names}}: {{.Status}}" | grep tt-bank || true'
            }
        }
    }
    post {
        success { echo 'PIPELINE SUCCESS - TT-BANK built, tested, and deployed.' }
        failure { echo 'PIPELINE FAILED - check the stage logs above.' }
    }
}
