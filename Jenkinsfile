pipeline {
    agent any

    tools {
        maven 'maven3'
        jdk 'java21'
    }

    environment {
        SONAR_PROJECT_KEY    = 'student-management'
        SONAR_TOKEN          = credentials('sonar-token')
        DOCKERHUB_CREDS      = credentials('dockerhub-credentials')
        MAIL_USERNAME        = credentials('MAIL_USERNAME')
        MAIL_PASSWORD        = credentials('MAIL_PASSWORD')
        JWT_SECRET           = credentials('JWT_SECRET')
        DB_PASSWORD          = credentials('DB_PASSWORD')
        DOCKERHUB_USERNAME   = 'sara3006lab'
        APP_IMAGE            = 'sara3006lab/student-management'
        IMAGE_TAG            = "${BUILD_NUMBER}"
    }

    stages {

        // ==========================================
        stage('📥 Checkout') {
        // ==========================================
            steps {
                git credentialsId: 'github-credentials',
                    url: 'https://github.com/sara3006-lab/student-management',
                    branch: 'main'
            }
        }

        // ==========================================
        stage('🔨 Build Backend') {
        // ==========================================
            steps {
                withEnv([
                    "MAIL_USERNAME=${env.MAIL_USERNAME}",
                    "MAIL_PASSWORD=${env.MAIL_PASSWORD}",
                    "JWT_SECRET=${env.JWT_SECRET}",
                    "DB_URL=jdbc:mariadb://127.0.0.1:3306/studentdb",
                    "DB_USERNAME=root",
                    "DB_PASSWORD=${env.DB_PASSWORD}"
                ]) {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        // ==========================================
        stage('🧪 Tests Backend') {
        // ==========================================
            steps {
                withEnv([
                    "MAIL_USERNAME=${env.MAIL_USERNAME}",
                    "MAIL_PASSWORD=${env.MAIL_PASSWORD}",
                    "JWT_SECRET=${env.JWT_SECRET}",
                    "DB_URL=jdbc:mariadb://127.0.0.1:3306/studentdb",
                    "DB_USERNAME=root",
                    "DB_PASSWORD=${env.DB_PASSWORD}"
                ]) {
                    sh 'mvn test jacoco:report'
                }
            }
        }

        // ==========================================
        stage('🔍 SonarQube Analysis') {
        // ==========================================
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.projectKey=student-management \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    '''
                }
            }
        }

        // ==========================================
        stage('⏳ Quality Gate') {
        // ==========================================
            steps {
                script {
                    sleep(time: 15, unit: 'SECONDS')
                    def qg = sh(
                        script: '''
                            curl -s \
                            "http://localhost:9000/api/qualitygates/project_status?projectKey=student-management" \
                            -u admin:admin1 | \
                            python3 -c "import sys,json; \
                            d=json.load(sys.stdin); \
                            print(d['projectStatus']['status'])"
                        ''',
                        returnStdout: true
                    ).trim()
                    echo "Quality Gate Status: ${qg}"
                    if (qg == 'ERROR') {
                        error "Quality Gate failed !"
                    } else {
                        echo "✅ Quality Gate passed: ${qg}"
                    }
                }
            }
        }

        // ==========================================
        stage('🔨 Build Frontend') {
        // ==========================================
            steps {
                dir('student-frontend-react') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }

        // ==========================================
        stage('🧪 Tests Frontend') {
        // ==========================================
            steps {
                dir('student-frontend-react') {
                    sh 'npm test -- --watchAll=false --passWithNoTests || true'
                }
            }
        }

        // ==========================================
        stage('🛡️ OWASP Dependency Check') {
        // ==========================================
            steps {
                dir('student-frontend-react') {
                    sh 'npm audit --json > npm-audit-report.json || true'
                }
                sh '''
                    mvn org.owasp:dependency-check-maven:check \
                        -DnvdApiKey=53ae4c6f-a483-4aaf-89f3-b50c7d0cf896 \
                        -DfailBuildOnCVSS=9 \
                        -Dformat=HTML \
                    || true
                '''
            }
        }

        // ==========================================
        stage('🐳 Docker Build') {
        // ==========================================
            steps {
                script {
                    // Écrire les secrets dans un fichier .env temporaire
                    // pour éviter l'exposition dans les logs
                    writeFile file: '.env.docker', text: """
MAIL_USERNAME=${env.MAIL_USERNAME}
MAIL_PASSWORD=${env.MAIL_PASSWORD}
JWT_SECRET=${env.JWT_SECRET}
DB_PASSWORD=${env.DB_PASSWORD}
DB_URL=jdbc:mariadb://host.docker.internal:3306/studentdb
DB_USERNAME=root
"""
                    sh """
                        docker build \
                            -t ${APP_IMAGE}:${IMAGE_TAG} \
                            -t ${APP_IMAGE}:latest \
                            .
                    """
                    sh 'rm -f .env.docker'
                    echo "✅ Image construite : ${APP_IMAGE}:${IMAGE_TAG}"
                }
            }
        }

        // ==========================================
        stage('🔒 Trivy Scan') {
        // ==========================================
            steps {
                script {
                    sh """
                        trivy image \
                            --severity HIGH,CRITICAL \
                            --format table \
                            --exit-code 0 \
                            --timeout 15m \
                            --no-progress \
                            --cache-dir /tmp/trivy-cache \
                            ${APP_IMAGE}:${IMAGE_TAG} \
                            > trivy-report.txt 2>&1 || true
                    """
                    sh 'cat trivy-report.txt'
                    echo "✅ Trivy scan terminé"
                }
            }
        }

        // ==========================================
        stage('📤 Push DockerHub') {
        // ==========================================
            steps {
                script {
                    // Login sécurisé sans exposer le token
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh '''
                            echo "$DOCKER_PASS" | \
                            docker login -u "$DOCKER_USER" \
                            --password-stdin
                        '''
                    }
                    sh "docker push ${APP_IMAGE}:${IMAGE_TAG}"
                    sh "docker push ${APP_IMAGE}:latest"
                    echo "✅ Images pushées sur DockerHub !"
                    echo "📦 ${APP_IMAGE}:${IMAGE_TAG}"
                    echo "📦 ${APP_IMAGE}:latest"
                }
            }
        }

    }

    post {
        success {
            echo """
            ✅ Pipeline réussi !
            📦 Image : ${APP_IMAGE}:${IMAGE_TAG}
            🔗 https://hub.docker.com/r/${APP_IMAGE}
            """
        }
        failure {
            echo '❌ Pipeline échoué !'
        }
        always {
            sh 'docker logout || true'
            sh 'rm -f .env.docker || true'
        }
    }
}
