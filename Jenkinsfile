pipeline {
    agent any

    tools {
        maven 'maven3'
        jdk 'java21'
    }

    environment {
        SONAR_PROJECT_KEY = 'student-management'
        SONAR_TOKEN = credentials('sonar-token')
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        MAIL_USERNAME = credentials('MAIL_USERNAME')
        MAIL_PASSWORD = credentials('MAIL_PASSWORD')
        JWT_SECRET = credentials('JWT_SECRET')
        DB_PASSWORD = credentials('DB_PASSWORD')
        DOCKERHUB_USERNAME = 'sara3006lab'
        APP_IMAGE = "${DOCKERHUB_USERNAME}/student-management"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {

        stage('📥 Checkout') {
            steps {
                git credentialsId: 'github-credentials',
                    url: 'https://github.com/sara3006-lab/student-management',
                    branch: 'main'
            }
        }

        stage('🔨 Build Backend') {
            steps {
                withEnv([
                    "MAIL_USERNAME=${MAIL_USERNAME}",
                    "MAIL_PASSWORD=${MAIL_PASSWORD}",
                    "JWT_SECRET=${JWT_SECRET}",
                    "DB_URL=jdbc:mariadb://127.0.0.1:3306/studentdb",
                    "DB_USERNAME=root",
                    "DB_PASSWORD=${DB_PASSWORD}"
                ]) {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('🧪 Tests Backend') {
            steps {
                withEnv([
                    "MAIL_USERNAME=${MAIL_USERNAME}",
                    "MAIL_PASSWORD=${MAIL_PASSWORD}",
                    "JWT_SECRET=${JWT_SECRET}",
                    "DB_URL=jdbc:mariadb://127.0.0.1:3306/studentdb",
                    "DB_USERNAME=root",
                    "DB_PASSWORD=${DB_PASSWORD}"
                ]) {
                    sh 'mvn test jacoco:report'
                }
            }
        }

        stage('🔍 SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''mvn sonar:sonar \
                        -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'''
                }
            }
        }

        stage('⏳ Quality Gate') {
            steps {
                script {
                    sleep(time: 15, unit: 'SECONDS')
                    def qg = sh(
                        script: '''curl -s "http://localhost:9000/api/qualitygates/project_status?projectKey=student-management" \
                        -u admin:admin1 | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['projectStatus']['status'])"''',
                        returnStdout: true
                    ).trim()
                    echo "Quality Gate: ${qg}"
                    if (qg == 'ERROR') {
                        error "Quality Gate failed: ${qg}"
                    } else {
                        echo "Quality Gate passed: ${qg}"
                    }
                }
            }
        }

        stage('🔨 Build Frontend') {
            steps {
                dir('student-frontend-react') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }

        stage('🧪 Tests Frontend') {
            steps {
                dir('student-frontend-react') {
                    sh 'npm install'
                    sh 'npm test -- --watchAll=false --passWithNoTests || true'
                }
            }
        }

        stage('🛡️ OWASP Dependency Check') {
            steps {
                dir('student-frontend-react') {
                    sh 'npm audit --json > npm-audit-report.json || true'
                    sh 'cat npm-audit-report.json'
                }
                sh '''mvn org.owasp:dependency-check-maven:check \
                    -DnvdApiKey=53ae4c6f-a483-4aaf-89f3-b50c7d0cf896 \
                    -DfailBuildOnCVSS=9 \
                    -Dformat=HTML \
                    || true'''
            }
        }

        stage('🐳 Docker Build') {
            steps {
                sh """
                    docker build \
                        --build-arg MAIL_USERNAME=${MAIL_USERNAME} \
                        --build-arg MAIL_PASSWORD=${MAIL_PASSWORD} \
                        --build-arg JWT_SECRET=${JWT_SECRET} \
                        --build-arg DB_PASSWORD=${DB_PASSWORD} \
                        -t ${APP_IMAGE}:${IMAGE_TAG} \
                        -t ${APP_IMAGE}:latest \
                        .
                """
                echo "✅ Image construite : ${APP_IMAGE}:${IMAGE_TAG}"
            }
        }

        stage('🔒 Trivy Scan') {
            steps {
                sh """
                    trivy image \
                        --severity HIGH,CRITICAL \
                        --format table \
                        --exit-code 0 \
                        ${APP_IMAGE}:${IMAGE_TAG} \
                        > trivy-report.txt 2>&1 || true
                    cat trivy-report.txt
                """
            }
        }

        stage('📤 Push DockerHub') {
            steps {
                sh "echo ${DOCKERHUB_CREDENTIALS_PSW} | docker login -u ${DOCKERHUB_CREDENTIALS_USR} --password-stdin"
                sh "docker push ${APP_IMAGE}:${IMAGE_TAG}"
                sh "docker push ${APP_IMAGE}:latest"
                echo "✅ Image pushée : ${APP_IMAGE}:${IMAGE_TAG}"
            }
        }

    }

    post {
        success {
            echo '✅ Pipeline réussi !'
        }
        failure {
            echo '❌ Pipeline échoué !'
        }
        always {
            sh 'docker logout || true'
        }
    }
}
