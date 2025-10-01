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

    stage('(Optional) Smoke test /hello') {
      when { expression { return false } } // bật thành true nếu muốn chạy thử
      steps {
        sh """
          set -e
          docker rm -f hello-smoke || true
          # Dockerfile EXPOSE 9090, map ra 19090
          docker run -d --name hello-smoke -p 19090:9090 ${IMAGE_REPO}:${APP_VERSION}

          # đợi app sẵn sàng tối đa ~30s (chỉnh endpoint theo app của bạn)
          for i in \$(seq 1 30); do
            sleep 1
            if curl -sf http://127.0.0.1:19090/hello >/dev/null; then
              break
            fi
            echo "waiting app..."
          done

          RESP=\$(curl -s http://127.0.0.1:19090/hello || true)
          echo "Response: \$RESP"
          # thay điều kiện tuỳ response thực tế:
          test -n "\$RESP"
        """
      }
      post {
        always {
          sh 'docker rm -f hello-smoke || true'
        }
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
