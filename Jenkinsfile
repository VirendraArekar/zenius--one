pipeline {
    agent any

    tools {
        jdk 'JDK21'
        nodejs 'Node22'
    }

    environment {
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        KUBE_NAMESPACE = 'zenius-one'
    }

    stages {
        stage('Microservices Build & Test') {
            steps {
                sh 'mvn -f services/pom.xml -B test'
            }
        }

        stage('Frontend Build & Unit Tests') {
            steps {
                sh 'npm --prefix frontend ci'
                sh 'npm --prefix frontend run build'
                sh 'npm --prefix frontend run test'
            }
        }

        stage('Container Package') {
            steps {
                sh 'docker compose --env-file .env.example build'
            }
        }

        stage('Kubernetes Deploy') {
            when { branch 'main' }
            steps {
                sh 'kubectl apply -f infrastructure/k8s/'
            }
        }
    }
}
