pipeline {
  agent any

  environment {
    // đặt tên image local của bạn (tuỳ ý)
    IMAGE_REPO = 'hello-local/jenkins-pipeline-test'
  }

  options {
    timestamps()
    skipDefaultCheckout(false)
  }

  stages {
    stage('Checkout') {
      steps {
        // nếu job tạo từ "Pipeline script from SCM", chỉ cần checkout scm
        checkout([$class: 'GitSCM',
          branches: [[name: '*/main']],
          userRemoteConfigs: [[url: 'https://github.com/duykhanhxx03/jenkins-pipeline-test.git']]
        ])
        sh 'git --version'
      }
    }

    stage('Set Version') {
      steps {
        script {
          def sha = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
          env.APP_VERSION = "dev-${sha}"
          echo "App version: ${env.APP_VERSION}"
        }
      }
    }

    stage('Build JAR (Maven in Docker)') {
      steps {
        sh """
          # build bằng container maven để khỏi cài JDK/Maven trên agent
          docker run --rm \
            -v "\$PWD":/workspace -w /workspace \
            maven:3.9.8-eclipse-temurin-21 \
            mvn -B -e -DskipTests clean package
        """
      }
      post {
        always {
          // thu thập báo cáo test nếu có
          junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
          archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
      }
    }

    stage('Docker Build (local)') {
      steps {
        sh """
          docker version
          docker build -t ${IMAGE_REPO}:${APP_VERSION} -t ${IMAGE_REPO}:latest .
        """
      }
    }

    stage('Deploy Local') {
      steps {
        sh """
          APP=hello-app
          docker rm -f $APP || true
          docker run -d --name $APP --restart=always -p 9090:9090 ${IMAGE_REPO}:${APP_VERSION}
        """
      }
    }

  }

  post {
    success {
      echo "✅ Built local image: ${IMAGE_REPO}:${APP_VERSION}"
    }
    failure {
      echo "❌ Pipeline FAILED."
    }
  }
}
